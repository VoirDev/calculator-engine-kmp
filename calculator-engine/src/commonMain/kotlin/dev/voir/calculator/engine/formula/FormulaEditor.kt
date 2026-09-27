package dev.voir.calculator.engine.formula

import dev.voir.calculator.engine.CalculatorConfig
import dev.voir.calculator.engine.CalculatorOperation
import dev.voir.calculator.engine.InputRejection
import dev.voir.decimal.Decimal

/**
 * Outcome of one key edit.
 *
 * An [Applied] edit may still produce the same tokens, for example a second leading zero; the
 * caller reports that as [InputRejection.NoChange].
 */
internal sealed interface FormulaEdit {
    /**
     * The key was applied.
     *
     * @property tokens Tokens after the edit.
     */
    data class Applied(val tokens: List<FormulaToken>) : FormulaEdit

    /**
     * The key would make the formula invalid.
     *
     * @property reason Why the key was rejected.
     */
    data class Rejected(val reason: InputRejection) : FormulaEdit
}

/**
 * Calculator-key edits applied to a token list.
 *
 * Every function is pure. Key edits only change the end of the list, so every number before the
 * last one stays complete.
 */
internal object FormulaEditor {
    /**
     * Append one digit to the current number, or start a new number after an operation.
     *
     * A lone leading zero is replaced instead of producing text such as `007`, and an evaluated
     * result is replaced by the new number.
     *
     * @param tokens Current tokens.
     * @param digit Digit in `'0'..'9'`.
     * @param config Supplies the fractional and total digit limits.
     * @return Edited tokens, or the limit that prevents another digit.
     */
    fun appendDigit(tokens: List<FormulaToken>, digit: Char, config: CalculatorConfig): FormulaEdit {
        val last = tokens.lastOrNull()
        if (last !is FormulaToken.Num || last.isResult) {
            val start = if (last is FormulaToken.Num) tokens.dropLast(1) else tokens
            return FormulaEdit.Applied(start + FormulaToken.Num(digit.toString()))
        }
        val text = when (last.text) {
            "", "0" -> digit.toString()
            "-", "-0" -> "-$digit"
            else -> last.text + digit
        }
        return when {
            text.fractionDigits() > config.inputMaxDecimals -> FormulaEdit.Rejected(InputRejection.DecimalLimitReached)
            text.count { it.isAsciiDigit() } > config.inputMaxDigits -> FormulaEdit.Rejected(InputRejection.DigitLimitReached)
            else -> FormulaEdit.Applied(tokens.replaceLast(FormulaToken.Num(text)))
        }
    }

    /**
     * Append a decimal separator to the current number, or start `0.` after an operation or in
     * place of an evaluated result.
     *
     * @param tokens Current tokens.
     * @param config Supplies the fractional digit limit.
     * @return Edited tokens, or why a separator is not allowed.
     */
    fun appendDecimalSeparator(tokens: List<FormulaToken>, config: CalculatorConfig): FormulaEdit {
        if (config.inputMaxDecimals == 0) return FormulaEdit.Rejected(InputRejection.DecimalsDisabled)
        val last = tokens.lastOrNull()
        if (last !is FormulaToken.Num || last.isResult) {
            val start = if (last is FormulaToken.Num) tokens.dropLast(1) else tokens
            return FormulaEdit.Applied(start + FormulaToken.Num("0."))
        }
        if ('.' in last.text) return FormulaEdit.Rejected(InputRejection.DuplicateDecimalSeparator)
        val text = when (last.text) {
            // A bare sign becomes a negative zero-prefixed decimal.
            "-" -> "-0."
            "" -> "0."
            else -> last.text + "."
        }
        return FormulaEdit.Applied(tokens.replaceLast(FormulaToken.Num(text)))
    }

