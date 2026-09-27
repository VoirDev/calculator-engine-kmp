package dev.voir.calculator.engine

/**
 * Receives diagnostic messages from a [Calculator].
 *
 * The engine has no logging dependency; forward messages to Kermit, Timber, `os_log`, or any other
 * logger. Messages are built lazily, so a logger that ignores a level should not call `message`.
 *
 * Formula text can be sensitive (for example amounts in a finance app), so the engine includes it
 * only in [CalculatorLogLevel.Debug] messages.
 *
 * Implementations are called synchronously on the thread that drives the calculator and should
 * return quickly.
 */
public fun interface CalculatorLogger {
    /**
     * Record one message.
     *
     * @param level Severity of the message.
     * @param message Builds the message text; call it only when [level] is enabled.
     */
    public fun log(level: CalculatorLogLevel, message: () -> String)

    /** Loggers provided by the library. */
    public companion object {
        /** Discards every message without building it. The default for [Calculator]. */
        public val None: CalculatorLogger = CalculatorLogger { _, _ -> }
    }
}

/** Severity of a [CalculatorLogger] message. */
public enum class CalculatorLogLevel {
    /** Every key press, formula change, and evaluation, including formula text. */
    Debug,

    /** Configuration changes and unusual evaluation failures; never includes formula text. */
    Info,

    /** Probable integration bugs, such as unparsable text passed to [CalculatorApi.setFormula]. */
    Warning,

    /** Failures that leave the calculator unusable. The engine currently reports none. */
    Error,
}
