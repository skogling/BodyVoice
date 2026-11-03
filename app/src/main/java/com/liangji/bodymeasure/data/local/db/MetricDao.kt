package com.liangji.bodymeasure.data.local.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.liangji.bodymeasure.data.local.entity.MetricEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MetricDao {
    @Query(
        """
        SELECT * FROM metric_def
        WHERE deleted_at IS NULL AND is_archived = 0
        ORDER BY sort_order ASC, created_at ASC
        """,
    )
    fun observeActive(): Flow<List<MetricEntity>>

    @Query(
        """
        SELECT * FROM metric_def
        WHERE deleted_at IS NULL
        ORDER BY sort_order ASC, created_at ASC
        """,
    )
    fun observeAll(): Flow<List<MetricEntity>>

    @Query(
        """
        SELECT * FROM metric_def
        WHERE deleted_at IS NULL AND is_archived = 1
        ORDER BY sort_order ASC, created_at ASC
        """,
    )
    fun observeArchived(): Flow<List<MetricEntity>>

    @Query("SELECT COALESCE(MAX(sort_order), -1) FROM metric_def WHERE deleted_at IS NULL")
    suspend fun maxSortOrder(): Int

    @Query(
        """
        SELECT * FROM metric_def
        WHERE deleted_at IS NULL AND is_archived = 0
        ORDER BY sort_order ASC, created_at ASC
        """,
    )
    suspend fun getActive(): List<MetricEntity>

    @Query(
        """
        SELECT * FROM metric_def
        WHERE id = :id AND deleted_at IS NULL
        """,
    )
    fun observeById(id: String): Flow<MetricEntity?>

    @Query(
        """
        SELECT * FROM metric_def
        WHERE id = :id AND deleted_at IS NULL
        """,
    )
    suspend fun getById(id: String): MetricEntity?

    @Query("SELECT * FROM metric_def WHERE deleted_at IS NULL")
    suspend fun getAll(): List<MetricEntity>

    @Query("SELECT * FROM metric_def WHERE deleted_at IS NULL AND id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<MetricEntity>

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM metric_def
            WHERE deleted_at IS NULL
              AND LOWER(TRIM(name)) = LOWER(TRIM(:name))
              AND id != :excludeId
        )
        """,
    )
    suspend fun existsByName(name: String, excludeId: String): Boolean

    @Upsert
    suspend fun upsert(metric: MetricEntity)

    @Query(
        """
        UPDATE metric_def
        SET is_archived = :archived, updated_at = :updatedAt
        WHERE id = :id AND deleted_at IS NULL
        """,
    )
    suspend fun setArchived(id: String, archived: Boolean, updatedAt: Long)

    @Query(
        """
        UPDATE metric_def
        SET deleted_at = :deletedAt, updated_at = :deletedAt
        WHERE id = :id AND deleted_at IS NULL
        """,
    )
    suspend fun softDelete(id: String, deletedAt: Long)

    @Query("DELETE FROM metric_def")
    suspend fun deleteAll()
}
