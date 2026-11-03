package com.liangji.bodymeasure.ui.metrics

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liangji.bodymeasure.core.common.NumberFormats
import com.liangji.bodymeasure.domain.formula.DependencyGraph
import com.liangji.bodymeasure.domain.formula.FormulaEvaluator
import com.liangji.bodymeasure.domain.formula.FormulaParseResult
import com.liangji.bodymeasure.domain.formula.FormulaParser
import com.liangji.bodymeasure.domain.formula.FormulaPresetCatalog
import com.liangji.bodymeasure.domain.formula.FormulaPrinter
import com.liangji.bodymeasure.domain.formula.FormulaZeroGuard
import com.liangji.bodymeasure.domain.model.FormulaHistory
import com.liangji.bodymeasure.domain.model.Measurement
import com.liangji.bodymeasure.domain.model.Metric
import com.liangji.bodymeasure.domain.model.MetricDraft
import com.liangji.bodymeasure.domain.model.MetricIconKeys
import com.liangji.bodymeasure.domain.model.MetricKind
import com.liangji.bodymeasure.domain.model.MetricPalette
import com.liangji.bodymeasure.domain.model.MetricWriteResult
import com.liangji.bodymeasure.domain.model.PairingPolicy
import com.liangji.bodymeasure.domain.model.UnitDimensions
import com.liangji.bodymeasure.domain.repository.MeasurementRepository
import com.liangji.bodymeasure.domain.repository.MetricRepository
import com.liangji.bodymeasure.domain.summary.DerivedSeriesCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MetricEditorState(
    val isNew: Boolean = true,
    val loading: Boolean = false,
    val missing: Boolean = false,
    val name: String = "",
    val unit: String = "",
    val precision: Int = 1,
    val colorArgb: Long = MetricPalette.CLAY,
    val iconKey: String = MetricIconKeys.GENERIC,
    val kind: MetricKind = MetricKind.RAW,
    val isConstant: Boolean = false,
    val formula: String = "",
    val formulaError: String? = null,
    val formulaPreview: String? = null,
    val formulaSample: Double? = null,
    val formulaZeroHint: FormulaZeroGuard.Hint? = null,
    val formulaPresets: List<FormulaPresetCatalog.Preset> = emptyList(),
    val selectedPresetId: String? = null,
    val referencedIds: List<String> = emptyList(),
    val pairingPolicy: PairingPolicy = PairingPolicy.NEAREST,
    val pairingWindowDays: Int = 2,
    val formulaHistory: List<FormulaHistory> = emptyList(),
    val availableMetrics: List<Metric> = emptyList(),
    val nameError: String? = null,
    val saveError: String? = null,
    val saving: Boolean = false,
    val saved: Boolean = false,
) {
    val kindLocked: Boolean get() = !isNew

    val canSave: Boolean
        get() = name.isNotBlank() &&
            !saving &&
            !saved &&
            !loading &&
            !missing &&
            (kind == MetricKind.RAW || (formula.isNotBlank() && formulaError == null))
}

