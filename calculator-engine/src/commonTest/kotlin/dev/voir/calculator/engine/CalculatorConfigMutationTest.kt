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
        assertFalse(calculator.inputDigit('4'))

        calculator.updateConfig { it.copy(inputMaxDecimals = 4) }

        assertTrue(calculator.inputDigit('4'))
        assertTrue(calculator.inputDigit('5'))
        assertFalse(calculator.inputDigit('6'))
        calculator.assertFormula("1.2345")
    }

    @Test
    fun `decreasing input precision does not rewrite existing formula but affects future input`() {
        val calculator = Calculator(CalculatorConfig(inputMaxDecimals = 4))

        calculator.input("1.234")
        calculator.updateConfig { it.copy(inputMaxDecimals = 2) }

        calculator.assertFormula("1.234")
        calculator.assertSuccess("1.234")
        assertFalse(calculator.inputDigit('5'))
    }

    @Test
    fun `config change keeps incomplete formula coherent`() {
        val calculator = Calculator()

        calculator.input("-")
        calculator.updateConfig { it.copy(inputMaxDecimals = 0, precedence = CalculatorPrecedence.Standard) }

        calculator.assertFormula("-")
        calculator.assertSuccess("0")
        assertFalse(calculator.inputDecimalSeparator())
    }
}
