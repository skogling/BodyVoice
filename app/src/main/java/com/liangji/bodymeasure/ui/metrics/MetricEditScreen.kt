package com.liangji.bodymeasure.ui.metrics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.liangji.bodymeasure.core.common.DateFormats
import com.liangji.bodymeasure.core.common.NumberFormats
import com.liangji.bodymeasure.core.designsystem.AppIcons
import com.liangji.bodymeasure.core.designsystem.BodyMeasureTheme
import com.liangji.bodymeasure.core.designsystem.GlassDivider
import com.liangji.bodymeasure.core.designsystem.GlassIconButton
import com.liangji.bodymeasure.core.designsystem.Motion
import com.liangji.bodymeasure.core.designsystem.ScreenColumn
import com.liangji.bodymeasure.core.designsystem.SoftGlassSurface
import com.liangji.bodymeasure.core.designsystem.SoftGradientBackground
import com.liangji.bodymeasure.core.designsystem.SoftActionButton
import com.liangji.bodymeasure.core.designsystem.Spacing
import com.liangji.bodymeasure.core.designsystem.metricColor
import com.liangji.bodymeasure.domain.formula.FormulaPresetCatalog
import com.liangji.bodymeasure.domain.formula.FormulaZeroGuard
import com.liangji.bodymeasure.domain.model.Metric
import com.liangji.bodymeasure.domain.model.MetricIconKeys
import com.liangji.bodymeasure.domain.model.MetricKind
import com.liangji.bodymeasure.domain.model.MetricPalette
import com.liangji.bodymeasure.domain.model.PairingPolicy
import com.liangji.bodymeasure.domain.model.ThemeMode
import com.liangji.bodymeasure.domain.model.UnitDimensions
import com.liangji.bodymeasure.ui.components.MetricBadge
import com.liangji.bodymeasure.ui.components.SoftTextField
import com.liangji.bodymeasure.ui.components.metricGlyph
import com.liangji.bodymeasure.ui.components.precisionHint
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.delay

@Composable
fun MetricEditScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: MetricEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) {
        if (state.saved) {
            delay(Motion.quick.toLong())
            onSaved()
        }
    }

    MetricEditorContent(
        state = state,
        onBack = onBack,
        onNameChange = viewModel::setName,
        onUnitChange = viewModel::setUnit,
        onPrecisionChange = viewModel::setPrecision,
        onColorChange = viewModel::setColor,
        onIconChange = viewModel::setIcon,
        onKindChange = viewModel::setKind,
        onConstantChange = viewModel::setConstant,
        onFormulaChange = viewModel::setFormula,
        onInsertReference = viewModel::insertReference,
        onApplyFormulaFix = viewModel::applyFormulaFix,
        onApplyPreset = viewModel::applyFormulaPreset,
        onPairingPolicyChange = viewModel::setPairingPolicy,
        onPairingWindowChange = viewModel::setPairingWindowDays,
        onSave = viewModel::save,
    )
}

