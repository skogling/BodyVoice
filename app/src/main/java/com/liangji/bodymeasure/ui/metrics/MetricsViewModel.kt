package com.liangji.bodymeasure.ui.metrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liangji.bodymeasure.core.common.AppClock
import com.liangji.bodymeasure.domain.model.Measurement
import com.liangji.bodymeasure.domain.model.MeasurementChange
import com.liangji.bodymeasure.domain.model.MeasurementInput
import com.liangji.bodymeasure.domain.model.MeasurementWriteResult
import com.liangji.bodymeasure.domain.model.Metric
import com.liangji.bodymeasure.domain.model.MetricKind
import com.liangji.bodymeasure.domain.model.MetricMove
import com.liangji.bodymeasure.domain.model.MetricPreset
import com.liangji.bodymeasure.domain.model.MetricWriteResult
import com.liangji.bodymeasure.domain.repository.MeasurementRepository
import com.liangji.bodymeasure.domain.repository.MetricRepository
import com.liangji.bodymeasure.domain.repository.SettingsRepository
import com.liangji.bodymeasure.domain.summary.DerivedSeriesCalculator
import com.liangji.bodymeasure.domain.summary.MetricSnapshot
import com.liangji.bodymeasure.domain.summary.MetricSummaries
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val today: LocalDate,
    val snapshots: List<MetricSnapshot> = emptyList(),
    val measurementsByMetric: Map<String, List<Measurement>> = emptyMap(),
    val lastMetricId: String? = null,
    val isLoading: Boolean = true,
) {
    val metrics: List<Metric> get() = snapshots.map { it.metric }

    val recordableMetrics: List<Metric>
        get() = snapshots.map { it.metric }.filter { it.kind == MetricKind.RAW }

    val recordedTodayCount: Int get() = snapshots.count { it.recordedToday && it.metric.kind == MetricKind.RAW }

    val featuredSnapshot: MetricSnapshot?
        get() = snapshots
            .filter { it.latest != null }
            .maxWithOrNull(
                compareBy<MetricSnapshot>({ it.latest!!.date }, { it.latest!!.measuredAt }),
            )
            ?: snapshots.firstOrNull()

    val otherSnapshots: List<MetricSnapshot>
        get() {
            val featuredId = featuredSnapshot?.metric?.id
            return snapshots.filter { it.metric.id != featuredId }
        }
}

data class TemplateResult(
    val created: Int,
    val skipped: List<String>,
)

@HiltViewModel
class MetricsViewModel @Inject constructor(
    private val metricRepository: MetricRepository,
    private val measurementRepository: MeasurementRepository,
    private val settingsRepository: SettingsRepository,
    clock: AppClock,
) : ViewModel() {
    private val today = clock.today()

    val uiState: StateFlow<HomeUiState> = combine(
        metricRepository.observeAllMetrics(),
        measurementRepository.observeAllActive(),
        settingsRepository.lastMetricId,
    ) { allMetrics, measurements, lastMetricId ->
        val activeMetrics = allMetrics.filter { !it.isArchived }
        val derived = DerivedSeriesCalculator.build(allMetrics, measurements)
        val merged = if (derived.isEmpty()) measurements else measurements + derived.values.flatten()
        val recordable = activeMetrics.filter { it.kind == MetricKind.RAW }
        HomeUiState(
            today = today,
            snapshots = MetricSummaries.build(activeMetrics, merged, today),
            measurementsByMetric = merged.groupBy { it.metricId },
            lastMetricId = lastMetricId?.takeIf { id -> recordable.any { it.id == id } },
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(today = today),
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

    suspend fun move(id: String, move: MetricMove) {
        metricRepository.move(id, move)
    }

    suspend fun setArchived(id: String, archived: Boolean) {
        metricRepository.setArchived(id, archived)
    }

    suspend fun createFromPresets(presets: List<MetricPreset>): TemplateResult {
        var created = 0
        val skipped = mutableListOf<String>()
        for (preset in presets) {
            when (val result = metricRepository.create(preset.toDraft())) {
                is MetricWriteResult.Saved -> created++
                is MetricWriteResult.DuplicateName -> skipped += result.name
                else -> Unit
            }
        }
        return TemplateResult(created = created, skipped = skipped)
    }
}
