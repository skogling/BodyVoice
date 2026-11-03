package com.liangji.bodymeasure.domain.backup

import com.liangji.bodymeasure.domain.formula.DependencyGraph
import com.liangji.bodymeasure.domain.formula.FormulaParseResult
import com.liangji.bodymeasure.domain.formula.FormulaParser
import com.liangji.bodymeasure.domain.formula.FormulaPrinter
import com.liangji.bodymeasure.domain.model.FormulaHistory
import com.liangji.bodymeasure.domain.model.Measurement
import com.liangji.bodymeasure.domain.model.MeasurementSource
import com.liangji.bodymeasure.domain.model.Metric
import com.liangji.bodymeasure.domain.model.MetricKind
import com.liangji.bodymeasure.domain.model.PairingPolicy
import com.liangji.bodymeasure.domain.model.UnitDimension
import java.time.Instant
import java.time.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object BackupCodec {
    const val FORMAT = "com.liangji.bodymeasure.backup"
    const val CURRENT_VERSION = 1
    const val MAX_FILE_BYTES = 8 * 1024 * 1024

    private const val MAX_METRICS = 500
    private const val MAX_MEASUREMENTS = 100_000
    private const val MAX_HISTORY = 20_000

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun encode(payload: BackupPayload): String {
        val file = BackupFileDto(
            format = FORMAT,
            version = CURRENT_VERSION,
            appVersion = payload.appVersion,
            exportedAt = payload.exportedAt.toString(),
            metrics = payload.metrics
                .filter { it.deletedAt == null }
                .map { it.toDto() },
            measurements = payload.measurements
                .filter { it.deletedAt == null }
                .map { it.toDto() },
            formulaHistory = payload.formulaHistory.map { it.toDto() },
        )
        return json.encodeToString(file)
    }

    fun decode(text: String): BackupDecodeResult {
        val file = try {
            json.decodeFromString<BackupFileDto>(text)
        } catch (_: Exception) {
            return BackupDecodeResult.Failure("这不是身语的备份文件，或文件已经损坏。")
        }
        if (file.format != FORMAT) {
            return BackupDecodeResult.Failure("这不是身语的备份文件。")
        }
        if (file.version !in 1..CURRENT_VERSION) {
            return BackupDecodeResult.Failure("备份来自更新版本的身语，请先更新应用。")
        }
        if (file.metrics.isEmpty()) {
            return BackupDecodeResult.Failure("备份里没有可导入的指标。")
        }
        if (file.metrics.size > MAX_METRICS) {
            return BackupDecodeResult.Failure("备份里的指标太多，无法导入。")
        }
        if (file.measurements.size > MAX_MEASUREMENTS) {
            return BackupDecodeResult.Failure("备份里的记录太多，无法导入。")
        }
        if (file.formulaHistory.size > MAX_HISTORY) {
            return BackupDecodeResult.Failure("备份里的公式历史太多，无法导入。")
        }
        val exportedAt = try {
            Instant.parse(file.exportedAt)
        } catch (_: Exception) {
            return BackupDecodeResult.Failure("备份时间格式不正确。")
        }
        val appVersion = file.appVersion.takeIf { it.isNotBlank() } ?: "未知版本"

        val metrics = ArrayList<Metric>(file.metrics.size)
        val metricIds = HashSet<String>(file.metrics.size)
        val metricNames = HashSet<String>(file.metrics.size)
        for ((index, dto) in file.metrics.withIndex()) {
            val id = dto.id.trim()
            if (id.isEmpty()) {
                return BackupDecodeResult.Failure("第 ${index + 1} 个指标缺少编号。")
            }
            if (!metricIds.add(id)) {
                return BackupDecodeResult.Failure("备份里有重复的指标编号。")
            }
            val name = dto.name.trim()
            if (name.isEmpty()) {
                return BackupDecodeResult.Failure("第 ${index + 1} 个指标缺少名称。")
            }
            if (!metricNames.add(name.lowercase())) {
                return BackupDecodeResult.Failure("备份里有重复的指标名称「$name」。")
            }
            val kind = enumOrNull<MetricKind>(dto.kind)
                ?: return BackupDecodeResult.Failure("指标「$name」的类型无法识别。")
            val unitDimension = enumOrNull<UnitDimension>(dto.unitDimension)
                ?: return BackupDecodeResult.Failure("指标「$name」的单位类型无法识别。")
            val pairingPolicy = enumOrNull<PairingPolicy>(dto.pairingPolicy)
                ?: return BackupDecodeResult.Failure("指标「$name」的配对方式无法识别。")
            if (dto.precision !in 0..3) {
                return BackupDecodeResult.Failure("指标「$name」的小数位数不正确。")
            }
            if (dto.pairingWindowDays !in 0..31) {
                return BackupDecodeResult.Failure("指标「$name」的配对天数不正确。")
            }
            val formula = dto.formula?.trim()?.takeIf { it.isNotEmpty() }
            if (kind == MetricKind.DERIVED && formula == null) {
                return BackupDecodeResult.Failure("派生指标「$name」缺少公式。")
            }
            val parsedFormula = formula?.let { FormulaParser.parse(it) }
            if (parsedFormula is FormulaParseResult.Invalid) {
                return BackupDecodeResult.Failure("指标「$name」的公式无法解析。")
            }
            val canonical = (parsedFormula as? FormulaParseResult.Valid)
                ?.let { FormulaPrinter.canonical(it.expression) }
            metrics += Metric(
                id = id,
                name = name,
                kind = kind,
                unit = dto.unit.trim(),
                unitDimension = unitDimension,
                precision = dto.precision,
                colorArgb = dto.colorArgb,
                iconKey = dto.iconKey.trim().ifEmpty { "generic" },
                formula = if (kind == MetricKind.DERIVED) canonical else null,
                formulaDisplay = if (kind == MetricKind.DERIVED) {
                    dto.formulaDisplay?.trim()?.takeIf { it.isNotEmpty() } ?: canonical
                } else {
                    null
                },
                formulaVersion = dto.formulaVersion.coerceAtLeast(1),
                pairingPolicy = if (kind == MetricKind.DERIVED) {
                    pairingPolicy
                } else {
                    PairingPolicy.NEAREST
                },
                pairingWindowDays = if (kind == MetricKind.DERIVED) dto.pairingWindowDays else 2,
                isConstant = dto.isConstant,
                sortOrder = dto.sortOrder.coerceAtLeast(0),
                isArchived = dto.isArchived,
                createdAt = dto.createdAt.coerceAtLeast(0),
                updatedAt = dto.updatedAt.coerceAtLeast(0),
                deletedAt = null,
                originDeviceId = dto.originDeviceId.trim().ifEmpty { "unknown" },
            )
        }

        val metricsById = metrics.associateBy { it.id }
        val edges = mutableMapOf<String, Set<String>>()
        for (metric in metrics.filter { it.kind == MetricKind.DERIVED }) {
            val parsed = FormulaParser.parse(metric.formula.orEmpty())
                as? FormulaParseResult.Valid
                ?: return BackupDecodeResult.Failure("指标「${metric.name}」的公式无法解析。")
            val references = parsed.references.map { it.metricId }.toSet()
            if (metric.id in references) {
                return BackupDecodeResult.Failure("指标「${metric.name}」不能引用自己。")
            }
            if (references.any { it !in metricsById }) {
                return BackupDecodeResult.Failure("指标「${metric.name}」引用了备份中不存在的指标。")
            }
            edges[metric.id] = references
        }
        if (DependencyGraph.findCycle(edges) != null) {
            return BackupDecodeResult.Failure("备份里的公式存在循环引用。")
        }
        val derivedIds = metrics.filter { it.kind == MetricKind.DERIVED }.map { it.id }.toSet()
        if (derivedIds.any { DependencyGraph.derivedDepth(it, derivedIds, edges) > DependencyGraph.MAX_DERIVED_DEPTH }) {
            return BackupDecodeResult.Failure("备份里的公式嵌套层级过深。")
        }

        val measurementIds = HashSet<String>(file.measurements.size)
        val measurementDays = HashSet<Pair<String, LocalDate>>(file.measurements.size)
        val measurements = ArrayList<Measurement>(file.measurements.size)
        for ((index, dto) in file.measurements.withIndex()) {
            val id = dto.id.trim()
            if (id.isEmpty()) {
                return BackupDecodeResult.Failure("第 ${index + 1} 条记录缺少编号。")
            }
            if (!measurementIds.add(id)) {
                return BackupDecodeResult.Failure("备份里有重复的记录编号。")
            }
            val metricId = dto.metricId.trim()
            if (metricId !in metricsById) {
                return BackupDecodeResult.Failure("有记录指向了备份中不存在的指标。")
            }
            val date = try {
                LocalDate.parse(dto.date)
            } catch (_: Exception) {
                return BackupDecodeResult.Failure("有记录的日期格式不正确。")
            }
            if (!measurementDays.add(metricId to date)) {
                return BackupDecodeResult.Failure("备份里同一天有两条重复记录。")
            }
            if (!dto.value.isFinite()) {
                return BackupDecodeResult.Failure("有记录的数值不正确。")
            }
            val source = enumOrNull<MeasurementSource>(dto.source)
                ?: return BackupDecodeResult.Failure("有记录的来源无法识别。")
            measurements += Measurement(
                id = id,
                metricId = metricId,
                date = date,
                measuredAt = dto.measuredAt.coerceAtLeast(0),
                value = dto.value,
                note = dto.note?.takeIf { it.isNotBlank() },
                source = source,
                createdAt = dto.createdAt.coerceAtLeast(0),
                updatedAt = dto.updatedAt.coerceAtLeast(0),
                deletedAt = null,
                originDeviceId = dto.originDeviceId.trim().ifEmpty { "unknown" },
            )
        }

        val historyIds = HashSet<String>(file.formulaHistory.size)
        val history = ArrayList<FormulaHistory>(file.formulaHistory.size)
        for (dto in file.formulaHistory) {
            val id = dto.id.trim()
            if (id.isEmpty()) {
                return BackupDecodeResult.Failure("公式历史缺少编号。")
            }
            if (!historyIds.add(id)) {
                return BackupDecodeResult.Failure("备份里有重复的公式历史编号。")
            }
            val metricId = dto.metricId.trim()
            if (metricId !in metricsById) {
                return BackupDecodeResult.Failure("公式历史指向了备份中不存在的指标。")
            }
            val formula = dto.formula.trim()
            if (formula.isEmpty()) {
                return BackupDecodeResult.Failure("公式历史里有一条空公式。")
            }
            history += FormulaHistory(
                id = id,
                metricId = metricId,
                version = dto.version.coerceAtLeast(1),
                formula = formula,
                formulaDisplay = dto.formulaDisplay.trim().ifEmpty { formula },
                unit = dto.unit,
                changeNote = dto.changeNote?.takeIf { it.isNotBlank() },
                createdAt = dto.createdAt.coerceAtLeast(0),
                originDeviceId = dto.originDeviceId.trim().ifEmpty { "unknown" },
            )
        }

        return BackupDecodeResult.Success(
            BackupPayload(
                appVersion = appVersion,
                exportedAt = exportedAt,
                metrics = metrics,
                measurements = measurements,
                formulaHistory = history,
            ),
        )
    }

    private inline fun <reified T : Enum<T>> enumOrNull(value: String): T? =
        enumValues<T>().firstOrNull { it.name.equals(value, ignoreCase = true) }

    private fun Metric.toDto(): MetricDto = MetricDto(
        id = id,
        name = name,
        kind = kind.name,
        unit = unit,
        unitDimension = unitDimension.name,
        precision = precision,
        colorArgb = colorArgb,
        iconKey = iconKey,
        formula = formula,
        formulaDisplay = formulaDisplay,
        formulaVersion = formulaVersion,
        pairingPolicy = pairingPolicy.name,
        pairingWindowDays = pairingWindowDays,
        isConstant = isConstant,
        sortOrder = sortOrder,
        isArchived = isArchived,
        createdAt = createdAt,
        updatedAt = updatedAt,
        originDeviceId = originDeviceId,
    )

    private fun Measurement.toDto(): MeasurementDto = MeasurementDto(
        id = id,
        metricId = metricId,
        date = date.toString(),
        measuredAt = measuredAt,
        value = value,
        note = note,
        source = source.name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        originDeviceId = originDeviceId,
    )

    private fun FormulaHistory.toDto(): FormulaHistoryDto = FormulaHistoryDto(
        id = id,
        metricId = metricId,
        version = version,
        formula = formula,
        formulaDisplay = formulaDisplay,
        unit = unit,
        changeNote = changeNote,
        createdAt = createdAt,
        originDeviceId = originDeviceId,
    )

    @Serializable
    private data class BackupFileDto(
        val format: String,
        val version: Int,
        val appVersion: String = "",
        val exportedAt: String,
        val metrics: List<MetricDto> = emptyList(),
        val measurements: List<MeasurementDto> = emptyList(),
        val formulaHistory: List<FormulaHistoryDto> = emptyList(),
    )

    @Serializable
    private data class MetricDto(
        val id: String,
        val name: String,
        val kind: String,
        val unit: String = "",
        val unitDimension: String,
        val precision: Int = 1,
        val colorArgb: Long,
        val iconKey: String = "generic",
        val formula: String? = null,
        val formulaDisplay: String? = null,
        val formulaVersion: Int = 1,
        val pairingPolicy: String = "NEAREST",
        val pairingWindowDays: Int = 2,
        val isConstant: Boolean = false,
        val sortOrder: Int = 0,
        val isArchived: Boolean = false,
        val createdAt: Long = 0,
        val updatedAt: Long = 0,
        val originDeviceId: String = "unknown",
    )

    @Serializable
    private data class MeasurementDto(
        val id: String,
        val metricId: String,
        val date: String,
        val measuredAt: Long = 0,
        val value: Double,
        val note: String? = null,
        val source: String = "MANUAL",
        val createdAt: Long = 0,
        val updatedAt: Long = 0,
        val originDeviceId: String = "unknown",
    )

    @Serializable
    private data class FormulaHistoryDto(
        val id: String,
        val metricId: String,
        val version: Int = 1,
        val formula: String,
        val formulaDisplay: String = "",
        val unit: String = "",
        val changeNote: String? = null,
        val createdAt: Long = 0,
        val originDeviceId: String = "unknown",
    )
}
