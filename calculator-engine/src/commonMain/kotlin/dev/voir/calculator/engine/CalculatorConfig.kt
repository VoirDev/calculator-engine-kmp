package dev.voir.calculator.engine

import dev.voir.decimal.Rounding

/**
 * Runtime settings that control calculator input, parsing, and evaluation.
 *
 * Every default is a constant, so `copy()` changes only the properties you pass.
 *
 * @property inputMaxDecimals Maximum fractional digits in a number, from `0` through `9`. Applies to
 * key input and [CalculatorApi.setFormula], and sets the precision an evaluated result is shown
 * with in the formula. Lowering it rounds numbers already in the formula.
 * @property inputMaxDigits Maximum digits in a number typed with [CalculatorApi.inputDigit],
 * counting a leading zero, from `1` through `38`. It must be greater than [inputMaxDecimals].
 * Formulas set as text and evaluated results are limited only by what `Decimal` can represent.
 * @property divisionScale Maximum fractional digits kept by each division, from `0` through
 * `32767`. It may be lower than [inputMaxDecimals].
 * @property precedence Whether operations run left to right or with standard precedence.
 * @property rounding Rounding for division and for results shown at [inputMaxDecimals].
 */
public data class CalculatorConfig(
    public val inputMaxDecimals: Int = 2,
    public val inputMaxDigits: Int = 15,
    public val divisionScale: Int = 12,
    public val precedence: CalculatorPrecedence = CalculatorPrecedence.Chain,
    public val rounding: Rounding = Rounding.HALF_UP,
) {
    init {
        // Keep input precision intentionally small for a basic calculator engine.
        require(inputMaxDecimals in 0..9) { "inputMaxDecimals must be between 0 and 9, was $inputMaxDecimals." }
        // Decimal holds at most 38 significant digits.
        require(inputMaxDigits in 1..38) { "inputMaxDigits must be between 1 and 38, was $inputMaxDigits." }
        require(inputMaxDigits > inputMaxDecimals) {
            "inputMaxDigits ($inputMaxDigits) must be greater than inputMaxDecimals ($inputMaxDecimals)."
        }
        // Decimal rejects division scales outside this range.
        require(divisionScale in 0..32767) { "divisionScale must be between 0 and 32767, was $divisionScale." }
    }
}
