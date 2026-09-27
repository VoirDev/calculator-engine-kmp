package dev.voir.calculator.engine.formula

import dev.voir.calculator.engine.CalculatorOperation
import dev.voir.calculator.engine.FormulaError
import dev.voir.calculator.engine.FormulaErrorKind
import dev.voir.calculator.engine.FormulaSyntaxException

/**
 * Converts canonical formula text into [FormulaToken]s.
 *
 * Canonical text uses the ASCII digits `0`–`9`, `.`, `+`, `-`, `*`, and `/`; whitespace is ignored
 * anywhere, even inside a number. A `-` starts a negative number at the beginning of the formula or
 * right after an operation, and is a subtraction everywhere else. Reported positions are indexes
 * into the original text, whitespace included.
 */
internal object FormulaParser {
    /**
     * Parse a complete formula and validate its shape.
     *
     * Every number must contain a digit and at most [maxDecimals] fractional digits, and every
     * operation must follow a number. A trailing operation is allowed. Numbers are stored in the
     * form typing would produce: leading zeros are removed (`007` becomes `7`), a leading decimal
     * point gets a zero (`.5` becomes `0.5`), and a trailing decimal point before an operation is
     * dropped (`1.+2` becomes `1+2`). Trailing fractional zeros and a final trailing decimal point
     * are kept as in-progress input.
     *
     * @param formula Canonical formula text.
     * @param maxDecimals Maximum fractional digits allowed in each number.
     * @return Parsed tokens.
     * @throws FormulaSyntaxException For the first unsupported character, misplaced number or
     * operation, number without digits, or number with too many fractional digits.
     */
    fun parse(formula: String, maxDecimals: Int): List<FormulaToken> {
        val (text, sourceIndexes) = formula.withoutWhitespace()
        val tokens = mutableListOf<FormulaToken>()
        var index = 0
        while (index < text.length) {
            val char = text[index]
            val previous = tokens.lastOrNull()
            when {
                startsNumber(char, previous) -> {
                    val position = sourceIndexes[index]
                    if (previous is FormulaToken.Num) fail(FormulaErrorKind.UnexpectedNumber, position, char.toString())
                    val end = text.numberEndIndex(index)
                    val number = text.substring(index, end)
                    // Values like "-" or "." are edit states, not numbers, when passed as bulk text.
                    if (number.none { it.isAsciiDigit() }) fail(FormulaErrorKind.IncompleteNumber, position, number)
                    if (number.fractionDigits() > maxDecimals) fail(FormulaErrorKind.TooManyDecimals, position, number)
                    tokens.add(FormulaToken.Num(number.toCanonicalNumber()))
                    index = end
                }

                char.isOperationSymbol() -> {
                    if (previous !is FormulaToken.Num) {
                        fail(FormulaErrorKind.UnexpectedOperation, sourceIndexes[index], char.toString())
                    }
                    // Commit `1.` as `1` rather than carrying an in-progress decimal point.
                    if (previous.text.endsWith('.')) {
                        tokens[tokens.lastIndex] = FormulaToken.Num(previous.text.dropLast(1))
                    }
                    tokens.add(FormulaToken.Op(CalculatorOperation.fromSymbol(char)))
                    index++
                }

                else -> fail(FormulaErrorKind.InvalidCharacter, sourceIndexes[index], char.toString())
            }
        }
        return tokens
    }

    /**
     * Split formula text into tokens without validating its shape.
     *
     * Used for display, where in-progress text such as `-`, `1.`, or `5+` must be kept as typed.
     *
     * @param formula Canonical formula text.
     * @return Tokens in formula order.
     * @throws FormulaSyntaxException When the formula has an unsupported character.
     */
    fun tokenize(formula: String): List<FormulaToken> {
        val (text, sourceIndexes) = formula.withoutWhitespace()
        val tokens = mutableListOf<FormulaToken>()
        var index = 0
        while (index < text.length) {
            val char = text[index]
            when {
                startsNumber(char, tokens.lastOrNull()) -> {
                    val end = text.numberEndIndex(index)
                    tokens.add(FormulaToken.Num(text.substring(index, end)))
                    index = end
                }

                char.isOperationSymbol() -> {
                    tokens.add(FormulaToken.Op(CalculatorOperation.fromSymbol(char)))
                    index++
                }

                else -> fail(FormulaErrorKind.InvalidCharacter, sourceIndexes[index], char.toString())
            }
        }
        return tokens
    }

    /**
     * Stop parsing with a [FormulaSyntaxException].
     *
     * @param kind Kind of problem.
     * @param position Index in the original formula text.
     * @param text Offending character or number text.
     * @throws FormulaSyntaxException Always.
     */
    private fun fail(kind: FormulaErrorKind, position: Int, text: String): Nothing =
        throw FormulaSyntaxException(FormulaError(kind, position, text))

    /**
     * Remove whitespace while remembering where each remaining character came from.
     *
     * @return Text without whitespace, and for each of its characters the index in this string.
     */
    private fun String.withoutWhitespace(): Pair<String, IntArray> {
        val sourceIndexes = IntArray(count { !it.isWhitespace() })
        var next = 0
        val text = buildString {
            this@withoutWhitespace.forEachIndexed { index, char ->
                if (!char.isWhitespace()) {
                    append(char)
                    sourceIndexes[next++] = index
                }
            }
        }
        return text to sourceIndexes
    }

    /**
     * Rewrite parsed number text the way typing would have produced it.
     *
     * @return Text with leading integer zeros removed and a zero before a leading decimal point.
     */
    private fun String.toCanonicalNumber(): String {
        val sign = if (startsWith('-')) "-" else ""
        val unsigned = removePrefix("-")
        val integerPart = unsigned.substringBefore('.').trimStart('0').ifEmpty { "0" }
        val fraction = if ('.' in unsigned) "." + unsigned.substringAfter('.') else ""
        return sign + integerPart + fraction
    }

    /**
     * Check whether [char] begins a number token.
     *
     * @param char Current character.
     * @param previous Token before [char], or `null` at the start of the formula.
     * @return `true` for a digit, a decimal point, or a `-` in sign position.
     */
    private fun startsNumber(char: Char, previous: FormulaToken?): Boolean =
        char.isAsciiDigit() || char == '.' || (char == '-' && (previous == null || previous is FormulaToken.Op))

    /**
     * Check whether this character is a canonical operation symbol.
     *
     * @return `true` for `+`, `-`, `*`, or `/`.
     */
    private fun Char.isOperationSymbol(): Boolean = this == '+' || this == '-' || this == '*' || this == '/'

    /**
     * Find where the number that starts at [start] ends.
     *
     * A number is an optional leading `-` followed by digits with at most one `.`.
     *
     * @param start Index of the number's first character.
     * @return Index just past the number's last character.
     */
    private fun String.numberEndIndex(start: Int): Int {
        var index = start
        if (this[index] == '-') index++
        var hasDecimalPoint = false
        while (index < length) {
            val char = this[index]
            when {
                char.isAsciiDigit() -> index++
                char == '.' && !hasDecimalPoint -> {
                    hasDecimalPoint = true
                    index++
                }

                else -> break
            }
        }
        return index
    }

    /**
     * Count the digits after the decimal point.
     *
     * @return Fractional digit count, or zero without a decimal point.
     */
    private fun String.fractionDigits(): Int = if ('.' in this) substringAfter('.').length else 0
}
