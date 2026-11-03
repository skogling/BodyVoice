package com.liangji.bodymeasure.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.liangji.bodymeasure.data.local.entity.MetricDependencyEntity

@Dao
abstract class MetricDependencyDao {
    @Query("SELECT depends_on_id FROM metric_dependency WHERE metric_id = :metricId")
    abstract suspend fun dependenciesOf(metricId: String): List<String>

    @Query("SELECT metric_id FROM metric_dependency WHERE depends_on_id = :metricId")
    abstract suspend fun dependentsOf(metricId: String): List<String>

    @Query(
        """
        SELECT m.name FROM metric_dependency d
        JOIN metric_def m ON m.id = d.metric_id
        WHERE d.depends_on_id = :metricId AND m.deleted_at IS NULL
        ORDER BY m.name
        """,
    )
    abstract suspend fun activeDependentNamesOf(metricId: String): List<String>

    @Query("SELECT * FROM metric_dependency")
    abstract suspend fun getAll(): List<MetricDependencyEntity>

    @Query("DELETE FROM metric_dependency WHERE metric_id = :metricId")
    abstract suspend fun deleteFor(metricId: String)

    @Query("DELETE FROM metric_dependency")
    abstract suspend fun deleteAll()

    @Query(
        """
        DELETE FROM metric_dependency
        WHERE depends_on_id = :metricId
          AND metric_id NOT IN (SELECT id FROM metric_def WHERE deleted_at IS NULL)
        """,
    )
    abstract suspend fun deleteIncomingFromDeleted(metricId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertAll(rows: List<MetricDependencyEntity>)

    @Transaction
    open suspend fun replace(metricId: String, dependsOnIds: List<String>) {
        deleteFor(metricId)
        if (dependsOnIds.isNotEmpty()) {
            insertAll(dependsOnIds.map { MetricDependencyEntity(metricId, it) })
        }
    }
}
