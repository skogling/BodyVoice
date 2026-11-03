package com.liangji.bodymeasure.ui.metrics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.liangji.bodymeasure.core.designsystem.SoftActionButton
import com.liangji.bodymeasure.core.designsystem.SoftGlassSurface
import com.liangji.bodymeasure.core.designsystem.SoftGradientBackground
import com.liangji.bodymeasure.core.designsystem.Spacing
import com.liangji.bodymeasure.domain.model.FormulaHistory
import com.liangji.bodymeasure.domain.model.Measurement
import com.liangji.bodymeasure.domain.model.MeasurementInput
import com.liangji.bodymeasure.domain.model.Metric
import com.liangji.bodymeasure.domain.model.MetricIconKeys
import com.liangji.bodymeasure.domain.model.MetricKind
import com.liangji.bodymeasure.domain.model.MetricPalette
import com.liangji.bodymeasure.domain.model.PairingPolicy
import com.liangji.bodymeasure.domain.model.ThemeMode
import com.liangji.bodymeasure.domain.model.UnitDimension
import com.liangji.bodymeasure.domain.summary.MetricSnapshot
import com.liangji.bodymeasure.ui.components.AnimatedMetricValue
import com.liangji.bodymeasure.ui.components.UndoBanner
import com.liangji.bodymeasure.ui.components.UndoPrompt
import com.liangji.bodymeasure.ui.components.UndoPromptEffect
import com.liangji.bodymeasure.ui.components.metricAccent
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun MetricDetailScreen(
    onBack: () -> Unit,
    onEditMetric: (String) -> Unit,
    viewModel: MetricDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var prompt by remember { mutableStateOf<UndoPrompt?>(null) }
    var quickOpen by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Measurement?>(null) }

    val metric = state.metric

    MetricDetailContent(
        state = state,
        prompt = prompt,
        onBack = onBack,
        onEditMetric = { metric?.let { onEditMetric(it.id) } },
        onArchive = {
            if (metric != null && !metric.isArchived) {
                scope.launch {
                    viewModel.setArchived(true)
                    prompt = UndoPrompt(System.nanoTime(), "已归档「${metric.name}」") {
                        scope.launch {
                            viewModel.setArchived(false)
                            prompt = UndoPrompt(System.nanoTime(), "已恢复「${metric.name}」")
                        }
                    }
                }
            }
        },
        onRestore = {
            if (metric != null && metric.isArchived) {
                scope.launch {
                    viewModel.setArchived(false)
                    prompt = UndoPrompt(System.nanoTime(), "已恢复「${metric.name}」")
                }
            }
        },
        onRecord = { quickOpen = true },
        onEditMeasurement = { editing = it },
        onUndo = { prompt?.onUndo?.invoke() },
        onDismissPrompt = { prompt = null },
    )

    if (quickOpen && metric != null && metric.kind == MetricKind.RAW) {
        QuickRecordSheet(
            metrics = listOf(metric),
            today = state.today,
            initialMetricId = metric.id,
            measurementsByMetric = mapOf(metric.id to state.measurements),
            onDismiss = { quickOpen = false },
            onSave = viewModel::save,
            onSaved = { change ->
                quickOpen = false
                val value = NumberFormats.format(change.saved.value, metric.precision)
                val message = "已记录 ${metric.name} $value ${metric.unit}".trim()
                prompt = UndoPrompt(System.nanoTime(), message) {
                    scope.launch {
                        viewModel.undo(change)
                        prompt = UndoPrompt(System.nanoTime(), "已撤销这次记录")
                    }
                }
            },
        )
    }

    val editingMeasurement = editing
    if (editingMeasurement != null && metric != null && metric.kind == MetricKind.RAW) {
        MeasurementEditSheet(
            measurement = editingMeasurement,
            metric = metric,
            today = state.today,
            onDismiss = { editing = null },
            onSave = { value, note ->
                viewModel.save(
                    MeasurementInput(
                        metricId = metric.id,
                        date = editingMeasurement.date,
                        value = value,
                        note = note,
                        existingId = editingMeasurement.id,
                    ),
                    replaceExisting = false,
                )
            },
            onSaved = { change ->
                editing = null
                prompt = UndoPrompt(
                    System.nanoTime(),
                    "已更新 ${DateFormats.displayDate(editingMeasurement.date, state.today)} 的记录",
                ) {
                    scope.launch {
                        viewModel.undo(change)
                        prompt = UndoPrompt(System.nanoTime(), "已还原修改前的值")
                    }
                }
            },
            onDelete = {
                editing = null
                scope.launch {
                    val deleted = viewModel.delete(editingMeasurement.id)
                    if (deleted != null) {
                        prompt = UndoPrompt(
                            System.nanoTime(),
                            "已删除 ${DateFormats.displayDate(deleted.date, state.today)} 的记录",
                        ) {
                            scope.launch {
                                viewModel.restore(deleted)
                                prompt = UndoPrompt(System.nanoTime(), "已恢复这条记录")
                            }
                        }
                    }
                }
            },
        )
    }
}

