package dev.voir.calculator.engine.format

import dev.voir.calculator.engine.CalculatorOperationSymbol
import dev.voir.decimal.Rounding

/**
 * Display settings used only when formatting calculator state.
 */
data class CalculatorFormatConfig(
    /** Character rendered and accepted as the decimal separator in formatted text. */
    val decimalSeparator: Char = '.',
    /** Grouping separator rendered and ignored during formatted parsing. */
    val groupingSeparator: Char = ' ',
    /** Maximum fractional precision shown in formatted results. */
    val maxDecimals: Int = 2,
    /** Minimum fractional precision shown in formatted numbers. */
    val minimumFractionDigits: Int = 0,
    /** Rounding behavior used when formatting results. */
    val rounding: Rounding = Rounding.HALF_UP,
    /** Mapping between calculator operations and user-facing operation characters. */
    val operationSymbols: CalculatorOperationSymbol = CalculatorOperationSymbol(),
    /** Label used when formatting a failed calculation result. */
    val errorLabel: String = "Error",
) {
    init {
        require(decimalSeparator != groupingSeparator) {
            "decimalSeparator must not be equal to groupingSeparator"
        }
        require(!decimalSeparator.isWhitespace()) {
            "decimalSeparator must not be whitespace"
        }
        require(minimumFractionDigits in 0..9) {
            "minimumFractionDigits must be between 0 and 9."
        }
        require(maxDecimals in 0..9) { "maxDecimals must be between 0 and 9." }
        require(minimumFractionDigits <= maxDecimals) {
            "minimumFractionDigits must be <= maxDecimals."
        }
        val opSymbols = listOf(
            operationSymbols.add,
            operationSymbols.subtract,
            operationSymbols.multiply,
            operationSymbols.divide
        )
        require(opSymbols.distinct().size == opSymbols.size) {
            "operation symbols must be unique"
        }
        require(opSymbols.none { it.isWhitespace() }) {
            "operation symbols must not be whitespace"
        }
        require(decimalSeparator !in opSymbols) {
            "decimalSeparator must not equal any operation symbol"
        }
        require(groupingSeparator !in opSymbols) {
            "groupingSeparator must not equal any operation symbol"
        }
    }
}
