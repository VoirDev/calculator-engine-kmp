package dev.voir.calculator.engine

import dev.voir.decimal.Rounding
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class CalculatorInitialStateTest {
    @Test
    fun `new calculator publishes empty formula and zero result`() {
        val calculator = Calculator()

        calculator.assertFormula("")
        calculator.assertSuccess("0")
        assertEquals(CalculatorConfig(), calculator.config.value)
    }

    @Test
    fun `new calculator uses provided initial config`() {
        val config = CalculatorConfig(
            inputMaxDecimals = 4,
            divisionScale = 7,
            precedence = CalculatorPrecedence.Standard,
            rounding = Rounding.HALF_UP,
        )

        val calculator = Calculator(config)

        assertEquals(config, calculator.config.value)
        calculator.assertFormula("")
        calculator.assertSuccess("0")
    }

    @Test
    fun `evaluating empty formula stores zero as continuable formula`() {
        val calculator = Calculator()

        val evaluated = calculator.evaluate()

        assertIs<CalculatorResult.Success>(evaluated)
        calculator.assertFormula("0")
        calculator.assertSuccess("0")
    }

    @Test
    fun `clear on empty formula returns false`() {
        assertFalse(Calculator().clear())
    }
}
