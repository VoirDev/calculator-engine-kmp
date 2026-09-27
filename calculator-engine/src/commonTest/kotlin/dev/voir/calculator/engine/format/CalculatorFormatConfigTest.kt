package dev.voir.calculator.engine.format

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
        assertEquals(2, config.maximumFractionDigits)
        assertEquals(Rounding.HALF_UP, config.rounding)
        assertEquals("Error", config.errorLabel)
        assertEquals(CalculatorOperationSymbols(), config.operationSymbols)
    }

    @Test
    fun `format config allows documented boundary values`() {
        CalculatorFormatConfig(maximumFractionDigits = 0)
        CalculatorFormatConfig(maximumFractionDigits = 32767)
        CalculatorFormatConfig(minimumFractionDigits = 0, maximumFractionDigits = 0)
        CalculatorFormatConfig(minimumFractionDigits = 32767, maximumFractionDigits = 32767)
        CalculatorFormatConfig(groupingSeparator = '_')
        CalculatorFormatConfig(groupingSeparator = null)
        CalculatorFormatConfig(negativeSign = '−', operationSymbols = CalculatorOperationSymbols(subtract = '−'))
        CalculatorFormatConfig(decimalSeparator = ',', groupingSeparator = '.')
        CalculatorFormatConfig(
            operationSymbols = CalculatorOperationSymbols(
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
    fun `format config rejects maximum fraction digits outside supported range`() {
        assertFailsWith<IllegalArgumentException> { CalculatorFormatConfig(maximumFractionDigits = -1) }
        assertFailsWith<IllegalArgumentException> { CalculatorFormatConfig(maximumFractionDigits = 32768) }
    }

    @Test
    fun `format config rejects minimum fraction digits outside supported range`() {
        assertFailsWith<IllegalArgumentException> { CalculatorFormatConfig(minimumFractionDigits = -1) }
        assertFailsWith<IllegalArgumentException> {
            CalculatorFormatConfig(minimumFractionDigits = 32768, maximumFractionDigits = 32767)
        }
    }

    @Test
    fun `format config rejects digits as separators signs or symbols`() {
        assertFailsWith<IllegalArgumentException> { CalculatorFormatConfig(groupingSeparator = '1') }
        assertFailsWith<IllegalArgumentException> { CalculatorFormatConfig(decimalSeparator = '5') }
        assertFailsWith<IllegalArgumentException> { CalculatorFormatConfig(negativeSign = '0') }
        assertFailsWith<IllegalArgumentException> {
            CalculatorFormatConfig(operationSymbols = CalculatorOperationSymbols(add = '٣'))
        }
    }

    @Test
    fun `format config allows the negative sign to equal only the subtract symbol`() {
        assertFailsWith<IllegalArgumentException> { CalculatorFormatConfig(negativeSign = '×') }
        assertFailsWith<IllegalArgumentException> { CalculatorFormatConfig(negativeSign = '.') }
        assertFailsWith<IllegalArgumentException> { CalculatorFormatConfig(negativeSign = ' ') }
    }

    @Test
    fun `format config rejects minimum fraction digits greater than max decimals`() {
        assertFailsWith<IllegalArgumentException> {
            CalculatorFormatConfig(minimumFractionDigits = 3, maximumFractionDigits = 2)
        }
    }

    @Test
    fun `format config rejects duplicate operation symbols`() {
        assertFailsWith<IllegalArgumentException> {
            CalculatorFormatConfig(
                operationSymbols = CalculatorOperationSymbols(
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
                operationSymbols = CalculatorOperationSymbols(multiply = ' ')
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
