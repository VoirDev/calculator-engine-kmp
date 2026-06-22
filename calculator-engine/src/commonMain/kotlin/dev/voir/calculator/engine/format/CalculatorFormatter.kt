package dev.voir.calculator.engine.format

import dev.voir.calculator.engine.CalculatorOperation
import dev.voir.calculator.engine.CalculatorResult
import dev.voir.decimal.Decimal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Formatter entry points for canonical calculator state.
 */
object CalculatorFormatter {
    /**
     * Format a canonical formula using configured separators and operation symbols.
     */
    fun formatFormula(
        formula: String,
        config: CalculatorFormatConfig = CalculatorFormatConfig()
    ): String {
        return renderFormula(parseFormulaForDisplay(formula), config)
    }

    /**
     * Build a formatted formula StateFlow from canonical formula state.
     */
    fun formatFormula(
        formula: StateFlow<String>,
        scope: CoroutineScope,
        config: CalculatorFormatConfig = CalculatorFormatConfig(),
        started: SharingStarted = SharingStarted.WhileSubscribed(5_000)
    ): StateFlow<String> {
        return formula.map { f -> formatFormula(f, config) }
            .stateIn(scope, started, formatFormula(formula.value, config))
    }

    /**
     * Convert user-facing formula text back to the calculator's canonical formula alphabet.
     */
    fun normalizeFormula(
        string: String,
        config: CalculatorFormatConfig = CalculatorFormatConfig()
    ): String {
        val ops = config.operationSymbols
        val sb = StringBuilder()
        for (c in string) when {
            c.isWhitespace() -> {}
            c == config.groupingSeparator -> {}
            c == config.decimalSeparator -> sb.append('.')
            c == ops.add -> sb.append('+')
            c == ops.subtract -> sb.append('-')
            c == ops.multiply -> sb.append('*')
            c == ops.divide -> sb.append('/')
            else -> sb.append(c)
        }
        return sb.toString()
    }

    /**
     * Format a calculator result. Errors become [CalculatorFormatConfig.errorLabel].
     */
    fun formatResult(
        result: CalculatorResult,
        config: CalculatorFormatConfig = CalculatorFormatConfig()
    ): String {
        return when (result) {
            is CalculatorResult.Success -> result.value.toConfiguredFormattedString(config)
            is CalculatorResult.Error -> config.errorLabel
        }
    }

    /**
     * Build a formatted result StateFlow from calculator result state.
     */
    fun formatResult(
        result: StateFlow<CalculatorResult>,
        scope: CoroutineScope,
        config: CalculatorFormatConfig = CalculatorFormatConfig(),
        started: SharingStarted = SharingStarted.WhileSubscribed(5_000)
    ): StateFlow<String> {
        return result.map { current -> formatResult(current, config) }
            .stateIn(scope, started, formatResult(result.value, config))
    }
}

private sealed interface FormatToken {
    data class Num(val text: String) : FormatToken
    data class Op(val op: CalculatorOperation) : FormatToken
}

private fun renderFormula(tokens: List<FormatToken>, config: CalculatorFormatConfig): String =
    buildString {
        for (token in tokens) when (token) {
            is FormatToken.Num -> append(formatNumberText(token.text, config))
            is FormatToken.Op -> append(' ').append(config.operationSymbols.forOp(token.op))
                .append(' ')
        }
    }

private fun formatNumberText(text: String, config: CalculatorFormatConfig): String {
    if (text == "-") return "-"
    if (canUseDecimalFormatterForNumberText(text)) {
        try {
            return Decimal.of(text).toConfiguredFormattedString(config)
        } catch (_: IllegalArgumentException) {
            // Fall back to edit-preserving formatting for raw text Decimal will not parse.
        }
    }

    val negative = text.startsWith('-')
    val abs = if (negative) text.substring(1) else text
    val dotIdx = abs.indexOf('.')
    val intPart = if (dotIdx >= 0) abs.substring(0, dotIdx) else abs
    val fracPart = if (dotIdx >= 0) abs.substring(dotIdx + 1) else null

    return buildString {
        if (negative) append('-')
        append(intPart.ifEmpty { "0" })
        if (fracPart != null) {
            append(config.decimalSeparator).append(fracPart)
        }
    }
}

private fun canUseDecimalFormatterForNumberText(text: String): Boolean {
    if (text.endsWith('.')) return false
    val fraction = text.substringAfter('.', missingDelimiterValue = "")
    return fraction.isEmpty() || !fraction.endsWith('0')
}

private fun Decimal.toConfiguredFormattedString(config: CalculatorFormatConfig): String {
    return toFormattedString(
        decimalSeparator = config.decimalSeparator,
        groupingSeparator = config.groupingSeparator,
        minimumFractionDigits = config.minimumFractionDigits,
        maximumFractionDigits = config.maxDecimals,
        rounding = config.rounding,
    )
}

private fun parseFormulaForDisplay(formula: String): List<FormatToken> {
    val cleaned = formula.filter { !it.isWhitespace() }
    val result = mutableListOf<FormatToken>()
    var i = 0
    while (i < cleaned.length) {
        val c = cleaned[i]
        when {
            c.isDigit() || c == '.' ||
                    (c == '-' && (result.isEmpty() || result.last() is FormatToken.Op)) -> {
                val start = i
                if (c == '-') i++
                var hasDot = false
                while (i < cleaned.length) {
                    val ch = cleaned[i]
                    when {
                        ch.isDigit() -> i++
                        ch == '.' && !hasDot -> {
                            hasDot = true
                            i++
                        }

                        else -> break
                    }
                }
                result.add(FormatToken.Num(cleaned.substring(start, i)))
            }

            c == '+' || c == '-' || c == '*' || c == '/' -> {
                result.add(FormatToken.Op(CalculatorOperation.fromSymbol(c)))
                i++
            }

            else -> throw IllegalArgumentException("Invalid character '$c' at position $i")
        }
    }
    return result
}
