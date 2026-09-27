package dev.voir.calculator.engine.format

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CalculatorFormatterFormulaTest {
    @Test
    fun `format formula returns empty string for empty formula`() {
        assertEquals("", CalculatorFormatter.formatFormula(""))
    }

    @Test
    fun `format formula renders default operation symbols with spaces`() {
        assertEquals("1 + 2", CalculatorFormatter.formatFormula("1+2"))
        assertEquals("5 - 3", CalculatorFormatter.formatFormula("5-3"))
        assertEquals("4 × 6", CalculatorFormatter.formatFormula("4*6"))
        assertEquals("8 ÷ 2", CalculatorFormatter.formatFormula("8/2"))
    }

    @Test
    fun `format formula renders custom operation symbols with spaces`() {
        val config = CalculatorFormatConfig(
            operationSymbols = CalculatorOperationSymbols(
                add = 'p',
                subtract = 'm',
                multiply = 'x',
                divide = 'd',
            )
        )

        assertEquals("1 p 2 m 3 x 4 d 5", CalculatorFormatter.formatFormula("1+2-3*4/5", config))
    }

    @Test
    fun `format formula applies custom decimal separator`() {
        val config = CalculatorFormatConfig(decimalSeparator = ',')

        assertEquals("1,25 + 2,5", CalculatorFormatter.formatFormula("1.25+2.5", config))
        assertEquals("-0,5 × 8", CalculatorFormatter.formatFormula("-0.5*8", config))
    }

    @Test
    fun `format formula never rounds or pads typed numbers`() {
        val config = CalculatorFormatConfig(
            decimalSeparator = ',',
            minimumFractionDigits = 2,
            maximumFractionDigits = 2,
        )

        assertEquals("1 + 2,5", CalculatorFormatter.formatFormula("1+2.5", config))
        assertEquals("1,239", CalculatorFormatter.formatFormula("1.239", config))
    }

    @Test
    fun `format formula keeps grouping while trailing zeros or separator are typed`() {
        assertEquals("1 234.5", CalculatorFormatter.formatFormula("1234.5"))
        assertEquals("1 234.50", CalculatorFormatter.formatFormula("1234.50"))
        assertEquals("1 234.", CalculatorFormatter.formatFormula("1234."))
        assertEquals("-1 234.0", CalculatorFormatter.formatFormula("-1234.0"))
    }

    @Test
    fun `format formula without grouping separator leaves integers ungrouped`() {
        val config = CalculatorFormatConfig(groupingSeparator = null)

        assertEquals("1234567 + 8.5", CalculatorFormatter.formatFormula("1234567+8.5", config))
    }

    @Test
    fun `format formula renders the configured negative sign`() {
        val config = CalculatorFormatConfig(
            negativeSign = '−',
            operationSymbols = CalculatorOperationSymbols(subtract = '−'),
        )

        assertEquals("−5 − 3 × −2", CalculatorFormatter.formatFormula("-5-3*-2", config))
        assertEquals("−", CalculatorFormatter.formatFormula("-", config))
    }

    @Test
    fun `format formula preserves editable trailing decimal zeros`() {
        val config = CalculatorFormatConfig(decimalSeparator = ',')

        assertEquals("1,20 + 3,00", CalculatorFormatter.formatFormula("1.20+3.00", config))
        assertEquals("1, + 2,", CalculatorFormatter.formatFormula("1.+2.", config))
    }

    @Test
    fun `format formula preserves bare negative edit state`() {
        assertEquals("-", CalculatorFormatter.formatFormula("-"))
        assertEquals("- + 1", CalculatorFormatter.formatFormula("-+1"))
    }

    @Test
    fun `format formula zero prefixes leading decimal text`() {
        val config = CalculatorFormatConfig(decimalSeparator = ',')

        assertEquals("0,5", CalculatorFormatter.formatFormula(".5", config))
        assertEquals("-0,5", CalculatorFormatter.formatFormula("-.5", config))
        assertEquals("0, + -0,", CalculatorFormatter.formatFormula(".+-.", config))
    }

    @Test
    fun `format formula applies default grouping separator`() {
        assertEquals("1 234 567 + 8 900", CalculatorFormatter.formatFormula("1234567+8900"))
        assertEquals("1 + 2", CalculatorFormatter.formatFormula("1+2"))
    }

    @Test
    fun `format formula applies custom grouping separator`() {
        val config = CalculatorFormatConfig(groupingSeparator = '_')

        assertEquals("1_234_567 + 8_900", CalculatorFormatter.formatFormula("1234567+8900", config))
    }

    @Test
    fun `format formula tolerates whitespace in canonical input`() {
        assertEquals("1 + 2 × 3", CalculatorFormatter.formatFormula(" 1 + 2 * 3 "))
    }

    @Test
    fun `format formula throws for unsupported canonical characters`() {
        assertFailsWith<IllegalArgumentException> {
            CalculatorFormatter.formatFormula("1,2")
        }
        assertFailsWith<IllegalArgumentException> {
            CalculatorFormatter.formatFormula("1×2")
        }
        assertFailsWith<IllegalArgumentException> {
            CalculatorFormatter.formatFormula("abc")
        }
    }
}
