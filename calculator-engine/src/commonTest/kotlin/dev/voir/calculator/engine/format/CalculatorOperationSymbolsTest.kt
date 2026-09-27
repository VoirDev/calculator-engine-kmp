package dev.voir.calculator.engine.format

import dev.voir.calculator.engine.CalculatorOperation
import kotlin.test.Test
import kotlin.test.assertEquals

class CalculatorOperationSymbolsTest {
    @Test
    fun `default operation symbols match expected display characters`() {
        val symbols = CalculatorOperationSymbols()

        assertEquals('+', symbols.symbolFor(CalculatorOperation.Add))
        assertEquals('-', symbols.symbolFor(CalculatorOperation.Subtract))
        assertEquals('×', symbols.symbolFor(CalculatorOperation.Multiply))
        assertEquals('÷', symbols.symbolFor(CalculatorOperation.Divide))
    }

    @Test
    fun `custom operation symbols map each operation independently`() {
        val symbols = CalculatorOperationSymbols(
            add = 'p',
            subtract = 'm',
            multiply = 'x',
            divide = 'd',
        )

        assertEquals('p', symbols.symbolFor(CalculatorOperation.Add))
        assertEquals('m', symbols.symbolFor(CalculatorOperation.Subtract))
        assertEquals('x', symbols.symbolFor(CalculatorOperation.Multiply))
        assertEquals('d', symbols.symbolFor(CalculatorOperation.Divide))
    }
}
