package dev.voir.calculator.engine

import dev.voir.decimal.Decimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A reactive expression-evaluator.
 *
 * The class enforces a valid math formula at every input step:
 * - You cannot insert two decimal separators into one number.
 * - You cannot start with a binary operator (only `-` is accepted as a sign).
 * - Pressing an operator twice replaces the previous one (calc-style).
 * - Decimal digits are capped at [CalculatorConfig.inputMaxDecimals].
 *
 * The class is intended to be driven from a single thread (typically the UI
 * thread). All mutating functions return `true` when the input was accepted.
 *
 * @param initialConfig Configuration used for the initial state and all input,
 * parsing, and evaluation until [setConfig] or [updateConfig] is called.
 */
class Calculator(initialConfig: CalculatorConfig = CalculatorConfig()) : CalculatorApi {

    // -- internal token model --------------------------------------------------
    /**
     * Minimal token model for the expression currently being edited.
     *
     * Keeping numbers as text preserves in-progress user input such as `0.`,
     * `-`, or `1.20` until evaluation chooses to normalize it.
     */
    private sealed interface Token {
        /**
         * Number text in canonical raw form.
         *
         * @property text Raw number text such as `12`, `-3.4`, `-`, or `0.`.
         */
        data class Num(val text: String) : Token

        /**
         * Binary operation token between two number tokens.
         *
         * @property op Operation represented by this token.
         */
        data class Op(val op: CalculatorOperation) : Token
    }

    /**
     * Internal result wrapper that keeps arithmetic failures distinct from zero.
     */
    private sealed interface EvalResult {
        /**
         * Successful evaluation.
         *
         * @property value Decimal result for the expression.
         */
        data class Success(val value: Decimal) : EvalResult

        /** Failed evaluation with a public error value. */
        data class Failure(val error: CalculatorError) : EvalResult
    }

    // -- reactive state --------------------------------------------------------
    /** Mutable backing config; exposed as read-only [StateFlow] through [config]. */
    private val _config = MutableStateFlow(initialConfig)
    override val config: StateFlow<CalculatorConfig> get() = _config.asStateFlow()

    /** Current expression token list; this is the single source of truth. */
    private var tokens: List<Token> = emptyList()

    /** Mutable backing formula; emitted after every accepted mutation. */
    private val _formula = MutableStateFlow("")
    override val formula: StateFlow<String> get() = _formula.asStateFlow()

    /** Mutable backing result; emitted after every accepted mutation. */
    private val _result =
        MutableStateFlow<CalculatorResult>(CalculatorResult.Success(Decimal.zero()))
    override val result: StateFlow<CalculatorResult> get() = _result.asStateFlow()

    init {
        // Publish the initial empty formula/result so every flow has a coherent value.
        emit()
    }

    // -- configuration ---------------------------------------------------------

    /**
     * Replace the calculator configuration and re-emit all derived state.
     *
     * @param newConfig Complete configuration to use for later input, parsing, and evaluation.
     */
    override fun setConfig(newConfig: CalculatorConfig) {
        // Avoid unnecessary re-evaluation when the config is unchanged.
        if (_config.value == newConfig) return
        _config.value = newConfig
        emit()
    }

    /**
     * Apply a caller-provided config transform and publish the result.
     *
     * @param transform Function that maps the current config to a new config.
     */
    override fun updateConfig(transform: (CalculatorConfig) -> CalculatorConfig) =
        setConfig(transform(_config.value))

    // -- input -----------------------------------------------------------------

    /**
     * Append a single decimal digit.
     *
     * @param d Digit character to append.
     * @return `true` if the formula changed.
     */
    override fun inputDigit(d: Char): Boolean {
        // Fail fast for programmer errors; UI callers should only pass one digit.
        require(d in '0'..'9') { "Not a digit: '$d'" }
        val cfg = _config.value
        return mutate { current ->
            val list = current.toMutableList()
            when (val last = list.lastOrNull()) {
                // A new number starts after an empty formula or a previous operator.
                null, is Token.Op -> list.add(Token.Num(d.toString()))
                is Token.Num -> {
                    // Existing number text is preserved unless precision/leading-zero rules reject it.
                    val newText =
                        appendDigitToNumber(last.text, d, cfg.inputMaxDecimals)
                            ?: return@mutate null
                    list[list.lastIndex] = Token.Num(newText)
                }
            }
            list
        }
    }