@Composable
internal fun MetricEditorContent(
    state: MetricEditorState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onUnitChange: (String) -> Unit,
    onPrecisionChange: (Int) -> Unit,
    onColorChange: (Long) -> Unit,
    onIconChange: (String) -> Unit,
    onKindChange: (MetricKind) -> Unit = {},
    onConstantChange: (Boolean) -> Unit = {},
    onFormulaChange: (String) -> Unit = {},
    onInsertReference: (Metric) -> Unit = {},
    onApplyFormulaFix: () -> Unit = {},
    onApplyPreset: (FormulaPresetCatalog.Preset) -> Unit = {},
    onPairingPolicyChange: (PairingPolicy) -> Unit = {},
    onPairingWindowChange: (Int) -> Unit = {},
    onSave: () -> Unit,
) {
    val colors = BodyMeasureTheme.colors
    val focusManager = LocalFocusManager.current

    ScreenColumn(
        modifier = Modifier.padding(horizontal = Spacing.screen),
        maxWidth = 640.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassIconButton(
                imageVector = AppIcons.ArrowBack,
                contentDescription = "返回",
                onClick = onBack,
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = if (state.isNew) "新建指标" else "编辑指标",
                style = MaterialTheme.typography.headlineLarge,
                color = colors.ink,
            )
        }

        when {
            state.loading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "正在读取指标…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                )
            }

            state.missing -> MissingState(
                onBack = onBack,
                modifier = Modifier.weight(1f),
            )

            else -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = when {
                            state.kind == MetricKind.DERIVED ->
                                "用其他指标算出结果，保存后会随记录自动更新。"
                            state.isNew -> "给它一个名字和单位，之后随时可以改。"
                            else -> "修改名称、单位与显示方式，历史记录不会变动。"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                    )

                    Spacer(Modifier.height(Spacing.md))
                    PreviewCard(state = state)

                    if (state.isNew) {
                        Spacer(Modifier.height(Spacing.lg))
                        SectionLabel("类型")
                        Spacer(Modifier.height(Spacing.xs))
                        KindCard(state = state, onKindChange = onKindChange)
                    }

                    Spacer(Modifier.height(Spacing.lg))
                    SectionLabel("基本信息")
                    Spacer(Modifier.height(Spacing.xs))
                    BasicInfoCard(
                        state = state,
                        onNameChange = onNameChange,
                        onUnitChange = onUnitChange,
                        onConstantChange = onConstantChange,
                        onNameNext = { focusManager.moveFocus(FocusDirection.Down) },
                    )

                    if (state.kind == MetricKind.DERIVED) {
                        Spacer(Modifier.height(Spacing.lg))
                        SectionLabel("公式")
                        Spacer(Modifier.height(Spacing.xs))
                        FormulaCard(
                            state = state,
                            onFormulaChange = onFormulaChange,
                            onInsertReference = onInsertReference,
                            onApplyFormulaFix = onApplyFormulaFix,
                            onApplyPreset = onApplyPreset,
                        )

                        Spacer(Modifier.height(Spacing.lg))
                        SectionLabel("配对")
                        Spacer(Modifier.height(Spacing.xs))
                        PairingCard(
                            state = state,
                            onPairingPolicyChange = onPairingPolicyChange,
                            onPairingWindowChange = onPairingWindowChange,
                        )

                        if (!state.isNew && state.formulaHistory.size > 1) {
                            Spacer(Modifier.height(Spacing.lg))
                            SectionLabel("公式记录")
                            Spacer(Modifier.height(Spacing.xs))
                            FormulaHistoryCard(state = state)
                        }
                    }

                    Spacer(Modifier.height(Spacing.lg))
                    SectionLabel("显示")
                    Spacer(Modifier.height(Spacing.xs))
                    DisplayCard(
                        state = state,
                        onPrecisionChange = onPrecisionChange,
                        onColorChange = onColorChange,
                        onIconChange = onIconChange,
                    )

                    val hint = when {
                        state.kind == MetricKind.DERIVED ->
                            "创建后会出现在首页，结果随关联指标更新。"
                        state.isNew -> "创建后会出现在首页，随时可以记录。"
                        else -> null
                    }
                    if (hint != null) {
                        Spacer(Modifier.height(Spacing.lg))
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.muted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Spacer(Modifier.height(Spacing.xl))
                    Spacer(Modifier.height(88.dp))
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (state.saveError != null) {
                        Text(
                            text = state.saveError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(Spacing.xs))
                    }
                    SoftActionButton(
                        text = when {
                            state.saving -> "保存中…"
                            state.saved -> "已保存"
                            state.isNew -> "创建指标"
                            else -> "保存修改"
                        },
                        onClick = onSave,
                        enabled = state.canSave || state.saved,
                        icon = if (state.saved) AppIcons.Check else null,
                        busy = state.saving,
                    )
                    Spacer(Modifier.height(Spacing.xs))
                }
            }
        }
    }
}

