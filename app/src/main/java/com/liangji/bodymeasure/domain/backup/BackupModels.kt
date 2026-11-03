package com.liangji.bodymeasure.domain.backup

import com.liangji.bodymeasure.domain.model.FormulaHistory
import com.liangji.bodymeasure.domain.model.Measurement
import com.liangji.bodymeasure.domain.model.Metric
import com.liangji.bodymeasure.domain.model.MetricKind
import java.time.Instant

data class BackupPayload(
    val appVersion: String,
    val exportedAt: Instant,
    val metrics: List<Metric>,
    val measurements: List<Measurement>,
    val formulaHistory: List<FormulaHistory>,
) {
    val summary: BackupSummary
        get() = BackupSummary(
            metricCount = metrics.size,
            derivedCount = metrics.count { it.kind == MetricKind.DERIVED },
            measurementCount = measurements.size,
            formulaHistoryCount = formulaHistory.size,
            exportedAt = exportedAt,
            appVersion = appVersion,
        )
}

data class BackupSummary(
    val metricCount: Int,
    val derivedCount: Int,
    val measurementCount: Int,
    val formulaHistoryCount: Int,
    val exportedAt: Instant?,
    val appVersion: String?,
)

sealed interface BackupDecodeResult {
    data class Success(val payload: BackupPayload) : BackupDecodeResult

    data class Failure(val message: String) : BackupDecodeResult
}

enum class ImportMode {
    MERGE,
    REPLACE,
}

data class ImportSummary(
    val mode: ImportMode,
    val metricsAdded: Int,
    val metricsMatched: Int,
    val measurementsAdded: Int,
    val measurementsSkipped: Int,
    val historyAdded: Int,
)

sealed interface BackupExportResult {
    data class Success(val summary: BackupSummary) : BackupExportResult

    data class Failure(val message: String) : BackupExportResult
}

sealed interface CsvExportResult {
    data class Success(val metricCount: Int, val measurementCount: Int) : CsvExportResult

    data class Failure(val message: String) : CsvExportResult
}

sealed interface BackupPreviewResult {
    data class Success(val summary: BackupSummary) : BackupPreviewResult

    data class Failure(val message: String) : BackupPreviewResult
}

sealed interface BackupImportResult {
    data class Success(val summary: ImportSummary) : BackupImportResult

    data class Failure(val message: String) : BackupImportResult
}
