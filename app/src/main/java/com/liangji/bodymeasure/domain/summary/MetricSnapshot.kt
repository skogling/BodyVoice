package com.liangji.bodymeasure.domain.summary

import com.liangji.bodymeasure.domain.model.Measurement
import com.liangji.bodymeasure.domain.model.Metric
import java.time.LocalDate
import kotlin.math.abs

data class MetricDelta(
    val value: Double,
    val ratio: Double?,
    val fromDate: LocalDate,
)

data class MetricSnapshot(
    val metric: Metric,
    val latest: Measurement?,
    val previous: Measurement?,
    val recordedToday: Boolean,
    val sparkline: List<Double>,
) {
    val delta: MetricDelta? = if (latest != null && previous != null) {
        val change = latest.value - previous.value
        MetricDelta(
            value = change,
            ratio = if (previous.value != 0.0) change / abs(previous.value) else null,
            fromDate = previous.date,
        )
    } else {
        null
    }
}

object MetricSummaries {
    const val SPARKLINE_DAYS = 365L

    fun build(
        metrics: List<Metric>,
        measurements: List<Measurement>,
        today: LocalDate,
    ): List<MetricSnapshot> {
        val byMetric = measurements.groupBy { it.metricId }
        val sparklineStart = today.minusDays(SPARKLINE_DAYS - 1L)
        return metrics.map { metric ->
            val ordered = byMetric[metric.id].orEmpty().sortedByDescending { it.date }
            val latest = ordered.firstOrNull()
            MetricSnapshot(
                metric = metric,
                latest = latest,
                previous = ordered.getOrNull(1),
                recordedToday = latest?.date == today,
                sparkline = ordered
                    .filter { !it.date.isBefore(sparklineStart) && !it.date.isAfter(today) }
                    .reversed()
                    .map { it.value },
            )
        }
    }
}
