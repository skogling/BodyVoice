package com.liangji.bodymeasure.domain.summary

import com.liangji.bodymeasure.testing.measured
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartStatisticsTest {
    private val today = LocalDate.of(2026, 10, 30)

    @Test
    fun daily_values_keep_latest_record_per_day() {
        val morning = measured("w", today, 79.0).copy(measuredAt = 1_000)
        val evening = measured("w", today, 78.4, id = "evening").copy(measuredAt = 2_000)
        val older = measured("w", today.minusDays(1), 79.5)

        val daily = ChartStatistics.dailyValues(listOf(morning, evening, older))

        assertEquals(2, daily.size)
        assertEquals(78.4, daily.last().value, 0.0)
        assertEquals(today.minusDays(1), daily.first().date)
    }

    @Test
    fun range_filter_includes_edges_and_drops_future() {
        val daily = listOf(
            DailyValue(today.minusDays(30), 80.0),
            DailyValue(today.minusDays(29), 79.0),
            DailyValue(today, 78.0),
            DailyValue(today.plusDays(1), 77.0),
        )

        val filtered = ChartStatistics.withinRange(daily, ChartRange.ONE_MONTH, today)

        assertEquals(listOf(today.minusDays(29), today), filtered.map { it.date })
    }

    @Test
    fun trend_uses_seven_day_rolling_average() {
        val daily = (1..8).map { day ->
            DailyValue(today.minusDays((8 - day).toLong()), if (day == 8) 77.0 else 70.0)
        }

        val chart = ChartStatistics.trend(daily, ChartRange.ALL, today)

        assertEquals(8, chart.count)
        assertEquals(8, chart.points.size)
        assertEquals(8, chart.averagePoints.size)
        assertEquals(70.0, chart.averagePoints.first().value, 1e-9)
        assertEquals(71.0, chart.averagePoints.last().value, 1e-9)
        assertEquals(70.875, chart.meanValue!!, 1e-9)
        assertEquals(7.0, chart.change!!, 1e-9)
        assertEquals(daily.first().date, chart.minPoint!!.date)
        assertEquals(daily.last().date, chart.maxPoint!!.date)
    }

    @Test
    fun trend_downsamples_long_series_while_keeping_edges() {
        val daily = (0 until 400).map { index ->
            DailyValue(
                today.minusDays(399L - index),
                70.0 + (index % 50) * 0.1,
            )
        }

        val chart = ChartStatistics.trend(daily, ChartRange.ALL, today, maxPoints = 40)

        assertTrue(chart.points.size <= 40)
        assertTrue(chart.points.size >= 3)
        assertEquals(daily.first().date, chart.points.first().date)
        assertEquals(daily.last().date, chart.points.last().date)
        assertTrue(chart.points.zipWithNext().all { (a, b) -> a.date.isBefore(b.date) })
        assertEquals(chart.points.size, chart.averagePoints.size)
    }

    @Test
    fun trend_default_sampling_stays_within_mobile_node_budget() {
        val daily = (0 until 400).map { index ->
            DailyValue(
                today.minusDays(399L - index),
                70.0 + (index % 47) * 0.03,
            )
        }

        val chart = ChartStatistics.trend(daily, ChartRange.ALL, today)

        assertTrue(chart.points.size <= ChartStatistics.MAX_TREND_POINTS)
        assertTrue(chart.points.size >= 3)
        assertEquals(daily.first().date, chart.points.first().date)
        assertEquals(daily.last().date, chart.points.last().date)
    }

    @Test
    fun trend_empty_range_returns_empty_chart() {
        val daily = listOf(DailyValue(today.minusDays(200), 70.0))

        val chart = ChartStatistics.trend(daily, ChartRange.THREE_MONTHS, today)

        assertTrue(chart.isEmpty)
        assertNull(chart.meanValue)
        assertNull(chart.change)
    }

    @Test
    fun distribution_counts_every_value_once() {
        val daily = (0..9).map { index ->
            DailyValue(today.minusDays(index.toLong()), 71.0 + index * 0.2)
        }

        val distribution = ChartStatistics.distribution(daily)!!

        assertEquals(10, distribution.count)
        assertEquals(10, distribution.bins.sumOf { it.count })
        assertEquals(71.9, distribution.mean, 1e-9)
        assertEquals(71.9, distribution.median, 1e-9)
        assertEquals(71.0, distribution.minimum, 1e-9)
        assertEquals(72.8, distribution.maximum, 1e-9)
        assertTrue(distribution.bins.size in 3..8)
        assertTrue(distribution.bins.zipWithNext().all { (a, b) -> a.upper == b.lower })
        daily.forEach { point ->
            assertTrue(
                distribution.bins.any { point.value >= it.lower - 1e-9 && point.value <= it.upper + 1e-9 },
            )
        }
    }

    @Test
    fun distribution_with_identical_values_uses_one_bin() {
        val daily = listOf(
            DailyValue(today, 70.0),
            DailyValue(today.minusDays(1), 70.0),
        )

        val distribution = ChartStatistics.distribution(daily)!!

        assertEquals(1, distribution.bins.size)
        assertEquals(2, distribution.bins.single().count)
        assertEquals(70.0, distribution.median, 0.0)
    }

    @Test
    fun calendar_starts_on_monday_and_pads_six_weeks() {
        val daily = listOf(
            DailyValue(today, 78.5),
            DailyValue(today.minusDays(1), 78.9),
            DailyValue(today.minusDays(20), 79.4),
            DailyValue(today.plusDays(1), 77.0),
        )

        val month = ChartStatistics.calendar(daily, YearMonth.of(2026, 10), today)

        assertEquals(6, month.weeks.size)
        assertTrue(month.weeks.all { it.size == 7 })
        val firstWeek = month.weeks.first()
        assertNull(firstWeek[0])
        assertNull(firstWeek[1])
        assertNull(firstWeek[2])
        assertEquals(1, firstWeek[3]!!.date.dayOfMonth)
        val cells = month.weeks.flatten().filterNotNull().associateBy { it.date }
        assertEquals(78.5, cells.getValue(today).value!!, 0.0)
        assertEquals(78.9, cells.getValue(today.minusDays(1)).value!!, 0.0)
        assertNull(cells.getValue(today.plusDays(1)).value)
        assertEquals(3, month.count)
        assertEquals(78.93333333333334, month.meanValue!!, 1e-9)
        assertEquals(78.5, month.minimum!!, 0.0)
        assertEquals(79.4, month.maximum!!, 0.0)
    }

    @Test
    fun comparison_splits_current_and_previous_periods() {
        val daily = listOf(
            DailyValue(today.minusDays(16), 80.0),
            DailyValue(today.minusDays(7), 79.0),
            DailyValue(today.minusDays(6), 78.0),
            DailyValue(today, 77.0),
        )

        val comparison = ChartStatistics.comparison(daily, today, ComparisonSpan.WEEK)

        assertEquals(today.minusDays(6), comparison.current.start)
        assertEquals(today, comparison.current.end)
        assertEquals(2, comparison.current.count)
        assertEquals(77.5, comparison.current.average!!, 1e-9)
        assertEquals(today.minusDays(13), comparison.previous.start)
        assertEquals(today.minusDays(7), comparison.previous.end)
        assertEquals(1, comparison.previous.count)
        assertEquals(79.0, comparison.previous.average!!, 1e-9)
        assertEquals(-1.5, comparison.averageChange!!, 1e-9)
        assertEquals(-1.5 / 79.0, comparison.averageChangeRatio!!, 1e-9)
    }

    @Test
    fun comparison_without_previous_data_has_null_change() {
        val daily = listOf(DailyValue(today, 77.0))

        val comparison = ChartStatistics.comparison(daily, today, ComparisonSpan.MONTH)

        assertEquals(0, comparison.previous.count)
        assertNull(comparison.previous.average)
        assertNull(comparison.averageChange)
        assertNull(comparison.averageChangeRatio)
        assertNotNull(comparison.current.average)
        assertFalse(comparison.current.average!!.isNaN())
    }
}
