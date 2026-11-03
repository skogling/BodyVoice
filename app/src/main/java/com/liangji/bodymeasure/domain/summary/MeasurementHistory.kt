package com.liangji.bodymeasure.domain.summary

import com.liangji.bodymeasure.core.common.DateFormats
import com.liangji.bodymeasure.domain.model.Measurement
import java.time.YearMonth

data class MonthSection(
    val month: YearMonth,
    val title: String,
    val items: List<Measurement>,
)

object MeasurementHistory {
    fun groupByMonthDescending(measurements: List<Measurement>): List<MonthSection> =
        measurements
            .groupBy { YearMonth.from(it.date) }
            .toSortedMap(compareByDescending { it })
            .map { (month, items) ->
                MonthSection(
                    month = month,
                    title = DateFormats.monthTitle(month),
                    items = items.sortedByDescending { it.date },
                )
            }
}