@Composable
private fun MissingState(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BodyMeasureTheme.colors
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        SoftGlassSurface(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(Spacing.lg),
        ) {
            Text(
                text = "这个指标不存在或已被删除。",
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "回到首页看看其他指标。",
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
            )
            Spacer(Modifier.height(Spacing.xs))
            TextButton(
                onClick = onBack,
                contentPadding = PaddingValues(horizontal = 0.dp),
            ) {
                Text("返回", color = colors.clay)
            }
        }
    }
}

@Composable
private fun PreviewCard(state: MetricEditorState) {
    val colors = BodyMeasureTheme.colors
    val metric = remember(state.name, state.unit, state.colorArgb, state.iconKey, state.kind) {
        previewMetric(
            name = state.name,
            unit = state.unit,
            colorArgb = state.colorArgb,
            iconKey = state.iconKey,
            kind = state.kind,
        )
    }
    val unit = state.unit.trim()
    val derived = state.kind == MetricKind.DERIVED
    val value = if (derived) state.formulaSample else 68.4

    SoftGlassSurface(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MetricBadge(metric = metric, size = 52.dp, glyphSize = 21.sp)
            Spacer(Modifier.width(Spacing.sm))
            Column(Modifier.weight(1f)) {
                Text(
                    text = state.name.trim().ifEmpty { "未命名指标" },
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (state.name.isBlank()) colors.muted else colors.ink,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = buildString {
                        append(if (unit.isEmpty()) "不设单位" else unit)
                        append(" · 精确到 ")
                        append(precisionHint(state.precision))
                        if (derived) {
                            append(" · 派生")
                        } else if (state.isConstant) {
                            append(" · 固定值")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                )
            }
            Spacer(Modifier.width(Spacing.xs))
            Text(
                text = value?.let { NumberFormats.format(it, state.precision) } ?: "—",
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 28.sp, lineHeight = 32.sp),
                color = if (value != null) colors.ink else colors.muted.copy(alpha = 0.5f),
            )
            if (unit.isNotEmpty() && value != null) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun KindCard(state: MetricEditorState, onKindChange: (MetricKind) -> Unit) {
    val colors = BodyMeasureTheme.colors
    SoftGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            KindOption(
                title = "直接记录",
                caption = "每次手动记下一个数",
                selected = state.kind == MetricKind.RAW,
                onClick = { onKindChange(MetricKind.RAW) },
                modifier = Modifier.weight(1f),
            )
            KindOption(
                title = "公式计算",
                caption = "由其他指标自动算出",
                selected = state.kind == MetricKind.DERIVED,
                onClick = { onKindChange(MetricKind.DERIVED) },
                modifier = Modifier.weight(1f),
            )
        }
        if (state.kindLocked) {
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "类型创建后不能修改。",
                style = MaterialTheme.typography.labelSmall,
                color = colors.muted,
            )
        }
    }
}

