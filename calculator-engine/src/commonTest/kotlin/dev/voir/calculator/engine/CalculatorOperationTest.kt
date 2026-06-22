package dev.voir.calculator.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
        assertFailsWith<IllegalStateException> { CalculatorOperation.fromSymbol('%') }
        assertFailsWith<IllegalStateException> { CalculatorOperation.fromSymbol('×') }
        assertFailsWith<IllegalStateException> { CalculatorOperation.fromSymbol(' ') }
    }
}