@Composable
internal fun MetricDetailContent(
    state: MetricDetailState,
    prompt: UndoPrompt?,
    onBack: () -> Unit,
    onEditMetric: () -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onRecord: () -> Unit,
    onEditMeasurement: (Measurement) -> Unit,
    onUndo: () -> Unit,
    onDismissPrompt: () -> Unit,
) {
    val colors = BodyMeasureTheme.colors
    UndoPromptEffect(prompt = prompt, onDismiss = onDismissPrompt)

    ScreenColumn(
        modifier = Modifier
            .padding(horizontal = Spacing.screen)
            .padding(top = Spacing.xs),
        maxWidth = 760.dp,
    ) {
        DetailNav(
            metric = state.metric,
            onBack = onBack,
            onEditMetric = onEditMetric,
            onArchive = onArchive,
        )

        val metric = state.metric
        val snapshot = state.snapshot
        when {
            state.loading -> CenteredNote(
                text = "正在读取记录…",
                modifier = Modifier.weight(1f),
            )

            metric == null || snapshot == null -> MissingDetail(
                onBack = onBack,
                modifier = Modifier.weight(1f),
            )

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = 18.dp,
                            bottom = if (metric.isArchived) Spacing.sm else 80.dp,
                        ),
                    ) {
                    if (metric.isArchived) {
                        item(key = "archived") {
                            ArchivedBanner(onRestore = onRestore)
                            Spacer(Modifier.height(Spacing.md))
                        }
                    }
                    item(key = "hero") {
                        DetailHero(snapshot = snapshot, today = state.today)
                    }
                    if (state.isDerived) {
                        item(key = "formula") {
                            Spacer(Modifier.height(Spacing.md))
                            DerivedInfoCard(metric = metric, history = state.formulaHistory)
                        }
                    }
                    if (state.measurements.isNotEmpty()) {
                        item(key = "charts") {
                            Spacer(Modifier.height(if (state.isDerived) Spacing.md else 18.dp))
                            MetricChartSection(
                                metric = metric,
                                measurements = state.measurements,
                                today = state.today,
                            )
                        }
                    }
                    item(key = "history-header") {
                        HistoryHeader(
                            count = state.recordCount,
                            derived = state.isDerived,
                        )
                    }
                    val rows = state.sections.flatMap { it.items }
                    if (rows.isEmpty()) {
                        item(key = "empty") {
                            EmptyHistory(archived = metric.isArchived, derived = state.isDerived)
                        }
                    } else {
                        rows.forEachIndexed { index, measurement ->
                            item(key = "row-${measurement.id}") {
                                HistoryRow(
                                    measurement = measurement,
                                    metric = metric,
                                    today = state.today,
                                    onClick = if (state.isDerived) {
                                        null
                                    } else {
                                        { onEditMeasurement(measurement) }
                                    },
                                )
                                if (index != rows.lastIndex) {
                                    GlassDivider()
                                }
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                ) {
                    AnimatedVisibility(
                        visible = prompt != null,
                        enter = fadeIn(tween(Motion.quick)) +
                            slideInVertically(tween(Motion.standard)) { it / 3 },
                        exit = fadeOut(tween(Motion.quick)) +
                            slideOutVertically(tween(Motion.quick)) { it / 3 },
                    ) {
                        if (prompt != null) {
                            Column {
                                UndoBanner(prompt = prompt, onUndo = onUndo)
                                Spacer(Modifier.height(Spacing.xs))
                            }
                        }
                    }
                    if (!metric.isArchived) {
                        if (state.isDerived) {
                            SoftActionButton(
                                text = "编辑公式",
                                onClick = onEditMetric,
                                icon = AppIcons.Edit,
                            )
                        } else {
                            SoftActionButton(
                                text = "记下此刻",
                                onClick = onRecord,
                            )
                        }
                    }
                    Spacer(Modifier.height(Spacing.xs))
                }
                }
            }
        }
    }
}

