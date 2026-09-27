package dev.voir.calculator.engine.format

import dev.voir.decimal.Rounding

/**
 * Display settings used only when formatting calculator state.
 *
 * Formula numbers are always shown exactly as typed, with separators applied; only results use
 * [maximumFractionDigits], [minimumFractionDigits], and [rounding].
 *
 * @property decimalSeparator Character rendered and accepted as the decimal separator.
 * @property groupingSeparator Character inserted every three integer digits and ignored when
 * normalizing, or `null` to disable grouping.
 * @property negativeSign Character rendered and accepted for a negative number's sign. It may equal
 * [CalculatorOperationSymbols.subtract]; normalizing tells them apart by position.
 * @property maximumFractionDigits Maximum fractional digits shown in a formatted result, from `0`
 * through `32767`.
 * @property minimumFractionDigits Minimum fractional digits shown in a formatted result, padded with
 * zeros; at most [maximumFractionDigits].
 * @property rounding Rounding applied to results with more than [maximumFractionDigits] digits.
 * @property operationSymbols Characters rendered and accepted for each operation.
 * @property emptyLabel Text for a result that is still empty, or `null` to show zero.
 * @property errorLabel Text for a failed calculation, unless a formatter call supplies per-error
 * labels.
 * @throws IllegalArgumentException When a separator, sign, or symbol is a digit or whitespace
 * (grouping may be whitespace), when two of them collide, or when fraction digits are out of range.
 */
public data class CalculatorFormatConfig(
    public val decimalSeparator: Char = '.',
    public val groupingSeparator: Char? = ' ',
    public val negativeSign: Char = '-',
    public val maximumFractionDigits: Int = 2,
    public val minimumFractionDigits: Int = 0,
    public val rounding: Rounding = Rounding.HALF_UP,
    public val operationSymbols: CalculatorOperationSymbols = CalculatorOperationSymbols(),
    public val emptyLabel: String? = null,
    public val errorLabel: String = "Error",
) {
    init {
        require(!decimalSeparator.isDigit() && !decimalSeparator.isWhitespace()) {
            "decimalSeparator must not be a digit or whitespace, was '$decimalSeparator'."
        }
        if (groupingSeparator != null) {
            require(!groupingSeparator.isDigit()) { "groupingSeparator must not be a digit, was '$groupingSeparator'." }
            require(groupingSeparator != decimalSeparator) { "groupingSeparator must differ from decimalSeparator." }
        }
        require(!negativeSign.isDigit() && !negativeSign.isWhitespace()) {
            "negativeSign must not be a digit or whitespace, was '$negativeSign'."
        }
        require(negativeSign != decimalSeparator && negativeSign != groupingSeparator) {
            "negativeSign must differ from the decimal and grouping separators."
        }

        val symbols = operationSymbols.all
        require(symbols.distinct().size == symbols.size) { "operation symbols must be unique, were $symbols." }
        require(symbols.none { it.isDigit() || it.isWhitespace() }) {
            "operation symbols must not be digits or whitespace, were $symbols."
        }
        require(decimalSeparator !in symbols) { "decimalSeparator must not equal an operation symbol." }
        require(groupingSeparator !in symbols) { "groupingSeparator must not equal an operation symbol." }
        require(negativeSign == operationSymbols.subtract || negativeSign !in symbols) {
            "negativeSign may only equal the subtract symbol among operation symbols."
        }

        // Decimal accepts fraction digit counts in this range.
        require(maximumFractionDigits in 0..32767) {
            "maximumFractionDigits must be between 0 and 32767, was $maximumFractionDigits."
        }
        require(minimumFractionDigits in 0..maximumFractionDigits) {
            "minimumFractionDigits must be between 0 and maximumFractionDigits ($maximumFractionDigits), " +
                "was $minimumFractionDigits."
        }
    }
}
