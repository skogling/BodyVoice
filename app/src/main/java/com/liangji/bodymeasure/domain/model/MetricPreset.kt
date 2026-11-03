package com.liangji.bodymeasure.domain.model

object MetricIconKeys {
    const val WEIGHT = "weight"
    const val CHEST = "chest"
    const val WAIST = "waist"
    const val HIP = "hip"
    const val ARM = "arm"
    const val THIGH = "thigh"
    const val CALF = "calf"
    const val HEIGHT = "height"
    const val GENERIC = "generic"

    val all = listOf(WEIGHT, CHEST, WAIST, HIP, ARM, THIGH, CALF, HEIGHT, GENERIC)
}

object MetricPalette {
    const val CLAY = 0xFFC96F4AL
    const val TEAL = 0xFF3F7F72L
    const val OCHRE = 0xFFB98A3EL
    const val INDIGO = 0xFF5D6FA8L
    const val PLUM = 0xFF8A6478L
    const val SAGE = 0xFF6F8A63L

    val all = listOf(CLAY, TEAL, OCHRE, INDIGO, PLUM, SAGE)
}

data class MetricPreset(
    val key: String,
    val name: String,
    val unit: String,
    val unitDimension: UnitDimension,
    val precision: Int,
    val colorArgb: Long,
    val iconKey: String,
    val isConstant: Boolean = false,
) {
    fun toDraft(): MetricDraft = MetricDraft(
        name = name,
        unit = unit,
        unitDimension = unitDimension,
        precision = precision,
        colorArgb = colorArgb,
        iconKey = iconKey,
        isConstant = isConstant,
    )
}

object MetricPresets {
    val all = listOf(
        MetricPreset(
            key = "weight",
            name = "体重",
            unit = "kg",
            unitDimension = UnitDimension.MASS,
            precision = 1,
            colorArgb = MetricPalette.CLAY,
            iconKey = MetricIconKeys.WEIGHT,
        ),
        MetricPreset(
            key = "chest",
            name = "胸围",
            unit = "cm",
            unitDimension = UnitDimension.LENGTH,
            precision = 1,
            colorArgb = MetricPalette.TEAL,
            iconKey = MetricIconKeys.CHEST,
        ),
        MetricPreset(
            key = "waist",
            name = "腰围",
            unit = "cm",
            unitDimension = UnitDimension.LENGTH,
            precision = 1,
            colorArgb = MetricPalette.OCHRE,
            iconKey = MetricIconKeys.WAIST,
        ),
        MetricPreset(
            key = "hip",
            name = "臀围",
            unit = "cm",
            unitDimension = UnitDimension.LENGTH,
            precision = 1,
            colorArgb = MetricPalette.PLUM,
            iconKey = MetricIconKeys.HIP,
        ),
        MetricPreset(
            key = "arm",
            name = "臂围",
            unit = "cm",
            unitDimension = UnitDimension.LENGTH,
            precision = 1,
            colorArgb = MetricPalette.INDIGO,
            iconKey = MetricIconKeys.ARM,
        ),
        MetricPreset(
            key = "thigh",
            name = "大腿围",
            unit = "cm",
            unitDimension = UnitDimension.LENGTH,
            precision = 1,
            colorArgb = MetricPalette.SAGE,
            iconKey = MetricIconKeys.THIGH,
        ),
        MetricPreset(
            key = "calf",
            name = "小腿围",
            unit = "cm",
            unitDimension = UnitDimension.LENGTH,
            precision = 1,
            colorArgb = MetricPalette.TEAL,
            iconKey = MetricIconKeys.CALF,
        ),
        MetricPreset(
            key = "height",
            name = "身高",
            unit = "cm",
            unitDimension = UnitDimension.LENGTH,
            precision = 1,
            colorArgb = MetricPalette.INDIGO,
            iconKey = MetricIconKeys.HEIGHT,
            isConstant = true,
        ),
    )
}
