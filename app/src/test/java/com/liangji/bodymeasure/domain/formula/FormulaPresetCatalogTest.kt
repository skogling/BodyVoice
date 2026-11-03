package com.liangji.bodymeasure.domain.formula

import com.liangji.bodymeasure.core.common.NumberFormats
import com.liangji.bodymeasure.domain.model.Metric
import com.liangji.bodymeasure.domain.model.UnitDimension
import com.liangji.bodymeasure.testing.rawMetric
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormulaPresetCatalogTest {

    private val weight = rawMetric("weight", name = "体重", unit = "kg")
    private val heightCm = rawMetric("height", name = "身高", unit = "cm")
        .copy(unitDimension = UnitDimension.LENGTH)
    private val heightM = heightCm.copy(unit = "m")
    private val waist = rawMetric("waist", name = "腰围", unit = "cm")
        .copy(unitDimension = UnitDimension.LENGTH)
    private val hip = rawMetric("hip", name = "臀围", unit = "cm")
        .copy(unitDimension = UnitDimension.LENGTH)
    private val chest = rawMetric("chest", name = "胸围", unit = "cm")
        .copy(unitDimension = UnitDimension.LENGTH)
    private val fatPercent = rawMetric("fat", name = "体脂率", unit = "%")
        .copy(unitDimension = UnitDimension.RATIO)

    @Test
    fun bmiConvertsCentimetresToMetres() {
        val metrics = listOf(weight, heightCm)

        val preset = preset("bmi", metrics)

        assertEquals("[体重] / ([身高] / 100) ^ 2", preset.formula)
        val value = evaluate(preset, metrics, mapOf("体重" to 71.2, "身高" to 172.0))
        assertEquals("24.1", NumberFormats.format(value, 1))
    }

    @Test
    fun bmiKeepsMetresAsIs() {
        val metrics = listOf(weight, heightM)

        val preset = preset("bmi", metrics)

        assertEquals("[体重] / [身高] ^ 2", preset.formula)
        val value = evaluate(preset, metrics, mapOf("体重" to 71.2, "身高" to 1.72))
        assertEquals("24.1", NumberFormats.format(value, 1))
    }

    @Test
    fun jinWeightIsHalved() {
        val metrics = listOf(weight.copy(unit = "斤"), heightCm)

        val preset = preset("bmi", metrics)

        assertEquals("([体重] / 2) / ([身高] / 100) ^ 2", preset.formula)
        val value = evaluate(preset, metrics, mapOf("体重" to 142.4, "身高" to 172.0))
        assertEquals("24.1", NumberFormats.format(value, 1))
    }

    @Test
    fun ratiosSkipConversionWhenUnitsMatch() {
        val metrics = listOf(waist, hip, chest, heightCm)

        assertEquals("[腰围] / [身高]", preset("whtr", metrics).formula)
        assertEquals("[腰围] / [臀围]", preset("whr", metrics).formula)
        assertEquals("[胸围] / [腰围]", preset("cwr", metrics).formula)
    }

    @Test
    fun ratiosConvertWhenUnitsDiffer() {
        val metrics = listOf(waist, hip.copy(unit = "m"))

        assertEquals("([腰围] / 100) / [臀围]", preset("whr", metrics).formula)
    }

    @Test
    fun bodySurfaceAreaUsesMosteller() {
        val metrics = listOf(weight, heightCm)

        val preset = preset("bsa", metrics)

        assertEquals("sqrt([身高] * [体重] / 3600)", preset.formula)
        assertEquals("m²", preset.unit)
        val value = evaluate(preset, metrics, mapOf("体重" to 71.2, "身高" to 172.0))
        assertEquals(1.844, value, 0.001)
    }

    @Test
    fun leanMassDividesPercentUnitByHundred() {
        val metrics = listOf(weight, fatPercent)

        val preset = preset("lean-mass", metrics)

        assertEquals("[体重] * (1 - ([体脂率] / 100))", preset.formula)
        assertEquals("kg", preset.unit)
        val value = evaluate(preset, metrics, mapOf("体重" to 71.2, "体脂率" to 21.0))
        assertEquals("56.2", NumberFormats.format(value, 1))
    }

    @Test
    fun leanMassKeepsFractionWhenUnitIsBlank() {
        val metrics = listOf(weight, fatPercent.copy(unit = ""))

        val preset = preset("lean-mass", metrics)

        assertEquals("[体重] * (1 - [体脂率])", preset.formula)
        val value = evaluate(preset, metrics, mapOf("体重" to 71.2, "体脂率" to 0.21))
        assertEquals("56.2", NumberFormats.format(value, 1))
    }

    @Test
    fun archivedAndDeletedMetricsAreIgnored() {
        assertTrue(
            FormulaPresetCatalog.suggest(
                listOf(weight, heightCm.copy(isArchived = true)),
            ).none { it.id == "bmi" },
        )
        assertTrue(
            FormulaPresetCatalog.suggest(
                listOf(weight, heightCm.copy(deletedAt = 1L)),
            ).none { it.id == "bmi" },
        )
    }

    @Test
    fun missingRolesOmitTheirPresets() {
        val weightOnly = FormulaPresetCatalog.suggest(listOf(weight))

        assertNull(weightOnly.firstOrNull { it.id == "bmi" })
        assertNull(weightOnly.firstOrNull { it.id == "bsa" })
        assertTrue(weightOnly.isEmpty())
    }

    private fun preset(id: String, metrics: List<Metric>): FormulaPresetCatalog.Preset =
        checkNotNull(FormulaPresetCatalog.suggest(metrics).firstOrNull { it.id == id })

    private fun evaluate(
        preset: FormulaPresetCatalog.Preset,
        metrics: List<Metric>,
        values: Map<String, Double>,
    ): Double {
        val byName = metrics.associateBy { it.name }
        val parsed = FormulaParser.parse(preset.formula) { name -> byName[name]?.id }
        val expression = (parsed as FormulaParseResult.Valid).expression
        val byId = metrics.associateBy { it.id }
        return checkNotNull(
            FormulaEvaluator.evaluate(expression) { id -> values[byId[id]?.name] },
        )
    }
}