    /**
     * Append a decimal separator to the current editable number.
     *
     * @return `true` if the separator was accepted and the formula changed.
     */
    override fun inputDecimalSeparator(): Boolean {
        // With zero fractional digits configured, decimal separators are never accepted.
        if (_config.value.inputMaxDecimals == 0) return false
        return mutate { current ->
            val list = current.toMutableList()
            when (val last = list.lastOrNull()) {
                // Starting with "." is normalized to "0." for easier rendering/evaluation.
                null, is Token.Op -> list.add(Token.Num("0."))
                is Token.Num -> {
                    // Only one decimal separator is allowed in a number token.
                    if ('.' in last.text) return@mutate null
                    val newText = when (last.text) {
                        // A negative sign becomes a negative zero-prefixed decimal.
                        "-" -> "-0."
                        "" -> "0."
                        else -> last.text + "."
                    }
                    list[list.lastIndex] = Token.Num(newText)
                }
            }
            list
        }
    }

    /**
     * Append (or replace) a binary operation.
     *  - At the start of an empty formula, only `Subtract` is accepted as a sign.
     *  - If the previous token is also an operator, it is replaced.
     *  - Trailing `.` on the previous number is normalized away (`"3." +` → `"3" +`).
     *
     * @param op Operation to append or use as the replacement operation.
     * @return `true` if the formula changed.
     */
    override fun inputOperation(op: CalculatorOperation): Boolean {
        return mutate { current ->
            val list = current.toMutableList()
            when (val last = list.lastOrNull()) {
                null -> {
                    // Let "-" start a negative number; other binary operators need a left operand.
                    if (op == CalculatorOperation.Subtract) list.add(Token.Num("-")) else return@mutate null
                }

                // Pressing an operator after an operator behaves like replacement.
                is Token.Op -> list[list.lastIndex] = Token.Op(op)
                is Token.Num -> {
                    // A bare sign is still an in-progress number, not a complete left operand.
                    if (last.text == "-" || last.text.isEmpty()) return@mutate null
                    // Commit `3.` as `3` before adding the operation token.
                    val normalized =
                        if (last.text.endsWith('.')) last.text.dropLast(1) else last.text
                    list[list.lastIndex] = Token.Num(normalized)
                    list.add(Token.Op(op))
                }
            }
            list
        }
    }

    /**
     * Backspace one character.
     *
     * @return `true` if a digit, sign, decimal separator, or operation was removed.
     */
    override fun deleteLast(): Boolean {
        return mutate { current ->
            // Nothing to delete from an empty expression.
            if (current.isEmpty()) return@mutate null
            val list = current.toMutableList()
            when (val last = list.last()) {
                // Operators are single-character tokens, so delete the whole token.
                is Token.Op -> list.removeAt(list.lastIndex)
                is Token.Num -> {
                    // Number tokens model edit text, so remove one character from the text.
                    val newText = last.text.dropLast(1)
                    if (newText.isEmpty()) {
                        list.removeAt(list.lastIndex)
                    } else {
                        list[list.lastIndex] = Token.Num(newText)
                    }
                }
            }
            list
        }
    }

    /**
     * Reset the formula to empty input.
     *
     * @return `true` if the formula was not already empty.
     */
    override fun clear(): Boolean {
        // Report `false` when clear would not change any observable state.
        if (tokens.isEmpty()) return false
        tokens = emptyList()
        emit()
        return true
    }

    /**
     * Evaluate the current expression. The formula is replaced by the
     * resulting number so the next operation continues from the result.
     *
     * Returns the result. Evaluation failures are exposed through [result] and
     * leave the formula unchanged.
     *
     * @return Evaluated Decimal result or calculation error.
     */
    override fun evaluate(): CalculatorResult {
        val cfg = _config.value
        val res = when (val evaluation = evaluateTokens(tokens, cfg)) {
            is EvalResult.Success -> evaluation.value
            is EvalResult.Failure -> {
                val errorResult = CalculatorResult.Error(evaluation.error)
                _result.value = errorResult
                return errorResult
            }
        }
        // Store an input-compatible result so users can continue calculating from it.
        val inputCompatible = res.setScale(cfg.inputMaxDecimals, cfg.rounding)
        tokens = listOf(Token.Num(stripTrailingZeros(inputCompatible.toPlainString())))
        emit()
        return _result.value
    }

    // -- bulk setters ----------------------------------------------------------

    /**
     * Replace the formula from raw text.
     *
     * @param formula Formula text using digits, `.`, `+`, `-`, `*`, and `/`.
     */
    override fun setFormula(formula: String) {
        // Parsing validates the entire input and throws when an invalid character/shape is found.
        tokens = parseRaw(formula, _config.value)
        emit()
    }

