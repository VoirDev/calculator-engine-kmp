package dev.voir.calculator.engine.format

import dev.voir.calculator.engine.Calculator
import dev.voir.calculator.engine.CalculatorConfig
import dev.voir.calculator.engine.CalculatorError
import dev.voir.calculator.engine.CalculatorPrecedence
import dev.voir.calculator.engine.CalculatorState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CalculatorFormatterFlowTest {
    @Test
    fun `formatted state exposes formula and result from the same snapshot`() = runTest {
        val calculator = Calculator()
        calculator.setFormula("1234.5*2")

        val display = calculator.state.formatted().first()

        assertEquals(CalculatorDisplay(formula = "1 234.5 × 2", result = "2 469", isShowingResult = false), display)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `formatted state re formats when the display config changes`() = runTest {
        val calculator = Calculator()
        calculator.setFormula("1234.5")
        val config = MutableStateFlow(CalculatorFormatConfig())
        val displays = mutableListOf<CalculatorDisplay>()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            calculator.state.formatted(config).toList(displays)
        }
        config.value = CalculatorFormatConfig(decimalSeparator = ',', groupingSeparator = '.')
        calculator.setFormula("1234.5+1")

        assertEquals(listOf("1 234.5", "1.234,5", "1.234,5 + 1"), displays.map { it.formula })
    }

    @Test
    fun `formatted state uses per error labels`() = runTest {
        val calculator = Calculator()
        calculator.setFormula("1/0")

        val display = calculator.state.formatted { error ->
            if (error == CalculatorError.DivisionByZero) "Cannot divide by zero" else "Error"
        }.first()

        assertEquals("Cannot divide by zero", display.result)
    }

    @Test
    fun `formatted state skips snapshots that format identically`() = runTest {
        val states = flowOf(
            CalculatorState(formula = "1"),
            // A different state whose display text is the same.
            CalculatorState(formula = "1", config = CalculatorConfig(precedence = CalculatorPrecedence.Standard)),
            CalculatorState(formula = "12"),
        )

        val displays = states.formatted().toList()

        assertEquals(listOf("1", "12"), displays.map { it.formula })
    }
}
