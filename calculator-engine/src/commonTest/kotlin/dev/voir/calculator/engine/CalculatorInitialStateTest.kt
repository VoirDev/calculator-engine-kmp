package dev.voir.calculator.engine

import dev.voir.decimal.Rounding
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class CalculatorInitialStateTest {
    @Test
    fun `new calculator publishes empty formula and empty result`() {
        val calculator = Calculator()

        calculator.assertFormula("")
        calculator.assertEmpty()
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
        calculator.assertEmpty()
    }

    @Test
    fun `evaluating empty formula keeps it empty`() {
        val calculator = Calculator()

        val evaluated = calculator.evaluate()

        assertEquals(CalculatorResult.Empty, evaluated)
        calculator.assertFormula("")
        calculator.assertEmpty()
    }

    @Test
    fun `clear on empty formula returns false`() {
        assertRejected(Calculator().clear())
    }
}
