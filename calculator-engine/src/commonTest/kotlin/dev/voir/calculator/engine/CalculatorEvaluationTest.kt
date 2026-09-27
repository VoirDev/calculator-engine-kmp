package dev.voir.calculator.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CalculatorEvaluationTest {
    @Test
    fun `evaluates basic arithmetic`() {
        assertFormulaResult("1+2", "3")
        assertFormulaResult("5-3", "2")
        assertFormulaResult("4*3", "12")
        assertFormulaResult("8/2", "4")
    }

    @Test
    fun `evaluates negative numbers across all operations`() {
        assertFormulaResult("-1+2", "1")
        assertFormulaResult("1+-2", "-1")
        assertFormulaResult("1--2", "3")
        assertFormulaResult("-2*-3", "6")
        assertFormulaResult("-8/-2", "4")
    }

    @Test
    fun `evaluates decimal arithmetic`() {
        assertFormulaResult("1.2+3.4", "4.6")
        assertFormulaResult("5.5-2.25", "3.25")
        assertFormulaResult("1.5*2", "3")
        assertFormulaResult("10.00/4", "2.5")
    }

    @Test
    fun `live result ignores incomplete trailing edit state`() {
        val calculator = Calculator()

        calculator.input("-")
        calculator.assertEmpty()

        calculator.clear()
        calculator.input("1+")
        calculator.assertSuccess("1")

        calculator.input("2*")
        calculator.assertSuccess("3")
    }

    @Test
    fun `result outside the decimal range produces arithmetic failure instead of throwing`() {
        val calculator = Calculator()
        // Five 38-digit factors exceed the Decimal exponent range of -128..127.
        val formula = List(5) { "9".repeat(38) }.joinToString("*")

        calculator.setFormula(formula)

        calculator.assertError(CalculatorError.ArithmeticFailure(CalculatorOperation.Multiply))
        assertIs<CalculatorResult.Error>(calculator.evaluate())
        calculator.assertFormula(formula)
    }

    @Test
    fun `division by zero produces explicit error for zero representations`() {
        val formulas = listOf("1/0", "1/0.0", "1/-0", "0/0", "-5/0.00")

        for (formula in formulas) {
            val calculator = Calculator()
            calculator.setFormula(formula)

            calculator.assertFormula(formula)
            calculator.assertError(CalculatorError.DivisionByZero)
            assertIs<CalculatorResult.Error>(calculator.evaluate())
            calculator.assertFormula(formula)
        }
    }

    @Test
    fun `successful evaluate shows the result rounded to input precision and keeps it exact`() {
        val calculator = Calculator(CalculatorConfig(inputMaxDecimals = 2, divisionScale = 8))

        calculator.setFormula("1/3")
        val evaluated = calculator.evaluate()

        assertIs<CalculatorResult.Success>(evaluated)
        calculator.assertFormula("0.33")
        calculator.assertSuccess("0.33333333")
    }

    @Test
    fun `evaluate returns the value the live result already showed`() {
        val calculator = Calculator()

        calculator.input("1/3")
        val live = calculator.result.value

        assertEquals(live, calculator.evaluate())
        assertEquals(live, calculator.result.value)
    }

    @Test
    fun `evaluated result keeps full precision for the next calculation`() {
        val calculator = Calculator()

        calculator.input("10/3")
        calculator.evaluate()
        calculator.input("*3")
        calculator.evaluate()

        calculator.assertFormula("10")
        calculator.assertSuccess("9.999999999999")
    }

    @Test
    fun `evaluate without a complete number leaves the formula unchanged`() {
        for (formula in listOf("", "-")) {
            val calculator = Calculator()
            calculator.input(formula)

            val evaluated = calculator.evaluate()

            assertEquals(CalculatorResult.Empty, evaluated)
            calculator.assertFormula(formula)
            calculator.assertEmpty()
        }
    }

    @Test
    fun `evaluate failure leaves formula unchanged`() {
        val calculator = Calculator()

        calculator.setFormula("9/0")
        val evaluated = calculator.evaluate()

        assertIs<CalculatorResult.Error>(evaluated)
        calculator.assertFormula("9/0")
        calculator.assertError(CalculatorError.DivisionByZero)
    }

    @Test
    fun `evaluate normalizes incomplete trailing operation to completed result`() {
        val calculator = Calculator()

        calculator.setFormula("1+2*")
        calculator.assertSuccess("3")
        val evaluated = calculator.evaluate()

        assertIs<CalculatorResult.Success>(evaluated)
        calculator.assertFormula("3")
        calculator.assertSuccess("3")
    }

    private fun assertFormulaResult(
        formula: String,
        expected: String,
        config: CalculatorConfig = CalculatorConfig(),
    ) {
        val calculator = Calculator(config)

        calculator.setFormula(formula)

        calculator.assertFormula(formula)
        calculator.assertSuccess(expected)
    }
}