    /**
     * Try to replace the formula from raw text.
     *
     * @param formula Formula text using digits, `.`, `+`, `-`, `*`, and `/`.
     * @return `true` when parsing succeeded and the formula changed.
     */
    override fun trySetFormula(formula: String): Boolean {
        val parsed = try {
            parseRaw(formula, _config.value)
        } catch (_: IllegalArgumentException) {
            return false
        }
        if (parsed == tokens) return false
        tokens = parsed
        emit()
        return true
    }


    // =========================================================================
    //  Internals
    // =========================================================================

    /**
     * Run a token-list mutation and publish derived state when it succeeds.
     *
     * @param transform Mutation function. Return `null` to reject the user input.
     * @return `true` when tokens changed and new state was emitted.
     */
    private inline fun mutate(transform: (List<Token>) -> List<Token>?): Boolean {
        val newTokens = transform(tokens) ?: return false
        if (newTokens == tokens) return false
        tokens = newTokens
        emit()
        return true
    }

    /**
     * Append one digit to an editable number token.
     *
     * @param text Current raw number text.
     * @param d Digit to append.
     * @param maxDecimals Maximum allowed fractional digits.
     * @return New number text, or `null` when the append should be rejected.
     */
    private fun appendDigitToNumber(text: String, d: Char, maxDecimals: Int): String? {
        when (text) {
            "" -> return d.toString()
            // Replace leading zero instead of building values like `0007`.
            "0" -> return d.toString()      // no leading zero
            "-0" -> return "-$d"
            "-" -> return "-$d"
        }
        val dotIdx = text.indexOf('.')
        if (dotIdx >= 0) {
            // Reject digits once the fractional part reaches the configured input precision.
            val decCount = text.length - dotIdx - 1
            if (decCount >= maxDecimals) return null
        }
        return text + d
    }

    /**
     * Recompute every observable value from [tokens] and the current config.
     */
    private fun emit() {
        val cfg = _config.value
        _formula.value = renderRaw(tokens)
        // In-progress formulas surface as zero; arithmetic failures surface explicitly.
        when (val evaluation = evaluateTokens(tokens, cfg)) {
            is EvalResult.Success -> {
                _result.value = CalculatorResult.Success(evaluation.value)
            }

            is EvalResult.Failure -> {
                _result.value = CalculatorResult.Error(evaluation.error)
            }
        }
    }

    // -- formula projection ----------------------------------------------------

    /**
     * Render tokens into the canonical raw formula alphabet.
     *
     * @param tokens Expression tokens to render.
     * @return Raw formula using digits, `.`, `+`, `-`, `*`, and `/`.
     */
    private fun renderRaw(tokens: List<Token>): String = buildString {
        for (t in tokens) when (t) {
            is Token.Num -> append(t.text)
            is Token.Op -> append(t.op.symbol)
        }
    }

    // -- parsing ---------------------------------------------------------------

    /**
     * Parse a canonical raw formula into tokens.
     *
     * @param raw Formula text using digits, `.`, `+`, `-`, `*`, and `/`.
     * @param cfg Configuration used to validate decimal precision.
     * @return Parsed token list.
     * @throws IllegalArgumentException when the formula shape or precision is invalid.
     */
    private fun parseRaw(raw: String, cfg: CalculatorConfig): List<Token> {
        // Raw formulas tolerate whitespace so callers can paste readable expressions.
        val cleaned = raw.filter { !it.isWhitespace() }
        val result = mutableListOf<Token>()
        var i = 0
        while (i < cleaned.length) {
            val c = cleaned[i]
            when {
                // A minus sign begins a number only at the start or immediately after an operator.
                c.isDigit() || c == '.' ||
                        (c == '-' && (result.isEmpty() || result.last() is Token.Op)) -> {
                    require(result.isEmpty() || result.last() is Token.Op) {
                        "Unexpected number at position $i"
                    }
                    val start = i
                    if (c == '-') i++
                    var hasDot = false
                    // Consume digits and at most one decimal point into the number token.
                    while (i < cleaned.length) {
                        val ch = cleaned[i]
                        when {
                            ch.isDigit() -> i++
                            ch == '.' && !hasDot -> {
                                hasDot = true; i++
                            }

                            else -> break
                        }
                    }
                    val numText = cleaned.substring(start, i)
                    // Values like "-" or "." are not complete numbers when passed as bulk text.
                    require(numText.any { it.isDigit() }) { "Invalid number '$numText'" }
                    validateNumberText(numText, cfg)
                    result.add(Token.Num(numText))
                }

                c == '+' || c == '-' || c == '*' || c == '/' -> {
                    // Binary operators must follow a completed number token.
                    require(result.isNotEmpty() && result.last() is Token.Num) {
                        "Unexpected operator '$c' at position $i"
                    }
                    val last = result.last() as Token.Num
                    // Normalize `1.+2` to `1+2` rather than carrying an in-progress decimal point.
                    if (last.text.endsWith('.')) {
                        result[result.lastIndex] = Token.Num(last.text.dropLast(1))
                    }
                    result.add(Token.Op(CalculatorOperation.fromSymbol(c)))
                    i++
                }

                else -> throw IllegalArgumentException("Invalid character '$c' at position $i")
            }
        }
        return result
    }

