package com.liangji.bodymeasure.data.repository

import androidx.room.withTransaction
import com.liangji.bodymeasure.core.common.AppClock
import com.liangji.bodymeasure.data.local.datastore.DeviceIdProvider
import com.liangji.bodymeasure.data.local.db.AppDatabase
import com.liangji.bodymeasure.data.local.db.FormulaHistoryDao
import com.liangji.bodymeasure.data.local.db.MetricDao
import com.liangji.bodymeasure.data.local.db.MetricDependencyDao
import com.liangji.bodymeasure.data.local.entity.FormulaHistoryEntity
import com.liangji.bodymeasure.data.local.entity.MetricEntity
import com.liangji.bodymeasure.data.local.entity.toDomain
import com.liangji.bodymeasure.domain.formula.DependencyGraph
import com.liangji.bodymeasure.domain.formula.FormulaParseResult
import com.liangji.bodymeasure.domain.formula.FormulaParser
import com.liangji.bodymeasure.domain.formula.FormulaPrinter
import com.liangji.bodymeasure.domain.model.FormulaHistory
import com.liangji.bodymeasure.domain.model.Metric
import com.liangji.bodymeasure.domain.model.MetricDeleteResult
import com.liangji.bodymeasure.domain.model.MetricDraft
import com.liangji.bodymeasure.domain.model.MetricKind
import com.liangji.bodymeasure.domain.model.MetricMove
import com.liangji.bodymeasure.domain.model.MetricWriteResult
import com.liangji.bodymeasure.domain.model.PairingPolicy
import com.liangji.bodymeasure.domain.repository.MetricRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class RoomMetricRepository @Inject constructor(
    private val database: AppDatabase,
    private val metricDao: MetricDao,
    private val dependencyDao: MetricDependencyDao,
    private val formulaHistoryDao: FormulaHistoryDao,
    private val clock: AppClock,
    private val deviceIdProvider: DeviceIdProvider,
) : MetricRepository {
    override fun observeActiveMetrics(): Flow<List<Metric>> =
        metricDao.observeActive().map { entities -> entities.map(MetricEntity::toDomain) }

    override fun observeAllMetrics(): Flow<List<Metric>> =
        metricDao.observeAll().map { entities -> entities.map(MetricEntity::toDomain) }

    override fun observeArchivedMetrics(): Flow<List<Metric>> =
        metricDao.observeArchived().map { entities -> entities.map(MetricEntity::toDomain) }

    override fun observeMetric(id: String): Flow<Metric?> =
        metricDao.observeById(id).map { it?.toDomain() }

    override fun observeFormulaHistory(metricId: String): Flow<List<FormulaHistory>> =
        formulaHistoryDao.observeByMetric(metricId)
            .map { rows -> rows.map(FormulaHistoryEntity::toDomain) }

    override suspend fun create(draft: MetricDraft): MetricWriteResult = database.withTransaction {
        val name = draft.name.trim()
        if (metricDao.existsByName(name, excludeId = "")) {
            return@withTransaction MetricWriteResult.DuplicateName(name)
        }
        val all = metricDao.getAll()
        val id = UUID.randomUUID().toString()
        val prepared = if (draft.kind == MetricKind.DERIVED) {
            when (val preparation = prepareFormula(draft, id, all, loadEdges())) {
                is FormulaPreparation.Rejected -> return@withTransaction preparation.result
                is FormulaPreparation.Ready -> preparation
            }
        } else {
            null
        }
        val now = clock.now().toEpochMilli()
        val unit = draft.unit.trim()
        val entity = MetricEntity(
            id = id,
            name = name,
            kind = draft.kind,
            unit = unit,
            unitDimension = draft.unitDimension,
            precision = draft.precision,
            colorArgb = draft.colorArgb,
            iconKey = draft.iconKey,
            formula = prepared?.canonical,
            formulaDisplay = prepared?.display,
            formulaVersion = 1,
            pairingPolicy = if (draft.kind == MetricKind.DERIVED) {
                draft.pairingPolicy
            } else {
                PairingPolicy.NEAREST
            },
            pairingWindowDays = draft.pairingWindowDays.coerceIn(0, MAX_PAIRING_WINDOW_DAYS),
            isConstant = draft.isConstant,
            sortOrder = metricDao.maxSortOrder() + 1,
            isArchived = false,
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
            originDeviceId = deviceIdProvider.getOrCreate(),
        )
        metricDao.upsert(entity)
        if (prepared != null) {
            dependencyDao.replace(id, prepared.references)
            formulaHistoryDao.insert(
                FormulaHistoryEntity(
                    id = UUID.randomUUID().toString(),
                    metricId = id,
                    version = 1,
                    formula = prepared.canonical,
                    formulaDisplay = prepared.display,
                    unit = unit,
                    changeNote = draft.formulaChangeNote?.trim()?.takeIf { it.isNotEmpty() },
                    createdAt = now,
                    originDeviceId = entity.originDeviceId,
                ),
            )
        }
        MetricWriteResult.Saved(entity.toDomain())
    }

    override suspend fun update(id: String, draft: MetricDraft): MetricWriteResult = database.withTransaction {
        val existing = metricDao.getById(id) ?: return@withTransaction MetricWriteResult.NotFound
        val name = draft.name.trim()
        if (metricDao.existsByName(name, excludeId = id)) {
            return@withTransaction MetricWriteResult.DuplicateName(name)
        }
        if (draft.kind != existing.kind) {
            return@withTransaction MetricWriteResult.KindLocked
        }
        val unit = draft.unit.trim()
        val unitChanged = unit != existing.unit || draft.unitDimension != existing.unitDimension
        if (unitChanged) {
            val dependents = dependencyDao.activeDependentNamesOf(id)
            if (dependents.isNotEmpty()) {
                return@withTransaction MetricWriteResult.ReferencedBy(dependents)
            }
        }
        val all = metricDao.getAll()
        val prepared = if (existing.kind == MetricKind.DERIVED) {
            when (val preparation = prepareFormula(draft, id, all, loadEdges())) {
                is FormulaPreparation.Rejected -> return@withTransaction preparation.result
                is FormulaPreparation.Ready -> preparation
            }
        } else {
            null
        }
        val now = clock.now().toEpochMilli()
        val formulaChanged = prepared != null && (prepared.canonical != existing.formula || unitChanged)
        val version = if (formulaChanged) existing.formulaVersion + 1 else existing.formulaVersion
        val entity = existing.copy(
            name = name,
            unit = unit,
            unitDimension = draft.unitDimension,
            precision = draft.precision,
            colorArgb = draft.colorArgb,
            iconKey = draft.iconKey,
            formula = prepared?.canonical ?: existing.formula,
            formulaDisplay = prepared?.display ?: existing.formulaDisplay,
            formulaVersion = version,
            pairingPolicy = if (existing.kind == MetricKind.DERIVED) {
                draft.pairingPolicy
            } else {
                existing.pairingPolicy
            },
            pairingWindowDays = if (existing.kind == MetricKind.DERIVED) {
                draft.pairingWindowDays.coerceIn(0, MAX_PAIRING_WINDOW_DAYS)
            } else {
                existing.pairingWindowDays
            },
            isConstant = draft.isConstant,
            updatedAt = now,
        )
        metricDao.upsert(entity)
        if (prepared != null) {
            dependencyDao.replace(id, prepared.references)
        }
        if (formulaChanged) {
            val changed = checkNotNull(prepared)
            formulaHistoryDao.insert(
                FormulaHistoryEntity(
                    id = UUID.randomUUID().toString(),
                    metricId = id,
                    version = version,
                    formula = changed.canonical,
                    formulaDisplay = changed.display,
                    unit = unit,
                    changeNote = draft.formulaChangeNote?.trim()?.takeIf { it.isNotEmpty() },
                    createdAt = now,
                    originDeviceId = entity.originDeviceId,
                ),
            )
        }
        if (name != existing.name) {
            refreshDependentDisplays(id, all + entity)
        }
        MetricWriteResult.Saved(entity.toDomain())
    }

    override suspend fun setArchived(id: String, archived: Boolean): Boolean =
        database.withTransaction {
            if (metricDao.getById(id) == null) return@withTransaction false
            metricDao.setArchived(id, archived, clock.now().toEpochMilli())
            true
        }

    override suspend fun move(id: String, move: MetricMove): Boolean = database.withTransaction {
        val ordered = metricDao.getActive()
        val index = ordered.indexOfFirst { it.id == id }
        if (index < 0) return@withTransaction false
        val target = when (move) {
            MetricMove.TOP -> 0
            MetricMove.UP -> index - 1
            MetricMove.DOWN -> index + 1
        }.coerceIn(0, ordered.lastIndex)

        val reordered = ordered.toMutableList().apply { add(target, removeAt(index)) }
        val now = clock.now().toEpochMilli()
        reordered.forEachIndexed { order, entity ->
            if (entity.sortOrder != order) {
                metricDao.upsert(entity.copy(sortOrder = order, updatedAt = now))
            }
        }
        true
    }

    override suspend fun delete(id: String): MetricDeleteResult = database.withTransaction {
        if (metricDao.getById(id) == null) return@withTransaction MetricDeleteResult.NotFound
        val dependents = dependencyDao.activeDependentNamesOf(id)
        if (dependents.isNotEmpty()) {
            return@withTransaction MetricDeleteResult.ReferencedBy(dependents)
        }
        dependencyDao.deleteFor(id)
        dependencyDao.deleteIncomingFromDeleted(id)
        metricDao.softDelete(id, clock.now().toEpochMilli())
        MetricDeleteResult.Deleted
    }

    private suspend fun loadEdges(): Map<String, Set<String>> =
        dependencyDao.getAll()
            .groupBy({ it.metricId }, { it.dependsOnId })
            .mapValues { (_, targets) -> targets.toSet() }

    private fun prepareFormula(
        draft: MetricDraft,
        selfId: String,
        all: List<MetricEntity>,
        existingEdges: Map<String, Set<String>>,
    ): FormulaPreparation {
        val byName = all.associateBy { it.name.trim().lowercase() }
        val parsed = when (
            val result = FormulaParser.parse(draft.formula.orEmpty()) { name ->
                byName[name.trim().lowercase()]?.id
            }
        ) {
            is FormulaParseResult.Invalid ->
                return FormulaPreparation.Rejected(MetricWriteResult.InvalidFormula(result.error))

            is FormulaParseResult.Valid -> result
        }
        val byId = all.associateBy { it.id }
        val unknown = parsed.references.map { it.metricId }.filter { it !in byId }
        if (unknown.isNotEmpty()) {
            return FormulaPreparation.Rejected(MetricWriteResult.UnknownReference(unknown))
        }
        val edges = existingEdges.toMutableMap()
        edges[selfId] = parsed.references.map { it.metricId }.toSet()
        val cycle = DependencyGraph.findCycle(edges)
        if (cycle != null) {
            val labels = cycle.map { referenced ->
                if (referenced == selfId) draft.name.trim().ifEmpty { selfId } else byId[referenced]?.name ?: referenced
            }
            return FormulaPreparation.Rejected(MetricWriteResult.CycleDetected(labels))
        }
        val derivedIds = all.filter { it.kind == MetricKind.DERIVED }.map { it.id }.toMutableSet() + selfId
        val depth = DependencyGraph.derivedDepth(selfId, derivedIds, edges)
        if (depth > DependencyGraph.MAX_DERIVED_DEPTH) {
            return FormulaPreparation.Rejected(MetricWriteResult.TooDeep(depth))
        }
        return FormulaPreparation.Ready(
            canonical = FormulaPrinter.canonical(parsed.expression),
            display = FormulaPrinter.display(parsed.expression) { byId[it]?.name },
            references = parsed.references.map { it.metricId },
        )
    }

    private suspend fun refreshDependentDisplays(changedId: String, all: List<MetricEntity>) {
        val dependents = dependencyDao.dependentsOf(changedId)
        if (dependents.isEmpty()) return
        val byId = all.associateBy { it.id }
        dependents.forEach { dependentId ->
            val dependent = byId[dependentId] ?: return@forEach
            val formula = dependent.formula ?: return@forEach
            val parsed = FormulaParser.parse(formula) as? FormulaParseResult.Valid ?: return@forEach
            val display = FormulaPrinter.display(parsed.expression) { byId[it]?.name }
            if (display != dependent.formulaDisplay) {
                metricDao.upsert(dependent.copy(formulaDisplay = display))
            }
        }
    }

    private sealed interface FormulaPreparation {
        data class Ready(
            val canonical: String,
            val display: String,
            val references: List<String>,
        ) : FormulaPreparation

        data class Rejected(val result: MetricWriteResult) : FormulaPreparation
    }

    private companion object {
        const val MAX_PAIRING_WINDOW_DAYS = 31
    }
}
