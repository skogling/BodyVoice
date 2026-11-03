package com.liangji.bodymeasure.domain.model

enum class MetricKind {
    RAW,
    DERIVED,
}

enum class UnitDimension {
    LENGTH,
    MASS,
    RATIO,
    COMPOSITE,
    CUSTOM,
}

enum class PairingPolicy {
    SAME_DAY,
    NEAREST,
    CARRY_FORWARD,
}

data class Metric(
    val id: String,
    val name: String,
    val kind: MetricKind,
    val unit: String,
    val unitDimension: UnitDimension,
    val precision: Int,
    val colorArgb: Long,
    val iconKey: String,
    val formula: String?,
    val formulaDisplay: String?,
    val formulaVersion: Int,
    val pairingPolicy: PairingPolicy,
    val pairingWindowDays: Int,
    val isConstant: Boolean,
    val sortOrder: Int,
    val isArchived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val originDeviceId: String,
)
