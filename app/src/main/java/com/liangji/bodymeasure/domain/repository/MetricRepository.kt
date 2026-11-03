package com.liangji.bodymeasure.domain.repository

import com.liangji.bodymeasure.domain.model.FormulaHistory
import com.liangji.bodymeasure.domain.model.Metric
import com.liangji.bodymeasure.domain.model.MetricDeleteResult
import com.liangji.bodymeasure.domain.model.MetricDraft
import com.liangji.bodymeasure.domain.model.MetricMove
import com.liangji.bodymeasure.domain.model.MetricWriteResult
import kotlinx.coroutines.flow.Flow

interface MetricRepository {
    fun observeActiveMetrics(): Flow<List<Metric>>

    fun observeAllMetrics(): Flow<List<Metric>>

    fun observeArchivedMetrics(): Flow<List<Metric>>

    fun observeMetric(id: String): Flow<Metric?>

    fun observeFormulaHistory(metricId: String): Flow<List<FormulaHistory>>

    suspend fun create(draft: MetricDraft): MetricWriteResult

    suspend fun update(id: String, draft: MetricDraft): MetricWriteResult

    suspend fun setArchived(id: String, archived: Boolean): Boolean

    suspend fun move(id: String, move: MetricMove): Boolean

    suspend fun delete(id: String): MetricDeleteResult
}
