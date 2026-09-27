package dev.voir.calculator.engine

import kotlin.test.Test

class CalculatorInputRejectionTest {
    @Test
    fun `delete and clear on empty formula report nothing to remove`() {
        val calculator = Calculator()

        assertRejected(InputRejection.NothingToRemove, calculator.deleteLast())
        assertRejected(InputRejection.NothingToRemove, calculator.clear())
    }

    @Test
    fun `operation without a left operand reports missing operand`() {
        val calculator = Calculator()

        assertRejected(InputRejection.MissingOperand, calculator.inputOperation(CalculatorOperation.Multiply))
        calculator.input("-")
        assertRejected(InputRejection.MissingOperand, calculator.inputOperation(CalculatorOperation.Add))
    }

    @Test
    fun `second decimal separator reports duplicate separator`() {
        val calculator = Calculator()
        calculator.input("1.")

        assertRejected(InputRejection.DuplicateDecimalSeparator, calculator.inputDecimalSeparator())
    }

    @Test
    fun `decimal separator with zero input decimals reports decimals disabled`() {
        val calculator = Calculator(CalculatorConfig(inputMaxDecimals = 0))

        assertRejected(InputRejection.DecimalsDisabled, calculator.inputDecimalSeparator())
    }

    @Test
    fun `digit beyond input decimals reports decimal limit`() {
        val calculator = Calculator(CalculatorConfig(inputMaxDecimals = 1))
        calculator.input("1.5")

        assertRejected(InputRejection.DecimalLimitReached, calculator.inputDigit('5'))
        calculator.assertFormula("1.5")
    }

    @Test
    fun `digit beyond input digits reports digit limit`() {
        val calculator = Calculator(CalculatorConfig(inputMaxDecimals = 2, inputMaxDigits = 3))
        calculator.input("12.3")

        assertRejected(InputRejection.DigitLimitReached, calculator.inputDigit('4'))
        calculator.assertFormula("12.3")
    }

    @Test
    fun `digit limit applies per number`() {
        val calculator = Calculator(CalculatorConfig(inputMaxDecimals = 0, inputMaxDigits = 2))

        calculator.input("99*99")

        calculator.assertFormula("99*99")
        assertRejected(InputRejection.DigitLimitReached, calculator.inputDigit('9'))
    }

    @Test
    fun `key that leaves the formula unchanged reports no change`() {
        val calculator = Calculator()
        calculator.input("0")

        assertRejected(InputRejection.NoChange, calculator.inputDigit('0'))

        calculator.input("+")
        assertRejected(InputRejection.NoChange, calculator.inputOperation(CalculatorOperation.Add))
    }
}
