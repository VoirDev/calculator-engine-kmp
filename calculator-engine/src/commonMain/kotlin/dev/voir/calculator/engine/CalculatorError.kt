package dev.voir.calculator.engine

/**
 * Explicit calculator failure states exposed through [CalculatorResult.Error].
 */
public sealed interface CalculatorError {
    /**
     * The current expression tried to divide by zero.
     */
    public data object DivisionByZero : CalculatorError

    /**
     * Decimal conversion failed for a number token that looked syntactically valid.
     *
     * @property text Raw number text that could not be converted.
     */
    public data class InvalidNumber(public val text: String) : CalculatorError

    /**
     * Decimal arithmetic failed for an operation other than a known divide-by-zero, for example
     * because the result is outside the range `Decimal` can represent.
     *
     * @property operation Operation that failed.
     */
    public data class ArithmeticFailure(public val operation: CalculatorOperation) : CalculatorError
}
