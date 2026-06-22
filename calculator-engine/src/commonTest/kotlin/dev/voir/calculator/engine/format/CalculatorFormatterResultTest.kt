package dev.voir.calculator.engine.format

import dev.voir.calculator.engine.CalculatorError
import dev.voir.calculator.engine.CalculatorOperation
import dev.voir.calculator.engine.CalculatorResult
import dev.voir.decimal.Decimal
import kotlin.test.Test
import kotlin.test.assertEquals

class CalculatorFormatterResultTest {
    @Test
    fun `format result returns configured error label for every calculator error`() {
        val config = CalculatorFormatConfig(errorLabel = "ERR")

        assertEquals("ERR", CalculatorFormatter.formatResult(CalculatorResult.Error(CalculatorError.DivisionByZero), config))
        assertEquals("ERR", CalculatorFormatter.formatResult(CalculatorResult.Error(CalculatorError.InvalidNumber("x")), config))
        assertEquals(
            "ERR",
            CalculatorFormatter.formatResult(
                CalculatorResult.Error(CalculatorError.ArithmeticFailure(CalculatorOperation.Divide)),
                config,
            )
        )
    }

    @Test
    fun `format result strips insignificant zeros`() {
        val config = CalculatorFormatConfig(maxDecimals = 4)

        assertEquals("1", CalculatorFormatter.formatResult(success("1.0000"), config))
        assertEquals("1.23", CalculatorFormatter.formatResult(success("1.2300"), config))
        assertEquals("0", CalculatorFormatter.formatResult(success("0.0000"), config))
    }

    @Test
    fun `format result applies max decimals and half up rounding`() {
        val config = CalculatorFormatConfig(maxDecimals = 2)

        assertEquals("1.24", CalculatorFormatter.formatResult(success("1.235"), config))
        assertEquals("-1.24", CalculatorFormatter.formatResult(success("-1.235"), config))
        assertEquals("999.99", CalculatorFormatter.formatResult(success("999.994"), config))
    }

    @Test
    fun `format result applies custom decimal separator`() {
        val config = CalculatorFormatConfig(decimalSeparator = ',', maxDecimals = 3)

        assertEquals("12,346", CalculatorFormatter.formatResult(success("12.3456"), config))
        assertEquals("-0,125", CalculatorFormatter.formatResult(success("-0.125"), config))
    }

    @Test
    fun `format result applies minimum fraction digits`() {
        val config = CalculatorFormatConfig(
            decimalSeparator = ',',
            minimumFractionDigits = 2,
            maxDecimals = 4,
        )

        assertEquals("1,00", CalculatorFormatter.formatResult(success("1"), config))
        assertEquals("1,20", CalculatorFormatter.formatResult(success("1.2"), config))
        assertEquals("1,234", CalculatorFormatter.formatResult(success("1.234"), config))
    }

    @Test
    fun `format result applies default grouping separator`() {
        assertEquals("1 234 567.89", CalculatorFormatter.formatResult(success("1234567.89")))
        assertEquals("1 234 567", CalculatorFormatter.formatResult(success("1234567")))
        assertEquals("-9 876 543.21", CalculatorFormatter.formatResult(success("-9876543.21")))
    }

    @Test
    fun `format result applies custom grouping and decimal separators`() {
        val config = CalculatorFormatConfig(decimalSeparator = ',', groupingSeparator = '.', maxDecimals = 2)

        assertEquals("1.234.567,89", CalculatorFormatter.formatResult(success("1234567.891"), config))
        assertEquals("-9.876.543,2", CalculatorFormatter.formatResult(success("-9876543.2"), config))
    }

    @Test
    fun `format result applies minimum fraction digits with grouping`() {
        val config = CalculatorFormatConfig(
            decimalSeparator = ',',
            groupingSeparator = '.',
            minimumFractionDigits = 2,
            maxDecimals = 4,
        )

        assertEquals("1.234,00", CalculatorFormatter.formatResult(success("1234"), config))
        assertEquals("1.234,50", CalculatorFormatter.formatResult(success("1234.5"), config))
        assertEquals("-1.234,567", CalculatorFormatter.formatResult(success("-1234.567"), config))
    }

    @Test
    fun `format result supports zero max decimals`() {
        val config = CalculatorFormatConfig(maxDecimals = 0)

        assertEquals("2", CalculatorFormatter.formatResult(success("1.5"), config))
        assertEquals("-2", CalculatorFormatter.formatResult(success("-1.5"), config))
        assertEquals("1 000", CalculatorFormatter.formatResult(success("999.5"), config))
    }

    @Test
    fun `format result supports max precision boundary`() {
        val config = CalculatorFormatConfig(maxDecimals = 9)

        assertEquals("0.123456789", CalculatorFormatter.formatResult(success("0.1234567894"), config))
        assertEquals("0.12345679", CalculatorFormatter.formatResult(success("0.1234567900"), config))
    }

    private fun success(text: String): CalculatorResult.Success =
        CalculatorResult.Success(Decimal.of(text))
}
