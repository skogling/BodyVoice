package com.liangji.bodymeasure.domain.formula

import java.math.BigDecimal

object FormulaPrinter {
    fun canonical(expression: FormulaExpr): String = render(expression, displayNames = null)

    fun display(expression: FormulaExpr, resolveName: (String) -> String?): String =
        render(expression, displayNames = resolveName)

    private fun render(
        expression: FormulaExpr,
        displayNames: ((String) -> String?)?,
    ): String = when (expression) {
        is FormulaExpr.Number -> formatNumber(expression.value)
        is FormulaExpr.Reference -> reference(expression.metricId, displayNames)
        is FormulaExpr.Negate -> {
            val operand = expression.operand
            val text = render(operand, displayNames)
            val needsParens = operand is FormulaExpr.Negate || precedence(operand) < PRECEDENCE_POWER
            "-" + if (needsParens) "($text)" else text
        }

        is FormulaExpr.Binary -> {
            val parentPrecedence = precedence(expression)
            val left = render(expression.left, displayNames)
            val right = render(expression.right, displayNames)
            val leftText = if (needsParensOnLeft(expression.left, parentPrecedence)) "($left)" else left
            val rightText = if (needsParensOnRight(expression.right, parentPrecedence, expression.operator)) {
                "($right)"
            } else {
                right
            }
            leftText + " " + expression.operator.symbol + " " + rightText
        }

        is FormulaExpr.Call -> expression.name + "(" +
            expression.arguments.joinToString(", ") { render(it, displayNames) } + ")"
    }

    private fun needsParensOnLeft(child: FormulaExpr, parentPrecedence: Int): Boolean {
        val childPrecedence = precedence(child)
        return childPrecedence < parentPrecedence ||
            (childPrecedence == parentPrecedence && parentPrecedence == PRECEDENCE_POWER)
    }

    private fun needsParensOnRight(
        child: FormulaExpr,
        parentPrecedence: Int,
        parentOperator: Operator,
    ): Boolean {
        val childPrecedence = precedence(child)
        if (childPrecedence < parentPrecedence) return true
        if (childPrecedence > parentPrecedence) return false
        return parentOperator != Operator.POWER
    }

    private fun precedence(expression: FormulaExpr): Int = when (expression) {
        is FormulaExpr.Binary -> when (expression.operator) {
            Operator.ADD, Operator.SUBTRACT -> PRECEDENCE_ADD
            Operator.MULTIPLY, Operator.DIVIDE -> PRECEDENCE_MULTIPLY
            Operator.POWER -> PRECEDENCE_POWER
        }

        is FormulaExpr.Negate -> PRECEDENCE_POWER
        is FormulaExpr.Number, is FormulaExpr.Reference, is FormulaExpr.Call -> PRECEDENCE_ATOM
    }

    private fun reference(metricId: String, displayNames: ((String) -> String?)?): String {
        val name = displayNames?.invoke(metricId)
        val printable = name != null &&
            name.isNotBlank() &&
            '[' !in name &&
            ']' !in name &&
            '\n' !in name
        return if (printable) "[${name.trim()}]" else "{$metricId}"
    }

    private fun formatNumber(value: Double): String =
        BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()

    private const val PRECEDENCE_ADD = 1
    private const val PRECEDENCE_MULTIPLY = 2
    private const val PRECEDENCE_POWER = 3
    private const val PRECEDENCE_ATOM = 4
}
