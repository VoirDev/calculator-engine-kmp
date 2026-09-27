package dev.voir.calculator.engine

import dev.voir.calculator.engine.evaluation.FormulaEvaluator
import dev.voir.calculator.engine.formula.FormulaEdit
import dev.voir.calculator.engine.formula.FormulaEditor
import dev.voir.calculator.engine.formula.FormulaParser
import dev.voir.calculator.engine.formula.FormulaToken
import dev.voir.calculator.engine.formula.toFormulaText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A reactive expression-evaluator.
 *
 * The class enforces a valid math formula at every input step:
 * - You cannot insert two decimal separators into one number.
 * - You cannot start with a binary operator (only `-` is accepted as a sign).
 * - Pressing an operator twice replaces the previous one (calc-style), except that `-` after `*`
 *   or `/` starts a negative number.
 * - Numbers are capped at [CalculatorConfig.inputMaxDecimals] fractional digits and
 *   [CalculatorConfig.inputMaxDigits] digits.
 *
 * After [evaluate], the formula shows the result rounded to the input precision while the next
 * operation continues from the exact value. Typing a digit or decimal separator then starts a new
 * number instead of appending to the result.
 *
 * The class is not thread-safe; drive it from a single thread, typically the UI thread.
 *
 * @param initialConfig Configuration used for the initial state and all input,
 * parsing, and evaluation until [setConfig] or [updateConfig] is called.
 * @param logger Receives diagnostic messages; formula text appears only at
 * [CalculatorLogLevel.Debug]. Discards everything by default.
 */
