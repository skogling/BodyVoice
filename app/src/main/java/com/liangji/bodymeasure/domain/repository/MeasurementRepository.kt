package com.liangji.bodymeasure.domain.repository

import com.liangji.bodymeasure.domain.model.Measurement
import com.liangji.bodymeasure.domain.model.MeasurementChange
import com.liangji.bodymeasure.domain.model.MeasurementInput
import com.liangji.bodymeasure.domain.model.MeasurementWriteResult
import kotlinx.coroutines.flow.Flow

interface MeasurementRepository {
    fun observeAllActive(): Flow<List<Measurement>>

    fun observeForMetric(metricId: String): Flow<List<Measurement>>

    suspend fun getForMetric(metricId: String): List<Measurement>

    suspend fun save(input: MeasurementInput, replaceExisting: Boolean): MeasurementWriteResult

    suspend fun delete(id: String): Measurement?

    suspend fun undo(change: MeasurementChange)

    suspend fun restore(measurement: Measurement)
}
