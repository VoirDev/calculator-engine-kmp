package dev.voir.calculator.engine

import kotlin.test.Test

class CalculatorRoundingAndScaleTest {
    @Test
    fun `division uses configured scale for live result`() {
        Calculator(CalculatorConfig(inputMaxDecimals = 2, divisionScale = 0)).apply {
            setFormula("1/2")
            assertRawSuccess("1")
        }

        Calculator(CalculatorConfig(inputMaxDecimals = 2, divisionScale = 1)).apply {
            setFormula("1/3")
            assertRawSuccess("0.3")
        }

        Calculator(CalculatorConfig(inputMaxDecimals = 2, divisionScale = 4)).apply {
            setFormula("2/3")
            assertRawSuccess("0.6667")
        }
    }

    @Test
    fun `evaluate shows formula at input max decimals and keeps exact result`() {
        val calculator = Calculator(CalculatorConfig(inputMaxDecimals = 3, divisionScale = 8))

        calculator.setFormula("2/3")
        calculator.assertRawSuccess("0.66666667")
        calculator.evaluate()

        calculator.assertFormula("0.667")
        calculator.assertRawSuccess("0.66666667")
    }

    @Test
    fun `division scale can be lower than input precision`() {
        val calculator = Calculator(CalculatorConfig(inputMaxDecimals = 8, divisionScale = 2))

        calculator.setFormula("1/3")

        calculator.assertRawSuccess("0.33")
        calculator.evaluate()
        calculator.assertFormula("0.33")
    }

    @Test
    fun `division scale can be higher than input precision`() {
        val calculator = Calculator(CalculatorConfig(inputMaxDecimals = 2, divisionScale = 8))

        calculator.setFormula("1/8")

        calculator.assertRawSuccess("0.125")
        calculator.evaluate()
        calculator.assertFormula("0.13")
    }

    @Test
    fun `multiplication can produce more decimals than the formula shows after evaluate`() {
        val calculator = Calculator(CalculatorConfig(inputMaxDecimals = 2))

        calculator.setFormula("1.23*4.56")
        calculator.assertSuccess("5.6088")

        calculator.evaluate()

        calculator.assertFormula("5.61")
        calculator.assertSuccess("5.6088")
    }

    @Test
    fun `half up rounding applies to negative evaluated formula`() {
        val calculator = Calculator(CalculatorConfig(inputMaxDecimals = 0, divisionScale = 1))

        calculator.setFormula("-5/2")
        calculator.assertRawSuccess("-2.5")
        calculator.evaluate()

        calculator.assertFormula("-3")
        calculator.assertRawSuccess("-2.5")
    }
}
