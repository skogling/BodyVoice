package com.liangji.bodymeasure.domain.model

import java.time.LocalDate

data class Measurement(
    val id: String,
    val metricId: String,
    val date: LocalDate,
    val measuredAt: Long,
    val value: Double,
    val note: String?,
    val source: MeasurementSource,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val originDeviceId: String,
)
