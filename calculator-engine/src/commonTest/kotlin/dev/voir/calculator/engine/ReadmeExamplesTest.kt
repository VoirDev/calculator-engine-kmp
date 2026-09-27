package dev.voir.calculator.engine

import dev.voir.calculator.engine.format.CalculatorFormatConfig
import dev.voir.calculator.engine.format.CalculatorFormatter
import dev.voir.calculator.engine.format.CalculatorOperationSymbols
import kotlin.test.Test
import kotlin.test.assertEquals

/** Keeps the examples in README.md true; update both together. */
class ReadmeExamplesTest {
    /**
     * One row of the README key table.
     *
     * @property before Formula to start from; typed as keys unless [afterEvaluate] is set.
     * @property key Key to press: a digit, `.`, an operation, `=`, or `<` for delete.
     * @property after Expected formula.
     * @property rejection Expected rejection, or `null` when the key is accepted.
     * @property afterEvaluate Whether [before] is evaluated before [key] is pressed.
     */
    private data class Row(
        val before: String,
        val key: Char,
        val after: String,
        val rejection: InputRejection? = null,
        val afterEvaluate: Boolean = false,
    )

    @Test
    fun `key table matches the engine`() {
        val rows = listOf(
            Row("", '7', "7"),
            Row("0", '0', "0", InputRejection.NoChange),
            Row("", '.', "0."),
            Row("1.5", '.', "1.5", InputRejection.DuplicateDecimalSeparator),
            Row("", '+', "", InputRejection.MissingOperand),
            Row("", '-', "-"),
            Row("5+", '*', "5*"),
            Row("5*", '-', "5*-"),
            Row("5*-", '+', "5+"),
            Row("3.", '+', "3+"),
            Row("10/3", '=', "3.33"),
            Row("10/3", '7', "7", afterEvaluate = true),
            Row("10/3", '<', "3.3", afterEvaluate = true),
            Row("5/0", '=', "5/0"),
        )

        for (row in rows) {
            val calculator = Calculator()
            calculator.input(row.before)
            if (row.afterEvaluate) calculator.evaluate()

            val result = when (row.key) {
                '=' -> {
                    calculator.evaluate()
                    InputResult.Accepted
                }

                '<' -> calculator.deleteLast()
                '.' -> calculator.inputDecimalSeparator()
                in '0'..'9' -> calculator.inputDigit(row.key)
                else -> calculator.inputOperation(CalculatorOperation.fromSymbol(row.key))
            }

            assertEquals(row.after, calculator.formula.value, row.toString())
            assertEquals(row.rejection?.let { InputResult.Rejected(it) } ?: InputResult.Accepted, result, row.toString())
        }
    }

    @Test
    fun `exact results continue into the next calculation`() {
        val calculator = Calculator()

        calculator.input("10/3")
        calculator.evaluate()
        calculator.assertSuccess("3.333333333333")
        calculator.input("*3")
        calculator.evaluate()

        calculator.assertFormula("10")
        calculator.assertSuccess("9.999999999999")
    }

    @Test
    fun `formatting example produces the documented text`() {
        val formatConfig = CalculatorFormatConfig(
            decimalSeparator = ',',
            groupingSeparator = ' ',
            negativeSign = '−',
            maximumFractionDigits = 2,
            minimumFractionDigits = 0,
            operationSymbols = CalculatorOperationSymbols(add = '+', subtract = '−', multiply = '×', divide = '÷'),
            emptyLabel = null,
            errorLabel = "Error",
        )
        val calculator = Calculator()

        calculator.setFormula("1200.5*2")
        val display = CalculatorFormatter.format(calculator.state.value, formatConfig)

        assertEquals("1 200,5 × 2", display.formula)
        assertEquals("2 401", display.result)
        assertEquals("1 200,50", CalculatorFormatter.formatFormula("1200.50", formatConfig))
        assertEquals("1200.5*2", CalculatorFormatter.normalizeFormula("1 200,5 × 2", formatConfig))
    }

    @Test
    fun `precedence examples evaluate as documented`() {
        val chain = Calculator()
        chain.setFormula("2+3*4")
        chain.assertSuccess("20")

        val standard = Calculator(CalculatorConfig(precedence = CalculatorPrecedence.Standard))
        standard.setFormula("2+3*4")
        standard.assertSuccess("14")
    }
}
