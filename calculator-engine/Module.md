# Module Calculator Engine

Kotlin Multiplatform calculator engine for apps that need calculator-style input, exact decimal
evaluation, and reactive UI state.

Start with [dev.voir.calculator.engine.Calculator]: feed it key presses or formula text, and collect
[dev.voir.calculator.engine.CalculatorApi.state]. Format that state for display with
[dev.voir.calculator.engine.format.CalculatorFormatter].

# Package dev.voir.calculator.engine

The calculator, its configuration, and the values it publishes: state snapshots, results, errors,
and the outcome of every key press and formula update.

# Package dev.voir.calculator.engine.format

Display helpers that turn canonical formulas and results into localized text, and localized text
back into canonical formulas.
