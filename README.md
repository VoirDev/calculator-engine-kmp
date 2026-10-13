# Calculator Engine KMP

[![Maven Central](https://img.shields.io/maven-central/v/dev.voir/calculator-engine)](https://central.sonatype.com/artifact/dev.voir/calculator-engine)

A small Kotlin Multiplatform calculator engine for apps that need reliable calculator-style input,
exact decimal evaluation, and reactive UI state.

The engine keeps the formula valid while the user types, publishes one consistent state snapshot
after every change, and includes display helpers for localized separators, signs, and operation
symbols.

## Features

- Kotlin Multiplatform library for JVM, Android, iOS (arm64 devices and arm64 simulators), and
  macOS arm64.
- One `StateFlow<CalculatorState>` with the formula, result, and config from the same snapshot.
- Calculator-style key input that reports why a key was rejected.
- Exact results: the formula shows a rounded result, but the next operation continues from the exact
  value.
- Canonical formula text for storage, paste, and tests, with precise syntax errors.
- Configurable input precision, digit limit, division scale, rounding, and operation precedence.
- Formatting for grouping and decimal separators, negative sign, operation symbols, and labels.
- Optional, privacy-aware logging with no logging dependency.
- Decimal arithmetic powered by `dev.voir:decimal`.

## Installation

The library is published to Maven Central. Replace `<latest>` with the latest version shown on the
badge above:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("dev.voir:calculator-engine:<latest>")
        }
    }
}
```

See [CHANGELOG.md](CHANGELOG.md) for changes and a migration guide from the initial implementation.

## Quick Start

```kotlin
import dev.voir.calculator.engine.Calculator
import dev.voir.calculator.engine.CalculatorOperation
import dev.voir.calculator.engine.CalculatorResult

val calculator = Calculator()

calculator.inputDigit('1')
calculator.inputDigit('2')
calculator.inputOperation(CalculatorOperation.Add)
calculator.inputDigit('3')

when (val result = calculator.evaluate()) {
    CalculatorResult.Empty -> println("nothing to evaluate yet")
    is CalculatorResult.Success -> println(result.value.toPlainString()) // 15
    is CalculatorResult.Error -> println(result.error)
}
```

The whole calculator is published as one `CalculatorState` after every change. Collect `state` when
a screen shows the formula and the result together, so they always come from the same snapshot:

```kotlin
calculator.state.collect { state ->
    render(
        formula = state.formula,
        result = state.result,
        isShowingResult = state.isShowingResult, // true right after evaluate()
    )
}
```

`formula`, `result`, and `config` are also available as separate flows. They are updated one after
the other, so combining them can briefly pair a new formula with the previous result.

`Calculator` is not thread-safe; drive it from one thread, typically the UI thread. Every call is
synchronous, so `state.value` is up to date as soon as a call returns.

### Jetpack Compose

```kotlin
class CalculatorViewModel : ViewModel() {
    private val calculator = Calculator()
    private val format = CalculatorFormatConfig(decimalSeparator = ',')

    val display: StateFlow<CalculatorDisplay> = calculator.state
        .formatted(format)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CalculatorFormatter.format(calculator.state.value, format),
        )

    fun onDigit(digit: Char) {
        calculator.inputDigit(digit)
    }

    fun onOperation(operation: CalculatorOperation) {
        calculator.inputOperation(operation)
    }

    fun onEquals() {
        calculator.evaluate()
    }
}

