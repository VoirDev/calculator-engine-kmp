package dev.voir.calculator.engine

/**
 * Problem found while parsing formula text.
 *
 * @property kind Kind of problem.
 * @property position Index in the original formula text, whitespace included, where the problem
 * starts.
 * @property text Offending character or number text.
 */
public data class FormulaError(
    public val kind: FormulaErrorKind,
    public val position: Int,
    public val text: String,
) {
    /** English description for logs and exception messages; not intended for end users. */
    public val message: String
        get() = when (kind) {
            FormulaErrorKind.InvalidCharacter -> "Invalid character '$text' at position $position"
            FormulaErrorKind.UnexpectedNumber -> "Unexpected number at position $position"
            FormulaErrorKind.UnexpectedOperation -> "Unexpected operator '$text' at position $position"
            FormulaErrorKind.IncompleteNumber -> "Invalid number '$text' at position $position"
            FormulaErrorKind.TooManyDecimals -> "Too many decimal digits in '$text' at position $position"
        }
}

/** Kind of [FormulaError]. */
public enum class FormulaErrorKind {
    /** A character other than an ASCII digit, `.`, `+`, `-`, `*`, `/`, or whitespace. */
    InvalidCharacter,

    /** A number directly after another number, such as the second `.` in `1.2.3`. */
    UnexpectedNumber,

    /** An operation without a number before it, such as `*2` or `1+*2`. */
    UnexpectedOperation,

    /** A number without digits, such as `-` or `.` on its own. */
    IncompleteNumber,

    /** A number with more fractional digits than [CalculatorConfig.inputMaxDecimals]. */
    TooManyDecimals,
}

/**
 * Thrown by [CalculatorApi.setFormula] when the formula cannot be parsed.
 *
 * Extends [IllegalArgumentException], so existing handlers keep working.
 *
 * @property error First problem found, scanning left to right.
 */
public class FormulaSyntaxException(public val error: FormulaError) : IllegalArgumentException(error.message)
