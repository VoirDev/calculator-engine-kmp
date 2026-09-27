package dev.voir.calculator.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class CalculatorSetFormulaTest {
    @Test
    fun `set formula accepts valid canonical formula shapes`() {
        val cases = mapOf(
            "" to "",
            "   " to "",
            " 1 + 2 * 3 " to "1+2*3",
            "1+2" to "1+2",
            "-1" to "-1",
            "-1+2" to "-1+2",
            "1+-2" to "1+-2",
            "1--2" to "1--2",
            "1*-2" to "1*-2",
            "1/-2" to "1/-2",
            ".5" to "0.5",
            "-.5" to "-0.5",
            "007" to "7",
            "-007+00.50" to "-7+0.50",
            "0" to "0",
            "1." to "1.",
            "1.+2" to "1+2",
            "1+" to "1+",
            "1+2*" to "1+2*",
        )

        for ((input, expectedFormula) in cases) {
            val calculator = Calculator()

            calculator.setFormula(input)

            calculator.assertFormula(expectedFormula)
        }
    }

    @Test
    fun `set formula rejects unsupported characters`() {
        val invalid = listOf(
            "abc",
            "1,2",
            "1_000",
            "1×2",
            "1÷2",
            "NaN",
            "Infinity",
            "1\u00002",
        )

        for (formula in invalid) {
            assertFailsWith<IllegalArgumentException>(formula) {
                Calculator().setFormula(formula)
            }
        }
    }

    @Test
    fun `set formula rejects non ascii digits`() {
        // Char.isDigit() accepts these, but Decimal cannot read them.
        for (formula in listOf("٣", "３+1", "1+६")) {
            assertFailsWith<IllegalArgumentException>(formula) {
                Calculator().setFormula(formula)
            }
        }
    }

    @Test
    fun `set formula reports error positions in the original text`() {
        val exception = assertFailsWith<FormulaSyntaxException> {
            Calculator().setFormula("1 + 2 + a")
        }

        assertEquals(FormulaError(FormulaErrorKind.InvalidCharacter, 8, "a"), exception.error)
        assertEquals("Invalid character 'a' at position 8", exception.message)
    }

    @Test
    fun `set formula rejects invalid operator positions`() {
        val invalid = listOf(
            "+",
            "*1",
            "/1",
            "1*/2",
            "1++2",
            "1//2",
            "1+*2",
            "1-/2",
        )

        for (formula in invalid) {
            assertFailsWith<IllegalArgumentException>(formula) {
                Calculator().setFormula(formula)
            }
        }
    }

    @Test
    fun `set formula rejects invalid number shapes`() {
        val invalid = listOf(
            ".",
            "-",
            "-.",
            "1..2",
            "1.2.3",
            "1+.",
            "1+-.",
        )

        for (formula in invalid) {
            assertFailsWith<IllegalArgumentException>(formula) {
                Calculator().setFormula(formula)
            }
        }
    }

    @Test
    fun `set formula honors configured input precision`() {
        val calculator = Calculator(CalculatorConfig(inputMaxDecimals = 3))

        calculator.setFormula("1.234+5.678")
        calculator.assertFormula("1.234+5.678")

        assertFailsWith<IllegalArgumentException> {
            calculator.setFormula("1.2345")
        }
        calculator.assertFormula("1.234+5.678")
    }

    @Test
    fun `set formula preserves huge integer operands when decimal conversion fails`() {
        val calculator = Calculator()
        val lhs = "9".repeat(120)
        val rhs = "1".repeat(120)

        calculator.setFormula("$lhs+$rhs")

        calculator.assertFormula("$lhs+$rhs")
        // Parsing is separate from Decimal conversion, so syntactically valid huge values may still error.
        assertIs<CalculatorResult.Error>(calculator.result.value)
    }
}
