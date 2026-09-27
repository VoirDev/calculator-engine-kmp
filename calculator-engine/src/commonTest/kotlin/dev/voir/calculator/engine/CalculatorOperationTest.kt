package dev.voir.calculator.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class CalculatorOperationTest {
    @Test
    fun `operation symbols map to expected operations`() {
        assertEquals(CalculatorOperation.Add, CalculatorOperation.fromSymbol('+'))
        assertEquals(CalculatorOperation.Subtract, CalculatorOperation.fromSymbol('-'))
        assertEquals(CalculatorOperation.Multiply, CalculatorOperation.fromSymbol('*'))
        assertEquals(CalculatorOperation.Divide, CalculatorOperation.fromSymbol('/'))
    }

    @Test
    fun `unsupported operation symbols throw`() {
        assertFailsWith<IllegalArgumentException> { CalculatorOperation.fromSymbol('%') }
        assertFailsWith<IllegalArgumentException> { CalculatorOperation.fromSymbol('×') }
        assertFailsWith<IllegalArgumentException> { CalculatorOperation.fromSymbol(' ') }
    }

    @Test
    fun `from symbol or null returns null for unsupported symbols`() {
        assertEquals(CalculatorOperation.Divide, CalculatorOperation.fromSymbolOrNull('/'))
        assertNull(CalculatorOperation.fromSymbolOrNull('÷'))
    }
}