@Composable
private fun DetailNav(
    metric: Metric?,
    onBack: () -> Unit,
    onEditMetric: () -> Unit,
    onArchive: () -> Unit,
) {
    val colors = BodyMeasureTheme.colors
    var menuOpen by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
    ) {
        GlassIconButton(
            imageVector = AppIcons.ArrowBack,
            contentDescription = "返回",
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        Text(
            text = metric?.name.orEmpty(),
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp,
            ),
            color = colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 56.dp),
        )
        if (metric != null) {
            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                GlassIconButton(
                    imageVector = AppIcons.More,
                    contentDescription = "更多操作",
                    onClick = { menuOpen = true },
                )
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    offset = DpOffset((-132).dp, 2.dp),
                    modifier = Modifier.width(180.dp),
                    shape = RoundedCornerShape(18.dp),
                    containerColor = colors.sheet,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    border = BorderStroke(1.dp, colors.line),
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "编辑指标",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.ink,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = AppIcons.Edit,
                                contentDescription = null,
                                tint = colors.inkSecondary,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        onClick = {
                            menuOpen = false
                            onEditMetric()
                        },
                    )
                    if (!metric.isArchived) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "归档",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.ink,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = AppIcons.Archive,
                                    contentDescription = null,
                                    tint = colors.clay,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            onClick = {
                                menuOpen = false
                                onArchive()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailHero(snapshot: MetricSnapshot, today: LocalDate) {
    val colors = BodyMeasureTheme.colors
    val metric = snapshot.metric
    val latest = snapshot.latest
    val derived = metric.kind == MetricKind.DERIVED
    val valueText = latest?.let { NumberFormats.format(it.value, metric.precision) } ?: "—"
    val valueSize = when {
        valueText.length > 10 -> 36.sp
        valueText.length > 7 -> 42.sp
        else -> 50.sp
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (latest == null) {
            Text(
                text = if (derived) "还没有结果" else "还没有记录",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.ink,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (derived) {
                    "记录关联指标后，结果会自动出现在这里。"
                } else {
                    "记下第一个数字，之后这里会显示变化。"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted,
            )
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
                AnimatedMetricValue(
                    text = valueText,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = valueSize,
                        lineHeight = (valueSize.value * 0.94f).sp,
                        letterSpacing = (-valueSize.value * 0.05f).sp,
                    ),
                    color = colors.ink,
                    contentAlignment = Alignment.BottomStart,
                )
                if (metric.unit.isNotBlank()) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = metric.unit,
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.muted,
                        modifier = Modifier.padding(bottom = 7.dp),
                    )
                }
            }
            Spacer(Modifier.height(9.dp))
            HeroMeta(snapshot = snapshot, today = today)
        }
    }
}

@Composable
private fun HeroMeta(snapshot: MetricSnapshot, today: LocalDate) {
    val colors = BodyMeasureTheme.colors
    val metric = snapshot.metric
    val latest = snapshot.latest ?: return
    val derived = metric.kind == MetricKind.DERIVED
    val delta = snapshot.delta
    val arrowTint = lerp(metricAccent(metric), colors.ink, 0.25f)
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        if (delta == null) {
            Text(
                text = if (derived) "第一条结果" else "第一条记录",
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
            )
        } else {
            val signed = NumberFormats.formatSigned(delta.value, metric.precision)
            Row(verticalAlignment = Alignment.CenterVertically) {
                when {
                    signed.startsWith("+") -> {
                        Icon(
                            imageVector = AppIcons.ArrowUp,
                            contentDescription = null,
                            tint = arrowTint,
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(Modifier.width(2.dp))
                    }

                    signed.startsWith(NumberFormats.MINUS) -> {
                        Icon(
                            imageVector = AppIcons.ArrowDown,
                            contentDescription = null,
                            tint = arrowTint,
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(Modifier.width(2.dp))
                    }

                    else -> Unit
                }
                AnimatedMetricValue(
                    text = if (metric.unit.isBlank()) signed else "$signed ${metric.unit}",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = colors.inkSecondary,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "较 ${DateFormats.displayDate(delta.fromDate, today)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.muted,
                )
            }
        }
        Text(
            text = "·",
            style = MaterialTheme.typography.labelSmall,
            color = colors.muted.copy(alpha = 0.6f),
        )
        Text(
            text = DateFormats.stamp(latest.date, latest.measuredAt, today),
            style = MaterialTheme.typography.labelSmall,
            color = colors.muted,
        )
    }
}

@Composable
private fun DerivedInfoCard(metric: Metric, history: List<FormulaHistory>) {
    val colors = BodyMeasureTheme.colors
    SoftGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "派生公式",
                style = MaterialTheme.typography.labelMedium,
                color = colors.muted,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "v${metric.formulaVersion}" +
                    if (history.size > 1) " · 已调整 ${history.size - 1} 次" else "",
                style = MaterialTheme.typography.labelSmall,
                color = colors.muted.copy(alpha = 0.8f),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = metric.formulaDisplay ?: metric.formula.orEmpty(),
            style = MaterialTheme.typography.titleMedium,
            color = colors.ink,
        )
        Spacer(Modifier.height(Spacing.xxs))
        Text(
            text = pairingDescription(metric.pairingPolicy, metric.pairingWindowDays),
            style = MaterialTheme.typography.bodySmall,
            color = colors.muted,
        )
    }
}

