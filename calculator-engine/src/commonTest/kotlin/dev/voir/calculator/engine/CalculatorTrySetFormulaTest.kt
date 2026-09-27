package dev.voir.calculator.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class CalculatorTrySetFormulaTest {
    @Test
    fun `try set formula reports changed for valid new formula`() {
        val calculator = Calculator()

        assertEquals(FormulaUpdate.Changed, calculator.trySetFormula("1+2"))

        calculator.assertFormula("1+2")
        calculator.assertSuccess("3")
    }

    @Test
    fun `try set formula reports unchanged for same parsed formula`() {
        val calculator = Calculator()

        assertEquals(FormulaUpdate.Changed, calculator.trySetFormula("1+2"))
        assertEquals(FormulaUpdate.Unchanged, calculator.trySetFormula(" 1 + 2 "))

        calculator.assertFormula("1+2")
    }

    @Test
    fun `try set formula reports unchanged when re setting a shown result`() {
        val calculator = Calculator()
        calculator.setFormula("10/4")
        calculator.evaluate()

        assertEquals(FormulaUpdate.Unchanged, calculator.trySetFormula(calculator.formula.value))

        assertEquals(true, calculator.state.value.isShowingResult)
    }

    @Test
    fun `try set formula reports misplaced operation and preserves state`() {
        val calculator = Calculator()

        calculator.setFormula("1+2")
        val update = calculator.trySetFormula("1++2")

        assertEquals(invalid(FormulaErrorKind.UnexpectedOperation, 2, "+"), update)
        calculator.assertFormula("1+2")
        calculator.assertSuccess("3")
    }

    @Test
    fun `try set formula reports malformed decimals and preserves state`() {
        val calculator = Calculator()

        calculator.setFormula("1+2")

        assertEquals(invalid(FormulaErrorKind.UnexpectedNumber, 2, "."), calculator.trySetFormula("1..2"))
        assertEquals(invalid(FormulaErrorKind.UnexpectedNumber, 3, "."), calculator.trySetFormula("1.2.3"))
        calculator.assertFormula("1+2")
        calculator.assertSuccess("3")
    }

    @Test
    fun `try set formula reports too many decimals and preserves state`() {
        val calculator = Calculator(CalculatorConfig(inputMaxDecimals = 2))

        calculator.setFormula("1.23")
        val update = calculator.trySetFormula("1+1.234")

        assertEquals(invalid(FormulaErrorKind.TooManyDecimals, 2, "1.234"), update)
        calculator.assertFormula("1.23")
        calculator.assertSuccess("1.23")
    }

    @Test
    fun `try set formula reports a number without digits`() {
        assertEquals(invalid(FormulaErrorKind.IncompleteNumber, 2, "-"), Calculator().trySetFormula("1*-"))
    }

    @Test
    fun `try set formula accepts syntactically valid division by zero formula`() {
        val calculator = Calculator()

        assertEquals(FormulaUpdate.Changed, calculator.trySetFormula("1/0"))

        calculator.assertFormula("1/0")
        calculator.assertError(CalculatorError.DivisionByZero)
    }

    @Test
    fun `try set formula rejects non ascii digits`() {
        val calculator = Calculator()

        assertEquals(invalid(FormulaErrorKind.InvalidCharacter, 0, "٣"), calculator.trySetFormula("٣+1"))

        calculator.assertFormula("")
    }

    private fun invalid(kind: FormulaErrorKind, position: Int, text: String): FormulaUpdate =
        FormulaUpdate.Invalid(FormulaError(kind, position, text))
}
