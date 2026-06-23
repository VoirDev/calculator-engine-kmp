package dev.voir.calculator.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CalculatorDigitInputTest {
    @Test
    fun `digits append and update formula and result`() {
        val calculator = Calculator()

        assertTrue(calculator.inputDigit('1'))
        assertTrue(calculator.inputDigit('2'))
        assertTrue(calculator.inputDigit('3'))

        calculator.assertFormula("123")
        calculator.assertSuccess("123")
    }

    @Test
    fun `input digit rejects non ascii decimal digits and symbols`() {
        val calculator = Calculator()

        assertFailsWith<IllegalArgumentException> { calculator.inputDigit('a') }
        assertFailsWith<IllegalArgumentException> { calculator.inputDigit('.') }
        assertFailsWith<IllegalArgumentException> { calculator.inputDigit('+') }
        assertFailsWith<IllegalArgumentException> { calculator.inputDigit(' ') }
        assertFailsWith<IllegalArgumentException> { calculator.inputDigit('١') }
    }

    @Test
    fun `leading zero is replaced before integer digits`() {
        val calculator = Calculator()

        assertTrue(calculator.inputDigit('0'))
        assertFalse(calculator.inputDigit('0'))
        assertTrue(calculator.inputDigit('7'))

        calculator.assertFormula("7")
        calculator.assertSuccess("7")
    }

    @Test
    fun `leading zero is preserved for decimals`() {
        val calculator = Calculator()

        calculator.input("0.50")

        calculator.assertFormula("0.50")
        calculator.assertSuccess("0.5")
    }

    @Test
    fun `negative leading zero is replaced before integer digits`() {
        val calculator = Calculator()

        calculator.input("-07")

        calculator.assertFormula("-7")
        calculator.assertSuccess("-7")
    }

    @Test
    fun `digits are rejected after configured fractional precision`() {
        val calculator = Calculator(CalculatorConfig(inputMaxDecimals = 2))

        val accepted = calculator.input("1.234")

        assertEquals(listOf(true, true, true, true, false), accepted)
        calculator.assertFormula("1.23")
        calculator.assertSuccess("1.23")
    }

    @Test
    fun `nine fractional digits are accepted at max precision and tenth is rejected`() {
        val calculator = Calculator(CalculatorConfig(inputMaxDecimals = 9))

        val accepted = calculator.input("1.1234567890")

        assertEquals(List(11) { true } + false, accepted)
        calculator.assertFormula("1.123456789")
        calculator.assertSuccess("1.123456789")
    }
}
