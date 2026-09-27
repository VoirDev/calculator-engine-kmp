package dev.voir.calculator.engine

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CalculatorOperationInputTest {
    @Test
    fun `empty formula accepts only subtract as negative sign`() {
        val calculator = Calculator()

        assertRejected(calculator.inputOperation(CalculatorOperation.Add))
        assertRejected(calculator.inputOperation(CalculatorOperation.Multiply))
        assertRejected(calculator.inputOperation(CalculatorOperation.Divide))
        assertAccepted(calculator.inputOperation(CalculatorOperation.Subtract))

        calculator.assertFormula("-")
        calculator.assertEmpty()
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

        calculator.input("1*/+-")

        calculator.assertFormula("1-")
        calculator.assertSuccess("1")
    }

    @Test
    fun `operation after bare negative sign is rejected`() {
        val calculator = Calculator()

        calculator.input("-")

        assertRejected(calculator.inputOperation(CalculatorOperation.Add))
        assertRejected(calculator.inputOperation(CalculatorOperation.Multiply))
        assertRejected(calculator.inputOperation(CalculatorOperation.Divide))
        assertRejected(calculator.inputOperation(CalculatorOperation.Subtract))
        calculator.assertFormula("-")
    }

    @Test
    fun `subtract after multiply or divide starts a negative operand`() {
        val multiply = Calculator()
        multiply.input("5*-3")
        multiply.assertFormula("5*-3")
        multiply.assertSuccess("-15")

        val divide = Calculator()
        divide.input("6/-3")
        divide.assertFormula("6/-3")
        divide.assertSuccess("-2")
    }

    @Test
    fun `subtract after add or subtract replaces the operation`() {
        val calculator = Calculator()

        calculator.input("5+-")

        calculator.assertFormula("5-")
    }

    @Test
    fun `operation after a negative sign in operand position replaces the preceding operation`() {
        val calculator = Calculator()
        calculator.input("5*-")

        assertRejected(calculator.inputOperation(CalculatorOperation.Subtract))
        calculator.assertFormula("5*-")

        assertAccepted(calculator.inputOperation(CalculatorOperation.Add))
        calculator.assertFormula("5+")
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
        assertAccepted(calculator.inputOperation(CalculatorOperation.Multiply))
        assertAccepted(calculator.inputDigit('3'))

        calculator.assertFormula("3*3")
        calculator.assertSuccess("9")
    }
}
