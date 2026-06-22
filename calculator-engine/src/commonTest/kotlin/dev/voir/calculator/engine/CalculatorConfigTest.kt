package dev.voir.calculator.engine

import dev.voir.decimal.Rounding
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CalculatorConfigTest {
    @Test
    fun `default config uses expected calculator settings`() {
        val config = CalculatorConfig()

        assertEquals(2, config.maxDecimals)
        assertEquals(2, config.inputMaxDecimals)
        assertEquals(12, config.divisionScale)
        assertEquals(CalculatorPrecedence.Chain, config.precedence)
        assertEquals(Rounding.HALF_UP, config.rounding)
    }

    @Test
    fun `config allows documented boundary values`() {
        CalculatorConfig(maxDecimals = 0, inputMaxDecimals = 0, divisionScale = 0)
        CalculatorConfig(maxDecimals = 9, inputMaxDecimals = 9, divisionScale = 0)
        CalculatorConfig(maxDecimals = 2, inputMaxDecimals = 9, divisionScale = 100)
        CalculatorConfig(maxDecimals = 9, inputMaxDecimals = 0, divisionScale = 1)
    }

    @Test
    fun `config rejects values outside documented ranges`() {
        assertFailsWith<IllegalArgumentException> { CalculatorConfig(maxDecimals = -1) }
        assertFailsWith<IllegalArgumentException> { CalculatorConfig(maxDecimals = 10) }
        assertFailsWith<IllegalArgumentException> { CalculatorConfig(inputMaxDecimals = -1) }
        assertFailsWith<IllegalArgumentException> { CalculatorConfig(inputMaxDecimals = 10) }
        assertFailsWith<IllegalArgumentException> { CalculatorConfig(divisionScale = -1) }
    }

    @Test
    fun `default division scale follows input max decimals`() {
        assertEquals(10, CalculatorConfig(maxDecimals = 9, inputMaxDecimals = 0).divisionScale)
        assertEquals(19, CalculatorConfig(maxDecimals = 0, inputMaxDecimals = 9).divisionScale)
    }
}
