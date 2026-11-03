package com.liangji.bodymeasure.domain.backup

import com.liangji.bodymeasure.domain.model.FormulaHistory
import com.liangji.bodymeasure.domain.model.Measurement
import com.liangji.bodymeasure.domain.model.MeasurementSource
import com.liangji.bodymeasure.domain.model.Metric
import com.liangji.bodymeasure.testing.derivedMetric
import com.liangji.bodymeasure.testing.measured
import com.liangji.bodymeasure.testing.rawMetric
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {
    private val today = LocalDate.of(2026, 10, 30)

    @Test
    fun roundTripPreservesEverything() {
        val payload = payload()

        val decoded = BackupCodec.decode(BackupCodec.encode(payload))

        assertTrue(decoded is BackupDecodeResult.Success)
        val restored = (decoded as BackupDecodeResult.Success).payload
        assertEquals(payload.metrics, restored.metrics)
        assertEquals(payload.measurements, restored.measurements)
        assertEquals(payload.formulaHistory, restored.formulaHistory)
        assertEquals(Instant.parse("2026-10-30T08:00:00Z"), restored.exportedAt)
        assertEquals("0.4.0", restored.appVersion)
        assertEquals(3, restored.summary.metricCount)
        assertEquals(1, restored.summary.derivedCount)
        assertEquals(3, restored.summary.measurementCount)
    }

    @Test
    fun rejectsForeignFile() {
        val result = decode("""{"format":"other","version":1,"exportedAt":"2026-10-30T08:00:00Z"}""")

        assertFailureContains(result, "不是身语的备份")
    }

    @Test
    fun rejectsNewerVersion() {
        val json = BackupCodec.encode(payload()).replace("\"version\": 1", "\"version\": 9")

        assertFailureContains(decode(json), "更新版本")
    }

    @Test
    fun rejectsBrokenJson() {
        assertFailureContains(decode("{oops"), "损坏")
    }

    @Test
    fun rejectsEmptyMetrics() {
        val json = """
            {
              "format": "${BackupCodec.FORMAT}",
              "version": 1,
              "appVersion": "0.4.0",
              "exportedAt": "2026-10-30T08:00:00Z",
              "metrics": [],
              "measurements": [],
              "formulaHistory": []
            }
        """.trimIndent()

        assertFailureContains(decode(json), "没有可导入的指标")
    }

    @Test
    fun rejectsDuplicateMetricNames() {
        val weight = rawMetric("weight", name = "体重")
        val duplicate = rawMetric("weight-2", name = " 体重 ")
        val result = decode(BackupCodec.encode(payload(metrics = listOf(weight, duplicate))))

        assertFailureContains(result, "重复的指标名称")
    }

    @Test
    fun rejectsMeasurementWithoutMetric() {
        val measurement = measured("missing", today, 70.0)
        val result = decode(BackupCodec.encode(payload(measurements = listOf(measurement))))

        assertFailureContains(result, "不存在的指标")
    }

    @Test
    fun rejectsTwoMeasurementsOnTheSameDay() {
        val metric = rawMetric("weight", name = "体重")
        val measurements = listOf(
            measured("weight", today, 70.0, id = "a"),
            measured("weight", today, 71.0, id = "b"),
        )

        val result = decode(BackupCodec.encode(payload(metrics = listOf(metric), measurements = measurements)))

        assertFailureContains(result, "同一天有两条")
    }

    @Test
    fun rejectsDerivedWithoutFormula() {
        val broken = derivedMetric("bmi", formula = "", name = "BMI").copy(formula = null)
        val result = decode(BackupCodec.encode(payload(metrics = listOf(broken))))

        assertFailureContains(result, "缺少公式")
    }

    @Test
    fun rejectsFormulaCycle() {
        val a = derivedMetric("a", formula = "{b} + 1", name = "A")
        val b = derivedMetric("b", formula = "{a} + 1", name = "B")

        val result = decode(BackupCodec.encode(payload(metrics = listOf(a, b))))

        assertFailureContains(result, "循环")
    }

    @Test
    fun rejectsUnknownFormulaReference() {
        val a = derivedMetric("a", formula = "{missing} + 1", name = "A")

        val result = decode(BackupCodec.encode(payload(metrics = listOf(a))))

        assertFailureContains(result, "不存在的指标")
    }

    @Test
    fun acceptsUnknownFieldsForForwardCompatibility() {
        val json = BackupCodec.encode(payload())
            .replace("\"format\":", "\"futureField\": 42,\n  \"format\":")

        val decoded = BackupCodec.decode(json)

        assertTrue(decoded is BackupDecodeResult.Success)
    }

    private fun payload(
        metrics: List<Metric> = defaultMetrics(),
        measurements: List<Measurement> = defaultMeasurements(),
    ): BackupPayload {
        val metricIds = metrics.mapTo(HashSet()) { it.id }
        return BackupPayload(
            appVersion = "0.4.0",
            exportedAt = Instant.parse("2026-10-30T08:00:00Z"),
            metrics = metrics,
            measurements = measurements,
            formulaHistory = listOf(
                FormulaHistory(
                    id = "history-1",
                    metricId = "bmi",
                    version = 1,
                    formula = "{weight} / {height} ^ 2",
                    formulaDisplay = "[体重] / [身高] ^ 2",
                    unit = "",
                    changeNote = "第一版",
                    createdAt = 1_780_000_000_000,
                    originDeviceId = "test-device",
                ),
            ).filter { it.metricId in metricIds },
        )
    }

    private fun defaultMetrics(): List<Metric> = listOf(
        rawMetric("weight", name = "体重", unit = "kg"),
        rawMetric("height", name = "身高", unit = "m").copy(
            unitDimension = com.liangji.bodymeasure.domain.model.UnitDimension.LENGTH,
            isConstant = true,
        ),
        derivedMetric(
            id = "bmi",
            formula = "{weight} / {height} ^ 2",
            name = "BMI",
            unit = "",
        ).copy(formulaDisplay = "[体重] / [身高] ^ 2"),
    )

    private fun defaultMeasurements(): List<Measurement> = listOf(
        measured("weight", today, 71.2, note = "早晨空腹"),
        measured("weight", today.minusDays(7), 71.6),
        measured("bmi", today, 24.1),
    ).map { if (it.metricId == "bmi") it.copy(source = MeasurementSource.DERIVED) else it }

    private fun decode(json: String): BackupDecodeResult = BackupCodec.decode(json)

    private fun assertFailureContains(result: BackupDecodeResult, text: String) {
        assertTrue("Expected failure but got $result", result is BackupDecodeResult.Failure)
        assertTrue(
            "Expected message containing \"$text\" but was \"${(result as BackupDecodeResult.Failure).message}\"",
            text in result.message,
        )
    }
}
