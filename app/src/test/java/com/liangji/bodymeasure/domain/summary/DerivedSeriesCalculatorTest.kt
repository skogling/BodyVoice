package com.liangji.bodymeasure.domain.summary

import com.liangji.bodymeasure.domain.model.MeasurementSource
import com.liangji.bodymeasure.domain.model.PairingPolicy
import com.liangji.bodymeasure.testing.derivedMetric
import com.liangji.bodymeasure.testing.measured
import com.liangji.bodymeasure.testing.rawMetric
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DerivedSeriesCalculatorTest {
    private val jan1 = LocalDate.of(2026, 1, 1)
    private val jan2 = LocalDate.of(2026, 1, 2)
    private val jan3 = LocalDate.of(2026, 1, 3)
    private val jan4 = LocalDate.of(2026, 1, 4)
    private val jan5 = LocalDate.of(2026, 1, 5)

    @Test
    fun computes_series_on_same_day_anchors() {
        val metrics = listOf(
            rawMetric("w"),
            rawMetric("h", unit = "cm", isConstant = true),
            derivedMetric(
                "bmi",
                formula = "{w} / ({h} / 100) ^ 2",
                pairingPolicy = PairingPolicy.SAME_DAY,
            ),
        )
        val measurements = listOf(
            measured("w", jan1, 80.0),
            measured("w", jan3, 78.0),
            measured("h", LocalDate.of(2025, 6, 1), 180.0),
        )

        val points = DerivedSeriesCalculator.build(metrics, measurements).getValue("bmi")

        assertEquals(listOf(jan1, jan3), points.map { it.date })
        assertEquals(80.0 / (1.8 * 1.8), points[0].value, 1e-9)
        assertEquals(78.0 / (1.8 * 1.8), points[1].value, 1e-9)
        assertEquals(MeasurementSource.DERIVED, points[0].source)
        assertEquals("bmi", points[0].metricId)
    }

    @Test
    fun nearest_pairs_across_gaps_and_prefers_earlier_on_tie() {
        val metrics = listOf(
            rawMetric("w"),
            rawMetric("waist", unit = "cm"),
            derivedMetric(
                "ratio",
                formula = "{w} / {waist}",
                pairingPolicy = PairingPolicy.NEAREST,
                pairingWindowDays = 2,
            ),
        )
        val measurements = listOf(
            measured("w", jan1, 80.0),
            measured("w", jan3, 78.0),
            measured("waist", jan2, 90.0),
            measured("waist", jan4, 92.0),
        )

        val points = DerivedSeriesCalculator.build(metrics, measurements).getValue("ratio")

        assertEquals(listOf(jan1, jan2, jan3, jan4), points.map { it.date })
        assertEquals(80.0 / 90.0, points[0].value, 1e-9)
        assertEquals(80.0 / 90.0, points[1].value, 1e-9)
        assertEquals(78.0 / 90.0, points[2].value, 1e-9)
        assertEquals(78.0 / 92.0, points[3].value, 1e-9)
    }

    @Test
    fun carry_forward_uses_past_values_within_window() {
        val metrics = listOf(
            rawMetric("w"),
            rawMetric("waist", unit = "cm"),
            derivedMetric(
                "ratio",
                formula = "{w} / {waist}",
                pairingPolicy = PairingPolicy.CARRY_FORWARD,
                pairingWindowDays = 2,
            ),
        )
        val measurements = listOf(
            measured("w", jan1, 80.0),
            measured("w", jan5, 78.0),
            measured("waist", jan1, 88.0),
            measured("waist", jan3, 90.0),
        )

        val points = DerivedSeriesCalculator.build(metrics, measurements).getValue("ratio")

        assertEquals(listOf(jan1, jan3, jan5), points.map { it.date })
        assertEquals(80.0 / 88.0, points[0].value, 1e-9)
        assertEquals(80.0 / 90.0, points[1].value, 1e-9)
        assertEquals(78.0 / 90.0, points[2].value, 1e-9)
    }

    @Test
    fun carry_forward_ignores_future_and_out_of_window_values() {
        val metrics = listOf(
            rawMetric("w"),
            rawMetric("waist", unit = "cm"),
            derivedMetric(
                "ratio",
                formula = "{w} / {waist}",
                pairingPolicy = PairingPolicy.CARRY_FORWARD,
                pairingWindowDays = 2,
            ),
        )
        val measurements = listOf(
            measured("w", jan1, 80.0),
            measured("waist", jan3, 90.0),
        )

        val points = DerivedSeriesCalculator.build(metrics, measurements).getValue("ratio")

        assertEquals(listOf(jan3), points.map { it.date })
    }

    @Test
    fun missing_dependency_creates_gaps() {
        val metrics = listOf(
            rawMetric("w"),
            rawMetric("waist", unit = "cm"),
            derivedMetric(
                "ratio",
                formula = "{w} / {waist}",
                pairingPolicy = PairingPolicy.SAME_DAY,
            ),
        )
        val measurements = listOf(
            measured("w", jan1, 80.0),
            measured("w", jan2, 79.0),
            measured("w", jan3, 78.0),
            measured("waist", jan2, 90.0),
        )

        val points = DerivedSeriesCalculator.build(metrics, measurements).getValue("ratio")

        assertEquals(listOf(jan2), points.map { it.date })
        assertEquals(79.0 / 90.0, points.single().value, 1e-9)
    }

    @Test
    fun nested_derived_series_are_resolved_recursively() {
        val metrics = listOf(
            rawMetric("w"),
            rawMetric("h", unit = "cm", isConstant = true),
            derivedMetric(
                "bmi",
                formula = "{w} / ({h} / 100) ^ 2",
                pairingPolicy = PairingPolicy.SAME_DAY,
            ),
            derivedMetric(
                "adjusted",
                formula = "{bmi} + 1",
                pairingPolicy = PairingPolicy.SAME_DAY,
            ),
        )
        val measurements = listOf(
            measured("w", jan1, 80.0),
            measured("w", jan2, 78.0),
            measured("h", jan1, 180.0),
        )

        val result = DerivedSeriesCalculator.build(metrics, measurements)
        val bmi = result.getValue("bmi")
        val adjusted = result.getValue("adjusted")

        assertEquals(listOf(jan1, jan2), adjusted.map { it.date })
        assertEquals(bmi[0].value + 1.0, adjusted[0].value, 1e-9)
        assertEquals(bmi[1].value + 1.0, adjusted[1].value, 1e-9)
    }

    @Test
    fun formula_with_only_constants_yields_single_point() {
        val metrics = listOf(
            rawMetric("h", unit = "cm", isConstant = true),
            derivedMetric("hMeters", formula = "{h} / 100"),
        )
        val measurements = listOf(measured("h", jan1, 180.0))

        val points = DerivedSeriesCalculator.build(metrics, measurements).getValue("hMeters")

        assertEquals(listOf(jan1), points.map { it.date })
        assertEquals(1.8, points.single().value, 1e-9)
    }

    @Test
    fun cyclic_graph_does_not_crash() {
        val metrics = listOf(
            derivedMetric("a", formula = "{b} + 1"),
            derivedMetric("b", formula = "{a} + 1"),
        )

        val result = DerivedSeriesCalculator.build(metrics, emptyList())

        assertTrue(result.getValue("a").isEmpty())
        assertTrue(result.getValue("b").isEmpty())
    }

    @Test
    fun invalid_formula_yields_no_points() {
        val metrics = listOf(
            rawMetric("w"),
            derivedMetric("broken", formula = "{w} +"),
        )

        val points = DerivedSeriesCalculator.build(metrics, listOf(measured("w", jan1, 80.0)))
            .getValue("broken")

        assertTrue(points.isEmpty())
    }

    @Test
    fun deleted_measurements_are_ignored() {
        val metrics = listOf(
            rawMetric("w"),
            derivedMetric("doubled", formula = "{w} * 2", pairingPolicy = PairingPolicy.SAME_DAY),
        )
        val measurements = listOf(
            measured("w", jan1, 80.0),
            measured("w", jan2, 90.0).copy(deletedAt = 1L),
        )

        val points = DerivedSeriesCalculator.build(metrics, measurements).getValue("doubled")

        assertEquals(listOf(jan1), points.map { it.date })
        assertEquals(160.0, points.single().value, 1e-9)
    }
}
