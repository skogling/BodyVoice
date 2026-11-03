package com.liangji.bodymeasure.domain.repository

import com.liangji.bodymeasure.domain.backup.BackupExportResult
import com.liangji.bodymeasure.domain.backup.BackupImportResult
import com.liangji.bodymeasure.domain.backup.BackupPreviewResult
import com.liangji.bodymeasure.domain.backup.CsvExportResult
import com.liangji.bodymeasure.domain.backup.ImportMode

interface BackupRepository {
    suspend fun exportBackup(uri: String): BackupExportResult

    suspend fun exportCsv(uri: String): CsvExportResult

    suspend fun previewBackup(uri: String): BackupPreviewResult

    suspend fun importBackup(uri: String, mode: ImportMode): BackupImportResult
}
