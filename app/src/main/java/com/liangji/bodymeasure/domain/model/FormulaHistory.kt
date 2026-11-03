package com.liangji.bodymeasure.domain.model

data class FormulaHistory(
    val id: String,
    val metricId: String,
    val version: Int,
    val formula: String,
    val formulaDisplay: String,
    val unit: String,
    val changeNote: String?,
    val createdAt: Long,
    val originDeviceId: String,
)
