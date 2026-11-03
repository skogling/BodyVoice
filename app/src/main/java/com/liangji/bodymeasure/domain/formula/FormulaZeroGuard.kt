package com.liangji.bodymeasure.domain.formula

import com.liangji.bodymeasure.domain.model.Metric
import com.liangji.bodymeasure.domain.model.UnitDimension
import kotlin.math.abs
import kotlin.math.pow

object FormulaZeroGuard {
    private const val CM_PER_M = 100.0

    data class Hint(
        val rawValue: Double,
        val suggestion: String?,
    )

    fun check(
        expression: FormulaExpr,
        sample: Double,
        precision: Int,
        metrics: Map<String, Metric>,
        resolveName: (String) -> String?,
    ): Hint? {
        if (!sample.isFinite() || sample == 0.0) return null
        val threshold = 0.5 * 10.0.pow(-precision.coerceIn(0, 6))
        if (abs(sample) >= threshold) return null

        val centimetres = expression.references().filter { id ->
            val metric = metrics[id] ?: return@filter false
            metric.unitDimension == UnitDimension.LENGTH &&
                metric.unit.trim().equals("cm", ignoreCase = true)
        }
        if (centimetres.isEmpty()) return Hint(sample, suggestion = null)

        val converted = convertToMetres(expression) { it in centimetres }
        return Hint(
            rawValue = sample,
            suggestion = FormulaPrinter.display(converted, resolveName),
        )
    }

    private fun convertToMetres(
        expression: FormulaExpr,
        isCentimetre: (String) -> Boolean,
    ): FormulaExpr = when (expression) {
        is FormulaExpr.Number -> expression
        is FormulaExpr.Reference -> if (isCentimetre(expression.metricId)) {
            FormulaExpr.Binary(
                Operator.DIVIDE,
                expression,
                FormulaExpr.Number(CM_PER_M),
            )
        } else {
            expression
        }

        is FormulaExpr.Negate -> FormulaExpr.Negate(convertToMetres(expression.operand, isCentimetre))
        is FormulaExpr.Binary -> FormulaExpr.Binary(
            expression.operator,
            convertToMetres(expression.left, isCentimetre),
            convertToMetres(expression.right, isCentimetre),
        )

        is FormulaExpr.Call -> FormulaExpr.Call(
            expression.name,
            expression.arguments.map { convertToMetres(it, isCentimetre) },
        )
    }

    private fun FormulaExpr.references(): Set<String> {
        val found = LinkedHashSet<String>()
        collectReferences(found)
        return found
    }

    private fun FormulaExpr.collectReferences(into: MutableSet<String>) {
        when (this) {
            is FormulaExpr.Number -> Unit
            is FormulaExpr.Reference -> into.add(metricId)
            is FormulaExpr.Negate -> operand.collectReferences(into)
            is FormulaExpr.Binary -> {
                left.collectReferences(into)
                right.collectReferences(into)
            }

            is FormulaExpr.Call -> arguments.forEach { it.collectReferences(into) }
        }
    }
}