@Composable
private fun KindOption(
    title: String,
    caption: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BodyMeasureTheme.colors
    val shape = RoundedCornerShape(16.dp)
    val background by animateColorAsState(
        targetValue = if (selected) {
            colors.clay.copy(alpha = if (colors.isDark) 0.16f else 0.08f)
        } else {
            colors.ink.copy(alpha = if (colors.isDark) 0.05f else 0.03f)
        },
        animationSpec = tween(Motion.quick),
        label = "kindOptionBackground",
    )
    val outline by animateColorAsState(
        targetValue = if (selected) colors.clay.copy(alpha = 0.5f) else colors.line,
        animationSpec = tween(Motion.quick),
        label = "kindOptionOutline",
    )
    Column(
        modifier = modifier
            .clip(shape)
            .background(background)
            .border(1.dp, outline, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) colors.ink else colors.inkSecondary,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = caption,
            style = MaterialTheme.typography.labelSmall,
            color = colors.muted,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BasicInfoCard(
    state: MetricEditorState,
    onNameChange: (String) -> Unit,
    onUnitChange: (String) -> Unit,
    onConstantChange: (Boolean) -> Unit,
    onNameNext: () -> Unit,
) {
    val colors = BodyMeasureTheme.colors
    SoftGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(18.dp),
    ) {
        FieldLabel(text = "名称", hint = "必填")
        Spacer(Modifier.height(6.dp))
        SoftTextField(
            value = state.name,
            onValueChange = onNameChange,
            placeholder = "例如：腰围",
            isError = state.nameError != null,
            imeAction = ImeAction.Next,
            onImeAction = onNameNext,
        )
        if (state.nameError != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = state.nameError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.height(Spacing.md))
        FieldLabel(text = "单位", hint = "可留空")
        Spacer(Modifier.height(6.dp))
        SoftTextField(
            value = state.unit,
            onValueChange = onUnitChange,
            placeholder = "kg、cm、%…",
            imeAction = ImeAction.Done,
        )
        Spacer(Modifier.height(Spacing.xs))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            UNIT_SUGGESTIONS.forEach { suggestion ->
                SuggestionChip(
                    text = suggestion,
                    selected = state.unit.trim() == suggestion,
                    onClick = { onUnitChange(suggestion) },
                )
            }
        }

        if (state.kind == MetricKind.RAW) {
            Spacer(Modifier.height(18.dp))
            FieldLabel(text = "固定值", hint = "可选")
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SuggestionChip(
                    text = if (state.isConstant) "作为常数" else "普通指标",
                    selected = state.isConstant,
                    onClick = { onConstantChange(!state.isConstant) },
                )
                Spacer(Modifier.width(Spacing.xs))
                Text(
                    text = if (state.isConstant) {
                        "公式里始终取最新一次记录"
                    } else {
                        "适合身高、目标值这类不常变的数"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.muted,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormulaCard(
    state: MetricEditorState,
    onFormulaChange: (String) -> Unit,
    onInsertReference: (Metric) -> Unit,
    onApplyFormulaFix: () -> Unit,
    onApplyPreset: (FormulaPresetCatalog.Preset) -> Unit,
) {
    val colors = BodyMeasureTheme.colors
    SoftGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(18.dp),
    ) {
        FieldLabel(text = "公式", hint = "${state.formula.length}/512")
        Spacer(Modifier.height(6.dp))
        FormulaField(
            value = state.formula,
            onValueChange = onFormulaChange,
            isError = state.formulaError != null,
        )
        if (state.formulaError != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = state.formulaError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        } else if (state.formulaZeroHint != null) {
            Spacer(Modifier.height(6.dp))
            ZeroRoundingNotice(
                hint = state.formulaZeroHint,
                precision = state.precision,
                onApplyFix = onApplyFormulaFix,
            )
        } else if (state.formulaPreview != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = state.formulaPreview,
                style = MaterialTheme.typography.bodySmall,
                color = if (state.formulaSample != null) colors.sage else colors.muted,
            )
        }

        if (state.formulaPresets.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            FieldLabel(text = "常用公式", hint = "点一下填入")
            Spacer(Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                state.formulaPresets.forEach { preset ->
                    SuggestionChip(
                        text = preset.title,
                        selected = preset.id == state.selectedPresetId,
                        onClick = { onApplyPreset(preset) },
                    )
                }
            }
            val selectedPreset = state.formulaPresets.firstOrNull {
                it.id == state.selectedPresetId
            }
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = selectedPreset?.description
                    ?: "按现有指标自动生成，填入后还能手动调整。",
                style = MaterialTheme.typography.labelSmall,
                color = colors.muted,
            )
        }

        Spacer(Modifier.height(16.dp))
        FieldLabel(text = "插入指标", hint = "点一下追加到末尾")
        Spacer(Modifier.height(6.dp))
        if (state.availableMetrics.isEmpty()) {
            Text(
                text = "还没有其他指标可以引用，先创建直接记录的指标。",
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                state.availableMetrics.forEach { candidate ->
                    SuggestionChip(
                        text = "＋ ${candidate.name}",
                        selected = candidate.id in state.referencedIds,
                        onClick = { onInsertReference(candidate) },
                    )
                }
            }
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = "用 [指标名] 引用。支持 + - * / ^ 与 abs、sqrt、pow、round、min、max、clamp。" +
                "\n公式按记录原值计算，单位不会自动换算；身高用 cm 时写成 [身高] / 100。",
            style = MaterialTheme.typography.labelSmall,
            color = colors.muted,
        )
    }
}

@Composable
private fun ZeroRoundingNotice(
    hint: FormulaZeroGuard.Hint,
    precision: Int,
    onApplyFix: () -> Unit,
) {
    val colors = BodyMeasureTheme.colors
    val digits = (precision + 3).coerceAtMost(NumberFormats.MAX_PRECISION)
    val raw = NumberFormats.format(hint.rawValue, digits)
    Column {
        Text(
            text = if (hint.suggestion != null) {
                "按当前精度会显示为 0.0（试算约 $raw）。公式按记录原值计算，厘米不会自动换算成米。"
            } else {
                "按当前精度会显示为 0.0（试算约 $raw）。可以提高精度或检查单位。"
            },
            style = MaterialTheme.typography.bodySmall,
            color = colors.ochre,
        )
        if (hint.suggestion != null) {
            TextButton(
                onClick = onApplyFix,
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "把厘米换算成米（÷100）",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.clay,
                )
            }
        }
    }
}

@Composable
private fun FormulaField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
) {
    val colors = BodyMeasureTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.ink.copy(alpha = if (colors.isDark) 0.06f else 0.035f))
            .border(
                width = 1.dp,
                color = if (isError) {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                } else {
                    colors.line
                },
                shape = shape,
            )
            .padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        if (value.isEmpty()) {
            Text(
                text = "例如：[体重] / ([身高] / 100) ^ 2",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.muted.copy(alpha = 0.55f),
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.ink),
            cursorBrush = SolidColor(colors.clay),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PairingCard(
    state: MetricEditorState,
    onPairingPolicyChange: (PairingPolicy) -> Unit,
    onPairingWindowChange: (Int) -> Unit,
) {
    val colors = BodyMeasureTheme.colors
    SoftGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(18.dp),
    ) {
        FieldLabel(text = "配对方式", hint = "关联记录日期不一致时")
        Spacer(Modifier.height(Spacing.xs))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            ChoiceChip(
                text = "同一天",
                selected = state.pairingPolicy == PairingPolicy.SAME_DAY,
                onClick = { onPairingPolicyChange(PairingPolicy.SAME_DAY) },
            )
            ChoiceChip(
                text = "就近",
                selected = state.pairingPolicy == PairingPolicy.NEAREST,
                onClick = { onPairingPolicyChange(PairingPolicy.NEAREST) },
            )
            ChoiceChip(
                text = "沿用",
                selected = state.pairingPolicy == PairingPolicy.CARRY_FORWARD,
                onClick = { onPairingPolicyChange(PairingPolicy.CARRY_FORWARD) },
            )
        }

        if (state.pairingPolicy != PairingPolicy.SAME_DAY) {
            Spacer(Modifier.height(16.dp))
            FieldLabel(text = "允许相差", hint = "天")
            Spacer(Modifier.height(Spacing.xs))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                (listOf(1, 2, 3, 7) + state.pairingWindowDays)
                    .distinct()
                    .sorted()
                    .forEach { days ->
                        ChoiceChip(
                            text = "$days 天",
                            selected = state.pairingWindowDays == days,
                            onClick = { onPairingWindowChange(days) },
                        )
                    }
            }
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = pairingDescription(state.pairingPolicy, state.pairingWindowDays),
            style = MaterialTheme.typography.labelSmall,
            color = colors.muted,
        )
    }
}

