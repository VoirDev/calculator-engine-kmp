package dev.voir.calculator.engine.format

import dev.voir.calculator.engine.CalculatorOperation
import dev.voir.calculator.engine.CalculatorOperationSymbol
import kotlin.test.Test
import kotlin.test.assertEquals

class CalculatorOperationSymbolFormatTest {
    @Test
    fun `default operation symbols match expected display characters`() {
        val symbols = CalculatorOperationSymbol()

        assertEquals('+', symbols.forOp(CalculatorOperation.Add))
        assertEquals('-', symbols.forOp(CalculatorOperation.Subtract))
        assertEquals('×', symbols.forOp(CalculatorOperation.Multiply))
        assertEquals('÷', symbols.forOp(CalculatorOperation.Divide))
    }

    @Test
    fun `custom operation symbols map each operation independently`() {
        val symbols = CalculatorOperationSymbol(
            add = 'p',
            subtract = 'm',
            multiply = 'x',
            divide = 'd',
        )

        assertEquals('p', symbols.forOp(CalculatorOperation.Add))
        assertEquals('m', symbols.forOp(CalculatorOperation.Subtract))
        assertEquals('x', symbols.forOp(CalculatorOperation.Multiply))
        assertEquals('d', symbols.forOp(CalculatorOperation.Divide))
    }
}
