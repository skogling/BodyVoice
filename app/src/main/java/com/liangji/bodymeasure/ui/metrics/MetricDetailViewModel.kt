package com.liangji.bodymeasure.ui.metrics

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liangji.bodymeasure.core.common.AppClock
import com.liangji.bodymeasure.domain.model.FormulaHistory
import com.liangji.bodymeasure.domain.model.Measurement
import com.liangji.bodymeasure.domain.model.MeasurementChange
import com.liangji.bodymeasure.domain.model.MeasurementInput
import com.liangji.bodymeasure.domain.model.MeasurementWriteResult
import com.liangji.bodymeasure.domain.model.Metric
import com.liangji.bodymeasure.domain.model.MetricKind
import com.liangji.bodymeasure.domain.repository.MeasurementRepository
import com.liangji.bodymeasure.domain.repository.MetricRepository
import com.liangji.bodymeasure.domain.repository.SettingsRepository
import com.liangji.bodymeasure.domain.summary.DerivedSeriesCalculator
import com.liangji.bodymeasure.domain.summary.MeasurementHistory
import com.liangji.bodymeasure.domain.summary.MetricSnapshot
import com.liangji.bodymeasure.domain.summary.MetricSummaries
import com.liangji.bodymeasure.domain.summary.MonthSection
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class MetricDetailState(
    val today: LocalDate,
    val loading: Boolean = true,
    val missing: Boolean = false,
    val metric: Metric? = null,
    val snapshot: MetricSnapshot? = null,
    val measurements: List<Measurement> = emptyList(),
    val sections: List<MonthSection> = emptyList(),
    val formulaHistory: List<FormulaHistory> = emptyList(),
) {
    val recordCount: Int get() = measurements.size

    val isDerived: Boolean get() = metric?.kind == MetricKind.DERIVED
}

internal fun metricDetailState(
    metric: Metric?,
    measurements: List<Measurement>,
    today: LocalDate,
    loading: Boolean = false,
): MetricDetailState {
    val sorted = measurements.sortedWith(
        compareByDescending<Measurement> { it.date }.thenByDescending { it.measuredAt },
    )
    return MetricDetailState(
        today = today,
        loading = loading,
        missing = !loading && metric == null,
        metric = metric,
        snapshot = metric?.let { MetricSummaries.build(listOf(it), sorted, today).single() },
        measurements = sorted,
        sections = MeasurementHistory.groupByMonthDescending(sorted),
    )
}

@HiltViewModel
class MetricDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val metricRepository: MetricRepository,
    private val measurementRepository: MeasurementRepository,
    private val settingsRepository: SettingsRepository,
    clock: AppClock,
) : ViewModel() {
    private val metricId: String = checkNotNull(savedStateHandle["metricId"])
    private val today = clock.today()

    val uiState: StateFlow<MetricDetailState> = combine(
        metricRepository.observeMetric(metricId),
        metricRepository.observeAllMetrics(),
        measurementRepository.observeAllActive(),
        metricRepository.observeFormulaHistory(metricId),
    ) { metric, allMetrics, allMeasurements, history ->
        val measurements = when {
            metric == null -> emptyList()
            metric.kind == MetricKind.DERIVED ->
                DerivedSeriesCalculator.build(allMetrics, allMeasurements)[metricId].orEmpty()
            else -> allMeasurements.filter { it.metricId == metricId }
        }
        metricDetailState(metric, measurements, today).copy(formulaHistory = history)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MetricDetailState(today = today),
    )

    suspend fun save(input: MeasurementInput, replaceExisting: Boolean): MeasurementWriteResult {
        val result = measurementRepository.save(input, replaceExisting)
        if (result is MeasurementWriteResult.Saved) {
            settingsRepository.setLastMetricId(input.metricId)
        }
        return result
    }

    suspend fun undo(change: MeasurementChange) {
        measurementRepository.undo(change)
    }

    suspend fun delete(id: String): Measurement? = measurementRepository.delete(id)

    suspend fun restore(measurement: Measurement) {
        measurementRepository.restore(measurement)
    }

    suspend fun setArchived(archived: Boolean): Boolean =
        metricRepository.setArchived(metricId, archived)
}
