package com.liangji.bodymeasure.domain.summary

import com.liangji.bodymeasure.domain.formula.FormulaEvaluator
import com.liangji.bodymeasure.domain.formula.FormulaExpr
import com.liangji.bodymeasure.domain.formula.FormulaParseResult
import com.liangji.bodymeasure.domain.formula.FormulaParser
import com.liangji.bodymeasure.domain.model.Measurement
import com.liangji.bodymeasure.domain.model.MeasurementSource
import com.liangji.bodymeasure.domain.model.Metric
import com.liangji.bodymeasure.domain.model.MetricKind
import com.liangji.bodymeasure.domain.model.PairingPolicy
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

object DerivedSeriesCalculator {
    fun build(
        metrics: List<Metric>,
        measurements: List<Measurement>,
    ): Map<String, List<Measurement>> {
        val builder = Builder(metrics.associateBy { it.id }, measurements)
        return metrics
            .filter { it.kind == MetricKind.DERIVED }
            .associate { metric ->
                metric.id to builder.series(metric.id).map { it.toMeasurement(metric) }
            }
    }

    internal fun pairingValue(
        points: List<Measurement>,
        anchor: LocalDate,
        policy: PairingPolicy,
        windowDays: Int,
    ): Double? = pairPoints(
        points = points.map { Point(it.date, it.value) },
        anchor = anchor,
        policy = policy,
        windowDays = windowDays,
    )

    private fun pairPoints(
        points: List<Point>,
        anchor: LocalDate,
        policy: PairingPolicy,
        windowDays: Int,
    ): Double? {
        if (points.isEmpty()) return null
        val window = windowDays.coerceAtLeast(0).toLong()
        return when (policy) {
            PairingPolicy.SAME_DAY -> points.lastOrNull { it.date == anchor }?.value
            PairingPolicy.NEAREST -> {
                var best: Point? = null
                var bestDistance = Long.MAX_VALUE
                for (point in points) {
                    val distance = abs(ChronoUnit.DAYS.between(point.date, anchor))
                    if (distance > window) continue
                    val current = best
                    if (current == null ||
                        distance < bestDistance ||
                        (distance == bestDistance && point.date.isBefore(current.date))
                    ) {
                        best = point
                        bestDistance = distance
                    }
                }
                best?.value
            }

            PairingPolicy.CARRY_FORWARD -> points.lastOrNull {
                !it.date.isAfter(anchor) && ChronoUnit.DAYS.between(it.date, anchor) <= window
            }?.value
        }
    }

    private data class Point(val date: LocalDate, val value: Double) {
        fun toMeasurement(metric: Metric): Measurement {
            val millis = date.toEpochDay() * MILLIS_PER_DAY
            return Measurement(
                id = "derived:${metric.id}:${date.toEpochDay()}",
                metricId = metric.id,
                date = date,
                measuredAt = millis,
                value = value,
                note = null,
                source = MeasurementSource.DERIVED,
                createdAt = millis,
                updatedAt = millis,
                deletedAt = null,
                originDeviceId = metric.originDeviceId,
            )
        }
    }

    private class Builder(
        private val metrics: Map<String, Metric>,
        measurements: List<Measurement>,
    ) {
        private val rawSeries: Map<String, List<Point>> = measurements
            .filter { it.deletedAt == null }
            .groupBy { it.metricId }
            .mapValues { (_, rows) ->
                rows.groupBy { it.date }
                    .map { (date, sameDay) -> Point(date, sameDay.maxBy { it.measuredAt }.value) }
                    .sortedBy { it.date }
            }

        private val cache = HashMap<String, List<Point>>()
        private val visiting = HashSet<String>()

        fun series(metricId: String): List<Point> {
            val metric = metrics[metricId] ?: return emptyList()
            if (metric.kind == MetricKind.RAW) return rawSeries[metricId].orEmpty()
            cache[metricId]?.let { return it }
            if (!visiting.add(metricId)) return emptyList()
            val points = buildDerived(metric)
            visiting.remove(metricId)
            cache[metricId] = points
            return points
        }

        private fun buildDerived(metric: Metric): List<Point> {
            val formula = metric.formula ?: return emptyList()
            val parsed = FormulaParser.parse(formula) as? FormulaParseResult.Valid
                ?: return emptyList()
            val references = parsed.references.map { it.metricId }.distinct()
            if (references.isEmpty()) return emptyList()
            val dependencies = references.associateWith { series(it) }
            val variableReferences = references.filter { metrics[it]?.isConstant != true }
            val anchors = if (variableReferences.isEmpty()) {
                listOfNotNull(
                    references.mapNotNull { dependencies[it].orEmpty().lastOrNull()?.date }.maxOrNull(),
                )
            } else {
                variableReferences
                    .flatMap { dependencies[it].orEmpty().map(Point::date) }
                    .distinct()
                    .sorted()
            }
            return anchors.mapNotNull { date ->
                evaluate(metric, parsed.expression, references, dependencies, date)?.let { value ->
                    Point(date, value)
                }
            }
        }

        private fun evaluate(
            metric: Metric,
            expression: FormulaExpr,
            references: List<String>,
            dependencies: Map<String, List<Point>>,
            date: LocalDate,
        ): Double? {
            val values = HashMap<String, Double?>(references.size)
            for (reference in references) {
                val dependency = metrics[reference] ?: return null
                values[reference] = if (dependency.isConstant) {
                    dependencies[reference].orEmpty().lastOrNull()?.value
                } else {
                    pairPoints(
                        points = dependencies[reference].orEmpty(),
                        anchor = date,
                        policy = metric.pairingPolicy,
                        windowDays = metric.pairingWindowDays,
                    )
                }
            }
            return FormulaEvaluator.evaluate(expression) { values[it] }
        }
    }

    private const val MILLIS_PER_DAY = 86_400_000L
}