@HiltViewModel
class MetricEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val metricRepository: MetricRepository,
    private val measurementRepository: MeasurementRepository,
) : ViewModel() {
    private val metricId: String? = savedStateHandle["metricId"]
    private var original: Metric? = null
    private var allMetrics: List<Metric> = emptyList()
    private var latestValues: Map<String, Double> = emptyMap()

    private val _state = MutableStateFlow(
        MetricEditorState(isNew = metricId == null, loading = metricId != null),
    )
    val state: StateFlow<MetricEditorState> = _state.asStateFlow()

    init {
        if (metricId == null) {
            _state.update { it.copy(loading = false) }
        } else {
            viewModelScope.launch {
                val metric = metricRepository.observeMetric(metricId).first()
                if (metric == null) {
                    _state.update { it.copy(loading = false, missing = true) }
                } else {
                    original = metric
                    _state.update {
                        revalidate(
                            it.copy(
                                loading = false,
                                name = metric.name,
                                unit = metric.unit,
                                precision = metric.precision,
                                colorArgb = metric.colorArgb,
                                iconKey = metric.iconKey,
                                kind = metric.kind,
                                isConstant = metric.isConstant,
                                formula = if (metric.kind == MetricKind.DERIVED) {
                                    metric.formulaDisplay ?: metric.formula.orEmpty()
                                } else {
                                    ""
                                },
                                pairingPolicy = metric.pairingPolicy,
                                pairingWindowDays = metric.pairingWindowDays,
                            ),
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            combine(
                metricRepository.observeAllMetrics(),
                measurementRepository.observeAllActive(),
                metricRepository.observeFormulaHistory(metricId ?: ""),
            ) { metrics, measurements, history ->
                Triple(metrics, measurements, history)
            }.collect { (metrics, measurements, history) ->
                allMetrics = metrics
                latestValues = latestValuesOf(metrics, measurements)
                _state.update { current ->
                    revalidate(
                        current.copy(
                            availableMetrics = metrics
                                .filter { it.id != metricId }
                                .sortedWith(
                                    compareBy<Metric> { it.isArchived }.thenBy { it.sortOrder },
                                ),
                            formulaHistory = history,
                        ),
                    )
                }
            }
        }
    }

    fun setName(value: String) {
        _state.update { revalidate(it.copy(name = value.take(24), nameError = null)) }
    }

    fun setUnit(value: String) {
        _state.update { revalidate(it.copy(unit = value.take(8))) }
    }

    fun setPrecision(value: Int) {
        _state.update { revalidate(it.copy(precision = value.coerceIn(0, 3))) }
    }

    fun setColor(argb: Long) {
        _state.update { it.copy(colorArgb = argb) }
    }

    fun setIcon(key: String) {
        _state.update { it.copy(iconKey = key) }
    }

    fun setKind(kind: MetricKind) {
        if (!_state.value.isNew) return
        _state.update {
            revalidate(
                it.copy(
                    kind = kind,
                    isConstant = if (kind == MetricKind.RAW) it.isConstant else false,
                    formulaError = null,
                    saveError = null,
                ),
            )
        }
    }

    fun setConstant(value: Boolean) {
        _state.update {
            if (it.kind == MetricKind.RAW) it.copy(isConstant = value) else it
        }
    }

    fun setFormula(value: String) {
        _state.update { revalidate(it.copy(formula = value.take(MAX_FORMULA_LENGTH), saveError = null)) }
    }

    fun insertReference(metric: Metric) {
        val reference = if (metric.name.any { it == '[' || it == ']' || it == '\n' }) {
            "{${metric.id}}"
        } else {
            "[${metric.name}]"
        }
        _state.update {
            val current = it.formula.trimEnd()
            val next = if (current.isEmpty()) reference else "$current $reference"
            revalidate(it.copy(formula = next.take(MAX_FORMULA_LENGTH), saveError = null))
        }
    }

    fun applyFormulaFix() {
        val suggestion = _state.value.formulaZeroHint?.suggestion ?: return
        _state.update {
            revalidate(it.copy(formula = suggestion.take(MAX_FORMULA_LENGTH), saveError = null))
        }
    }

    fun applyFormulaPreset(preset: FormulaPresetCatalog.Preset) {
        _state.update {
            revalidate(it.copy(formula = preset.formula.take(MAX_FORMULA_LENGTH), saveError = null))
        }
    }

    fun setPairingPolicy(policy: PairingPolicy) {
        _state.update { it.copy(pairingPolicy = policy) }
    }

    fun setPairingWindowDays(days: Int) {
        _state.update { it.copy(pairingWindowDays = days.coerceIn(0, MAX_PAIRING_WINDOW_DAYS)) }
    }

    fun save() {
        val current = _state.value
        if (!current.canSave) return
        _state.update { it.copy(saving = true, nameError = null, saveError = null) }
        viewModelScope.launch {
            val unit = current.unit.trim()
            val previous = original
            val draft = MetricDraft(
                name = current.name,
                unit = unit,
                unitDimension = if (previous != null && previous.unit.trim() == unit) {
                    previous.unitDimension
                } else {
                    UnitDimensions.infer(unit)
                },
                precision = current.precision,
                colorArgb = current.colorArgb,
                iconKey = current.iconKey,
                isConstant = if (current.kind == MetricKind.RAW) {
                    current.isConstant
                } else {
                    previous?.isConstant ?: false
                },
                kind = current.kind,
                formula = if (current.kind == MetricKind.DERIVED) current.formula.trim() else null,
                pairingPolicy = current.pairingPolicy,
                pairingWindowDays = current.pairingWindowDays,
            )
            val result = if (metricId == null) {
                metricRepository.create(draft)
            } else {
                metricRepository.update(metricId, draft)
            }
            val errorText = when (result) {
                is MetricWriteResult.UnknownReference -> {
                    val names = result.ids.map { id ->
                        allMetrics.firstOrNull { it.id == id }?.name ?: "已删除的指标"
                    }
                    "找不到公式里的指标：${names.joinToString("、")}"
                }

                is MetricWriteResult.CycleDetected ->
                    "公式会形成循环：${result.path.joinToString(" → ")}"

                is MetricWriteResult.TooDeep ->
                    "派生层级太深（${result.depth} 层），最多 ${DependencyGraph.MAX_DERIVED_DEPTH} 层"

                is MetricWriteResult.ReferencedBy ->
                    "「${result.names.joinToString("、")}」正在引用它，暂时不能修改单位"

                MetricWriteResult.KindLocked -> "指标类型创建后不能修改"

                else -> null
            }
            when (result) {
                is MetricWriteResult.Saved -> _state.update {
                    it.copy(saving = false, saved = true)
                }

                is MetricWriteResult.DuplicateName -> _state.update {
                    it.copy(saving = false, nameError = "已经有同名指标「${result.name}」")
                }

                is MetricWriteResult.InvalidFormula -> _state.update {
                    it.copy(saving = false, formulaError = result.error.displayMessage())
                }

                MetricWriteResult.NotFound -> _state.update {
                    it.copy(saving = false, missing = true)
                }

                else -> _state.update {
                    it.copy(saving = false, saveError = errorText)
                }
            }
        }
    }

    private fun revalidate(current: MetricEditorState): MetricEditorState {
        val validated = validateFormula(current)
        if (validated.kind != MetricKind.DERIVED) {
            return validated.copy(formulaPresets = emptyList(), selectedPresetId = null)
        }
        val presets = FormulaPresetCatalog.suggest(allMetrics.filter { it.id != metricId })
        val byName = referencedByName()
        val canonical = canonicalFormula(validated.formula, byName)
        val selected = if (canonical == null) {
            null
        } else {
            presets.firstOrNull { canonicalFormula(it.formula, byName) == canonical }
        }
        return validated.copy(formulaPresets = presets, selectedPresetId = selected?.id)
    }

    private fun referencedByName(): Map<String, Metric> = allMetrics
        .filter { it.id != metricId }
        .associateBy { it.name.trim().lowercase() }

    private fun canonicalFormula(text: String, byName: Map<String, Metric>): String? {
        if (text.isBlank()) return null
        val parsed = FormulaParser.parse(text) { name -> byName[name.trim().lowercase()]?.id }
        return when (parsed) {
            is FormulaParseResult.Valid -> FormulaPrinter.canonical(parsed.expression)
            is FormulaParseResult.Invalid -> null
        }
    }

    private fun validateFormula(current: MetricEditorState): MetricEditorState {
        if (current.kind == MetricKind.RAW) {
            return current.cleared().copy(
                formulaError = null,
                formulaPreview = null,
                formulaSample = null,
                referencedIds = emptyList(),
            )
        }
        if (current.formula.isBlank()) {
            return current.cleared().copy(
                formulaError = null,
                formulaPreview = "写出公式后会在这里试算",
                formulaSample = null,
                referencedIds = emptyList(),
            )
        }
        val byName = referencedByName()
        val parsed = FormulaParser.parse(current.formula) { name ->
            byName[name.trim().lowercase()]?.id
        }
        if (parsed is FormulaParseResult.Invalid) {
            return current.cleared().copy(
                formulaError = parsed.error.displayMessage(),
                formulaPreview = null,
                formulaSample = null,
                referencedIds = emptyList(),
            )
        }
        parsed as FormulaParseResult.Valid
        val refs = parsed.references.map { it.metricId }.distinct()
        if (refs.isEmpty()) {
            return current.cleared().copy(
                formulaError = "公式需要至少引用一个指标",
                formulaPreview = null,
                formulaSample = null,
                referencedIds = emptyList(),
            )
        }
        val namesById = allMetrics.associate { it.id to it.name }
        val byId = allMetrics.associateBy { it.id }
        val unknown = refs.filter { it !in namesById }
        if (unknown.isNotEmpty()) {
            return current.cleared().copy(
                formulaError = "找不到公式里的指标，可能已被删除",
                formulaPreview = null,
                formulaSample = null,
                referencedIds = refs,
            )
        }
        val selfKey = metricId ?: DRAFT_ID
        val edges = metricEdges().toMutableMap()
        edges[selfKey] = refs.toSet()
        val cycle = DependencyGraph.findCycle(edges)
        if (cycle != null) {
            val labels = cycle.map { id ->
                if (id == selfKey) current.name.trim().ifEmpty { "新指标" } else namesById[id] ?: id
            }
            return current.cleared().copy(
                formulaError = "公式会形成循环：${labels.joinToString(" → ")}",
                formulaPreview = null,
                formulaSample = null,
                referencedIds = refs,
            )
        }
        val derivedIds = allMetrics
            .filter { it.kind == MetricKind.DERIVED }
            .map { it.id }
            .toMutableSet()
            .apply { add(selfKey) }
        val depth = DependencyGraph.derivedDepth(selfKey, derivedIds, edges)
        if (depth > DependencyGraph.MAX_DERIVED_DEPTH) {
            return current.cleared().copy(
                formulaError = "派生层级太深，最多 ${DependencyGraph.MAX_DERIVED_DEPTH} 层",
                formulaPreview = null,
                formulaSample = null,
                referencedIds = refs,
            )
        }
        val missing = refs
            .filter { latestValues[it] == null }
            .map { namesById[it] ?: "已删除的指标" }
        val preview: String?
        val sample: Double?
        var zeroHint: FormulaZeroGuard.Hint? = null
        if (missing.isNotEmpty()) {
            preview = "还缺少「${missing.joinToString("、")}」的记录"
            sample = null
        } else {
            val value = FormulaEvaluator.evaluate(parsed.expression) { latestValues[it] }
            if (value == null) {
                preview = "按最近记录暂时算不出结果"
                sample = null
            } else {
                sample = value
                zeroHint = FormulaZeroGuard.check(
                    expression = parsed.expression,
                    sample = value,
                    precision = current.precision,
                    metrics = byId,
                    resolveName = namesById::get,
                )
                val unit = current.unit.trim()
                val formatted = NumberFormats.format(value, current.precision)
                preview = if (zeroHint == null) {
                    "按最近记录试算：${if (unit.isEmpty()) formatted else "$formatted $unit"}"
                } else {
                    null
                }
            }
        }
        return current.cleared().copy(
            formulaError = null,
            formulaPreview = preview,
            formulaSample = sample,
            formulaZeroHint = zeroHint,
            referencedIds = refs,
        )
    }

    private fun MetricEditorState.cleared(): MetricEditorState = copy(formulaZeroHint = null)

    private fun metricEdges(): Map<String, Set<String>> = allMetrics
        .mapNotNull { metric ->
            val formula = metric.formula ?: return@mapNotNull null
            val parsed = FormulaParser.parse(formula) as? FormulaParseResult.Valid
                ?: return@mapNotNull null
            metric.id to parsed.references.map { it.metricId }.toSet()
        }
        .toMap()

    private fun latestValuesOf(
        metrics: List<Metric>,
        measurements: List<Measurement>,
    ): Map<String, Double> {
        val derived = DerivedSeriesCalculator.build(metrics, measurements)
        val merged = if (derived.isEmpty()) measurements else measurements + derived.values.flatten()
        return merged
            .groupBy { it.metricId }
            .mapValues { (_, rows) -> rows.maxBy { it.measuredAt }.value }
    }

    private companion object {
        const val MAX_FORMULA_LENGTH = 512
        const val MAX_PAIRING_WINDOW_DAYS = 31
        const val DRAFT_ID = "draft"
    }
}
