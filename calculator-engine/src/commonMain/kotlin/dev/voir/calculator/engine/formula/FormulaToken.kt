package dev.voir.calculator.engine.formula

import dev.voir.calculator.engine.CalculatorOperation
import dev.voir.decimal.Decimal

/**
 * One element of a canonical formula.
 *
 * A well-formed token list starts with a [Num] and alternates [Num] and [Op]; it may end with an
 * [Op] while the user is still typing. Numbers keep their edit text so in-progress input such as
 * `0.`, `-`, or `1.20` survives until evaluation decides how to read it.
 */
internal sealed interface FormulaToken {
    /**
     * Number text in canonical form.
     *
     * @property text Raw number text such as `12`, `-3.4`, `-`, or `0.`.
     * @property exactValue Unrounded value when this number is an evaluated result; [text] then
     * holds that value rounded to the input precision. `null` for typed numbers. Typing a digit or
     * decimal separator replaces a result instead of appending to it.
     */
    data class Num(val text: String, val exactValue: Decimal? = null) : FormulaToken {
        /** Whether this number came from evaluation rather than typing. */
        val isResult: Boolean get() = exactValue != null

        /**
         * Whether this number has a value; `false` for edit states without digits such as `-`,
         * `.`, or `-.`.
         */
        val isComplete: Boolean get() = isResult || text.any { it.isAsciiDigit() }
    }

    /**
     * Binary operation between two numbers.
     *
     * @property operation Operation represented by this token.
     */
    data class Op(val operation: CalculatorOperation) : FormulaToken
}

/**
 * Check whether this character is a canonical formula digit.
 *
 * Unlike [Char.isDigit], only `'0'..'9'` qualify; other Unicode digits cannot be read by `Decimal`.
 *
 * @return `true` for `'0'..'9'`.
 */
internal fun Char.isAsciiDigit(): Boolean = this in '0'..'9'

/**
 * Render tokens as canonical formula text.
 *
 * @return Formula using digits, `.`, `+`, `-`, `*`, and `/`, with no whitespace.
 */
internal fun List<FormulaToken>.toFormulaText(): String = buildString {
    for (token in this@toFormulaText) when (token) {
        is FormulaToken.Num -> append(token.text)
        is FormulaToken.Op -> append(token.operation.symbol)
    }
}
