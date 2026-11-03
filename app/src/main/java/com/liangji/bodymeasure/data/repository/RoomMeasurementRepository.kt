package com.liangji.bodymeasure.data.repository

import androidx.room.withTransaction
import com.liangji.bodymeasure.core.common.AppClock
import com.liangji.bodymeasure.data.local.datastore.DeviceIdProvider
import com.liangji.bodymeasure.data.local.db.AppDatabase
import com.liangji.bodymeasure.data.local.db.MeasurementDao
import com.liangji.bodymeasure.data.local.db.MetricDao
import com.liangji.bodymeasure.data.local.entity.MeasurementEntity
import com.liangji.bodymeasure.data.local.entity.toDomain
import com.liangji.bodymeasure.data.local.entity.toEntity
import com.liangji.bodymeasure.domain.model.Measurement
import com.liangji.bodymeasure.domain.model.MeasurementChange
import com.liangji.bodymeasure.domain.model.MeasurementInput
import com.liangji.bodymeasure.domain.model.MeasurementSource
import com.liangji.bodymeasure.domain.model.MeasurementWriteResult
import com.liangji.bodymeasure.domain.model.MetricKind
import com.liangji.bodymeasure.domain.repository.MeasurementRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class RoomMeasurementRepository @Inject constructor(
    private val database: AppDatabase,
    private val measurementDao: MeasurementDao,
    private val metricDao: MetricDao,
    private val clock: AppClock,
    private val deviceIdProvider: DeviceIdProvider,
) : MeasurementRepository {
    override fun observeAllActive(): Flow<List<Measurement>> =
        measurementDao.observeAllActive().map { entities -> entities.map(MeasurementEntity::toDomain) }

    override fun observeForMetric(metricId: String): Flow<List<Measurement>> =
        measurementDao.observeByMetric(metricId).map { entities -> entities.map(MeasurementEntity::toDomain) }

    override suspend fun getForMetric(metricId: String): List<Measurement> =
        measurementDao.getActiveByMetric(metricId).map(MeasurementEntity::toDomain)

    override suspend fun save(
        input: MeasurementInput,
        replaceExisting: Boolean,
    ): MeasurementWriteResult {
        if (!input.value.isFinite()) return MeasurementWriteResult.Invalid
        return database.withTransaction {
            val metric = metricDao.getById(input.metricId)
                ?: return@withTransaction MeasurementWriteResult.NotFound
            if (metric.kind != MetricKind.RAW) {
                return@withTransaction MeasurementWriteResult.Invalid
            }

            val now = clock.now().toEpochMilli()
            val localDate = input.date.toEpochDay()
            val note = input.note?.trim()?.takeIf { it.isNotEmpty() }

            val existing = input.existingId?.let { id ->
                measurementDao.getActiveById(id) ?: return@withTransaction MeasurementWriteResult.NotFound
            }
            val sameDay = measurementDao.getByMetricAndDate(input.metricId, localDate)
            val replaced = sameDay?.takeIf { it.id != existing?.id }
            if (replaced != null) {
                if (!replaceExisting) {
                    return@withTransaction MeasurementWriteResult.Conflict(replaced.toDomain())
                }
                measurementDao.softDelete(replaced.id, now)
            }

            val saved = if (existing != null) {
                existing.copy(
                    localDate = localDate,
                    value = input.value,
                    note = note,
                    updatedAt = now,
                )
            } else {
                MeasurementEntity(
                    id = UUID.randomUUID().toString(),
                    metricId = input.metricId,
                    localDate = localDate,
                    measuredAt = now,
                    value = input.value,
                    note = note,
                    source = MeasurementSource.MANUAL,
                    createdAt = now,
                    updatedAt = now,
                    deletedAt = null,
                    originDeviceId = deviceIdProvider.getOrCreate(),
                )
            }
            measurementDao.upsert(saved)

            MeasurementWriteResult.Saved(
                MeasurementChange(
                    saved = saved.toDomain(),
                    previous = existing?.toDomain(),
                    replaced = replaced?.toDomain(),
                ),
            )
        }
    }

    override suspend fun delete(id: String): Measurement? = database.withTransaction {
        val existing = measurementDao.getActiveById(id) ?: return@withTransaction null
        measurementDao.softDelete(id, clock.now().toEpochMilli())
        existing.toDomain()
    }

    override suspend fun undo(change: MeasurementChange) {
        database.withTransaction {
            val now = clock.now().toEpochMilli()
            val previous = change.previous
            if (previous == null) {
                measurementDao.softDelete(change.saved.id, now)
            } else {
                measurementDao.upsert(previous.copy(updatedAt = now).toEntity())
            }
            change.replaced?.let { replaced ->
                measurementDao.upsert(replaced.copy(updatedAt = now, deletedAt = null).toEntity())
            }
        }
    }

    override suspend fun restore(measurement: Measurement) {
        measurementDao.upsert(
            measurement.copy(deletedAt = null, updatedAt = clock.now().toEpochMilli()).toEntity(),
        )
    }
}
