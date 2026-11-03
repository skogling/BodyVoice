package com.liangji.bodymeasure.domain.summary

import com.liangji.bodymeasure.testing.measured
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnomalyDetectorTest {
    private val today = LocalDate.of(2026, 10, 29)

    private fun history(vararg values: Double): List<com.liangji.bodymeasure.domain.model.Measurement> =
        values.mapIndexed { index, value ->
            measured("w", today.minusDays(index.toLong() + 1), value)
        }

    @Test
    fun no_history_is_never_anomalous() {
        assertNull(AnomalyDetector.check(90.0, emptyList()))
    }

    @Test
    fun flags_jump_above_twenty_percent() {
        val warning = AnomalyDetector.check(90.0, history(70.0))!!

        assertTrue(warning.isBigJump)
        assertFalse(warning.isFarFromMedian)
        assertEquals(90.0 / 70.0 - 1.0, warning.changeRatio!!, 1e-9)
    }

    @Test
    fun accepts_small_changes() {
        assertNull(AnomalyDetector.check(77.0, history(70.0)))
        assertNull(AnomalyDetector.check(84.0, history(70.0)))
    }

    @Test
    fun flags_value_far_above_median() {
        val warning = AnomalyDetector.check(600.0, history(50.0, 52.0, 48.0))!!

        assertTrue(warning.isFarFromMedian)
        assertEquals(12.0, warning.multipleOfMedian!!, 1e-9)
    }

    @Test
    fun flags_value_far_below_median() {
        val warning = AnomalyDetector.check(3.0, history(50.0, 52.0, 48.0))!!

        assertTrue(warning.isFarFromMedian)
        assertEquals(0.06, warning.multipleOfMedian!!, 1e-9)
    }

    @Test
    fun zero_previous_skips_jump_ratio() {
        assertNull(AnomalyDetector.check(5.0, history(0.0)))
    }

    @Test
    fun median_uses_only_latest_thirty_entries() {
        val bulk = history(
            *DoubleArray(15) { 100.0 },
            *DoubleArray(15) { 1.0 },
            10_000.0,
        )

        val warning = AnomalyDetector.check(600.0, bulk)!!

        assertEquals(600.0 / 50.5, warning.multipleOfMedian!!, 1e-9)
        assertTrue(warning.isFarFromMedian)
    }
}