@Composable
fun CalculatorDisplayText(viewModel: CalculatorViewModel) {
    val display by viewModel.display.collectAsStateWithLifecycle()

    Column {
        Text(display.formula, style = MaterialTheme.typography.headlineMedium)
        // The formula already is the result right after evaluate(), so skip the preview then.
        if (!display.isShowingResult) Text(display.result)
    }
}
```

### Swift

The library is consumed from Swift through the Kotlin framework like any KMP module. Kotlin `Char`
parameters, such as the digit passed to `inputDigit`, appear in Swift as `unichar` (`UInt16`).
Because calls are synchronous, reading `calculator.state.value` after each call is enough to refresh
a SwiftUI view without collecting the flow.

## Key Input

Each key press returns an `InputResult`, so a UI can react to rejected keys:

```kotlin
when (val input = calculator.inputDigit('7')) {
    InputResult.Accepted -> Unit
    is InputResult.Rejected -> when (input.reason) {
        InputRejection.DecimalLimitReached, InputRejection.DigitLimitReached -> vibrate()
        else -> Unit
    }
}
```

Keys follow common calculator rules:

| Key | Formula before     | Formula after | Notes                                                         |
|-----|--------------------|---------------|---------------------------------------------------------------|
| `7` | empty              | `7`           |                                                               |
| `0` | `0`                | `0`           | Rejected: `NoChange`                                          |
| `.` | empty              | `0.`          |                                                               |
| `.` | `1.5`              | `1.5`         | Rejected: `DuplicateDecimalSeparator`                         |
| `+` | empty              | empty         | Rejected: `MissingOperand`                                    |
| `-` | empty              | `-`           | Starts a negative number                                      |
| `*` | `5+`               | `5*`          | Replaces the operation                                        |
| `-` | `5*`               | `5*-`         | Starts a negative operand after `*` or `/`                    |
| `+` | `5*-`              | `5+`          | Replaces the sign and the operation before it                 |
| `+` | `3.`               | `3+`          | Drops the trailing decimal separator                          |
| `=` | `10/3`             | `3.33`        | Shows the result at `inputMaxDecimals`; keeps `3.333333333333` |
| `7` | `3.33` after `=`   | `7`           | Starts a new number                                           |
| `⌫` | `3.33` after `=`   | `3.3`         | Edits the result as typed text                                |
| `=` | `5/0`              | `5/0`         | Result is `DivisionByZero`; the formula is kept               |

Because the exact value is kept, `10 / 3 = * 3 =` gives `9.999999999999` (shown as `10`) rather than
`9.99`. Lowering `inputMaxDecimals` rounds numbers already in the formula.

## Formula Text

For text-driven UIs, use canonical formula text:

```kotlin
calculator.setFormula("10.5*2")
calculator.evaluate()
```

`setFormula` throws `FormulaSyntaxException` for text it cannot parse. If the text comes from the
user, use `trySetFormula` instead:

```kotlin
when (val update = calculator.trySetFormula(text)) {
    FormulaUpdate.Changed, FormulaUpdate.Unchanged -> Unit
    is FormulaUpdate.Invalid -> showError(update.error.kind, update.error.position)
}
```

Canonical formula text uses:

- ASCII digits `0` through `9`
- decimal separator `.`
- operations `+`, `-`, `*`, `/`

Whitespace is ignored, and error positions refer to the original text. Numbers are stored the way
typing would produce them, so `007` becomes `7` and `.5` becomes `0.5`.

## Configuration

Create a calculator with custom evaluation rules:

```kotlin
import dev.voir.calculator.engine.Calculator
import dev.voir.calculator.engine.CalculatorConfig
import dev.voir.calculator.engine.CalculatorPrecedence
import dev.voir.decimal.Rounding

val calculator = Calculator(
    CalculatorConfig(
        inputMaxDecimals = 4,
        inputMaxDigits = 12,
        divisionScale = 12,
        precedence = CalculatorPrecedence.Standard,
        rounding = Rounding.HALF_UP,
    )
)
```

You can update configuration later:

```kotlin
calculator.updateConfig { current ->
    current.copy(inputMaxDecimals = currency.decimalDigits)
}
```

| Option             | Default                      | Description                                                                                                                     |
|--------------------|------------------------------|---------------------------------------------------------------------------------------------------------------------------------|
| `inputMaxDecimals` | `2`                          | Maximum fractional digits in typed and parsed numbers, and the precision results are shown with in the formula. Must be `0..9`. |
| `inputMaxDigits`   | `15`                         | Maximum digits in a typed number, counting a leading zero. Must be `1..38` and greater than `inputMaxDecimals`.                 |
| `divisionScale`    | `12`                         | Maximum fractional digits kept by each division. Must be `0..32767`.                                                            |
| `precedence`       | `CalculatorPrecedence.Chain` | Evaluation mode for chained operations.                                                                                         |
| `rounding`         | `Rounding.HALF_UP`           | Rounding mode used for division and for results shown at `inputMaxDecimals`.                                                    |

Every default is a constant, so `copy()` changes only the options you pass.

`CalculatorPrecedence.Chain` evaluates left to right like a basic calculator (`2 + 3 * 4 = 20`);
`CalculatorPrecedence.Standard` uses standard arithmetic precedence (`2 + 3 * 4 = 14`).

## Formatting

The engine stores formulas in canonical form. Use `CalculatorFormatter` when presenting formulas and
results in a UI.

```kotlin
import dev.voir.calculator.engine.format.CalculatorFormatConfig
import dev.voir.calculator.engine.format.CalculatorFormatter
import dev.voir.calculator.engine.format.CalculatorOperationSymbols

val formatConfig = CalculatorFormatConfig(
    decimalSeparator = ',',
    groupingSeparator = ' ',      // null disables grouping
    negativeSign = '−',
    maximumFractionDigits = 2,    // results only
    minimumFractionDigits = 0,    // results only
    operationSymbols = CalculatorOperationSymbols(
        add = '+',
        subtract = '−',
        multiply = '×',
        divide = '÷',
    ),
    emptyLabel = null,            // null shows zero while the formula is empty
    errorLabel = "Error",
)

