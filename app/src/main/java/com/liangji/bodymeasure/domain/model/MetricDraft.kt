package com.liangji.bodymeasure.domain.model

import com.liangji.bodymeasure.domain.formula.FormulaError

data class MetricDraft(
    val name: String,
    val unit: String,
    val unitDimension: UnitDimension,
    val precision: Int,
    val colorArgb: Long,
    val iconKey: String,
    val isConstant: Boolean = false,
    val kind: MetricKind = MetricKind.RAW,
    val formula: String? = null,
    val pairingPolicy: PairingPolicy = PairingPolicy.NEAREST,
    val pairingWindowDays: Int = 2,
    val formulaChangeNote: String? = null,
)

sealed interface MetricWriteResult {
    data class Saved(val metric: Metric) : MetricWriteResult

    data class DuplicateName(val name: String) : MetricWriteResult

    data class InvalidFormula(val error: FormulaError) : MetricWriteResult

    data class UnknownReference(val ids: List<String>) : MetricWriteResult

    data class CycleDetected(val path: List<String>) : MetricWriteResult

    data class TooDeep(val depth: Int) : MetricWriteResult

    data class ReferencedBy(val names: List<String>) : MetricWriteResult

    data object KindLocked : MetricWriteResult

    data object NotFound : MetricWriteResult
}

sealed interface MetricDeleteResult {
    data object Deleted : MetricDeleteResult

    data object NotFound : MetricDeleteResult

    data class ReferencedBy(val names: List<String>) : MetricDeleteResult
}
