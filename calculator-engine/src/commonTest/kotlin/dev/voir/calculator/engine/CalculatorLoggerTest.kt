package dev.voir.calculator.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CalculatorLoggerTest {
    private class RecordingLogger : CalculatorLogger {
        val entries = mutableListOf<Pair<CalculatorLogLevel, String>>()

        override fun log(level: CalculatorLogLevel, message: () -> String) {
            entries += level to message()
        }

        fun messages(level: CalculatorLogLevel): List<String> = entries.filter { it.first == level }.map { it.second }
    }

    @Test
    fun `key presses are logged at debug with outcome and formula`() {
        val logger = RecordingLogger()
        val calculator = Calculator(CalculatorConfig(inputMaxDecimals = 1), logger)

        calculator.input("1.5")
        calculator.inputDigit('7')

        assertEquals(
            listOf(
                "inputDigit('1') accepted; formula '1'",
                "inputDecimalSeparator() accepted; formula '1.'",
                "inputDigit('5') accepted; formula '1.5'",
                "inputDigit('7') rejected (DecimalLimitReached); formula '1.5'",
            ),
            logger.messages(CalculatorLogLevel.Debug),
        )
    }

    @Test
    fun `config change is logged at info`() {
        val logger = RecordingLogger()
        val calculator = Calculator(logger = logger)

        calculator.updateConfig { it.copy(precedence = CalculatorPrecedence.Standard) }

        val info = logger.messages(CalculatorLogLevel.Info).single()
        assertTrue(info.startsWith("Config changed from "), info)
    }

    @Test
    fun `unparsable set formula text is logged as a warning and still throws`() {
        val logger = RecordingLogger()
        val calculator = Calculator(logger = logger)

        assertFailsWith<FormulaSyntaxException> { calculator.setFormula("12+a") }

        assertEquals(
            listOf("setFormula() rejected text: InvalidCharacter at position 3"),
            logger.messages(CalculatorLogLevel.Warning),
        )
    }

    @Test
    fun `unusual evaluation failure is logged at info`() {
        val logger = RecordingLogger()
        val calculator = Calculator(logger = logger)
        calculator.setFormula(List(5) { "9".repeat(38) }.joinToString("*"))

        calculator.evaluate()

        assertEquals(listOf("evaluate() failed: ArithmeticFailure(Multiply)"), logger.messages(CalculatorLogLevel.Info))
    }

    @Test
    fun `division by zero is not logged above debug`() {
        val logger = RecordingLogger()
        val calculator = Calculator(logger = logger)
        calculator.setFormula("1/0")

        calculator.evaluate()

        assertTrue(logger.entries.none { it.first > CalculatorLogLevel.Debug }, logger.entries.toString())
    }

    @Test
    fun `formula text appears only in debug messages`() {
        val logger = RecordingLogger()
        val calculator = Calculator(CalculatorConfig(inputMaxDecimals = 4), logger)
        val secret = "98765"

        calculator.input("$secret.4321*1")
        calculator.updateConfig { it.copy(inputMaxDecimals = 2) }
        calculator.evaluate()
        calculator.trySetFormula("$secret+x")
        assertFailsWith<FormulaSyntaxException> { calculator.setFormula("$secret+x") }
        calculator.setFormula("$secret${"9".repeat(40)}")
        calculator.evaluate()

        val leaks = logger.entries.filter { (level, message) -> level > CalculatorLogLevel.Debug && secret in message }
        assertEquals(emptyList(), leaks)
        assertTrue(logger.messages(CalculatorLogLevel.Debug).any { secret in it })
    }
}
