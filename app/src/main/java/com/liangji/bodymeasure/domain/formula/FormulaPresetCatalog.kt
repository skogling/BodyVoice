package com.liangji.bodymeasure.domain.formula

import com.liangji.bodymeasure.domain.model.Metric
import com.liangji.bodymeasure.domain.model.UnitDimension

object FormulaPresetCatalog {
    data class Preset(
        val id: String,
        val title: String,
        val description: String,
        val formula: String,
        val unit: String,
    )

    private val WEIGHT_NAMES = setOf("体重", "重量", "weight", "mass")
    private val HEIGHT_NAMES = setOf("身高", "height", "stature")
    private val WAIST_NAMES = setOf("腰围", "waist")
    private val HIP_NAMES = setOf("臀围", "臀", "hip", "hips")
    private val CHEST_NAMES = setOf("胸围", "胸", "chest", "bust")
    private val FAT_NAMES = setOf("体脂率", "体脂", "体脂百分比", "bodyfat", "body fat", "fat%")
    private val PERCENT_UNITS = setOf("%", "％", "百分比", "percent", "pct")

    fun suggest(metrics: List<Metric>): List<Preset> {
        val active = metrics.filter { it.deletedAt == null && !it.isArchived }
        val weight = find(active, WEIGHT_NAMES, UnitDimension.MASS)
        val height = find(active, HEIGHT_NAMES, UnitDimension.LENGTH)
        val waist = find(active, WAIST_NAMES, UnitDimension.LENGTH)
        val hip = find(active, HIP_NAMES, UnitDimension.LENGTH)
        val chest = find(active, CHEST_NAMES, UnitDimension.LENGTH)
        val fat = find(active, FAT_NAMES, dimension = null)

        return buildList {
            if (weight != null && height != null) {
                add(
                    Preset(
                        id = "bmi",
                        title = "BMI",
                        description = "体重 ÷ 身高²，自动按 kg 和米换算",
                        formula = "${massKg(weight)} / ${lengthMetres(height)} ^ 2",
                        unit = "",
                    ),
                )
            }
            if (waist != null && height != null) {
                add(ratio("whtr", "腰高比", "腰围 ÷ 身高", waist, height))
            }
            if (waist != null && hip != null) {
                add(ratio("whr", "腰臀比", "腰围 ÷ 臀围", waist, hip))
            }
            if (waist != null && chest != null) {
                add(ratio("cwr", "胸腰比", "胸围 ÷ 腰围", chest, waist))
            }
            if (weight != null && height != null) {
                add(
                    Preset(
                        id = "bsa",
                        title = "体表面积",
                        description = "Mosteller 公式，结果单位 m²",
                        formula = "sqrt(${lengthCm(height)} * ${massKg(weight)} / 3600)",
                        unit = "m²",
                    ),
                )
            }
            if (weight != null && fat != null) {
                add(
                    Preset(
                        id = "lean-mass",
                        title = "去脂体重",
                        description = "体重 × (1 − 体脂率)，结果单位 kg；体脂率单位是 % 时自动除以 100",
                        formula = "${massKg(weight)} * (1 - ${percent(fat)})",
                        unit = "kg",
                    ),
                )
            }
        }
    }

    private fun find(
        metrics: List<Metric>,
        names: Set<String>,
        dimension: UnitDimension?,
    ): Metric? = metrics.firstOrNull { metric ->
        val normalized = metric.name.trim().lowercase()
        normalized in names && (dimension == null || metric.unitDimension == dimension)
    }

    private fun ratio(
        id: String,
        title: String,
        description: String,
        numerator: Metric,
        denominator: Metric,
    ): Preset {
        val left = lengthDivisor(numerator)
        val right = lengthDivisor(denominator)
        val formula = if (left == right) {
            "${reference(numerator)} / ${reference(denominator)}"
        } else {
            "${divide(numerator, left)} / ${divide(denominator, right)}"
        }
        return Preset(id = id, title = title, description = description, formula = formula, unit = "")
    }

    private fun reference(metric: Metric): String {
        val name = metric.name.trim()
        return if (name.any { it == '[' || it == ']' || it == '\n' }) {
            "{${metric.id}}"
        } else {
            "[$name]"
        }
    }

    private fun divide(metric: Metric, divisor: Double): String =
        if (divisor == 1.0) reference(metric) else "(${reference(metric)} / ${plain(divisor)})"

    private fun lengthDivisor(metric: Metric): Double = when (normalizedUnit(metric)) {
        "mm", "毫米" -> 1000.0
        "cm", "厘米" -> 100.0
        else -> 1.0
    }

    private fun lengthMetres(metric: Metric): String = divide(metric, lengthDivisor(metric))

    private fun lengthCm(metric: Metric): String = when (normalizedUnit(metric)) {
        "m", "米", "公尺" -> "(${reference(metric)} * 100)"
        "mm", "毫米" -> "(${reference(metric)} / 10)"
        else -> reference(metric)
    }

    private fun massKg(metric: Metric): String = when (normalizedUnit(metric)) {
        "斤", "市斤", "jin" -> "(${reference(metric)} / 2)"
        "g", "克" -> "(${reference(metric)} / 1000)"
        else -> reference(metric)
    }

    private fun percent(metric: Metric): String =
        if (normalizedUnit(metric) in PERCENT_UNITS) {
            "(${reference(metric)} / 100)"
        } else {
            reference(metric)
        }

    private fun normalizedUnit(metric: Metric): String = metric.unit.trim().lowercase()

    private fun plain(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
}
