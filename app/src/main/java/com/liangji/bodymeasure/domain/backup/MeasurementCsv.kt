package com.liangji.bodymeasure.domain.backup

import com.liangji.bodymeasure.domain.model.Measurement
import com.liangji.bodymeasure.domain.model.Metric
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object MeasurementCsv {
    private const val BOM = "\uFEFF"
    private val timestampFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun render(
        metrics: List<Metric>,
        measurements: List<Measurement>,
        zone: ZoneId = ZoneId.systemDefault(),
    ): String {
        val included = metrics.filter { it.deletedAt == null }
        val byId = included.associateBy { it.id }
        val order = included.withIndex().associate { (index, metric) -> metric.id to index }
        val rows = measurements
            .asSequence()
            .filter { it.deletedAt == null && it.metricId in byId }
            .sortedWith(
                compareBy<Measurement> { order[it.metricId] ?: Int.MAX_VALUE }
                    .thenBy { it.date }
                    .thenBy { it.createdAt },
            )
            .toList()

        val builder = StringBuilder(BOM)
        builder.append(line("指标", "日期", "数值", "单位", "备注", "记录时间"))
        rows.forEach { measurement ->
            val metric = byId.getValue(measurement.metricId)
            builder.append(
                line(
                    metric.name,
                    measurement.date.toString(),
                    formatValue(measurement.value, metric.precision),
                    metric.unit,
                    measurement.note.orEmpty(),
                    timestampFormat.format(
                        Instant.ofEpochMilli(measurement.measuredAt).atZone(zone),
                    ),
                ),
            )
        }
        return builder.toString()
    }

    private fun line(vararg fields: String): String =
        fields.joinToString(separator = ",", postfix = "\r\n") { escape(it) }

    private fun escape(value: String): String {
        val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuotes) return value
        return "\"" + value.replace("\"", "\"\"") + "\""
    }

    private fun formatValue(value: Double, precision: Int): String =
        BigDecimal.valueOf(value)
            .setScale(precision, RoundingMode.HALF_UP)
            .toPlainString()
}