calculator.setFormula("1200.5*2")
val display = CalculatorFormatter.format(calculator.state.value, formatConfig)
display.formula // "1 200,5 × 2"
display.result  // "2 401"
```

Formula numbers are always shown exactly as typed, so `1200.50` stays `1 200,50` while the user is
typing. Only results are rounded and padded with `maximumFractionDigits` and
`minimumFractionDigits`.

Give each error its own text with `errorLabel`:

```kotlin
CalculatorFormatter.formatResult(calculator.result.value, formatConfig) { error ->
    when (error) {
        CalculatorError.DivisionByZero -> "Cannot divide by zero"
        else -> "Error"
    }
}
```

Normalize formatted user input, such as pasted text, back to canonical formula text:

```kotlin
val raw = CalculatorFormatter.normalizeFormula("1 200,5 × 2", formatConfig)
calculator.trySetFormula(raw)
```

For reactive UI state, format the state flow as in the [Compose example](#jetpack-compose). Pass a
`Flow<CalculatorFormatConfig>` instead of a value when display settings can change, for example with
the locale.

## Error Handling

Evaluation returns `CalculatorResult`, which is also published through `state` and `result`:

```kotlin
when (val result = calculator.evaluate()) {
    CalculatorResult.Empty -> {
        // The formula has no complete number yet, for example "" or "-".
    }
    is CalculatorResult.Success -> {
        // result.value is the exact Decimal; format it for display.
    }
    is CalculatorResult.Error -> {
        // result.error is DivisionByZero, InvalidNumber, or ArithmeticFailure.
    }
}
```

`InvalidNumber` means a number `Decimal` cannot represent, such as more than 38 significant digits in
text passed to `setFormula`. `ArithmeticFailure` means a result outside `Decimal`'s range.

## Logging

The engine has no logging dependency. Pass a `CalculatorLogger` to forward its diagnostics to the
logger your app already uses; the default, `CalculatorLogger.None`, discards everything without
building messages.

| Level     | What is logged                                                                      |
|-----------|-------------------------------------------------------------------------------------|
| `Debug`   | Every key press with its outcome, formula changes, and evaluations                  |
| `Info`    | Configuration changes and unusual evaluation failures (overflow, unreadable number) |
| `Warning` | Unparsable text passed to `setFormula`, which usually means an integration bug      |
| `Error`   | Not used by the engine today                                                        |

Formula text can be sensitive, for example amounts in a finance app, so it only appears in `Debug`
messages. Messages are lambdas: call `message()` only for levels you keep.

With [Kermit](https://github.com/touchlab/Kermit) in common code:

```kotlin
val log = Logger.withTag("Calculator")

val calculator = Calculator(
    logger = { level, message ->
        when (level) {
            CalculatorLogLevel.Debug -> log.d { message() }
            CalculatorLogLevel.Info -> log.i { message() }
            CalculatorLogLevel.Warning -> log.w { message() }
            CalculatorLogLevel.Error -> log.e { message() }
        }
    },
)
```

With Android's `Log`, keeping only warnings and errors in release builds:

```kotlin
val logger = CalculatorLogger { level, message ->
    if (level >= CalculatorLogLevel.Warning || BuildConfig.DEBUG) {
        Log.println(if (level >= CalculatorLogLevel.Warning) Log.WARN else Log.DEBUG, "Calculator", message())
    }
}
```

## Development

Clone the repository and run the checks:

```bash
git clone https://github.com/VoirDev/calculator-engine-kmp.git
cd calculator-engine-kmp
./gradlew check
```

Useful Gradle tasks:

```bash
./gradlew :calculator-engine:allTests         # tests on every target the host can run
./gradlew :calculator-engine:jvmTest          # fastest feedback loop
./gradlew :calculator-engine:checkKotlinAbi   # fails when the public API changed
./gradlew :calculator-engine:updateKotlinAbi  # accept an intended public API change
./gradlew :calculator-engine:dokkaGenerate    # API reference in calculator-engine/build/dokka/html
./gradlew :calculator-engine:publishToMavenLocal
```

The project uses Kotlin Multiplatform in explicit API mode with a JVM toolchain of 21. Gradle
downloads the JDK automatically when it is not installed. The public API is recorded in
`calculator-engine/api`; `check` fails when it changes, so run `updateKotlinAbi` and commit the dump
together with an intended API change. iOS and macOS tests need a macOS host.

## Continuous Integration

The `Verify` workflow (`.github/workflows/verify.yml`) runs `./gradlew check` on macOS for every pull
request to `main`, so iOS and macOS tests run as well. Make `Verify / Build and check whole
repository` a required check in the `main` ruleset.

Adding `[skip verify]` to a pull request body skips the check; remove it to run the check again.

## Releasing

The `Release` workflow (`.github/workflows/release.yml`) publishes to Maven Central. The release
version is `projectVersion` in the root `build.gradle.kts`.

1. Run `Release` with mode `prepare-release-pr`. It opens a `release/vX.Y.Z` pull request that bumps
   the patch version, or sets the version given in the `version` input.
2. Merge the pull request. The workflow publishes `dev.voir:calculator-engine`, pushes tag `vX.Y.Z`,
   and creates a GitHub release from the pull request description.

Mode `publish-current` publishes the version already on `main` without a release pull request.

## License

Calculator Engine KMP is licensed under the Apache License, Version 2.0.

See [LICENSE](LICENSE) for the full license text.
