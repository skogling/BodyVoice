package com.liangji.bodymeasure.domain.model

import java.time.LocalDate

data class MeasurementInput(
    val metricId: String,
    val date: LocalDate,
    val value: Double,
    val note: String? = null,
    val existingId: String? = null,
)

data class MeasurementChange(
    val saved: Measurement,
    val previous: Measurement?,
    val replaced: Measurement?,
)

sealed interface MeasurementWriteResult {
    data class Saved(val change: MeasurementChange) : MeasurementWriteResult

    data class Conflict(val existing: Measurement) : MeasurementWriteResult

    data object Invalid : MeasurementWriteResult

    data object NotFound : MeasurementWriteResult
}