@Composable
private fun FormulaHistoryCard(state: MetricEditorState) {
    val colors = BodyMeasureTheme.colors
    val versions = state.formulaHistory.sortedByDescending { it.version }
    val current = versions.maxOfOrNull { it.version }
    SoftGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
    ) {
        versions.take(6).forEachIndexed { index, item ->
            if (index > 0) {
                GlassDivider()
            }
            Column(Modifier.padding(vertical = 11.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "v${item.version}",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (item.version == current) colors.clay else colors.inkSecondary,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = formulaDate(item.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.muted,
                    )
                    Spacer(Modifier.weight(1f))
                    if (item.version == current) {
                        Text(
                            text = "当前",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.clay,
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.formulaDisplay,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkSecondary,
                )
                if (!item.changeNote.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.changeNote,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.muted,
                    )
                }
            }
        }
    }
}

private fun formulaDate(millis: Long): String =
    DateFormats.fullDate(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate())

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DisplayCard(
    state: MetricEditorState,
    onPrecisionChange: (Int) -> Unit,
    onColorChange: (Long) -> Unit,
    onIconChange: (String) -> Unit,
) {
    val colors = BodyMeasureTheme.colors
    SoftGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(18.dp),
    ) {
        FieldLabel(text = "小数位")
        Spacer(Modifier.height(Spacing.xs))
        Row(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            PRECISION_OPTIONS.forEach { (value, label) ->
                ChoiceChip(
                    text = label,
                    selected = state.precision == value,
                    onClick = { onPrecisionChange(value) },
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        FieldLabel(text = "颜色")
        Spacer(Modifier.height(Spacing.xs))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricPalette.all.forEach { argb ->
                ColorSwatch(
                    argb = argb,
                    selected = state.colorArgb == argb,
                    onClick = { onColorChange(argb) },
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        FieldLabel(text = "图标", hint = "第一个跟随名称首字")
        Spacer(Modifier.height(Spacing.xs))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            GlyphTile(
                glyph = metricGlyph(MetricIconKeys.GENERIC, state.name),
                selected = state.iconKey == MetricIconKeys.GENERIC,
                accentArgb = state.colorArgb,
                onClick = { onIconChange(MetricIconKeys.GENERIC) },
            )
            MetricIconKeys.all
                .filter { it != MetricIconKeys.GENERIC }
                .forEach { key ->
                    GlyphTile(
                        glyph = metricGlyph(key, state.name),
                        selected = state.iconKey == key,
                        accentArgb = state.colorArgb,
                        onClick = { onIconChange(key) },
                    )
                }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "图标只影响显示，不会改变记录内容。",
            style = MaterialTheme.typography.labelSmall,
            color = colors.muted,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = BodyMeasureTheme.colors.muted,
        modifier = Modifier.padding(start = Spacing.xxs),
    )
}

@Composable
private fun FieldLabel(text: String, hint: String? = null) {
    val colors = BodyMeasureTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = colors.inkSecondary,
        )
        if (hint != null) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = hint,
                style = MaterialTheme.typography.labelSmall,
                color = colors.muted.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = BodyMeasureTheme.colors
    val shape = CircleShape
    val background by animateColorAsState(
        targetValue = if (selected) {
            colors.clay.copy(alpha = if (colors.isDark) 0.22f else 0.12f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(Motion.quick),
        label = "choiceChipBackground",
    )
    val outline by animateColorAsState(
        targetValue = if (selected) colors.clay.copy(alpha = 0.55f) else colors.line,
        animationSpec = tween(Motion.quick),
        label = "choiceChipOutline",
    )
    Box(
        modifier = Modifier
            .clip(shape)
            .background(background)
            .border(1.dp, outline, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .heightIn(min = 38.dp)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) colors.ink else colors.muted,
        )
    }
}

@Composable
private fun SuggestionChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = BodyMeasureTheme.colors
    val shape = CircleShape
    val background by animateColorAsState(
        targetValue = if (selected) {
            colors.clay.copy(alpha = if (colors.isDark) 0.18f else 0.1f)
        } else {
            colors.ink.copy(alpha = if (colors.isDark) 0.06f else 0.035f)
        },
        animationSpec = tween(Motion.quick),
        label = "suggestionChipBackground",
    )
    Box(
        modifier = Modifier
            .clip(shape)
            .background(background)
            .border(1.dp, if (selected) colors.clay.copy(alpha = 0.4f) else colors.line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) colors.clay else colors.muted,
        )
    }
}

@Composable
private fun ColorSwatch(argb: Long, selected: Boolean, onClick: () -> Unit) {
    val colors = BodyMeasureTheme.colors
    val accent = colors.metricColor(argb)
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .border(
                width = 1.5.dp,
                color = if (selected) accent.copy(alpha = 0.75f) else Color.Transparent,
                shape = CircleShape,
            )
            .padding(4.dp)
            .clip(CircleShape)
            .background(accent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = AppIcons.Check,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun GlyphTile(glyph: String, selected: Boolean, accentArgb: Long, onClick: () -> Unit) {
    val colors = BodyMeasureTheme.colors
    val accent = colors.metricColor(accentArgb)
    val shape = RoundedCornerShape(14.dp)
    val background by animateColorAsState(
        targetValue = if (selected) {
            accent.copy(alpha = if (colors.isDark) 0.18f else 0.1f)
        } else {
            colors.ink.copy(alpha = if (colors.isDark) 0.05f else 0.03f)
        },
        animationSpec = tween(Motion.quick),
        label = "glyphTileBackground",
    )
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(shape)
            .background(background)
            .border(1.dp, if (selected) accent.copy(alpha = 0.55f) else colors.line, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = glyph,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
            color = if (selected) accent else colors.inkSecondary,
        )
    }
}

private fun previewMetric(
    name: String,
    unit: String,
    colorArgb: Long,
    iconKey: String,
    kind: MetricKind,
): Metric = Metric(
    id = "preview",
    name = name.trim().ifEmpty { "未命名指标" },
    kind = kind,
    unit = unit.trim(),
    unitDimension = UnitDimensions.infer(unit),
    precision = 1,
    colorArgb = colorArgb,
    iconKey = iconKey,
    formula = null,
    formulaDisplay = null,
    formulaVersion = 1,
    pairingPolicy = PairingPolicy.NEAREST,
    pairingWindowDays = 2,
    isConstant = false,
    sortOrder = 0,
    isArchived = false,
    createdAt = 0,
    updatedAt = 0,
    deletedAt = null,
    originDeviceId = "preview",
)

private val UNIT_SUGGESTIONS = listOf("kg", "斤", "cm", "%")

private val PRECISION_OPTIONS = listOf(
    0 to "整数",
    1 to "0.1",
    2 to "0.01",
    3 to "0.001",
)

@Preview(name = "新建指标 · 浅色", widthDp = 360, heightDp = 900, showBackground = true)
@Composable
private fun MetricEditorLightPreview() {
    BodyMeasureTheme(themeMode = ThemeMode.LIGHT) {
        SoftGradientBackground {
            MetricEditorContent(
                state = MetricEditorState(
                    isNew = true,
                    name = "腰围",
                    unit = "cm",
                    precision = 1,
                    colorArgb = MetricPalette.OCHRE,
                    iconKey = MetricIconKeys.WAIST,
                ),
                onBack = {},
                onNameChange = {},
                onUnitChange = {},
                onPrecisionChange = {},
                onColorChange = {},
                onIconChange = {},
                onKindChange = {},
                onConstantChange = {},
                onFormulaChange = {},
                onInsertReference = {},
                onPairingPolicyChange = {},
                onPairingWindowChange = {},
                onSave = {},
            )
        }
    }
}

@Preview(name = "编辑指标 · 深色", widthDp = 360, heightDp = 900, showBackground = true)
@Composable
private fun MetricEditorDarkPreview() {
    BodyMeasureTheme(themeMode = ThemeMode.DARK) {
        SoftGradientBackground {
            MetricEditorContent(
                state = MetricEditorState(
                    isNew = false,
                    name = "体重",
                    unit = "kg",
                    precision = 1,
                    colorArgb = MetricPalette.CLAY,
                    iconKey = MetricIconKeys.WEIGHT,
                ),
                onBack = {},
                onNameChange = {},
                onUnitChange = {},
                onPrecisionChange = {},
                onColorChange = {},
                onIconChange = {},
                onKindChange = {},
                onConstantChange = {},
                onFormulaChange = {},
                onInsertReference = {},
                onPairingPolicyChange = {},
                onPairingWindowChange = {},
                onSave = {},
            )
        }
    }
}
