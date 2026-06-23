package dev.voir.calculator.engine

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CalculatorOperationInputTest {
    @Test
    fun `empty formula accepts only subtract as negative sign`() {
        val calculator = Calculator()

        assertFalse(calculator.inputOperation(CalculatorOperation.Add))
        assertFalse(calculator.inputOperation(CalculatorOperation.Multiply))
        assertFalse(calculator.inputOperation(CalculatorOperation.Divide))
        assertTrue(calculator.inputOperation(CalculatorOperation.Subtract))

        calculator.assertFormula("-")
        calculator.assertSuccess("0")
    }

    @Test
    fun `operation is appended after complete number`() {
        val calculator = Calculator()

        calculator.input("12+")

        calculator.assertFormula("12+")
        calculator.assertSuccess("12")
    }

    @Test
    fun `operation input replaces previous operation`() {
        val calculator = Calculator()

        calculator.input("1+*/-")

        calculator.assertFormula("1-")
        calculator.assertSuccess("1")
    }

    @Test
    fun `operation after bare negative sign is rejected`() {
        val calculator = Calculator()

        calculator.input("-")

        assertFalse(calculator.inputOperation(CalculatorOperation.Add))
        assertFalse(calculator.inputOperation(CalculatorOperation.Multiply))
        assertFalse(calculator.inputOperation(CalculatorOperation.Divide))
        assertFalse(calculator.inputOperation(CalculatorOperation.Subtract))
        calculator.assertFormula("-")
    }

    @Test
    fun `operation normalizes trailing decimal point`() {
        val calculator = Calculator()

        calculator.input("1.+")

        calculator.assertFormula("1+")
        calculator.assertSuccess("1")
    }

    @Test
    fun `operation after evaluate continues from stored result`() {
        val calculator = Calculator()

        calculator.setFormula("1+2")
        calculator.evaluate()
        assertTrue(calculator.inputOperation(CalculatorOperation.Multiply))
        assertTrue(calculator.inputDigit('3'))

        calculator.assertFormula("3*3")
        calculator.assertSuccess("9")
    }
}
