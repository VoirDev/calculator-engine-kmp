package dev.voir.calculator.engine.format

import dev.voir.calculator.engine.CalculatorOperation

/**
 * User-facing operation symbols used for formatted formula rendering and parsing.
 *
 * The canonical formula always uses `+`, `-`, `*`, and `/`; this class only controls the
 * display and input layer intended for UI text.
 *
 * @property add Character displayed and accepted for addition.
 * @property subtract Character displayed and accepted for subtraction.
 * @property multiply Character displayed and accepted for multiplication.
 * @property divide Character displayed and accepted for division.
 */
public data class CalculatorOperationSymbols(
    public val add: Char = '+',
    public val subtract: Char = '-',
    public val multiply: Char = '×',
    public val divide: Char = '÷',
) {
    /** All four symbols, in [CalculatorOperation] order. */
    public val all: List<Char> get() = listOf(add, subtract, multiply, divide)

    /**
     * Look up the configured user-facing character for an operation.
     *
     * @param operation Operation to render.
     * @return Configured character for [operation].
     */
    public fun symbolFor(operation: CalculatorOperation): Char = when (operation) {
        CalculatorOperation.Add -> add
        CalculatorOperation.Subtract -> subtract
        CalculatorOperation.Multiply -> multiply
        CalculatorOperation.Divide -> divide
    }
}