internal fun pairingDescription(policy: PairingPolicy, windowDays: Int): String = when (policy) {
    PairingPolicy.SAME_DAY -> "只使用同一天的关联记录"
    PairingPolicy.NEAREST -> "就近配对 ±$windowDays 天内的关联记录"
    PairingPolicy.CARRY_FORWARD -> "沿用 $windowDays 天内最近一次关联记录"
}

@Composable
private fun ArchivedBanner(onRestore: () -> Unit) {
    val colors = BodyMeasureTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.plum.copy(alpha = if (colors.isDark) 0.14f else 0.08f))
            .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(colors.plum.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AppIcons.Archive,
                contentDescription = null,
                tint = colors.plum,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = "已归档，仅历史可见",
                style = MaterialTheme.typography.titleSmall,
                color = colors.ink,
            )
            Text(
                text = "记录仍然保留，恢复后会回到首页。",
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
            )
        }
        TextButton(onClick = onRestore) {
            Text(
                text = "恢复",
                style = MaterialTheme.typography.labelLarge,
                color = colors.clay,
            )
        }
    }
}

@Composable
private fun HistoryHeader(count: Int, derived: Boolean) {
    val colors = BodyMeasureTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 22.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (derived) "计算结果" else "最近记录",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = colors.inkSecondary,
        )
        Spacer(Modifier.weight(1f))
        if (count > 0) {
            Text(
                text = if (derived) "自动生成" else "$count 条",
                style = MaterialTheme.typography.labelSmall,
                color = colors.muted,
            )
        }
    }
}

@Composable
private fun HistoryRow(
    measurement: Measurement,
    metric: Metric,
    today: LocalDate,
    onClick: (() -> Unit)?,
) {
    val colors = BodyMeasureTheme.colors
    val meta = measurement.note?.takeIf { it.isNotBlank() }
        ?: if (measurement.date == today) {
            DateFormats.stamp(measurement.date, measurement.measuredAt, today)
        } else {
            DateFormats.relativeDay(measurement.date, today)
        }
    val description = buildString {
        append(DateFormats.header(measurement.date))
        append("，")
        append(NumberFormats.format(measurement.value, metric.precision))
        if (metric.unit.isNotBlank()) {
            append(" ")
            append(metric.unit)
        }
        measurement.note?.takeIf { it.isNotBlank() }?.let {
            append("，备注：")
            append(it)
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.semantics(mergeDescendants = true) {
                        contentDescription = description
                    }
                } else {
                    Modifier
                },
            )
            .clickable(
                enabled = onClick != null,
                onClickLabel = "编辑这条记录",
                role = Role.Button,
            ) { onClick?.invoke() }
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = DateFormats.displayDate(measurement.date, today),
            style = MaterialTheme.typography.labelSmall,
            color = colors.muted,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
                .widthIn(min = 58.dp)
                .padding(end = 8.dp),
        )
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = NumberFormats.format(measurement.value, metric.precision),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = colors.ink,
                maxLines = 1,
            )
            if (metric.unit.isNotBlank()) {
                Spacer(Modifier.width(3.dp))
                Text(
                    text = metric.unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.muted,
                    maxLines = 1,
                    modifier = Modifier.padding(bottom = 1.dp),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = meta,
            style = MaterialTheme.typography.labelSmall,
            color = colors.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(max = 120.dp),
        )
        if (onClick != null) {
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = AppIcons.ChevronRight,
                contentDescription = null,
                tint = colors.muted.copy(alpha = 0.45f),
                modifier = Modifier.size(11.dp),
            )
        }
    }
}

