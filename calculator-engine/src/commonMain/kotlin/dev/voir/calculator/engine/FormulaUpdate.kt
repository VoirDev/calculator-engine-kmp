package dev.voir.calculator.engine

/** Outcome of [CalculatorApi.trySetFormula]. */
public sealed interface FormulaUpdate {
    /** The formula was parsed and replaced the previous one. */
    public data object Changed : FormulaUpdate

    /** The formula was parsed but is identical to the current one, so nothing was published. */
    public data object Unchanged : FormulaUpdate

    /**
     * The formula could not be parsed and the current one was kept.
     *
     * @property error First problem found, scanning left to right.
     */
    public data class Invalid(public val error: FormulaError) : FormulaUpdate
}
