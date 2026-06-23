package dev.voir.calculator.engine

import dev.voir.decimal.Rounding

/**
 * Runtime settings that control calculator parsing and evaluation.
 *
 * @property maxDecimals Compatibility default used by [inputMaxDecimals].
 * @property inputMaxDecimals Maximum number of fractional digits accepted from input.
 * @property divisionScale Internal Decimal scale used for division.
 * @property precedence Evaluation mode for chained operations.
 * @property rounding Rounding mode used for division and evaluated formula precision.
 */
data class CalculatorConfig(
    /** Compatibility default for [inputMaxDecimals]. */
    val maxDecimals: Int = 2,
    /** Maximum fractional precision accepted by input helpers and parsed formulas. */
    val inputMaxDecimals: Int = maxDecimals,
    /** Internal scale used for division. */
    val divisionScale: Int = inputMaxDecimals + 10,
    /** Chooses simple left-to-right evaluation or standard arithmetic precedence. */
    val precedence: CalculatorPrecedence = CalculatorPrecedence.Chain,
    /** Rounding behavior used for division and evaluated formula precision. */
    val rounding: Rounding = Rounding.HALF_UP,
) {
    init {
        // Keep input precision intentionally small for a basic calculator engine.
        require(maxDecimals in 0..9) { "maxDecimals must be between 0 and 9." }
        require(inputMaxDecimals in 0..9) { "inputMaxDecimals must be between 0 and 9." }
        require(divisionScale >= 0) {
            "divisionScale must be >= 0."
        }
    }
}
