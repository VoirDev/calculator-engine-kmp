package dev.voir.calculator.engine.evaluation

import dev.voir.calculator.engine.CalculatorConfig
import dev.voir.calculator.engine.CalculatorError
import dev.voir.calculator.engine.CalculatorOperation
import dev.voir.calculator.engine.CalculatorPrecedence
import dev.voir.calculator.engine.CalculatorResult
import dev.voir.calculator.engine.formula.FormulaToken
import dev.voir.decimal.Decimal

/**
 * Evaluates formula tokens with [Decimal] arithmetic.
 *
 * Trailing edit state (a bare sign, a lone decimal point, or a trailing operation) is ignored, so a
 * formula that is still being typed evaluates to its completed prefix. All numbers are converted
 * before any arithmetic runs, so an unreadable number is reported ahead of an arithmetic failure.
 */
internal object FormulaEvaluator {
    /**
     * Evaluate [tokens] using the precedence, division scale, and rounding from [config].
     *
     * @param tokens Formula tokens; every number except the last must be complete.
     * @param config Evaluation settings.
     * @return Result value, [CalculatorResult.Empty] for a formula with no complete number, or the
     * first failure.
     */
    fun evaluate(tokens: List<FormulaToken>, config: CalculatorConfig): CalculatorResult {
        val complete = tokens.dropLastWhile { it is FormulaToken.Op || (it is FormulaToken.Num && !it.isComplete) }
        if (complete.isEmpty()) return CalculatorResult.Empty

        val operands = mutableListOf<Decimal>()
        val operations = mutableListOf<CalculatorOperation>()
        for (token in complete) when (token) {
            // Evaluated results continue from their exact value, not the rounded text.
            is FormulaToken.Num -> operands += token.exactValue
                ?: Decimal.parseOrNull(token.text.removeSuffix("."))
                ?: return CalculatorResult.Error(CalculatorError.InvalidNumber(token.text))

            is FormulaToken.Op -> operations += token.operation
        }
        check(operands.size == operations.size + 1) {
            "Formula tokens must alternate numbers and operations: $tokens"
        }

        return try {
            val value = when (config.precedence) {
                CalculatorPrecedence.Chain -> evaluateChain(operands, operations, config)
                CalculatorPrecedence.Standard -> evaluateStandard(operands, operations, config)
            }
            CalculatorResult.Success(value)
        } catch (failure: ArithmeticFailure) {
            CalculatorResult.Error(failure.error)
        }
    }

    /**
     * Evaluate strictly left to right: `2 + 3 * 4` is `(2 + 3) * 4`.
     *
     * @param operands Numbers in formula order.
     * @param operations Operations in formula order; one fewer than [operands].
     * @param config Division scale and rounding.
     * @return Result value.
     * @throws ArithmeticFailure For the first failing operation.
     */
    private fun evaluateChain(
        operands: List<Decimal>,
        operations: List<CalculatorOperation>,
        config: CalculatorConfig,
    ): Decimal = operations.withIndex().fold(operands.first()) { accumulator, (index, operation) ->
        apply(accumulator, operation, operands[index + 1], config)
    }

    /**
     * Evaluate with standard precedence: `*` and `/` bind tighter than `+` and `-`, and operations
     * of equal precedence run left to right.
     *
     * Each run of multiplications and divisions is folded into a term, and terms are added or
     * subtracted as soon as the next additive operation (or the end) is reached. This performs the
     * same operations in the same order as a shunting-yard evaluation.
     *
     * @param operands Numbers in formula order.
     * @param operations Operations in formula order; one fewer than [operands].
     * @param config Division scale and rounding.
     * @return Result value.
     * @throws ArithmeticFailure For the first failing operation.
     */
    private fun evaluateStandard(
        operands: List<Decimal>,
        operations: List<CalculatorOperation>,
        config: CalculatorConfig,
    ): Decimal {
        var sum: Decimal? = null
        var pendingAdditive = CalculatorOperation.Add
        var term = operands.first()

        for ((index, operation) in operations.withIndex()) {
            val right = operands[index + 1]
            if (operation.isMultiplicative()) {
                term = apply(term, operation, right, config)
            } else {
                sum = if (sum == null) term else apply(sum, pendingAdditive, term, config)
                pendingAdditive = operation
                term = right
            }
        }
        return if (sum == null) term else apply(sum, pendingAdditive, term, config)
    }

    /**
     * Apply one operation.
     *
     * @param left Left operand.
     * @param operation Operation to apply.
     * @param right Right operand.
     * @param config Division scale and rounding.
     * @return Operation result.
     * @throws ArithmeticFailure With [CalculatorError.DivisionByZero] or
     * [CalculatorError.ArithmeticFailure].
     */
    private fun apply(
        left: Decimal,
        operation: CalculatorOperation,
        right: Decimal,
        config: CalculatorConfig,
    ): Decimal {
        if (operation == CalculatorOperation.Divide && right.isZero()) {
            throw ArithmeticFailure(CalculatorError.DivisionByZero)
        }
        return try {
            when (operation) {
                CalculatorOperation.Add -> left.add(right)
                CalculatorOperation.Subtract -> left.subtract(right)
                CalculatorOperation.Multiply -> left.multiply(right)
                CalculatorOperation.Divide -> left.divide(right, config.divisionScale, config.rounding)
            }
        } catch (_: ArithmeticException) {
            throw ArithmeticFailure(CalculatorError.ArithmeticFailure(operation))
        } catch (_: IllegalArgumentException) {
            // dev.voir:decimal reports results outside its portable exponent range this way.
            throw ArithmeticFailure(CalculatorError.ArithmeticFailure(operation))
        }
    }

    /**
     * Check whether this operation binds tighter than addition and subtraction.
     *
     * @return `true` for multiplication and division.
     */
    private fun CalculatorOperation.isMultiplicative(): Boolean =
        this == CalculatorOperation.Multiply || this == CalculatorOperation.Divide

    /**
     * Stops evaluation at the first failing operation; [evaluate] turns it into a result.
     *
     * @property error Failure to report.
     */
    private class ArithmeticFailure(val error: CalculatorError) : Exception()
}
