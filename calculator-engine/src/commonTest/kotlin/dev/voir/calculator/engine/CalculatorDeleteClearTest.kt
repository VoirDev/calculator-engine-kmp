package dev.voir.calculator.engine

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CalculatorDeleteClearTest {
    @Test
    fun `delete last returns false for empty formula`() {
        assertFalse(Calculator().deleteLast())
    }

    @Test
    fun `delete last removes digits one at a time`() {
        val calculator = Calculator()

        calculator.input("123")
        assertTrue(calculator.deleteLast())
        calculator.assertFormula("12")
        assertTrue(calculator.deleteLast())
        calculator.assertFormula("1")
        assertTrue(calculator.deleteLast())
        calculator.assertFormula("")
        assertFalse(calculator.deleteLast())
    }

    @Test
    fun `delete last removes decimal and negative characters one at a time`() {
        val calculator = Calculator()

        calculator.input("-1.2")
        assertTrue(calculator.deleteLast())
        calculator.assertFormula("-1.")
        assertTrue(calculator.deleteLast())
        calculator.assertFormula("-1")
        assertTrue(calculator.deleteLast())
        calculator.assertFormula("-")
        assertTrue(calculator.deleteLast())
        calculator.assertFormula("")
    }

    @Test
    fun `delete last removes whole operator token`() {
        val calculator = Calculator()

        calculator.input("1+")
        assertTrue(calculator.deleteLast())

        calculator.assertFormula("1")
        calculator.assertSuccess("1")
    }

    @Test
    fun `clear resets formula and result after success`() {
        val calculator = Calculator()

        calculator.setFormula("1+2")

        assertTrue(calculator.clear())
        calculator.assertFormula("")
        calculator.assertSuccess("0")
        assertFalse(calculator.clear())
    }

    @Test
    fun `clear resets formula and result after error`() {
        val calculator = Calculator()

        calculator.setFormula("1/0")
        calculator.assertError(CalculatorError.DivisionByZero)

        assertTrue(calculator.clear())

        calculator.assertFormula("")
        calculator.assertSuccess("0")
    }
}
