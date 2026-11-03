package com.liangji.bodymeasure.domain.formula

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

object FormulaEvaluator {
    const val MAX_ROUND_DIGITS = 6

    fun evaluate(expression: FormulaExpr, resolve: (String) -> Double?): Double? = when (expression) {
        is FormulaExpr.Number -> expression.value.finite()
        is FormulaExpr.Reference -> resolve(expression.metricId)?.finite()
        is FormulaExpr.Negate -> evaluate(expression.operand, resolve)?.let { (-it).finite() }
        is FormulaExpr.Binary -> evaluateBinary(expression, resolve)
        is FormulaExpr.Call -> evaluateCall(expression, resolve)
    }

    private fun evaluateBinary(
        expression: FormulaExpr.Binary,
        resolve: (String) -> Double?,
    ): Double? {
        val left = evaluate(expression.left, resolve) ?: return null
        val right = evaluate(expression.right, resolve) ?: return null
        return when (expression.operator) {
            Operator.ADD -> (left + right).finite()
            Operator.SUBTRACT -> (left - right).finite()
            Operator.MULTIPLY -> (left * right).finite()
            Operator.DIVIDE -> if (right == 0.0) null else (left / right).finite()
            Operator.POWER -> left.pow(right).finite()
        }
    }

    private fun evaluateCall(
        expression: FormulaExpr.Call,
        resolve: (String) -> Double?,
    ): Double? {
        val values = expression.arguments.map { evaluate(it, resolve) }
        if (values.any { it == null }) return null
        val args = values.map { checkNotNull(it) }
        return when (expression.name) {
            "abs" -> abs(args[0]).finite()
            "sqrt" -> if (args[0] < 0.0) null else sqrt(args[0]).finite()
            "pow" -> args[0].pow(args[1]).finite()
            "round" -> {
                val digits = args.getOrNull(1)?.toInt()?.coerceIn(0, MAX_ROUND_DIGITS) ?: 0
                BigDecimal.valueOf(args[0])
                    .setScale(digits, RoundingMode.HALF_UP)
                    .toDouble()
                    .finite()
            }

            "min" -> args.min().finite()
            "max" -> args.max().finite()
            "clamp" -> if (args[1] > args[2]) null else args[0].coerceIn(args[1], args[2]).finite()
            else -> null
        }
    }

    private fun Double.finite(): Double? = takeIf { it.isFinite() }
}
