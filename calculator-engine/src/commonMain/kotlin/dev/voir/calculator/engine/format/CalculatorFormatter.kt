package dev.voir.calculator.engine.format

import dev.voir.calculator.engine.CalculatorError
import dev.voir.calculator.engine.CalculatorResult
import dev.voir.calculator.engine.CalculatorState
import dev.voir.calculator.engine.FormulaSyntaxException
import dev.voir.calculator.engine.formula.FormulaParser
import dev.voir.calculator.engine.formula.FormulaToken
import dev.voir.decimal.Decimal

/**
 * Converts canonical calculator state to display text and back.
 *
 * Every function is pure and safe to call from any thread.
 */
public object CalculatorFormatter {
    /**
     * Format a canonical formula using configured separators, sign, and operation symbols.
     *
     * Numbers are shown exactly as typed, including trailing zeros and a trailing decimal
     * separator, so the display never changes a digit the user entered. Operations are surrounded
     * by single spaces.
     *
     * @param formula Canonical formula text, usually [dev.voir.calculator.engine.CalculatorApi.formula].
     * @param config Display settings.
     * @return Formatted formula text.
     * @throws FormulaSyntaxException When [formula] contains an unsupported character.
     */
    public fun formatFormula(
        formula: String,
        config: CalculatorFormatConfig = CalculatorFormatConfig(),
    ): String = buildString {
        for (token in FormulaParser.tokenize(formula)) when (token) {
            is FormulaToken.Num -> append(formatTypedNumber(token.text, config))
            is FormulaToken.Op -> append(' ').append(config.operationSymbols.symbolFor(token.operation)).append(' ')
        }
    }

    /**
     * Format a calculator result.
     *
     * Values are rounded to [CalculatorFormatConfig.maximumFractionDigits] and padded to
     * [CalculatorFormatConfig.minimumFractionDigits].
     *
     * @param result Result to format.
     * @param config Display settings.
     * @param errorLabel Text for each calculation error; defaults to
     * [CalculatorFormatConfig.errorLabel] for all of them.
     * @return Formatted value, [CalculatorFormatConfig.emptyLabel] (or zero) for an empty result, or
     * the error label.
     */
    public fun formatResult(
        result: CalculatorResult,
        config: CalculatorFormatConfig = CalculatorFormatConfig(),
        errorLabel: (CalculatorError) -> String = { config.errorLabel },
    ): String = when (result) {
        is CalculatorResult.Empty -> config.emptyLabel ?: formatValue(Decimal.zero(), config)
        is CalculatorResult.Success -> formatValue(result.value, config)
        is CalculatorResult.Error -> errorLabel(result.error)
    }

    /**
     * Format a whole calculator snapshot.
     *
     * @param state Snapshot to format, usually from [dev.voir.calculator.engine.CalculatorApi.state].
     * @param config Display settings.
     * @param errorLabel Text for each calculation error; defaults to
     * [CalculatorFormatConfig.errorLabel] for all of them.
     * @return Formatted formula and result from the same snapshot.
     */
    public fun format(
        state: CalculatorState,
        config: CalculatorFormatConfig = CalculatorFormatConfig(),
        errorLabel: (CalculatorError) -> String = { config.errorLabel },
    ): CalculatorDisplay = CalculatorDisplay(
        formula = formatFormula(state.formula, config),
        result = formatResult(state.result, config, errorLabel),
        isShowingResult = state.isShowingResult,
    )

    /**
     * Convert user-facing formula text back to the calculator's canonical formula alphabet.
     *
     * Whitespace and grouping separators are dropped; the configured separator, sign, and
     * operation symbols are mapped to canonical characters. Unknown characters are kept so the
     * parser can reject them with a precise position.
     *
     * @param text Formatted formula text, for example pasted by the user.
     * @param config Display settings used to produce [text].
     * @return Canonical formula text suitable for [dev.voir.calculator.engine.CalculatorApi.trySetFormula].
     */
    public fun normalizeFormula(
        text: String,
        config: CalculatorFormatConfig = CalculatorFormatConfig(),
    ): String {
        val symbols = config.operationSymbols
        return buildString {
            for (char in text) when (char) {
                config.groupingSeparator -> {}
                config.decimalSeparator -> append('.')
                // Checked before operations: when the sign equals the subtract symbol both map to '-'.
                config.negativeSign -> append('-')
                symbols.add -> append('+')
                symbols.subtract -> append('-')
                symbols.multiply -> append('*')
                symbols.divide -> append('/')
                else -> if (!char.isWhitespace()) append(char)
            }
        }
    }

    /**
     * Format typed number text without rounding or padding it.
     *
     * @param text Raw number text such as `1234.50`, `-`, or `0.`.
     * @param config Display settings.
     * @return Text with the configured sign, grouping, and decimal separator.
     */
    private fun formatTypedNumber(text: String, config: CalculatorFormatConfig): String {
        val isNegative = text.startsWith('-')
        val unsigned = text.removePrefix("-")
        val integerPart = unsigned.substringBefore('.')
        return buildString {
            if (isNegative) append(config.negativeSign)
            // A bare sign is still being typed; show it without an integer part.
            if (unsigned.isEmpty()) return@buildString
            append(group(integerPart.ifEmpty { "0" }, config.groupingSeparator))
            if ('.' in unsigned) append(config.decimalSeparator).append(unsigned.substringAfter('.'))
        }
    }

    /**
     * Format a result value with rounding, padding, separators, and sign.
     *
     * @param value Value to format.
     * @param config Display settings.
     * @return Formatted value.
     */
    private fun formatValue(value: Decimal, config: CalculatorFormatConfig): String {
        val formatted = value.toFormattedString(
            maximumFractionDigits = config.maximumFractionDigits,
            minimumFractionDigits = config.minimumFractionDigits,
            rounding = config.rounding,
            decimalSeparator = config.decimalSeparator,
            groupingSeparator = config.groupingSeparator,
        )
        return if (formatted.startsWith('-')) config.negativeSign + formatted.substring(1) else formatted
    }

    /**
     * Insert [separator] every three digits from the right.
     *
     * @param digits Integer digits without a sign.
     * @param separator Grouping separator, or `null` to leave [digits] unchanged.
     * @return Grouped digits.
     */
    private fun group(digits: String, separator: Char?): String {
        if (separator == null || digits.length <= 3) return digits
        val firstGroup = digits.length % 3
        return buildString {
            digits.forEachIndexed { index, digit ->
                if (index > 0 && (index - firstGroup) % 3 == 0) append(separator)
                append(digit)
            }
        }
    }
}
