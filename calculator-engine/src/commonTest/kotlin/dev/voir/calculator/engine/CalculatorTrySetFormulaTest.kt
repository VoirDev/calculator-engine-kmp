package dev.voir.calculator.engine

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CalculatorTrySetFormulaTest {
    @Test
    fun `try set formula returns true for valid changed formula`() {
        val calculator = Calculator()

        assertTrue(calculator.trySetFormula("1+2"))

        calculator.assertFormula("1+2")
        calculator.assertSuccess("3")
    }

    @Test
    fun `try set formula returns false for same parsed formula`() {
        val calculator = Calculator()

        assertTrue(calculator.trySetFormula("1+2"))
        assertFalse(calculator.trySetFormula(" 1 + 2 "))

        calculator.assertFormula("1+2")
    }

    @Test
    fun `try set formula returns false and preserves state for invalid formula`() {
        val calculator = Calculator()

        calculator.setFormula("1+2")
        assertFalse(calculator.trySetFormula("1++2"))

        calculator.assertFormula("1+2")
        calculator.assertSuccess("3")
    }

    @Test
    fun `try set formula returns false and preserves state for malformed decimal formula`() {
        val calculator = Calculator()

        calculator.setFormula("1+2")
        assertFalse(calculator.trySetFormula("1..2"))
        assertFalse(calculator.trySetFormula("1.2.3"))

        calculator.assertFormula("1+2")
        calculator.assertSuccess("3")
    }

    @Test
    fun `try set formula returns false and preserves state for too many decimals`() {
        val calculator = Calculator(CalculatorConfig(inputMaxDecimals = 2))

        calculator.setFormula("1.23")
        assertFalse(calculator.trySetFormula("1.234"))

        calculator.assertFormula("1.23")
        calculator.assertSuccess("1.23")
    }

    @Test
    fun `try set formula accepts syntactically valid division by zero formula`() {
        val calculator = Calculator()

        assertTrue(calculator.trySetFormula("1/0"))

        calculator.assertFormula("1/0")
        calculator.assertError(CalculatorError.DivisionByZero)
    }
}
