package com.liangji.bodymeasure.data.backup

import android.content.Context
import androidx.core.net.toUri
import androidx.room.withTransaction
import com.liangji.bodymeasure.BuildConfig
import com.liangji.bodymeasure.core.common.AppClock
import com.liangji.bodymeasure.data.local.db.AppDatabase
import com.liangji.bodymeasure.data.local.db.FormulaHistoryDao
import com.liangji.bodymeasure.data.local.db.MeasurementDao
import com.liangji.bodymeasure.data.local.db.MetricDao
import com.liangji.bodymeasure.data.local.db.MetricDependencyDao
import com.liangji.bodymeasure.data.local.entity.FormulaHistoryEntity
import com.liangji.bodymeasure.data.local.entity.MeasurementEntity
import com.liangji.bodymeasure.data.local.entity.MetricEntity
import com.liangji.bodymeasure.data.local.entity.toDomain
import com.liangji.bodymeasure.data.local.entity.toEntity
import com.liangji.bodymeasure.domain.backup.BackupCodec
import com.liangji.bodymeasure.domain.backup.BackupDecodeResult
import com.liangji.bodymeasure.domain.backup.BackupExportResult
import com.liangji.bodymeasure.domain.backup.BackupImportResult
import com.liangji.bodymeasure.domain.backup.BackupPayload
import com.liangji.bodymeasure.domain.backup.BackupPreviewResult
import com.liangji.bodymeasure.domain.backup.CsvExportResult
import com.liangji.bodymeasure.domain.backup.ImportMode
import com.liangji.bodymeasure.domain.backup.ImportSummary
import com.liangji.bodymeasure.domain.backup.MeasurementCsv
import com.liangji.bodymeasure.domain.formula.FormulaExpr
import com.liangji.bodymeasure.domain.formula.FormulaParseResult
import com.liangji.bodymeasure.domain.formula.FormulaParser
import com.liangji.bodymeasure.domain.formula.FormulaPrinter
import com.liangji.bodymeasure.domain.model.Measurement
import com.liangji.bodymeasure.domain.model.Metric
import com.liangji.bodymeasure.domain.model.MetricKind
import com.liangji.bodymeasure.domain.repository.BackupRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class RoomBackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val metricDao: MetricDao,
    private val measurementDao: MeasurementDao,
    private val dependencyDao: MetricDependencyDao,
    private val formulaHistoryDao: FormulaHistoryDao,
    private val clock: AppClock,
) : BackupRepository {
    override suspend fun exportBackup(uri: String): BackupExportResult = try {
        val metrics = metricDao.getAll().map(MetricEntity::toDomain)
        val metricIds = metrics.mapTo(HashSet()) { it.id }
        val measurements = measurementDao.getAllActive()
            .map(MeasurementEntity::toDomain)
            .filter { it.metricId in metricIds }
        val history = formulaHistoryDao.getAll()
            .map(FormulaHistoryEntity::toDomain)
            .filter { it.metricId in metricIds }
        val payload = BackupPayload(
            appVersion = BuildConfig.VERSION_NAME,
            exportedAt = clock.now(),
            metrics = metrics,
            measurements = measurements,
            formulaHistory = history,
        )
        writeText(uri, BackupCodec.encode(payload))
        BackupExportResult.Success(payload.summary)
    } catch (_: Exception) {
        BackupExportResult.Failure("备份没有写入成功，请换一个位置再试。")
    }

    override suspend fun exportCsv(uri: String): CsvExportResult = try {
        val metrics = metricDao.getAll().map(MetricEntity::toDomain)
        val metricIds = metrics.mapTo(HashSet()) { it.id }
        val measurements = measurementDao.getAllActive()
            .map(MeasurementEntity::toDomain)
            .filter { it.metricId in metricIds }
        writeText(uri, MeasurementCsv.render(metrics, measurements, ZoneId.systemDefault()))
        CsvExportResult.Success(
            metricCount = metrics.size,
            measurementCount = measurements.size,
        )
    } catch (_: Exception) {
        CsvExportResult.Failure("表格没有写入成功，请换一个位置再试。")
    }

    override suspend fun previewBackup(uri: String): BackupPreviewResult = when (val loaded = load(uri)) {
        is LoadResult.Failure -> BackupPreviewResult.Failure(loaded.message)
        is LoadResult.Success -> BackupPreviewResult.Success(loaded.payload.summary)
    }

    override suspend fun importBackup(uri: String, mode: ImportMode): BackupImportResult {
        val payload = when (val loaded = load(uri)) {
            is LoadResult.Failure -> return BackupImportResult.Failure(loaded.message)
            is LoadResult.Success -> loaded.payload
        }
        return try {
            when (mode) {
                ImportMode.MERGE -> merge(payload)
                ImportMode.REPLACE -> replace(payload)
            }
        } catch (_: Exception) {
            BackupImportResult.Failure("导入时出了点问题，现有数据没有改变。")
        }
    }

    private suspend fun load(uri: String): LoadResult {
        val text = try {
            readText(uri)
        } catch (_: FileTooLargeException) {
            return LoadResult.Failure("备份文件太大，无法导入。")
        } catch (_: Exception) {
            return LoadResult.Failure("无法读取这个文件，请重新选择。")
        }
        return when (val decoded = BackupCodec.decode(text.removePrefix("\uFEFF"))) {
            is BackupDecodeResult.Failure -> LoadResult.Failure(decoded.message)
            is BackupDecodeResult.Success -> LoadResult.Success(decoded.payload)
        }
    }

    private suspend fun merge(payload: BackupPayload): BackupImportResult = database.withTransaction {
        val existing = metricDao.getAll()
        val existingById = existing.associateBy { it.id }
        val existingByName = existing.associateBy { it.name.trim().lowercase() }
        val idMap = HashMap<String, String>(payload.metrics.size)
        val inserted = ArrayList<Metric>(payload.metrics.size)
        var matched = 0
        var nextSort = (existing.maxOfOrNull { it.sortOrder } ?: -1) + 1

        payload.metrics
            .sortedWith(compareBy({ it.sortOrder }, { it.createdAt }))
            .forEach { metric ->
                val target = existingById[metric.id]
                    ?: existingByName[metric.name.trim().lowercase()]
                if (target != null) {
                    if (target.kind != metric.kind ||
                        target.unit != metric.unit ||
                        target.unitDimension != metric.unitDimension
                    ) {
                        return@withTransaction BackupImportResult.Failure(
                            "当前已有同名指标「${target.name}」，但类型或单位不同，无法合并。",
                        )
                    }
                    idMap[metric.id] = target.id
                    matched++
                } else {
                    idMap[metric.id] = metric.id
                    inserted += metric.copy(sortOrder = nextSort++)
                }
            }

        val insertedIds = inserted.mapTo(HashSet()) { it.id }
        val dependencyPlans = ArrayList<Pair<String, List<String>>>(inserted.size)
        inserted.forEach { metric ->
            val formula = if (metric.kind == MetricKind.DERIVED) {
                remapFormula(metric.formula.orEmpty(), idMap)
                    ?: return@withTransaction BackupImportResult.Failure("备份里的公式无法解析。")
            } else {
                null
            }
            metricDao.upsert(metric.copy(formula = formula).toEntity())
            if (formula != null) {
                dependencyPlans += metric.id to referencesOf(formula)
            }
        }
        dependencyPlans.forEach { (metricId, references) ->
            dependencyDao.replace(metricId, references)
        }

        val activeMeasurements = measurementDao.getAllActive()
        val activeIds = activeMeasurements.mapTo(HashSet()) { it.id }
        val activeDays = activeMeasurements.mapTo(HashSet()) {
            it.metricId to LocalDate.ofEpochDay(it.localDate)
        }
        var measurementsAdded = 0
        var measurementsSkipped = 0
        payload.measurements.forEach { measurement ->
            val targetMetricId = idMap[measurement.metricId] ?: return@forEach
            if (measurement.id in activeIds || (targetMetricId to measurement.date) in activeDays) {
                measurementsSkipped++
            } else {
                measurementDao.upsert(measurement.copy(metricId = targetMetricId).toEntity())
                measurementsAdded++
            }
        }

        var historyAdded = 0
        payload.formulaHistory
            .filter { it.metricId in insertedIds }
            .forEach { history ->
                val targetMetricId = idMap[history.metricId] ?: return@forEach
                val formula = remapFormula(history.formula, idMap) ?: history.formula
                val rowId = formulaHistoryDao.insertIgnore(
                    history.copy(metricId = targetMetricId, formula = formula).toEntity(),
                )
                if (rowId != -1L) historyAdded++
            }

        BackupImportResult.Success(
            ImportSummary(
                mode = ImportMode.MERGE,
                metricsAdded = inserted.size,
                metricsMatched = matched,
                measurementsAdded = measurementsAdded,
                measurementsSkipped = measurementsSkipped,
                historyAdded = historyAdded,
            ),
        )
    }

    private suspend fun replace(payload: BackupPayload): BackupImportResult = database.withTransaction {
        formulaHistoryDao.deleteAll()
        dependencyDao.deleteAll()
        measurementDao.deleteAll()
        metricDao.deleteAll()

        val ordered = payload.metrics.sortedWith(compareBy({ it.sortOrder }, { it.createdAt }))
        ordered.forEach { metric -> metricDao.upsert(metric.toEntity()) }
        ordered.filter { it.kind == MetricKind.DERIVED }.forEach { metric ->
            dependencyDao.replace(metric.id, referencesOf(metric.formula.orEmpty()))
        }
        payload.measurements.forEach { measurementDao.upsert(it.toEntity()) }
        var historyAdded = 0
        payload.formulaHistory.forEach { history ->
            if (formulaHistoryDao.insertIgnore(history.toEntity()) != -1L) historyAdded++
        }

        BackupImportResult.Success(
            ImportSummary(
                mode = ImportMode.REPLACE,
                metricsAdded = ordered.size,
                metricsMatched = 0,
                measurementsAdded = payload.measurements.size,
                measurementsSkipped = 0,
                historyAdded = historyAdded,
            ),
        )
    }

    private fun remapFormula(formula: String, idMap: Map<String, String>): String? {
        val parsed = FormulaParser.parse(formula) as? FormulaParseResult.Valid ?: return null
        return FormulaPrinter.canonical(remapExpression(parsed.expression, idMap))
    }

    private fun remapExpression(
        expression: FormulaExpr,
        idMap: Map<String, String>,
    ): FormulaExpr = when (expression) {
        is FormulaExpr.Number -> expression
        is FormulaExpr.Reference -> FormulaExpr.Reference(
            idMap[expression.metricId] ?: expression.metricId,
        )

        is FormulaExpr.Negate -> FormulaExpr.Negate(remapExpression(expression.operand, idMap))
        is FormulaExpr.Binary -> FormulaExpr.Binary(
            operator = expression.operator,
            left = remapExpression(expression.left, idMap),
            right = remapExpression(expression.right, idMap),
        )

        is FormulaExpr.Call -> FormulaExpr.Call(
            name = expression.name,
            arguments = expression.arguments.map { remapExpression(it, idMap) },
        )
    }

    private fun referencesOf(formula: String): List<String> {
        val parsed = FormulaParser.parse(formula) as? FormulaParseResult.Valid
            ?: return emptyList()
        return parsed.references.map { it.metricId }
    }

    private suspend fun readText(uri: String): String = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(uri.toUri())
            ?: throw IOException("Unable to open input stream")
        stream.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (output.size() + read > BackupCodec.MAX_FILE_BYTES) {
                    throw FileTooLargeException()
                }
                output.write(buffer, 0, read)
            }
            output.toString(Charsets.UTF_8.name())
        }
    }

    private suspend fun writeText(uri: String, text: String) = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openOutputStream(uri.toUri())
            ?: throw IOException("Unable to open output stream")
        stream.use { output -> output.write(text.toByteArray(Charsets.UTF_8)) }
    }

    private sealed interface LoadResult {
        data class Success(val payload: BackupPayload) : LoadResult

        data class Failure(val message: String) : LoadResult
    }

    private class FileTooLargeException : Exception()
}
