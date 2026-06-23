package dev.voir.calculator.engine

import kotlin.test.assertEquals
import kotlin.test.assertIs

internal fun CalculatorApi.assertFormula(expected: String) {
    assertEquals(expected, formula.value)
}

internal fun CalculatorApi.assertSuccess(expected: String) {
    val current = result.value
    assertIs<CalculatorResult.Success>(current)
    assertEquals(expected, stripTrailingZeros(current.value.toPlainString()))
}

internal fun CalculatorApi.assertRawSuccess(expected: String) {
    val current = result.value
    assertIs<CalculatorResult.Success>(current)
    assertEquals(expected, current.value.toPlainString())
}

internal fun CalculatorApi.assertError(expected: CalculatorError) {
    val current = result.value
    assertIs<CalculatorResult.Error>(current)
    assertEquals(expected, current.error)
}

internal fun CalculatorApi.input(text: String): List<Boolean> =
    text.map { c ->
        when (c) {
            in '0'..'9' -> inputDigit(c)
            '.' -> inputDecimalSeparator()
            '+' -> inputOperation(CalculatorOperation.Add)
            '-' -> inputOperation(CalculatorOperation.Subtract)
            '*' -> inputOperation(CalculatorOperation.Multiply)
            '/' -> inputOperation(CalculatorOperation.Divide)
            else -> error("Unsupported test input: '$c'")
        }
    }