@Composable
private fun EmptyHistory(archived: Boolean, derived: Boolean) {
    val colors = BodyMeasureTheme.colors
    SoftGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.xl),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colors.ink.copy(alpha = if (colors.isDark) 0.06f else 0.04f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = AppIcons.Calendar,
                    contentDescription = null,
                    tint = colors.muted,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = when {
                    archived -> "归档期间不会新增记录"
                    derived -> "关联指标有记录后会自动算出结果"
                    else -> "第一条记录会出现在这里"
                },
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = when {
                    archived -> "恢复指标后可以继续记录。"
                    derived -> "结果会跟着关联指标一起更新。"
                    else -> "点下面的「记下此刻」开始。"
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CenteredNote(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = BodyMeasureTheme.colors.muted,
        )
    }
}

@Composable
private fun MissingDetail(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BodyMeasureTheme.colors
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "这个指标不存在",
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink,
            )
            Spacer(Modifier.height(Spacing.xxs))
            Text(
                text = "它可能已经被删除。",
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
            )
            Spacer(Modifier.height(Spacing.xs))
            TextButton(onClick = onBack) {
                Text(
                    text = "返回",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.clay,
                )
            }
        }
    }
}

@Preview(name = "单指标 · 浅色", widthDp = 360, heightDp = 900, showBackground = true)
@Composable
private fun DetailLightPreview() {
    BodyMeasureTheme(themeMode = ThemeMode.LIGHT) {
        SoftGradientBackground {
            MetricDetailContent(
                state = previewDetailState(archived = false),
                prompt = null,
                onBack = {},
                onEditMetric = {},
                onArchive = {},
                onRestore = {},
                onRecord = {},
                onEditMeasurement = {},
                onUndo = {},
                onDismissPrompt = {},
            )
        }
    }
}

@Preview(name = "单指标 · 归档深色", widthDp = 360, heightDp = 900, showBackground = true)
@Composable
private fun DetailArchivedDarkPreview() {
    BodyMeasureTheme(themeMode = ThemeMode.DARK) {
        SoftGradientBackground {
            MetricDetailContent(
                state = previewDetailState(archived = true),
                prompt = null,
                onBack = {},
                onEditMetric = {},
                onArchive = {},
                onRestore = {},
                onRecord = {},
                onEditMeasurement = {},
                onUndo = {},
                onDismissPrompt = {},
            )
        }
    }
}

private fun previewDetailState(archived: Boolean): MetricDetailState {
    val today = LocalDate.of(2026, 10, 30)
    val metric = Metric(
        id = "waist",
        name = "腰围",
        kind = MetricKind.RAW,
        unit = "cm",
        unitDimension = UnitDimension.LENGTH,
        precision = 1,
        colorArgb = MetricPalette.CLAY,
        iconKey = MetricIconKeys.WAIST,
        formula = null,
        formulaDisplay = null,
        formulaVersion = 1,
        pairingPolicy = PairingPolicy.NEAREST,
        pairingWindowDays = 2,
        isConstant = false,
        sortOrder = 0,
        isArchived = archived,
        createdAt = 0,
        updatedAt = 0,
        deletedAt = null,
        originDeviceId = "preview",
    )
    val notes = mapOf(today to "早晨空腹", today.minusDays(21) to "晚饭后")
    val measurements = listOf(
        today to 78.5,
        today.minusDays(7) to 79.7,
        today.minusDays(14) to 79.2,
        today.minusDays(21) to 80.1,
        today.minusDays(35) to 80.4,
    ).mapIndexed { index, (date, value) ->
        Measurement(
            id = "preview-$index",
            metricId = metric.id,
            date = date,
            measuredAt = 0,
            value = value,
            note = notes[date],
            source = com.liangji.bodymeasure.domain.model.MeasurementSource.MANUAL,
            createdAt = 0,
            updatedAt = 0,
            deletedAt = null,
            originDeviceId = "preview",
        )
    }
    return metricDetailState(metric, measurements, today)
}