public class Calculator(
    initialConfig: CalculatorConfig = CalculatorConfig(),
    private val logger: CalculatorLogger = CalculatorLogger.None,
) : CalculatorApi {

    /** Current expression tokens; the single source of truth for everything published. */
    private var tokens: List<FormulaToken> = emptyList()

    /** Mutable backing snapshot; exposed through [state]. */
    private val _state = MutableStateFlow(CalculatorState(config = initialConfig))
    override val state: StateFlow<CalculatorState> = _state.asStateFlow()

    /** Mutable backing config; mirrors `state.config`. */
    private val _config = MutableStateFlow(initialConfig)
    override val config: StateFlow<CalculatorConfig> = _config.asStateFlow()

    /** Mutable backing formula; mirrors `state.formula`. */
    private val _formula = MutableStateFlow("")
    override val formula: StateFlow<String> = _formula.asStateFlow()

    /** Mutable backing result; mirrors `state.result`. */
    private val _result = MutableStateFlow<CalculatorResult>(CalculatorResult.Empty)
    override val result: StateFlow<CalculatorResult> = _result.asStateFlow()

    /**
     * Replace the calculator configuration and re-emit all derived state.
     *
     * Numbers already in the formula are rounded to the new input precision.
     *
     * @param newConfig Complete configuration to use for later input, parsing, and evaluation.
     */
    override fun setConfig(newConfig: CalculatorConfig) {
        // Avoid unnecessary re-evaluation when the config is unchanged.
        val previous = _config.value
        if (previous == newConfig) return
        val rounded = FormulaEditor.applyPrecision(tokens, newConfig)
        val previousFormula = _formula.value
        publish(rounded, newConfig)
        logger.log(CalculatorLogLevel.Info) { "Config changed from $previous to $newConfig" }
        if (_formula.value != previousFormula) {
            logger.log(CalculatorLogLevel.Debug) {
                "Formula rounded to ${newConfig.inputMaxDecimals} decimals: '$previousFormula' -> '${_formula.value}'"
            }
        }
    }

    /**
     * Apply a caller-provided config transform and publish the result.
     *
     * @param transform Function that maps the current config to a new config.
     */
    override fun updateConfig(transform: (CalculatorConfig) -> CalculatorConfig) {
        setConfig(transform(_config.value))
    }

    /**
     * Append a single decimal digit.
     *
     * @param digit Digit character in `'0'..'9'`.
     * @return [InputResult.Accepted], or the reason the digit was ignored.
     */
    override fun inputDigit(digit: Char): InputResult {
        // Fail fast for programmer errors; UI callers should only pass one digit.
        require(digit in '0'..'9') { "Not a digit: '$digit'" }
        return applyEdit(FormulaEditor.appendDigit(tokens, digit, _config.value)).logged { "inputDigit('$digit')" }
    }

    /**
     * Append a decimal separator to the current editable number.
     *
     * @return [InputResult.Accepted], or the reason the separator was ignored.
     */
    override fun inputDecimalSeparator(): InputResult =
        applyEdit(FormulaEditor.appendDecimalSeparator(tokens, _config.value)).logged { "inputDecimalSeparator()" }

    /**
     * Append (or replace) a binary operation.
     *
     * @param operation Operation to append.
     * @return [InputResult.Accepted], or the reason the operation was ignored.
     */
    override fun inputOperation(operation: CalculatorOperation): InputResult =
        applyEdit(FormulaEditor.appendOperation(tokens, operation)).logged { "inputOperation($operation)" }

    /**
     * Backspace one character.
     *
     * @return [InputResult.Accepted], or [InputRejection.NothingToRemove] for an empty formula.
     */
    override fun deleteLast(): InputResult = applyEdit(FormulaEditor.deleteLast(tokens)).logged { "deleteLast()" }

    /**
     * Reset the formula to empty input.
     *
     * @return [InputResult.Accepted], or [InputRejection.NothingToRemove] for an empty formula.
     */
    override fun clear(): InputResult {
        val result = if (tokens.isEmpty()) {
            InputResult.Rejected(InputRejection.NothingToRemove)
        } else {
            applyEdit(FormulaEdit.Applied(emptyList()))
        }
        return result.logged { "clear()" }
    }

    /**
     * Evaluate the current expression. The formula is replaced by the
     * resulting number so the next operation continues from the result.
     *
     * Evaluation failures and [CalculatorResult.Empty] leave the formula unchanged.
     *
     * @return Evaluated Decimal result, [CalculatorResult.Empty], or calculation error; the same
     * value [result] holds.
     */
    override fun evaluate(): CalculatorResult {
        val config = _config.value
        val formulaBefore = _formula.value
        val evaluation = FormulaEvaluator.evaluate(tokens, config)
        logger.log(CalculatorLogLevel.Debug) { "evaluate(): '$formulaBefore' -> $evaluation" }
        if (evaluation is CalculatorResult.Error && evaluation.error !is CalculatorError.DivisionByZero) {
            logger.log(CalculatorLogLevel.Info) { "evaluate() failed: ${evaluation.error.describe()}" }
        }
        if (evaluation is CalculatorResult.Success) {
            // Show the result at input precision, but keep the exact value for the next operation.
            val shown = evaluation.value.setScale(config.inputMaxDecimals, config.rounding)
            publish(listOf(FormulaToken.Num(shown.toPlainString(), exactValue = evaluation.value)), config)
        }
        return evaluation
    }

    /**
     * Replace the formula from raw text.
     *
     * @param formula Formula text using ASCII digits, `.`, `+`, `-`, `*`, `/`, and whitespace.
     * @throws FormulaSyntaxException When [formula] cannot be parsed.
     */
    override fun setFormula(formula: String) {
        val parsed = try {
            FormulaParser.parse(formula, _config.value.inputMaxDecimals)
        } catch (exception: FormulaSyntaxException) {
            val error = exception.error
            logger.log(CalculatorLogLevel.Warning) { "setFormula() rejected text: ${error.kind} at position ${error.position}" }
            logger.log(CalculatorLogLevel.Debug) { "setFormula() rejected '$formula': ${error.message}" }
            throw exception
        }
        publish(parsed, _config.value)
        logger.log(CalculatorLogLevel.Debug) { "setFormula(): formula '${_formula.value}'" }
    }

    /**
     * Try to replace the formula from raw text.
     *
     * @param formula Formula text using ASCII digits, `.`, `+`, `-`, `*`, `/`, and whitespace.
     * @return Whether the formula changed, stayed the same, or was invalid.
     */
    override fun trySetFormula(formula: String): FormulaUpdate {
        val update = try {
            val parsed = FormulaParser.parse(formula, _config.value.inputMaxDecimals)
            // Compare text so re-setting a shown result is not reported as a change.
            if (parsed.toFormulaText() == tokens.toFormulaText()) {
                FormulaUpdate.Unchanged
            } else {
                publish(parsed, _config.value)
                FormulaUpdate.Changed
            }
        } catch (exception: FormulaSyntaxException) {
            FormulaUpdate.Invalid(exception.error)
        }
        logger.log(CalculatorLogLevel.Debug) { "trySetFormula('$formula') -> $update; formula '${_formula.value}'" }
        return update
    }

    /**
     * Log this key result at [CalculatorLogLevel.Debug], with the formula after the key.
     *
     * @param key Describes the key, for example `inputDigit('7')`; only called when logging.
     * @return This result, unchanged.
     */
    private inline fun InputResult.logged(crossinline key: () -> String): InputResult {
        val result = this
        logger.log(CalculatorLogLevel.Debug) {
            when (result) {
                is InputResult.Accepted -> "${key()} accepted; formula '${_formula.value}'"
                is InputResult.Rejected -> "${key()} rejected (${result.reason}); formula '${_formula.value}'"
            }
        }
        return result
    }

    /**
     * Describe this error without the number text it may carry.
     *
     * @return Error kind, plus the operation for arithmetic failures.
     */
    private fun CalculatorError.describe(): String = when (this) {
        CalculatorError.DivisionByZero -> "DivisionByZero"
        is CalculatorError.InvalidNumber -> "InvalidNumber"
        is CalculatorError.ArithmeticFailure -> "ArithmeticFailure($operation)"
    }

    /**
     * Publish an edit when it changes the tokens.
     *
     * @param edit Outcome of a key edit.
     * @return [InputResult.Accepted] when tokens changed, otherwise the rejection reason.
     */
    private fun applyEdit(edit: FormulaEdit): InputResult = when (edit) {
        is FormulaEdit.Rejected -> InputResult.Rejected(edit.reason)
        is FormulaEdit.Applied -> if (edit.tokens == tokens) {
            InputResult.Rejected(InputRejection.NoChange)
        } else {
            publish(edit.tokens, _config.value)
            InputResult.Accepted
        }
    }

    /**
     * Store [newTokens] and [newConfig], then publish a snapshot derived from them.
     *
     * [state] is updated first so it never lags behind the individual flows.
     *
     * @param newTokens Tokens to store.
     * @param newConfig Configuration to store and evaluate with.
     */
    private fun publish(newTokens: List<FormulaToken>, newConfig: CalculatorConfig) {
        tokens = newTokens
        val snapshot = CalculatorState(
            formula = newTokens.toFormulaText(),
            result = FormulaEvaluator.evaluate(newTokens, newConfig),
            config = newConfig,
            isShowingResult = newTokens.singleOrNull().let { it is FormulaToken.Num && it.isResult },
        )
        _state.value = snapshot
        _config.value = snapshot.config
        _formula.value = snapshot.formula
        _result.value = snapshot.result
    }
}
