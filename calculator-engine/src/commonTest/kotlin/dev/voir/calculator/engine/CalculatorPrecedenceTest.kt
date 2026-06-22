package dev.voir.calculator.engine

import kotlin.test.Test

class CalculatorPrecedenceTest {
    @Test
    fun `chain and standard precedence evaluate same formula differently`() {
        assertFormulaResult("1+2*3", "9", CalculatorPrecedence.Chain)
        assertFormulaResult("1+2*3", "7", CalculatorPrecedence.Standard)

        assertFormulaResult("10-2*3", "24", CalculatorPrecedence.Chain)
        assertFormulaResult("10-2*3", "4", CalculatorPrecedence.Standard)

        assertFormulaResult("2*3+4*5", "50", CalculatorPrecedence.Chain)
        assertFormulaResult("2*3+4*5", "26", CalculatorPrecedence.Standard)
    }

    @Test
    fun `same precedence operators remain left associative`() {
        assertFormulaResult("100/10/2", "5", CalculatorPrecedence.Chain)
        assertFormulaResult("100/10/2", "5", CalculatorPrecedence.Standard)

        assertFormulaResult("100-10-5", "85", CalculatorPrecedence.Chain)
        assertFormulaResult("100-10-5", "85", CalculatorPrecedence.Standard)

        assertFormulaResult("8/2*3", "12", CalculatorPrecedence.Chain)
        assertFormulaResult("8/2*3", "12", CalculatorPrecedence.Standard)
    }

    @Test
    fun `standard precedence handles negative numbers`() {
        assertFormulaResult("-2+3*4", "10", CalculatorPrecedence.Standard)
        assertFormulaResult("-2*-3+4", "10", CalculatorPrecedence.Standard)
        assertFormulaResult("18/-3+2*5", "4", CalculatorPrecedence.Standard)
    }

    @Test
    fun `changing precedence re evaluates existing formula`() {
        val calculator = Calculator(CalculatorConfig(precedence = CalculatorPrecedence.Chain))

        calculator.setFormula("1+2*3")
        calculator.assertSuccess("9")

        calculator.updateConfig { it.copy(precedence = CalculatorPrecedence.Standard) }

        calculator.assertFormula("1+2*3")
        calculator.assertSuccess("7")
    }

    private fun assertFormulaResult(
        formula: String,
        expected: String,
        precedence: CalculatorPrecedence,
    ) {
        val calculator = Calculator(CalculatorConfig(precedence = precedence))

        calculator.setFormula(formula)

        calculator.assertSuccess(expected)
    }
}
