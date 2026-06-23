package dev.voir.calculator.engine

import kotlinx.coroutines.flow.StateFlow

/**
 * Public contract for a reactive expression evaluator.
 *
 * Implementations expose every observable value as a [StateFlow] so UI code can
 * render formulas/results immediately after every accepted input action.
 */
interface CalculatorApi {

    /** Current evaluator configuration. */
    val config: StateFlow<CalculatorConfig>

    /** Formula using canonical symbols: digits, `.`, `+`, `-`, `*`, `/`. */
    val formula: StateFlow<String>

    /** Current calculated result or calculation error for the formula. */
    val result: StateFlow<CalculatorResult>

    /**
     * Replace the current configuration.
     *
     * @param newConfig Complete configuration to use from this point forward.
     *
     * Re-emits result using the new evaluation rules.
     */
    fun setConfig(newConfig: CalculatorConfig)

    /**
     * Update the current configuration using a transform function.
     *
     * @param transform Function that receives the current config and returns the replacement config.
     */
    fun updateConfig(transform: (CalculatorConfig) -> CalculatorConfig)

    /**
     * Append a single decimal digit.
     *
     * @param d Digit character to append.
     * @return `true` if the formula changed.
     * @throws IllegalArgumentException if `d` is not in `'0'..'9'`.
     */
    fun inputDigit(d: Char): Boolean

    /**
     * Append a decimal separator to the current number.
     *
     * Rejected if the current number already has a decimal separator
     * or if `config.inputMaxDecimals == 0`.
     *
     * @return `true` if the formula changed.
     */
    fun inputDecimalSeparator(): Boolean

    /**
     * Append or replace a binary operation.
     *
     * At the start of an empty formula, only subtract is accepted as a sign.
     * If the previous token is already an operation, it is replaced.
     *
     * @param op Operation to append or use as the replacement operation.
     * @return `true` if the formula changed.
     */
    fun inputOperation(op: CalculatorOperation): Boolean

    /**
     * Delete the last entered character or operation.
     *
     * @return `true` if the formula changed.
     */
    fun deleteLast(): Boolean

    /**
     * Clear the current formula.
     *
     * @return `true` if the formula was not already empty.
     */
    fun clear(): Boolean

    /**
     * Evaluate the current formula.
     *
     * On success, replaces the formula with the result so the next
     * operation can continue from that value.
     *
     * @return the evaluated result or calculation error.
     */
    fun evaluate(): CalculatorResult

    /**
     * Replace the current formula from raw canonical text.
     *
     * @param formula Formula text using canonical engine symbols.
     *
     * Supported input symbols are digits, `.`, `+`, `-`, `*`, and `/`.
     */
    fun setFormula(formula: String)

    /**
     * Try to replace the current formula from raw canonical text.
     *
     * @param formula Formula text using canonical engine symbols.
     * @return `true` when parsing succeeded and the formula changed.
     */
    fun trySetFormula(formula: String): Boolean
}
