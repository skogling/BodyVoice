package com.liangji.bodymeasure.data.local.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.liangji.bodymeasure.data.local.entity.MeasurementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementDao {
    @Query(
        """
        SELECT * FROM measurement
        WHERE metric_id = :metricId AND deleted_at IS NULL
        ORDER BY local_date DESC
        """,
    )
    fun observeByMetric(metricId: String): Flow<List<MeasurementEntity>>

    @Query(
        """
        SELECT * FROM measurement
        WHERE deleted_at IS NULL
        ORDER BY local_date DESC
        """,
    )
    fun observeAllActive(): Flow<List<MeasurementEntity>>

    @Query(
        """
        SELECT * FROM measurement
        WHERE id = :id AND deleted_at IS NULL
        LIMIT 1
        """,
    )
    suspend fun getActiveById(id: String): MeasurementEntity?

    @Query(
        """
        SELECT * FROM measurement
        WHERE metric_id = :metricId AND deleted_at IS NULL
        ORDER BY local_date DESC
        """,
    )
    suspend fun getActiveByMetric(metricId: String): List<MeasurementEntity>

    @Query(
        """
        SELECT * FROM measurement
        WHERE metric_id = :metricId AND deleted_at IS NULL
        ORDER BY local_date DESC
        LIMIT 1
        """,
    )
    fun observeLatest(metricId: String): Flow<MeasurementEntity?>

    @Query(
        """
        SELECT * FROM measurement
        WHERE metric_id = :metricId
          AND local_date = :localDate
          AND deleted_at IS NULL
        LIMIT 1
        """,
    )
    suspend fun getByMetricAndDate(metricId: String, localDate: Long): MeasurementEntity?

    @Upsert
    suspend fun upsert(measurement: MeasurementEntity)

    @Query(
        """
        UPDATE measurement
        SET deleted_at = :deletedAt, updated_at = :deletedAt
        WHERE id = :id AND deleted_at IS NULL
        """,
    )
    suspend fun softDelete(id: String, deletedAt: Long)

    @Query("SELECT * FROM measurement WHERE deleted_at IS NULL")
    suspend fun getAllActive(): List<MeasurementEntity>

    @Query("DELETE FROM measurement")
    suspend fun deleteAll()
}
