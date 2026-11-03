package com.liangji.bodymeasure.ui.metrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liangji.bodymeasure.domain.model.Metric
import com.liangji.bodymeasure.domain.model.MetricDeleteResult
import com.liangji.bodymeasure.domain.repository.MetricRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ArchivedMetricsViewModel @Inject constructor(
    private val metricRepository: MetricRepository,
) : ViewModel() {
    val metrics: StateFlow<List<Metric>> = metricRepository.observeArchivedMetrics().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun restore(id: String) {
        viewModelScope.launch {
            metricRepository.setArchived(id, false)
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            when (val result = metricRepository.delete(id)) {
                is MetricDeleteResult.ReferencedBy -> {
                    _message.value = "「${result.names.joinToString("、")}」正在引用它，暂时不能删除。"
                }

                else -> Unit
            }
        }
    }

    fun dismissMessage() {
        _message.value = null
    }
}
