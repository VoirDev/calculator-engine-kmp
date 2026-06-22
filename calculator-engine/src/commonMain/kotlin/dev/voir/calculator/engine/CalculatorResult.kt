package dev.voir.calculator.engine

import dev.voir.decimal.Decimal

/**
 * Current calculator output: either a Decimal value or a calculation error.
 */
sealed interface CalculatorResult {
    /** Successful calculation result. */
    data class Success(val value: Decimal) : CalculatorResult

    /** Failed calculation result. */
    data class Error(val error: CalculatorError) : CalculatorResult
}
