package dev.voir.calculator.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CalculatorConfigMutationTest {
    @Test
    fun `set config updates published config and re evaluates formula`() {
        val calculator = Calculator(CalculatorConfig(precedence = CalculatorPrecedence.Chain))

        calculator.setFormula("1+2*3")
        calculator.assertSuccess("9")

        val newConfig = CalculatorConfig(precedence = CalculatorPrecedence.Standard)
        calculator.setConfig(newConfig)

        assertEquals(newConfig, calculator.config.value)
        calculator.assertFormula("1+2*3")
        calculator.assertSuccess("7")
    }

    @Test
    fun `setting equivalent config does not change existing state`() {
        val calculator = Calculator()

        calculator.setFormula("1+2")
        val beforeFormula = calculator.formula.value
        val beforeResult = calculator.result.value

        calculator.setConfig(CalculatorConfig())

        assertEquals(beforeFormula, calculator.formula.value)
        assertEquals(beforeResult, calculator.result.value)
    }

    @Test
    fun `update config applies transform`() {
        val calculator = Calculator()

        calculator.updateConfig {
            it.copy(inputMaxDecimals = 4, divisionScale = 6, precedence = CalculatorPrecedence.Standard)
        }

        assertEquals(4, calculator.config.value.inputMaxDecimals)
        assertEquals(6, calculator.config.value.divisionScale)
        assertEquals(CalculatorPrecedence.Standard, calculator.config.value.precedence)
    }

    @Test
    fun `increasing input precision allows more fractional input`() {
        val calculator = Calculator(CalculatorConfig(inputMaxDecimals = 2))

        calculator.input("1.23")
        assertRejected(calculator.inputDigit('4'))

        calculator.updateConfig { it.copy(inputMaxDecimals = 4) }

        assertAccepted(calculator.inputDigit('4'))
        assertAccepted(calculator.inputDigit('5'))
        assertRejected(calculator.inputDigit('6'))
        calculator.assertFormula("1.2345")
    }

    @Test
    fun `decreasing input precision rounds numbers already in the formula`() {
        val calculator = Calculator(CalculatorConfig(inputMaxDecimals = 4))

        calculator.input("1.2345+2.5")
        calculator.updateConfig { it.copy(inputMaxDecimals = 2) }

        calculator.assertFormula("1.23+2.5")
        calculator.assertSuccess("3.73")
        assertAccepted(calculator.inputDigit('5'))
        assertRejected(calculator.inputDigit('5'))
    }

    @Test
    fun `zero input precision drops a trailing decimal separator`() {
        val calculator = Calculator()

        calculator.input("3.")
        calculator.updateConfig { it.copy(inputMaxDecimals = 0) }

        calculator.assertFormula("3")
    }

    @Test
    fun `precision change re-rounds an evaluated result from its exact value`() {
        val calculator = Calculator(CalculatorConfig(inputMaxDecimals = 1))
        calculator.setFormula("2/3")
        calculator.evaluate()
        calculator.assertFormula("0.7")

        calculator.updateConfig { it.copy(inputMaxDecimals = 3) }

        calculator.assertFormula("0.667")
    }

    @Test
    fun `config change keeps incomplete formula coherent`() {
        val calculator = Calculator()

        calculator.input("-")
        calculator.updateConfig { it.copy(inputMaxDecimals = 0, precedence = CalculatorPrecedence.Standard) }

        calculator.assertFormula("-")
        calculator.assertEmpty()
        assertRejected(calculator.inputDecimalSeparator())
    }
}
