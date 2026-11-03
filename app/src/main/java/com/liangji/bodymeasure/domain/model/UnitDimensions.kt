package com.liangji.bodymeasure.domain.model

object UnitDimensions {
    fun infer(unit: String): UnitDimension = when (unit.trim().lowercase()) {
        "kg", "g", "斤", "公斤", "克", "lb", "lbs" -> UnitDimension.MASS
        "cm", "mm", "m", "in", "inch", "厘米", "毫米", "米" -> UnitDimension.LENGTH
        "%", "％", "百分比" -> UnitDimension.RATIO
        else -> UnitDimension.CUSTOM
    }
}
