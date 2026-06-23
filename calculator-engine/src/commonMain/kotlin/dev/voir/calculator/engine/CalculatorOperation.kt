package dev.voir.calculator.engine

/**
 * Binary operations supported by the calculator engine.
 *
 * @property symbol Canonical raw formula character for this operation.
 */
enum class CalculatorOperation(val symbol: Char) {
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
    companion object Companion {
        /**
         * Convert a canonical operation character into a [CalculatorOperation].
         *
         * @param c Raw operation symbol: `+`, `-`, `*`, or `/`.
         * @return Matching operation enum value.
         * @throws IllegalStateException when [c] is not a supported operation symbol.
         */
        fun fromSymbol(c: Char): CalculatorOperation = when (c) {
            '+' -> Add
            '-' -> Subtract
            '*' -> Multiply
            '/' -> Divide
            else -> error("Unknown op: $c")
        }
    }
}
