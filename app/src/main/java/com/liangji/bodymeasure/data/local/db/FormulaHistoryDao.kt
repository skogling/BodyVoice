package com.liangji.bodymeasure.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.liangji.bodymeasure.data.local.entity.FormulaHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FormulaHistoryDao {
    @Query(
        """
        SELECT * FROM formula_history
        WHERE metric_id = :metricId
        ORDER BY created_at DESC
        """,
    )
    fun observeByMetric(metricId: String): Flow<List<FormulaHistoryEntity>>

    @Query("SELECT * FROM formula_history")
    suspend fun getAll(): List<FormulaHistoryEntity>

    @Insert
    suspend fun insert(history: FormulaHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(history: FormulaHistoryEntity): Long

    @Query("DELETE FROM formula_history")
    suspend fun deleteAll()
}