    /**
     * Append an operation, or replace the operation the formula already ends with.
     *
     * [CalculatorOperation.Subtract] starts a negative number in an empty formula and after `*` or
     * `/`, so `5*-3` can be typed. After `+` or `-` it replaces the operation instead. Any other
     * operation typed right after such a sign removes the sign and replaces the operation before it
     * (`5*-` then `+` gives `5+`). A trailing decimal point on the previous number is dropped
     * (`3.` becomes `3`).
     *
     * @param tokens Current tokens.
     * @param operation Operation to append.
     * @return Edited tokens, or [InputRejection.MissingOperand] when there is no complete number to
     * apply the operation to.
     */
    fun appendOperation(tokens: List<FormulaToken>, operation: CalculatorOperation): FormulaEdit {
        val edited = when (val last = tokens.lastOrNull()) {
            null -> if (operation == CalculatorOperation.Subtract) listOf(FormulaToken.Num("-")) else null

            is FormulaToken.Op -> when {
                operation == CalculatorOperation.Subtract && last.operation.isMultiplicative() ->
                    tokens + FormulaToken.Num("-")

                else -> tokens.replaceLast(FormulaToken.Op(operation))
            }

            is FormulaToken.Num -> when {
                last.isComplete -> tokens.replaceLast(last.copy(text = last.text.removeSuffix("."))) +
                    FormulaToken.Op(operation)

                // A sign after an operation: a second sign is rejected, anything else replaces it.
                last.text == "-" && tokens.size > 1 && operation != CalculatorOperation.Subtract ->
                    tokens.dropLast(2) + FormulaToken.Op(operation)

                // An incomplete number is not a left operand yet.
                else -> null
            }
        }
        return if (edited == null) FormulaEdit.Rejected(InputRejection.MissingOperand) else FormulaEdit.Applied(edited)
    }

    /**
     * Remove the last character of the formula.
     *
     * An operation is removed as a whole; a number loses its last character and disappears once
     * empty. An evaluated result becomes ordinary typed text once it is edited.
     *
     * @param tokens Current tokens.
     * @return Edited tokens, or [InputRejection.NothingToRemove] when [tokens] is empty.
     */
    fun deleteLast(tokens: List<FormulaToken>): FormulaEdit {
        val edited = when (val last = tokens.lastOrNull()) {
            null -> return FormulaEdit.Rejected(InputRejection.NothingToRemove)
            is FormulaToken.Op -> tokens.dropLast(1)
            is FormulaToken.Num -> {
                val text = last.text.dropLast(1)
                if (text.isEmpty()) tokens.dropLast(1) else tokens.replaceLast(FormulaToken.Num(text))
            }
        }
        return FormulaEdit.Applied(edited)
    }

    /**
     * Round every number to the input precision of a new configuration.
     *
     * Typed numbers with more than [CalculatorConfig.inputMaxDecimals] fractional digits are
     * rounded with [CalculatorConfig.rounding]; with zero decimals a trailing decimal point is
     * removed. Evaluated results are re-rounded from their exact value, which they keep. Incomplete
     * numbers such as `-` are left alone. The digit limit is not applied to existing numbers.
     *
     * @param tokens Current tokens.
     * @param config New configuration.
     * @return Tokens that respect the new input precision.
     */
    fun applyPrecision(tokens: List<FormulaToken>, config: CalculatorConfig): List<FormulaToken> {
        val maxDecimals = config.inputMaxDecimals
        return tokens.map { token ->
            when {
                token !is FormulaToken.Num -> token
                token.exactValue != null ->
                    token.copy(text = token.exactValue.setScale(maxDecimals, config.rounding).toPlainString())

                !token.isComplete -> token
                token.text.fractionDigits() > maxDecimals -> {
                    val value = Decimal.parse(token.text.removeSuffix("."))
                    FormulaToken.Num(value.setScale(maxDecimals, config.rounding).toPlainString())
                }

                maxDecimals == 0 -> FormulaToken.Num(token.text.removeSuffix("."))
                else -> token
            }
        }
    }

    /**
     * Replace the last token.
     *
     * @param token Replacement token.
     * @return Copy of this list with [token] in the last position.
     */
    private fun List<FormulaToken>.replaceLast(token: FormulaToken): List<FormulaToken> =
        dropLast(1) + token

    /**
     * Count the digits after the decimal point.
     *
     * @return Fractional digit count, or zero without a decimal point.
     */
    private fun String.fractionDigits(): Int = if ('.' in this) substringAfter('.').length else 0

    /**
     * Check whether this operation binds tighter than addition and subtraction.
     *
     * @return `true` for multiplication and division.
     */
    private fun CalculatorOperation.isMultiplicative(): Boolean =
        this == CalculatorOperation.Multiply || this == CalculatorOperation.Divide
}
