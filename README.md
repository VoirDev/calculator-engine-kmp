# Calculator Engine KMP

A small Kotlin Multiplatform calculator engine for apps that need reliable calculator-style input,
expression evaluation, and reactive UI state.

The engine keeps the formula valid while the user types, exposes state with `StateFlow`, supports
configurable precision and rounding, and includes display helpers for localized separators and
operation symbols.

## Features

- Kotlin Multiplatform library for JVM, Android, iOS, and macOS arm64.
- Reactive `StateFlow` state for configuration, formula, and result.
- Calculator-style input helpers for digits, decimal separators, operations, delete, clear, and
  evaluate.
- Raw formula support using canonical symbols: `+`, `-`, `*`, `/`, and `.`.
- Configurable input precision, division scale, rounding, and operation precedence.
- Formatting helpers for grouping separators, decimal separators, operation symbols, fixed fraction
  digits, and error labels.
- Decimal arithmetic powered by `dev.voir:decimal`.

## Installation

The package is not published to Maven Central or another public package repository yet. Public
installation will be available in a future release.

When published, the package coordinates will be:

```kotlin
dev.voir:calculator-engine:<version>
```

For now, use a local build from this repository. The default project version is `1.0.0` unless
`releaseVersion` is provided.

### Future Gradle Kotlin DSL

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("dev.voir:calculator-engine:1.0.0")
}
```

### Future Kotlin Multiplatform

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("dev.voir:calculator-engine:1.0.0")
        }
    }
}
```

### Local Development

Publish the library to your local Maven repository:

```bash
./gradlew :calculator-engine:publishToMavenLocal
```

Then consume it with `mavenLocal()` enabled in your app.

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

val result = calculator.evaluate()

when (result) {
    is CalculatorResult.Success -> println(result.value.toPlainString()) // 15
    is CalculatorResult.Error -> println(result.error)
}
```

The current state is always available from flows:

```kotlin
val formula = calculator.formula.value
val result = calculator.result.value
val config = calculator.config.value
```

## Formula Input

For button-driven calculator UIs, prefer the input helpers:

```kotlin
calculator.inputDigit('7')
calculator.inputDecimalSeparator()
calculator.inputDigit('5')
calculator.inputOperation(CalculatorOperation.Multiply)
calculator.inputDigit('2')
calculator.evaluate()
```

For text-driven UIs, use canonical formula text:

```kotlin
calculator.setFormula("10.5*2")
calculator.evaluate()
```

If user input may be invalid, use `trySetFormula`:

```kotlin
val accepted = calculator.trySetFormula("12/4")
```

Canonical formula text uses:

- digits `0` through `9`
- decimal separator `.`
- operations `+`, `-`, `*`, `/`

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
        divisionScale = 12,
        precedence = CalculatorPrecedence.Standard,
        rounding = Rounding.HALF_UP,
    )
)
```

You can update configuration later:

```kotlin
calculator.updateConfig { current ->
    current.copy(precedence = CalculatorPrecedence.Chain)
}
```

### CalculatorConfig

| Option             | Default                      | Description                                                                        |
|--------------------|------------------------------|------------------------------------------------------------------------------------|
| `maxDecimals`      | `2`                          | Compatibility default used by `inputMaxDecimals`.                                  |
| `inputMaxDecimals` | `maxDecimals`                | Maximum fractional digits accepted from input and parsed formulas. Must be `0..9`. |
| `divisionScale`    | `inputMaxDecimals + 10`      | Internal decimal scale used for division. Must be `>= 0`.                          |
| `precedence`       | `CalculatorPrecedence.Chain` | Evaluation mode for chained operations.                                            |
| `rounding`         | `Rounding.HALF_UP`           | Rounding mode used for division and evaluated formula precision.                   |

### Operation Precedence

`CalculatorPrecedence.Chain` evaluates like a basic calculator:

```text
2 + 3 * 4 = 20
```

`CalculatorPrecedence.Standard` uses standard arithmetic precedence:

```text
2 + 3 * 4 = 14
```

## Formatting

The engine stores formulas in canonical form. Use `CalculatorFormatter` when presenting formulas and
results in a UI.

```kotlin
import dev.voir.calculator.engine.CalculatorOperationSymbol
import dev.voir.calculator.engine.format.CalculatorFormatConfig
import dev.voir.calculator.engine.format.CalculatorFormatter

val formatConfig = CalculatorFormatConfig(
    decimalSeparator = ',',
    groupingSeparator = ' ',
    maxDecimals = 2,
    minimumFractionDigits = 0,
    operationSymbols = CalculatorOperationSymbol(
        add = '+',
        subtract = '-',
        multiply = '×',
        divide = '÷',
    ),
    errorLabel = "Error",
)

val displayFormula = CalculatorFormatter.formatFormula("1200.5*2", formatConfig)
val displayResult = CalculatorFormatter.formatResult(calculator.result.value, formatConfig)
```

You can also normalize formatted user input back to canonical formula text:

```kotlin
val raw = CalculatorFormatter.normalizeFormula("1 200,5 × 2", formatConfig)
calculator.trySetFormula(raw)
```

For reactive UI state, build formatted `StateFlow` values:

```kotlin
val formattedFormula = CalculatorFormatter.formatFormula(
    formula = calculator.formula,
    scope = viewModelScope,
    config = formatConfig,
)
```

## Error Handling

Evaluation returns `CalculatorResult`:

```kotlin
when (val result = calculator.evaluate()) {
    is CalculatorResult.Success -> {
        // result.value is a Decimal
    }
    is CalculatorResult.Error -> {
        // result.error is DivisionByZero, InvalidNumber, or ArithmeticFailure
    }
}
```

Calculation errors are also emitted through `calculator.result`.

## Development

Clone the repository and run the tests:

```bash
git clone https://github.com/VoirDev/calculator-engine-kmp.git
cd calculator-engine-kmp
./gradlew test
```

Useful Gradle tasks:

```bash
./gradlew :calculator-engine:test
./gradlew :calculator-engine:compileKotlinJvm
./gradlew :calculator-engine:publishToMavenLocal
```

The project uses Kotlin Multiplatform with a JVM toolchain of 21.

## License

Calculator Engine KMP is licensed under the Apache License, Version 2.0.

See [LICENSE](LICENSE) for the full license text.
