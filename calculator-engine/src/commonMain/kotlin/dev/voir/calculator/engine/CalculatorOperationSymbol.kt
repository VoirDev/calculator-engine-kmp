package dev.voir.calculator.engine

/**
 * User-facing operation symbols used for formatted formula rendering and parsing.
 *
 * The raw formula always uses `+`, `-`, `*`, and `/`; this class only controls
 * the display/input layer intended for UI text.
 *
 * @property add Character displayed and accepted for addition.
 * @property subtract Character displayed and accepted for subtraction.
 * @property multiply Character displayed and accepted for multiplication.
 * @property divide Character displayed and accepted for division.
 */
data class CalculatorOperationSymbol(
    /** User-facing addition character. */
    val add: Char = '+',
    /** User-facing subtraction character. */
    val subtract: Char = '-',
    /** User-facing multiplication character. */
    val multiply: Char = '×',
    /** User-facing division character. */
    val divide: Char = '÷',
) {
    /**
     * Look up the configured user-facing character for an operation.
     *
     * @param op Operation to render.
     * @return Configured character for [op].
     */
    fun forOp(op: CalculatorOperation): Char = when (op) {
        CalculatorOperation.Add -> add
        CalculatorOperation.Subtract -> subtract
        CalculatorOperation.Multiply -> multiply
        CalculatorOperation.Divide -> divide
    }
}
