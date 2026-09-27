package dev.voir.calculator.engine

/**
 * Consistent snapshot of a calculator, published as one value after every change.
 *
 * Prefer [CalculatorApi.state] when a UI shows the formula and the result together: the separate
 * [CalculatorApi.formula] and [CalculatorApi.result] flows are updated one after the other, so a
 * collector combining them can briefly see a new formula with the previous result.
 *
 * Defaults describe a new calculator, which is convenient for UI previews and fakes.
 *
 * @property formula Canonical formula text using ASCII digits, `.`, `+`, `-`, `*`, and `/`.
 * @property result Value of [formula], [CalculatorResult.Empty] while it has no complete number, or
 * the error that prevents evaluating it.
 * @property config Settings the formula was edited and evaluated with.
 * @property isShowingResult Whether [formula] is an evaluated result, so the next digit starts a new
 * number instead of appending to it.
 */
public data class CalculatorState(
    public val formula: String = "",
    public val result: CalculatorResult = CalculatorResult.Empty,
    public val config: CalculatorConfig = CalculatorConfig(),
    public val isShowingResult: Boolean = false,
)
