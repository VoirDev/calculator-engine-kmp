package dev.voir.calculator.engine

import dev.voir.decimal.Rounding
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CalculatorConfigTest {
    @Test
    fun `default config uses expected calculator settings`() {
        val config = CalculatorConfig()

        assertEquals(2, config.inputMaxDecimals)
        assertEquals(15, config.inputMaxDigits)
        assertEquals(12, config.divisionScale)
        assertEquals(CalculatorPrecedence.Chain, config.precedence)
        assertEquals(Rounding.HALF_UP, config.rounding)
    }

    @Test
    fun `config allows documented boundary values`() {
        CalculatorConfig(inputMaxDecimals = 0, inputMaxDigits = 1, divisionScale = 0)
        CalculatorConfig(inputMaxDecimals = 9, inputMaxDigits = 10, divisionScale = 0)
        CalculatorConfig(inputMaxDecimals = 9, inputMaxDigits = 38, divisionScale = 32767)
    }

    @Test
    fun `config rejects values outside documented ranges`() {
        assertFailsWith<IllegalArgumentException> { CalculatorConfig(inputMaxDecimals = -1) }
        assertFailsWith<IllegalArgumentException> { CalculatorConfig(inputMaxDecimals = 10) }
        assertFailsWith<IllegalArgumentException> { CalculatorConfig(inputMaxDigits = 0) }
        assertFailsWith<IllegalArgumentException> { CalculatorConfig(inputMaxDigits = 39) }
        assertFailsWith<IllegalArgumentException> { CalculatorConfig(divisionScale = -1) }
        assertFailsWith<IllegalArgumentException> { CalculatorConfig(divisionScale = 32768) }
    }

    @Test
    fun `config requires room for an integer digit`() {
        assertFailsWith<IllegalArgumentException> { CalculatorConfig(inputMaxDecimals = 5, inputMaxDigits = 5) }
    }

    @Test
    fun `copy changes only the given property`() {
        val config = CalculatorConfig().copy(inputMaxDecimals = 6)

        assertEquals(6, config.inputMaxDecimals)
        assertEquals(CalculatorConfig().divisionScale, config.divisionScale)
        assertEquals(CalculatorConfig().inputMaxDigits, config.inputMaxDigits)
    }
}