    /**
     * Validate a parsed raw number against current calculator limits.
     *
     * @param text Raw number text to validate.
     * @param cfg Configuration containing the maximum decimal count.
     * @throws IllegalArgumentException when [text] has too many fractional digits.
     */
    private fun validateNumberText(text: String, cfg: CalculatorConfig) {
        val abs = if (text.startsWith('-')) text.substring(1) else text
        val dotIdx = abs.indexOf('.')
        if (dotIdx >= 0) {
            // Only the fractional part is capped; integer length is currently unrestricted.
            val decCount = abs.length - dotIdx - 1
            require(decCount <= cfg.inputMaxDecimals) {
                "Too many decimal digits in '$text' (max ${cfg.inputMaxDecimals})"
            }
        }
    }

    // -- evaluation ------------------------------------------------------------

    /**
     * Convert a raw number token into a Decimal when it is complete enough to evaluate.
     *
     * @param text Raw number token text.
     * @return Decimal value, or `null` for in-progress number text.
     * @throws IllegalArgumentException when Decimal rejects the completed number text.
     */
    private fun numberTextToDecimal(text: String): Decimal? {
        // Empty text and a bare sign are edit states, not numeric values.
        if (text.isEmpty() || text == "-") return null
        val cleaned = if (text.endsWith('.')) text.dropLast(1) else text
        if (cleaned.isEmpty() || cleaned == "-") return null
        return Decimal.of(cleaned)
    }

    /**
     * Evaluate tokens according to the configured precedence mode.
     *
     * @param tokens Tokens to evaluate.
     * @param cfg Evaluation configuration.
     * @return Successful Decimal result or an explicit calculation failure.
     */
    private fun evaluateTokens(tokens: List<Token>, cfg: CalculatorConfig): EvalResult {
        if (tokens.isEmpty()) return EvalResult.Success(Decimal.zero())
        val complete = tokens.toMutableList()
        // Strip trailing incomplete number(s) and trailing operator(s).
        while (complete.isNotEmpty()) {
            when (val last = complete.last()) {
                is Token.Num -> {
                    val number = try {
                        numberTextToDecimal(last.text)
                    } catch (_: IllegalArgumentException) {
                        return EvalResult.Failure(CalculatorError.InvalidNumber(last.text))
                    }
                    if (number == null) {
                        complete.removeAt(complete.lastIndex)
                    } else break
                }

                is Token.Op -> complete.removeAt(complete.lastIndex)
            }
        }
        if (complete.isEmpty()) return EvalResult.Success(Decimal.zero())
        // Delegate to the selected evaluator once the trailing edit state is removed.
        return when (cfg.precedence) {
            CalculatorPrecedence.Chain -> evaluateChain(complete, cfg)
            CalculatorPrecedence.Standard -> evaluateStandard(complete, cfg)
        }
    }

    /**
     * Evaluate a token list from left to right, ignoring normal arithmetic precedence.
     *
     * @param tokens Complete token list that starts with a number and alternates op/number.
     * @param cfg Evaluation configuration.
     * @return Successful Decimal result or an explicit calculation failure.
     */
    private fun evaluateChain(tokens: List<Token>, cfg: CalculatorConfig): EvalResult {
        // Start with the first number, then fold each operation/right-hand number pair into it.
        var acc = try {
            numberTextToDecimal((tokens[0] as Token.Num).text)
        } catch (_: IllegalArgumentException) {
            return EvalResult.Failure(CalculatorError.InvalidNumber((tokens[0] as Token.Num).text))
        } ?: return EvalResult.Success(Decimal.zero())
        var i = 1
        while (i + 1 < tokens.size) {
            val op = (tokens[i] as Token.Op).op
            val rhsText = (tokens[i + 1] as Token.Num).text
            val rhs = try {
                numberTextToDecimal(rhsText)
            } catch (_: IllegalArgumentException) {
                return EvalResult.Failure(CalculatorError.InvalidNumber(rhsText))
            } ?: return EvalResult.Success(acc)
            acc = when (val applied = applyOp(acc, op, rhs, cfg)) {
                is EvalResult.Success -> applied.value
                is EvalResult.Failure -> return applied
            }
            i += 2
        }
        return EvalResult.Success(acc)
    }

