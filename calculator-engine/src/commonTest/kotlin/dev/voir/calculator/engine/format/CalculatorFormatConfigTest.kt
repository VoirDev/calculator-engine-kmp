package dev.voir.calculator.engine.format

import dev.voir.calculator.engine.CalculatorOperationSymbol
import dev.voir.decimal.Rounding
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CalculatorFormatConfigTest {
    @Test
    fun `default format config uses expected display settings`() {
        val config = CalculatorFormatConfig()

        assertEquals('.', config.decimalSeparator)
        assertEquals(' ', config.groupingSeparator)
        assertEquals(0, config.minimumFractionDigits)
        assertEquals(2, config.maxDecimals)
        assertEquals(Rounding.HALF_UP, config.rounding)
        assertEquals("Error", config.errorLabel)
        assertEquals(CalculatorOperationSymbol(), config.operationSymbols)
    }

    @Test
    fun `format config allows documented boundary values`() {
        CalculatorFormatConfig(maxDecimals = 0)
        CalculatorFormatConfig(maxDecimals = 9)
        CalculatorFormatConfig(minimumFractionDigits = 0, maxDecimals = 0)
        CalculatorFormatConfig(minimumFractionDigits = 9, maxDecimals = 9)
        CalculatorFormatConfig(groupingSeparator = '_')
        CalculatorFormatConfig(decimalSeparator = ',', groupingSeparator = '.')
        CalculatorFormatConfig(
            operationSymbols = CalculatorOperationSymbol(
                add = 'p',
                subtract = 'm',
                multiply = 'x',
                divide = 'd',
            )
        )
    }

    @Test
    fun `format config rejects decimal separator equal to grouping separator`() {
        assertFailsWith<IllegalArgumentException> {
            CalculatorFormatConfig(decimalSeparator = ',', groupingSeparator = ',')
        }
    }

    @Test
    fun `format config rejects whitespace decimal separator`() {
        assertFailsWith<IllegalArgumentException> {
            CalculatorFormatConfig(decimalSeparator = ' ', groupingSeparator = '_')
        }
        assertFailsWith<IllegalArgumentException> {
            CalculatorFormatConfig(decimalSeparator = '\n', groupingSeparator = '_')
        }
    }

    @Test
    fun `format config rejects max decimals outside supported range`() {
        assertFailsWith<IllegalArgumentException> { CalculatorFormatConfig(maxDecimals = -1) }
        assertFailsWith<IllegalArgumentException> { CalculatorFormatConfig(maxDecimals = 10) }
    }

    @Test
    fun `format config rejects minimum fraction digits outside supported range`() {
        assertFailsWith<IllegalArgumentException> { CalculatorFormatConfig(minimumFractionDigits = -1) }
        assertFailsWith<IllegalArgumentException> { CalculatorFormatConfig(minimumFractionDigits = 10, maxDecimals = 10) }
    }

    @Test
    fun `format config rejects minimum fraction digits greater than max decimals`() {
        assertFailsWith<IllegalArgumentException> {
            CalculatorFormatConfig(minimumFractionDigits = 3, maxDecimals = 2)
        }
    }

    @Test
    fun `format config rejects duplicate operation symbols`() {
        assertFailsWith<IllegalArgumentException> {
            CalculatorFormatConfig(
                operationSymbols = CalculatorOperationSymbol(
                    add = '+',
                    subtract = '+',
                    multiply = '*',
                    divide = '/',
                )
            )
        }
    }

    @Test
    fun `format config rejects whitespace operation symbols`() {
        assertFailsWith<IllegalArgumentException> {
            CalculatorFormatConfig(
                operationSymbols = CalculatorOperationSymbol(multiply = ' ')
            )
        }
    }

    @Test
    fun `format config rejects separators that conflict with operation symbols`() {
        assertFailsWith<IllegalArgumentException> {
            CalculatorFormatConfig(decimalSeparator = '+')
        }
        assertFailsWith<IllegalArgumentException> {
            CalculatorFormatConfig(groupingSeparator = '×')
        }
    }
}
