package dev.voir.calculator.engine

import kotlin.test.Test
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
        calculator.assertSuccess("0")

        calculator.clear()
        calculator.input("1+")
        calculator.assertSuccess("1")

        calculator.input("2*")
        calculator.assertSuccess("3")
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
    fun `successful evaluate replaces formula with input compatible result`() {
        val calculator = Calculator(CalculatorConfig(inputMaxDecimals = 2, divisionScale = 8))

        calculator.setFormula("1/3")
        val evaluated = calculator.evaluate()

        assertIs<CalculatorResult.Success>(evaluated)
        calculator.assertFormula("0.33")
        calculator.assertSuccess("0.33")
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
