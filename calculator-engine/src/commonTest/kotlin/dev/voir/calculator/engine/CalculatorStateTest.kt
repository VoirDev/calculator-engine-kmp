package dev.voir.calculator.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CalculatorStateTest {
    @Test
    fun `new calculator state describes an empty formula`() {
        val config = CalculatorConfig(inputMaxDecimals = 4)

        val state = Calculator(config).state.value

        assertEquals(CalculatorState(config = config), state)
    }

    @Test
    fun `state matches the individual flows after every kind of change`() {
        val calculator = Calculator()
        val changes = listOf<() -> Unit>(
            { calculator.input("12+3") },
            { calculator.deleteLast() },
            { calculator.evaluate() },
            { calculator.updateConfig { it.copy(precedence = CalculatorPrecedence.Standard) } },
            { calculator.setFormula("1/0") },
            { calculator.clear() },
        )

        for (change in changes) {
            change()

            val state = calculator.state.value
            assertEquals(calculator.formula.value, state.formula)
            assertEquals(calculator.result.value, state.result)
            assertEquals(calculator.config.value, state.config)
        }
    }

    @Test
    fun `showing result is true only right after evaluate`() {
        val calculator = Calculator()
        calculator.input("2+3")
        assertFalse(calculator.state.value.isShowingResult)

        calculator.evaluate()
        assertTrue(calculator.state.value.isShowingResult)

        calculator.input("*")
        assertFalse(calculator.state.value.isShowingResult)

        calculator.deleteLast()
        assertTrue(calculator.state.value.isShowingResult)

        calculator.input("7")
        assertFalse(calculator.state.value.isShowingResult)
    }

    @Test
    fun `failed evaluate does not show a result`() {
        val calculator = Calculator()
        calculator.setFormula("1/0")

        calculator.evaluate()

        assertFalse(calculator.state.value.isShowingResult)
    }
}
