package dev.voir.calculator.engine.format

import dev.voir.calculator.engine.CalculatorResult
import dev.voir.decimal.Decimal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals

class CalculatorFormatterFlowTest {
    @Test
    fun `format formula flow exposes formatted initial value`() {
        val formula = MutableStateFlow("1+2*3")
        val formatted = CalculatorFormatter.formatFormula(
            formula = formula,
            scope = CoroutineScope(EmptyCoroutineContext),
        )

        assertEquals("1 + 2 × 3", formatted.value)
    }

    @Test
    fun `format result flow exposes formatted initial value`() {
        val result = MutableStateFlow<CalculatorResult>(
            CalculatorResult.Success(Decimal.of("1234.567"))
        )
        val formatted = CalculatorFormatter.formatResult(
            result = result,
            scope = CoroutineScope(EmptyCoroutineContext),
            config = CalculatorFormatConfig(maxDecimals = 2),
        )

        assertEquals("1 234.57", formatted.value)
    }
}
