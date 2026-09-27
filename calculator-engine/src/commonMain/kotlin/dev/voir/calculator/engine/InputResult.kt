package dev.voir.calculator.engine

/**
 * Outcome of one key press, such as [CalculatorApi.inputDigit] or [CalculatorApi.deleteLast].
 *
 * A rejected key leaves the formula unchanged; UIs can use the [InputRejection] to give feedback
 * such as a haptic tick or a hint.
 */
public sealed interface InputResult {
    /** Whether the key changed the formula. */
    public val isAccepted: Boolean

    /** The key changed the formula. */
    public data object Accepted : InputResult {
        override val isAccepted: Boolean get() = true
    }

    /**
     * The key was ignored.
     *
     * @property reason Why the key could not be applied.
     */
    public data class Rejected(public val reason: InputRejection) : InputResult {
        override val isAccepted: Boolean get() = false
    }
}

/** Reason a key press left the formula unchanged. */
public enum class InputRejection {
    /** The formula is empty, so there is nothing to delete or clear. */
    NothingToRemove,

    /**
     * An operation needs a complete number before it, for example at the start of the formula or
     * after a bare `-`.
     */
    MissingOperand,

    /** The current number already has a decimal separator. */
    DuplicateDecimalSeparator,

    /** Decimal input is disabled because [CalculatorConfig.inputMaxDecimals] is `0`. */
    DecimalsDisabled,

    /** The current number already has [CalculatorConfig.inputMaxDecimals] fractional digits. */
    DecimalLimitReached,

    /** The current number already has [CalculatorConfig.inputMaxDigits] digits. */
    DigitLimitReached,

    /** The key would not change the formula, such as a second leading zero or the same operation again. */
    NoChange,
}
