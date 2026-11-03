package com.liangji.bodymeasure.domain.summary

import com.liangji.bodymeasure.testing.measured
import com.liangji.bodymeasure.testing.rawMetric
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MetricSummariesTest {
    private val today = LocalDate.of(2026, 10, 29)

    @Test
    fun empty_measurements_yield_empty_snapshot() {
        val snapshot = MetricSummaries.build(listOf(rawMetric("w")), emptyList(), today).single()

        assertNull(snapshot.latest)
        assertNull(snapshot.delta)
        assertFalse(snapshot.recordedToday)
        assertTrue(snapshot.sparkline.isEmpty())
    }

    @Test
    fun builds_latest_previous_and_delta() {
        val measurements = listOf(
            measured("w", today.minusDays(2), 79.1),
            measured("w", today, 78.5),
            measured("w", today.minusDays(5), 80.0),
        )

        val snapshot = MetricSummaries.build(listOf(rawMetric("w")), measurements, today).single()

        assertEquals(today, snapshot.latest!!.date)
        assertEquals(today.minusDays(2), snapshot.previous!!.date)
        val delta = snapshot.delta!!
        assertEquals(-0.6, delta.value, 1e-9)
        assertEquals(-0.6 / 79.1, delta.ratio!!, 1e-9)
        assertEquals(today.minusDays(2), delta.fromDate)
    }

    @Test
    fun marks_today_only_for_latest_entry() {
        val todaySnapshot = MetricSummaries.build(
            listOf(rawMetric("w")),
            listOf(measured("w", today, 78.5)),
            today,
        ).single()
        val olderSnapshot = MetricSummaries.build(
            listOf(rawMetric("w")),
            listOf(measured("w", today.minusDays(1), 78.5)),
            today,
        ).single()

        assertTrue(todaySnapshot.recordedToday)
        assertFalse(olderSnapshot.recordedToday)
    }

    @Test
    fun sparkline_covers_last_year_in_chronological_order() {
        val measurements = (1..20).map { day ->
            measured("w", today.minusDays(20L - day), day.toDouble())
        }

        val snapshot = MetricSummaries.build(listOf(rawMetric("w")), measurements, today).single()

        assertEquals((1..20).map { it.toDouble() }, snapshot.sparkline)
    }

    @Test
    fun sparkline_excludes_records_older_than_one_year() {
        val measurements = listOf(
            measured("w", today.minusDays(400), 70.0),
            measured("w", today.minusDays(1), 78.0),
            measured("w", today, 77.0),
        )

        val snapshot = MetricSummaries.build(listOf(rawMetric("w")), measurements, today).single()

        assertEquals(listOf(78.0, 77.0), snapshot.sparkline)
    }

    @Test
    fun delta_ratio_is_null_when_previous_is_zero() {
        val measurements = listOf(
            measured("w", today, 5.0),
            measured("w", today.minusDays(1), 0.0),
        )

        val delta = MetricSummaries.build(listOf(rawMetric("w")), measurements, today).single().delta!!

        assertEquals(5.0, delta.value, 0.0)
        assertNull(delta.ratio)
    }

    @Test
    fun ignores_other_metrics_and_preserves_order() {
        val metrics = listOf(rawMetric("a", name = "A"), rawMetric("b", name = "B"))
        val measurements = listOf(
            measured("a", today, 1.0),
            measured("other", today, 99.0),
        )

        val snapshots = MetricSummaries.build(metrics, measurements, today)

        assertEquals(listOf("a", "b"), snapshots.map { it.metric.id })
        assertEquals(1.0, snapshots[0].latest!!.value, 0.0)
        assertNull(snapshots[1].latest)
    }
}
