package dev.voir.calculator.engine

import dev.voir.decimal.Decimal

/**
 * Current calculator output: nothing yet, a Decimal value, or a calculation error.
 */
public sealed interface CalculatorResult {
    /** The formula has no complete number yet, for example while it is empty or a bare `-`. */
    public data object Empty : CalculatorResult

    /**
     * Successful calculation result.
     *
     * @property value Exact value of the current formula. It is not rounded to
     * [CalculatorConfig.inputMaxDecimals]; format it for display.
     */
    public data class Success(public val value: Decimal) : CalculatorResult

    /**
     * Failed calculation result.
     *
     * @property error Reason the formula could not be evaluated.
     */
    public data class Error(public val error: CalculatorError) : CalculatorResult
}
