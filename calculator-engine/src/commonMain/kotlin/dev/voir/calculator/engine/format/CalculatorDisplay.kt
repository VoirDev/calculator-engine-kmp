package dev.voir.calculator.engine.format

import dev.voir.calculator.engine.CalculatorError
import dev.voir.calculator.engine.CalculatorState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Display text for one [CalculatorState].
 *
 * @property formula Formatted formula.
 * @property result Formatted result, empty label, or error label.
 * @property isShowingResult Whether [formula] is an evaluated result; copied from the state so a UI
 * can, for example, hide a separate result preview.
 */
public data class CalculatorDisplay(
    public val formula: String,
    public val result: String,
    public val isShowingResult: Boolean,
)

/**
 * Format every calculator snapshot with a fixed configuration.
 *
 * The returned flow is cold; share it with `stateIn`, using
 * [CalculatorFormatter.format] of the current state as the initial value.
 *
 * @param config Display settings.
 * @param errorLabel Text for each calculation error; defaults to [CalculatorFormatConfig.errorLabel].
 * @return Display text for each distinct snapshot.
 */
public fun Flow<CalculatorState>.formatted(
    config: CalculatorFormatConfig = CalculatorFormatConfig(),
    errorLabel: (CalculatorError) -> String = { config.errorLabel },
): Flow<CalculatorDisplay> =
    map { state -> CalculatorFormatter.format(state, config, errorLabel) }.distinctUntilChanged()

/**
 * Format every calculator snapshot with the latest configuration, re-formatting when either changes.
 *
 * Use this when display settings can change at runtime, for example after a locale change.
 *
 * @param config Display settings over time.
 * @param errorLabel Text for each calculation error; receives the configuration in effect.
 * @return Display text for each distinct combination of snapshot and configuration.
 */
public fun Flow<CalculatorState>.formatted(
    config: Flow<CalculatorFormatConfig>,
    errorLabel: (CalculatorError, CalculatorFormatConfig) -> String = { _, current -> current.errorLabel },
): Flow<CalculatorDisplay> =
    combine(this, config) { state, current ->
        CalculatorFormatter.format(state, current) { error -> errorLabel(error, current) }
    }.distinctUntilChanged()
