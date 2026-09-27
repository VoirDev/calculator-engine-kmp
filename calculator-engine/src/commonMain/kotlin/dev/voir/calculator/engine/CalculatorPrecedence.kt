package dev.voir.calculator.engine

/**
 * Controls how formulas with more than one operation are evaluated.
 */
public enum class CalculatorPrecedence {
    /** Left-to-right: (((a op b) op c) op d) — simple-calculator style. */
    Chain,

    /** Standard math precedence (× / before + −) via shunting-yard. */
    Standard,
}
