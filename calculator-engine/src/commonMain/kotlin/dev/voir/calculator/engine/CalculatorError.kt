package dev.voir.calculator.engine

/**
 * Explicit calculator failure states exposed through [CalculatorResult.Error].
 */
sealed interface CalculatorError {
    /**
     * The current expression tried to divide by zero.
     */
    data object DivisionByZero : CalculatorError

    /**
     * Decimal conversion failed for a number token that looked syntactically valid.
     *
     * @property text Raw number text that could not be converted.
     */
    data class InvalidNumber(val text: String) : CalculatorError

    /**
     * Decimal arithmetic failed for an operation other than a known divide-by-zero.
     *
     * @property operation Operation that failed.
     */
    data class ArithmeticFailure(val operation: CalculatorOperation) : CalculatorError
}
