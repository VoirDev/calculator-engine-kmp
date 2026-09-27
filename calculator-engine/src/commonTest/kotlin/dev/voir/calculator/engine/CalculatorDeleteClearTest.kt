package dev.voir.calculator.engine

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CalculatorDeleteClearTest {
    @Test
    fun `delete last returns false for empty formula`() {
        assertRejected(Calculator().deleteLast())
    }

    @Test
    fun `delete last removes digits one at a time`() {
        val calculator = Calculator()

        calculator.input("123")
        assertAccepted(calculator.deleteLast())
        calculator.assertFormula("12")
        assertAccepted(calculator.deleteLast())
        calculator.assertFormula("1")
        assertAccepted(calculator.deleteLast())
        calculator.assertFormula("")
        assertRejected(calculator.deleteLast())
    }

    @Test
    fun `delete last removes decimal and negative characters one at a time`() {
        val calculator = Calculator()

        calculator.input("-1.2")
        assertAccepted(calculator.deleteLast())
        calculator.assertFormula("-1.")
        assertAccepted(calculator.deleteLast())
        calculator.assertFormula("-1")
        assertAccepted(calculator.deleteLast())
        calculator.assertFormula("-")
        assertAccepted(calculator.deleteLast())
        calculator.assertFormula("")
    }

    @Test
    fun `delete last removes whole operator token`() {
        val calculator = Calculator()

        calculator.input("1+")
        assertAccepted(calculator.deleteLast())

        calculator.assertFormula("1")
        calculator.assertSuccess("1")
    }

    @Test
    fun `clear resets formula and result after success`() {
        val calculator = Calculator()

        calculator.setFormula("1+2")

        assertAccepted(calculator.clear())
        calculator.assertFormula("")
        calculator.assertEmpty()
        assertRejected(calculator.clear())
    }

    @Test
    fun `clear resets formula and result after error`() {
        val calculator = Calculator()

        calculator.setFormula("1/0")
        calculator.assertError(CalculatorError.DivisionByZero)

        assertAccepted(calculator.clear())

        calculator.assertFormula("")
        calculator.assertEmpty()
    }

    @Test
    fun `delete after evaluate edits the displayed result as typed text`() {
        val calculator = Calculator()
        calculator.input("10/3")
        calculator.evaluate()
        calculator.assertFormula("3.33")

        assertAccepted(calculator.deleteLast())

        calculator.assertFormula("3.3")
        calculator.assertSuccess("3.3")
        // The edited number is ordinary input again, so digits append to it.
        assertAccepted(calculator.inputDigit('9'))
        calculator.assertFormula("3.39")
    }
}