    /**
     * Evaluate a token list using standard arithmetic precedence.
     *
     * @param tokens Complete token list that starts with a number and alternates op/number.
     * @param cfg Evaluation configuration.
     * @return Successful Decimal result or an explicit calculation failure.
     */
    private fun evaluateStandard(tokens: List<Token>, cfg: CalculatorConfig): EvalResult {
        // Shunting-yard → RPN
        val output = ArrayDeque<Token>()
        val opStack = ArrayDeque<CalculatorOperation>()
        for (t in tokens) when (t) {
            is Token.Num -> output.addLast(t)
            is Token.Op -> {
                // Operators are left-associative, so equal precedence also pops before pushing.
                while (opStack.isNotEmpty() && precedence(opStack.last()) >= precedence(t.op)) {
                    output.addLast(Token.Op(opStack.removeLast()))
                }
                opStack.addLast(t.op)
            }
        }
        while (opStack.isNotEmpty()) output.addLast(Token.Op(opStack.removeLast()))

        // Evaluate RPN
        val stack = ArrayDeque<Decimal>()
        for (t in output) when (t) {
            is Token.Num -> {
                val number = try {
                    numberTextToDecimal(t.text)
                } catch (_: IllegalArgumentException) {
                    return EvalResult.Failure(CalculatorError.InvalidNumber(t.text))
                } ?: return EvalResult.Success(Decimal.zero())
                stack.addLast(number)
            }

            is Token.Op -> {
                // Every binary operator needs two operands from the RPN stack.
                if (stack.size < 2) return EvalResult.Success(Decimal.zero())
                val rhs = stack.removeLast()
                val lhs = stack.removeLast()
                when (val applied = applyOp(lhs, t.op, rhs, cfg)) {
                    is EvalResult.Success -> stack.addLast(applied.value)
                    is EvalResult.Failure -> return applied
                }
            }
        }
        return EvalResult.Success(stack.lastOrNull() ?: Decimal.zero())
    }

    /**
     * Numeric precedence used by the shunting-yard evaluator.
     *
     * @param op Operation to rank.
     * @return Larger number for stronger binding.
     */
    private fun precedence(op: CalculatorOperation): Int = when (op) {
        CalculatorOperation.Add, CalculatorOperation.Subtract -> 1
        CalculatorOperation.Multiply, CalculatorOperation.Divide -> 2
    }

    /**
     * Internal precision for division.
     *
     * @param cfg Calculator configuration containing the division precision.
     * @return Configured division scale.
     */
    private fun divisionScale(cfg: CalculatorConfig): Int = cfg.divisionScale

    /**
     * Check whether a Decimal represents numeric zero.
     *
     * @param value Decimal value to inspect.
     * @return `true` when [value] is zero after insignificant fractional zeroes are stripped.
     */
    private fun isZero(value: Decimal): Boolean {
        return stripTrailingZeros(value.toPlainString()) == "0"
    }

    /**
     * Apply one Decimal operation.
     *
     * @param lhs Left-hand operand.
     * @param op Operation to apply.
     * @param rhs Right-hand operand.
     * @param cfg Configuration used for division scale and rounding.
     * @return Successful Decimal result or an explicit arithmetic failure.
     */
    private fun applyOp(
        lhs: Decimal,
        op: CalculatorOperation,
        rhs: Decimal,
        cfg: CalculatorConfig
    ): EvalResult {
        if (op == CalculatorOperation.Divide && isZero(rhs)) {
            return EvalResult.Failure(CalculatorError.DivisionByZero)
        }

        return try {
            // Delegate arithmetic to dev.voir:decimal so platform-specific Decimal behavior stays centralized.
            val result = when (op) {
                CalculatorOperation.Add -> lhs.add(rhs)
                CalculatorOperation.Subtract -> lhs.subtract(rhs)
                CalculatorOperation.Multiply -> lhs.multiply(rhs)
                CalculatorOperation.Divide -> lhs.divide(rhs, divisionScale(cfg), cfg.rounding)
            }
            EvalResult.Success(result)
        } catch (_: ArithmeticException) {
            EvalResult.Failure(CalculatorError.ArithmeticFailure(op))
        }
    }
}
