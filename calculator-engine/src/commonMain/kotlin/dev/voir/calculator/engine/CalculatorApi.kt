package dev.voir.calculator.engine

import kotlinx.coroutines.flow.StateFlow

/**
 * Public contract for a reactive expression evaluator.
 *
 * Implementations expose every observable value as a [StateFlow] so UI code can render formulas
 * and results immediately after every accepted input action. Implementations are not thread-safe;
 * drive one instance from a single thread, typically the UI thread.
 */
public interface CalculatorApi {

    /**
     * Formula, result, and config published together after every change.
     *
     * Prefer this over combining [formula], [result], and [config], which are updated one after the
     * other and can briefly disagree.
     */
    public val state: StateFlow<CalculatorState>

    /** Current evaluator configuration; the same value as `state.value.config`. */
    public val config: StateFlow<CalculatorConfig>

    /** Formula using canonical symbols: ASCII digits, `.`, `+`, `-`, `*`, `/`. */
    public val formula: StateFlow<String>

    /** Current calculated result, [CalculatorResult.Empty], or calculation error for the formula. */
    public val result: StateFlow<CalculatorResult>

    /**
     * Replace the current configuration and re-evaluate the formula.
     *
     * Numbers already in the formula are rounded to the new [CalculatorConfig.inputMaxDecimals].
     *
     * @param newConfig Complete configuration to use from this point forward.
     */
    public fun setConfig(newConfig: CalculatorConfig)

    /**
     * Update the current configuration using a transform function.
     *
     * @param transform Function that receives the current config and returns the replacement config.
     */
    public fun updateConfig(transform: (CalculatorConfig) -> CalculatorConfig)

    /**
     * Append a single decimal digit.
     *
     * Right after [evaluate] the digit starts a new number instead of appending to the result.
     *
     * @param digit Digit character in `'0'..'9'`.
     * @return [InputResult.Accepted], or the reason the digit was ignored.
     * @throws IllegalArgumentException If [digit] is not in `'0'..'9'`.
     */
    public fun inputDigit(digit: Char): InputResult

    /**
     * Append a decimal separator to the current number.
     *
     * After an operation, or right after [evaluate], this starts a new number `0.`.
     *
     * @return [InputResult.Accepted], or the reason the separator was ignored.
     */
    public fun inputDecimalSeparator(): InputResult

    /**
     * Append or replace a binary operation.
     *
     * At the start of the formula, and after `*` or `/`, subtract starts a negative number. After
     * any other operation the new operation replaces it.
     *
     * @param operation Operation to append.
     * @return [InputResult.Accepted], or the reason the operation was ignored.
     */
    public fun inputOperation(operation: CalculatorOperation): InputResult

    /**
     * Delete the last entered character or operation.
     *
     * @return [InputResult.Accepted], or [InputRejection.NothingToRemove] for an empty formula.
     */
    public fun deleteLast(): InputResult

    /**
     * Clear the current formula.
     *
     * @return [InputResult.Accepted], or [InputRejection.NothingToRemove] for an empty formula.
     */
    public fun clear(): InputResult

    /**
     * Evaluate the current formula.
     *
     * On success, replaces the formula with the result rounded to
     * [CalculatorConfig.inputMaxDecimals]; the next operation continues from the exact value. An
     * error or [CalculatorResult.Empty] leaves the formula unchanged.
     *
     * @return The same value [result] holds afterwards.
     */
    public fun evaluate(): CalculatorResult

    /**
     * Replace the current formula from canonical text.
     *
     * Use this for trusted text, such as a formula saved from [formula]; use [trySetFormula] for
     * user input.
     *
     * @param formula Formula using ASCII digits, `.`, `+`, `-`, `*`, `/`, and optional whitespace.
     * @throws FormulaSyntaxException When [formula] cannot be parsed.
     */
    public fun setFormula(formula: String)

    /**
     * Try to replace the current formula from canonical text.
     *
     * @param formula Formula using ASCII digits, `.`, `+`, `-`, `*`, `/`, and optional whitespace.
     * @return Whether the formula changed, stayed the same, or was invalid.
     */
    public fun trySetFormula(formula: String): FormulaUpdate
}
