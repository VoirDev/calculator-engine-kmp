package dev.voir.calculator.engine.format

import kotlin.test.Test
import kotlin.test.assertEquals

class CalculatorFormatterNormalizeTest {
    @Test
    fun `normalize formula removes whitespace and default grouping`() {
        assertEquals("1234+5678", CalculatorFormatter.normalizeFormula("1 234 + 5 678"))
        assertEquals("1+2*3/4", CalculatorFormatter.normalizeFormula(" 1 + 2 × 3 ÷ 4 "))
    }

    @Test
    fun `normalize formula converts custom decimal separator to canonical dot`() {
        val config = CalculatorFormatConfig(decimalSeparator = ',', groupingSeparator = ' ')

        assertEquals("1234.56+7.89", CalculatorFormatter.normalizeFormula("1 234,56 + 7,89", config))
    }

    @Test
    fun `normalize formula converts custom operation symbols to canonical operators`() {
        val config = CalculatorFormatConfig(
            groupingSeparator = '_',
            operationSymbols = CalculatorOperationSymbols(
                add = 'p',
                subtract = 'm',
                multiply = 'x',
                divide = 'd',
            )
        )

        assertEquals("1+2-3*4/5", CalculatorFormatter.normalizeFormula("1p2m3x4d5", config))
    }

    @Test
    fun `normalize formula preserves unknown non whitespace characters`() {
        assertEquals("1_000+abc", CalculatorFormatter.normalizeFormula("1_000 + abc"))
    }

    @Test
    fun `normalize formula removes whitespace independently from grouping separator`() {
        val config = CalculatorFormatConfig(groupingSeparator = '_')

        assertEquals("123+456", CalculatorFormatter.normalizeFormula("1 2 3 + 4 5 6", config))
    }

    @Test
    fun `format and normalize round trip common display formula`() {
        val config = CalculatorFormatConfig(decimalSeparator = ',', groupingSeparator = ' ')
        val canonical = "-1234.50+6.7*8/2"

        val formatted = CalculatorFormatter.formatFormula(canonical, config)
        val normalized = CalculatorFormatter.normalizeFormula(formatted, config)

        assertEquals(canonical, normalized)
    }

    @Test
    fun `normalize formula maps a distinct negative sign to canonical minus`() {
        val config = CalculatorFormatConfig(negativeSign = '−')

        assertEquals("-5--3", CalculatorFormatter.normalizeFormula("−5 - −3", config))
    }

    @Test
    fun `normalize formula keeps spaces out when grouping is disabled`() {
        val config = CalculatorFormatConfig(groupingSeparator = null)

        assertEquals("1234+5", CalculatorFormatter.normalizeFormula("1234 + 5", config))
    }

    @Test
    fun `format and normalize round trip with unicode minus for sign and subtraction`() {
        val config = CalculatorFormatConfig(negativeSign = '−', operationSymbols = CalculatorOperationSymbols(subtract = '−'))
        val canonical = "-1234.5--2*-3"

        val formatted = CalculatorFormatter.formatFormula(canonical, config)

        assertEquals(canonical, CalculatorFormatter.normalizeFormula(formatted, config))
    }
}
