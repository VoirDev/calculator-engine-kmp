# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project uses
[Semantic Versioning](https://semver.org/).

## [Unreleased]

## [1.0.1] - 2026-10-13

First public release. It is not compatible with the initial implementation that was available as
a local `1.0.0` build; see
[Migrating from the initial implementation](https://github.com/VoirDev/calculator-engine-kmp/blob/main/CHANGELOG.md#migrating-from-the-initial-implementation).

### Added

- `CalculatorApi.state`: one `StateFlow<CalculatorState>` with formula, result, config, and
  `isShowingResult` from the same snapshot.
- `InputResult` and `InputRejection`: every key press reports whether it was accepted and why not.
- `FormulaUpdate`, `FormulaError`, `FormulaErrorKind`, and `FormulaSyntaxException`: formula text
  errors carry their kind and position in the original text.
- `CalculatorResult.Empty` for formulas without a complete number.
- `CalculatorConfig.inputMaxDigits` (default 15) to cap typed numbers.
- `CalculatorLogger` and `CalculatorLogLevel`: optional logging with formula text only at `Debug`.
- `CalculatorDisplay`, `CalculatorFormatter.format(state, config)`, and
  `Flow<CalculatorState>.formatted(config)`, including a variant that follows a
  `Flow<CalculatorFormatConfig>`.
- `CalculatorFormatConfig.negativeSign`, `emptyLabel`, and a nullable `groupingSeparator`; per-error
  labels in `formatResult`.
- `CalculatorOperation.fromSymbolOrNull`.
- API reference generated with Dokka, also published as the javadoc jar.

### Changed

- After `evaluate()` the result keeps full precision: the formula shows it rounded to
  `inputMaxDecimals`, and the next operation continues from the exact value.
- Typing a digit or decimal separator right after `evaluate()` starts a new number.
- `-` after `*` or `/` starts a negative operand, so `5*-3` can be typed.
- Lowering `inputMaxDecimals` rounds numbers already in the formula.
- `setFormula` stores numbers the way typing would produce them (`007` → `7`, `.5` → `0.5`) and
  reports error positions in the original text.
- Formula display shows numbers exactly as typed, never rounded or padded, and keeps grouping while
  trailing zeros are typed.
- Requires `dev.voir:decimal` 1.0.3.

### Fixed

- `10 / 3 = * 3 =` gave `9.99`; it now gives `9.999999999999` (shown as `10`).
- `2 + 3 =` followed by `4` gave `54`.
- Results outside `Decimal`'s range could escape as an uncaught `IllegalArgumentException`; they now
  report `CalculatorError.ArithmeticFailure`.
- Unicode digits such as `٣` were accepted and then failed as `InvalidNumber`.
- `CalculatorConfig.copy(maxDecimals = …)` did not change the input precision, and
  `copy(inputMaxDecimals = …)` did not change the division scale.
- Evaluating an empty formula wrote `0` into it.
- Digits were accepted as format separators, so grouping `'1'` turned `1234` into `11234`.

### Removed

- `CalculatorConfig.maxDecimals`.
- The `iosX64` target (Intel iOS simulators).
- The `StateFlow` + `CoroutineScope` overloads of `CalculatorFormatter.formatFormula` and
  `formatResult`.

## Migrating from the initial implementation

**Dependencies.** Use `dev.voir:decimal:1.0.3`. Intel iOS simulators (`iosX64`) are no longer
supported.

**Configuration.** Remove `maxDecimals`. `divisionScale` now defaults to a constant `12` instead of
`inputMaxDecimals + 10`, so a single `copy(inputMaxDecimals = decimals)` is enough to change the
precision. Typed numbers are now limited to `inputMaxDigits` digits (15 by default).

```kotlin
// Before
config.copy(maxDecimals = decimals, inputMaxDecimals = decimals, divisionScale = decimals + 10)
// After
config.copy(inputMaxDecimals = decimals)
```

**Key presses** return `InputResult` instead of `Boolean`:

```kotlin
// Before
if (calculator.inputDigit('7')) { … }
// After
if (calculator.inputDigit('7').isAccepted) { … }
```

**`trySetFormula`** returns `FormulaUpdate` instead of `Boolean`; the old `true` is
`FormulaUpdate.Changed`. `setFormula` throws `FormulaSyntaxException`, which still extends
`IllegalArgumentException`, and its messages changed.

**Empty results.** An empty formula, or one that is only `-`, now evaluates to
`CalculatorResult.Empty` instead of `Success(0)`. Add the new branch to exhaustive `when`
expressions. Code that reads `Success` values stops seeing a zero after `clear()`, so treat `Empty`
as zero where a cleared keypad should mean zero:

```kotlin
val value = when (result) {
    CalculatorResult.Empty -> Decimal.zero()
    is CalculatorResult.Success -> result.value
    is CalculatorResult.Error -> return
}
```

**Evaluated results.** `CalculatorResult.Success.value` after `evaluate()` is the exact value, not
the value rounded to `inputMaxDecimals`; round it where you store it. `CalculatorOperation.fromSymbol`
throws `IllegalArgumentException` instead of `IllegalStateException`.

**Formatting.**

- `dev.voir.calculator.engine.CalculatorOperationSymbol` is now
  `dev.voir.calculator.engine.format.CalculatorOperationSymbols`, and `forOp` is `symbolFor`.
- `CalculatorFormatConfig.maxDecimals` is now `maximumFractionDigits`. It and
  `minimumFractionDigits` apply only to results.
- Replace the flow helpers with `state.formatted(…)`:

```kotlin
// Before
val formula = CalculatorFormatter.formatFormula(formula = calculator.formula, scope = scope, config = format)
val result = CalculatorFormatter.formatResult(result = calculator.result, scope = scope, config = format)

// After
val display: StateFlow<CalculatorDisplay> = calculator.state
    .formatted(format)
    .stateIn(scope, SharingStarted.WhileSubscribed(5_000), CalculatorFormatter.format(calculator.state.value, format))
```
