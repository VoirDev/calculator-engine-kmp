package dev.voir.calculator.engine

/**
 * Binary operations supported by the calculator engine.
 *
 * @property symbol Canonical raw formula character for this operation.
 */
public enum class CalculatorOperation(public val symbol: Char) {
    /** Adds the right-hand value to the left-hand value. */
    Add('+'),
    /** Subtracts the right-hand value from the left-hand value. */
    Subtract('-'),
    /** Multiplies the left-hand value by the right-hand value. */
    Multiply('*'),
    /** Divides the left-hand value by the right-hand value. */
    Divide('/');

    /**
     * Helpers for converting raw formula symbols into operations.
     */
    public companion object {
        /**
         * Convert a canonical operation character into a [CalculatorOperation].
         *
         * @param symbol Raw operation symbol: `+`, `-`, `*`, or `/`.
         * @return Matching operation.
         * @throws IllegalArgumentException When [symbol] is not a canonical operation symbol.
         */
        public fun fromSymbol(symbol: Char): CalculatorOperation =
            requireNotNull(fromSymbolOrNull(symbol)) { "Unknown operation symbol: '$symbol'" }

        /**
         * Convert a canonical operation character into a [CalculatorOperation], if it is one.
         *
         * @param symbol Candidate operation symbol.
         * @return Matching operation, or `null` when [symbol] is not `+`, `-`, `*`, or `/`.
         */
        public fun fromSymbolOrNull(symbol: Char): CalculatorOperation? = entries.firstOrNull { it.symbol == symbol }
    }
}
