package com.liangji.bodymeasure.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liangji.bodymeasure.core.common.AppClock
import com.liangji.bodymeasure.domain.backup.BackupExportResult
import com.liangji.bodymeasure.domain.backup.BackupImportResult
import com.liangji.bodymeasure.domain.backup.BackupPreviewResult
import com.liangji.bodymeasure.domain.backup.BackupSummary
import com.liangji.bodymeasure.domain.backup.CsvExportResult
import com.liangji.bodymeasure.domain.backup.ImportMode
import com.liangji.bodymeasure.domain.model.ThemeMode
import com.liangji.bodymeasure.domain.repository.BackupRepository
import com.liangji.bodymeasure.domain.repository.MetricRepository
import com.liangji.bodymeasure.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class BackupTask {
    EXPORT_BACKUP,
    EXPORT_CSV,
    PREVIEW_IMPORT,
    IMPORT,
}

data class PendingImport(
    val uri: String,
    val summary: BackupSummary,
)

data class BackupNotice(
    val title: String,
    val message: String,
    val isError: Boolean = false,
)

data class BackupUiState(
    val task: BackupTask? = null,
    val pendingImport: PendingImport? = null,
    val notice: BackupNotice? = null,
) {
    val isBusy: Boolean get() = task != null
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val metricRepository: MetricRepository,
    private val backupRepository: BackupRepository,
    private val clock: AppClock,
) : ViewModel() {
    val themeMode = settingsRepository.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ThemeMode.SYSTEM,
    )

    val archivedCount = metricRepository.observeArchivedMetrics()
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0,
        )

    private val _backup = MutableStateFlow(BackupUiState())
    val backup: StateFlow<BackupUiState> = _backup.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun backupFileName(): String = "身语备份-${clock.today()}.json"

    fun csvFileName(): String = "身语记录-${clock.today()}.csv"

    fun exportBackup(uri: String) = runTask(BackupTask.EXPORT_BACKUP) {
        when (val result = backupRepository.exportBackup(uri)) {
            is BackupExportResult.Success -> showNotice(
                BackupNotice("备份已导出", BackupMessages.exportSuccess(result.summary)),
            )

            is BackupExportResult.Failure -> showNotice(
                BackupNotice("备份没有导出", result.message, isError = true),
            )
        }
    }

    fun exportCsv(uri: String) = runTask(BackupTask.EXPORT_CSV) {
        when (val result = backupRepository.exportCsv(uri)) {
            is CsvExportResult.Success -> showNotice(
                BackupNotice(
                    "表格已导出",
                    BackupMessages.csvSuccess(result.metricCount, result.measurementCount),
                ),
            )

            is CsvExportResult.Failure -> showNotice(
                BackupNotice("表格没有导出", result.message, isError = true),
            )
        }
    }

    fun previewImport(uri: String) = runTask(BackupTask.PREVIEW_IMPORT) {
        when (val result = backupRepository.previewBackup(uri)) {
            is BackupPreviewResult.Success -> _backup.update {
                it.copy(pendingImport = PendingImport(uri, result.summary))
            }

            is BackupPreviewResult.Failure -> showNotice(
                BackupNotice("无法导入这个文件", result.message, isError = true),
            )
        }
    }

    fun confirmImport(mode: ImportMode) {
        val pending = _backup.value.pendingImport ?: return
        if (_backup.value.isBusy) return
        viewModelScope.launch {
            _backup.update { it.copy(pendingImport = null, task = BackupTask.IMPORT) }
            val notice = when (val result = backupRepository.importBackup(pending.uri, mode)) {
                is BackupImportResult.Success -> BackupNotice(
                    "导入完成",
                    BackupMessages.importSuccess(result.summary),
                )

                is BackupImportResult.Failure -> BackupNotice(
                    "导入没有完成",
                    result.message,
                    isError = true,
                )
            }
            _backup.update { it.copy(task = null, notice = notice) }
        }
    }

    fun cancelImport() {
        _backup.update { it.copy(pendingImport = null) }
    }

    fun dismissNotice() {
        _backup.update { it.copy(notice = null) }
    }

    private fun runTask(task: BackupTask, block: suspend () -> Unit) {
        if (_backup.value.isBusy) return
        viewModelScope.launch {
            _backup.update { it.copy(task = task) }
            try {
                block()
            } finally {
                _backup.update { it.copy(task = null) }
            }
        }
    }

    private fun showNotice(notice: BackupNotice) {
        _backup.update { it.copy(notice = notice) }
    }
}
