package com.liangji.bodymeasure.domain.backup

import com.liangji.bodymeasure.domain.model.MeasurementSource
import com.liangji.bodymeasure.testing.measured
import com.liangji.bodymeasure.testing.rawMetric
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeasurementCsvTest {
    private val today = LocalDate.of(2026, 10, 30)
    private val utc = ZoneId.of("UTC")

    @Test
    fun writesBomHeaderAndSortedRows() {
        val waist = rawMetric("waist", name = "腰围", unit = "cm")
        val weight = rawMetric("weight", name = "体重", unit = "kg")
        val measurements = listOf(
            measured("weight", today, 71.2),
            measured("waist", today, 78.5),
            measured("waist", today.minusDays(2), 79.0),
        )

        val csv = MeasurementCsv.render(listOf(waist, weight), measurements, utc)

        assertTrue(csv.startsWith("\uFEFF"))
        val lines = csv.removePrefix("\uFEFF").split("\r\n").filter { it.isNotEmpty() }
        assertEquals("指标,日期,数值,单位,备注,记录时间", lines[0])
        assertEquals("腰围,2026-10-28,79.0,cm,,2026-10-28 00:00", lines[1])
        assertEquals("腰围,2026-10-30,78.5,cm,,2026-10-30 00:00", lines[2])
        assertEquals("体重,2026-10-30,71.2,kg,,2026-10-30 00:00", lines[3])
    }

    @Test
    fun escapesCommasQuotesAndNewlines() {
        val metric = rawMetric("waist", name = "腰围,放松", unit = "cm")
        val measurement = measured(
            "waist",
            today,
            78.5,
            note = "早上 \"空腹\"\n第二次",
        ).copy(source = MeasurementSource.IMPORT)

        val csv = MeasurementCsv.render(listOf(metric), listOf(measurement), utc)

        assertTrue(csv.contains("\"腰围,放松\""))
        assertTrue(csv.contains("\"早上 \"\"空腹\"\"\n第二次\""))
    }

    @Test
    fun skipsDeletedRowsAndUnknownMetrics() {
        val metric = rawMetric("waist", name = "腰围", unit = "cm")
        val deletedMeasurement = measured("waist", today, 78.5, id = "gone").copy(
            deletedAt = 123,
        )
        val orphan = measured("missing", today, 1.0)
        val deletedMetric = rawMetric("old", name = "旧指标").copy(deletedAt = 456)

        val csv = MeasurementCsv.render(
            metrics = listOf(metric, deletedMetric),
            measurements = listOf(deletedMeasurement, orphan),
            zone = utc,
        )

        val lines = csv.removePrefix("\uFEFF").split("\r\n").filter { it.isNotEmpty() }
        assertEquals(1, lines.size)
        assertEquals("指标,日期,数值,单位,备注,记录时间", lines[0])
        assertFalse(csv.contains("旧指标"))
    }

    @Test
    fun appliesMetricPrecision() {
        val metric = rawMetric("weight", name = "体重", unit = "kg", precision = 2)
        val derived = rawMetric("ratio", name = "体脂率", unit = "%", precision = 0)

        val csv = MeasurementCsv.render(
            metrics = listOf(metric, derived),
            measurements = listOf(
                measured("weight", today, 71.256),
                measured("ratio", today, 18.6).copy(source = MeasurementSource.MANUAL),
            ),
            zone = utc,
        )

        assertTrue(csv.contains("体重,2026-10-30,71.26,kg,"))
        assertTrue(csv.contains("体脂率,2026-10-30,19,%,", ignoreCase = false))
    }

    @Test
    fun emptyInputStillWritesHeader() {
        val csv = MeasurementCsv.render(emptyList(), emptyList(), utc)

        assertEquals("\uFEFF指标,日期,数值,单位,备注,记录时间\r\n", csv)
    }
}
