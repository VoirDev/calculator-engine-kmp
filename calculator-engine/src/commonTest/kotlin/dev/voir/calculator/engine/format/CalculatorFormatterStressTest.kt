package dev.voir.calculator.engine.format

import dev.voir.calculator.engine.CalculatorResult
import dev.voir.decimal.Decimal
import kotlin.test.Test
import kotlin.test.assertEquals

class CalculatorFormatterStressTest {
    @Test
    fun `format formula handles long alternating operation chain`() {
        val formula = buildString {
            append("1")
            repeat(250) { index ->
                append(
                    when (index % 4) {
                        0 -> "+"
                        1 -> "-"
                        2 -> "*"
                        else -> "/"
                    }
                )
                append(index + 2)
            }
        }

        val formatted = CalculatorFormatter.formatFormula(
            formula
        )

        // The formatted chain should remain display-only text: operators are spaced, operands are preserved.
        assertEquals(250, formatted.count { it == ' ' } / 2)
        assertEquals(formula, CalculatorFormatter.normalizeFormula(formatted))
    }

    @Test
    fun `normalize formula handles long grouped decimal formula`() {
        val config = CalculatorFormatConfig(decimalSeparator = ',', groupingSeparator = ' ')
        val display = List(300) { index ->
            "${index + 1} 234,50"
        }.joinToString(" + ")

        val normalized = CalculatorFormatter.normalizeFormula(display, config)

        // Every group contributes one canonical decimal number and plus separators between groups.
        assertEquals(300, normalized.split('+').size)
        assertEquals("1234.50", normalized.substringBefore('+'))
    }

    @Test
    fun `format result handles large grouped value`() {
        val result = CalculatorResult.Success(Decimal.of("123456789012345.67"))

        assertEquals("123 456 789 012 345.67", CalculatorFormatter.formatResult(result))
    }

    @Test
    fun `format and normalize round trip many generated formulas`() {
        val config = CalculatorFormatConfig(decimalSeparator = ',', groupingSeparator = ' ')

        repeat(100) { index ->
            val canonical = "${index + 1000}.${index % 10}${(index + 1) % 10}+${index + 1}*-${index + 2}.50"
            val formatted = CalculatorFormatter.formatFormula(canonical, config)

            assertEquals(canonical, CalculatorFormatter.normalizeFormula(formatted, config))
        }
    }
}
