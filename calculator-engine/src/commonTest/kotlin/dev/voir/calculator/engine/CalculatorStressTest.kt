package dev.voir.calculator.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CalculatorStressTest {
    @Test
    fun `repeatedly replacing operation keeps single stable operator`() {
        val calculator = Calculator()

        calculator.inputDigit('1')
        repeat(1_000) { index ->
            val op = when (index % 4) {
                0 -> CalculatorOperation.Add
                1 -> CalculatorOperation.Subtract
                2 -> CalculatorOperation.Multiply
                else -> CalculatorOperation.Divide
            }
            assertAccepted(calculator.inputOperation(op))
        }

        // Operator replacement should mutate the final token instead of appending a long operator chain.
        calculator.assertFormula("1/")
        calculator.assertSuccess("1")
    }

    @Test
    fun `repeated input and delete returns to empty state`() {
        val calculator = Calculator(CalculatorConfig(inputMaxDecimals = 4))

        repeat(250) {
            calculator.input("123.4+56.7*-8/")
            repeat(calculator.formula.value.length) {
                assertAccepted(calculator.deleteLast())
            }
            calculator.assertFormula("")
            calculator.assertEmpty()
        }
    }

    @Test
    fun `long addition formula evaluates correctly`() {
        val terms = 1_000
        val formula = List(terms) { "1" }.joinToString("+")
        val calculator = Calculator()

        calculator.setFormula(formula)

        calculator.assertSuccess(terms.toString())
    }

    @Test
    fun `long alternating signed formula evaluates correctly`() {
        val formula = buildString {
            append("1000")
            repeat(500) {
                append("+-1")
                append("+2")
            }
        }
        val calculator = Calculator()

        calculator.setFormula(formula)

        calculator.assertSuccess("1500")
    }

    @Test
    fun `long standard precedence formula evaluates correctly`() {
        // Each repeated group is worth 5 under standard precedence: 1 + 6 - 2.
        val formula = List(200) { "1+2*3-4/2" }.joinToString("+")
        val calculator = Calculator(
            CalculatorConfig(precedence = CalculatorPrecedence.Standard, divisionScale = 4)
        )

        calculator.setFormula(formula)

        calculator.assertSuccess("1000")
    }

    @Test
    fun `long decimal formula at maximum precision evaluates`() {
        val formula = List(100) { "1.123456789" }.joinToString("+")
        val calculator = Calculator(CalculatorConfig(inputMaxDecimals = 9))

        calculator.setFormula(formula)

        calculator.assertSuccess("112.3456789")
    }

    @Test
    fun `invalid character near end of long formula is rejected and preserves state`() {
        val calculator = Calculator()
        val valid = List(500) { "1" }.joinToString("+")
        val invalid = "$valid+x"

        calculator.setFormula("2+2")
        val update = calculator.trySetFormula(invalid)

        assertEquals(
            FormulaUpdate.Invalid(FormulaError(FormulaErrorKind.InvalidCharacter, invalid.lastIndex, "x")),
            update,
        )

        // Failed bulk parsing must not partially replace the previous known-good expression.
        calculator.assertFormula("2+2")
        calculator.assertSuccess("4")
    }

    @Test
    fun `division by zero can be recovered from by clearing and entering valid formula`() {
        val calculator = Calculator()

        calculator.setFormula("1/0")
        calculator.assertError(CalculatorError.DivisionByZero)

        calculator.clear()
        calculator.input("7*6")

        calculator.assertFormula("7*6")
        calculator.assertSuccess("42")
    }

    @Test
    fun `deterministic valid formula matrix does not throw`() {
        val operations = listOf("+", "-", "*", "/")
        val values = listOf("1", "2", "-3", "4.5", "-6.25")

        for (a in values) {
            for (op in operations) {
                for (b in values) {
                    val calculator = Calculator(CalculatorConfig(inputMaxDecimals = 2, divisionScale = 6))
                    calculator.setFormula("$a$op$b")
                    assertIs<CalculatorResult.Success>(calculator.result.value)
                }
            }
        }
    }
}
