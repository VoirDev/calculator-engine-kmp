package dev.voir.calculator.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CalculatorDecimalSeparatorInputTest {
    @Test
    fun `decimal separator starts zero prefixed decimal`() {
        val calculator = Calculator()

        assertTrue(calculator.inputDecimalSeparator())

        calculator.assertFormula("0.")
        calculator.assertSuccess("0")
    }

    @Test
    fun `decimal separator after operator starts zero prefixed right hand side`() {
        val calculator = Calculator()

        calculator.input("1+.")

        calculator.assertFormula("1+0.")
        calculator.assertSuccess("1")
    }

    @Test
    fun `decimal separator after negative sign starts negative zero prefixed decimal`() {
        val calculator = Calculator()

        calculator.input("-.5")

        calculator.assertFormula("-0.5")
        calculator.assertSuccess("-0.5")
    }

    @Test
    fun `second decimal separator in same number is rejected`() {
        val calculator = Calculator()

        val accepted = calculator.input("1.2.3")

        assertEquals(listOf(true, true, true, false, true), accepted)
        calculator.assertFormula("1.23")
        calculator.assertSuccess("1.23")
    }

    @Test
    fun `decimal separator is rejected when input max decimals is zero`() {
        val calculator = Calculator(CalculatorConfig(inputMaxDecimals = 0))

        assertFalse(calculator.inputDecimalSeparator())
        assertEquals(listOf(true, false), calculator.input("1."))

        calculator.assertFormula("1")
        calculator.assertSuccess("1")
    }
}
