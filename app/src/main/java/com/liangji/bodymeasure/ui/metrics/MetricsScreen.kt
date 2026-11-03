package com.liangji.bodymeasure.ui.metrics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.liangji.bodymeasure.BuildConfig
import com.liangji.bodymeasure.core.common.DateFormats
import com.liangji.bodymeasure.core.common.NumberFormats
import com.liangji.bodymeasure.core.designsystem.AppIcons
import com.liangji.bodymeasure.core.designsystem.BodyMeasureTheme
import com.liangji.bodymeasure.core.designsystem.BrandWaveMark
import com.liangji.bodymeasure.core.designsystem.GlassIconButton
import com.liangji.bodymeasure.core.designsystem.Motion
import com.liangji.bodymeasure.core.designsystem.SoftActionButton
import com.liangji.bodymeasure.core.designsystem.SoftGlassSurface
import com.liangji.bodymeasure.core.designsystem.SoftGradientBackground
import com.liangji.bodymeasure.core.designsystem.SoftPill
import com.liangji.bodymeasure.core.designsystem.Spacing
import com.liangji.bodymeasure.core.designsystem.accentGlow
import com.liangji.bodymeasure.core.designsystem.onAccent
import com.liangji.bodymeasure.domain.model.Metric
import com.liangji.bodymeasure.domain.model.MetricKind
import com.liangji.bodymeasure.domain.model.MetricMove
import com.liangji.bodymeasure.domain.model.PairingPolicy
import com.liangji.bodymeasure.domain.model.ThemeMode
import com.liangji.bodymeasure.domain.model.UnitDimension
import com.liangji.bodymeasure.domain.summary.MetricSnapshot
import com.liangji.bodymeasure.ui.components.AnimatedMetricValue
import com.liangji.bodymeasure.ui.components.MetricSparkline
import com.liangji.bodymeasure.ui.components.UndoBanner
import com.liangji.bodymeasure.ui.components.UndoPrompt
import com.liangji.bodymeasure.ui.components.UndoPromptEffect
import com.liangji.bodymeasure.ui.components.metricAccent
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun MetricsScreen(
    onOpenSettings: () -> Unit,
    onCreateMetric: () -> Unit,
    onEditMetric: (String) -> Unit,
    onOpenMetric: (String) -> Unit,
    viewModel: MetricsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var templatesOpen by rememberSaveable { mutableStateOf(false) }
    var quickOpen by rememberSaveable { mutableStateOf(false) }
    var quickMetricId by rememberSaveable { mutableStateOf<String?>(null) }
    var actionMetricId by rememberSaveable { mutableStateOf<String?>(null) }
    var prompt by remember { mutableStateOf<UndoPrompt?>(null) }

    MetricsContent(
        state = state,
        prompt = prompt,
        onOpenSettings = onOpenSettings,
        onCreateMetric = onCreateMetric,
        onOpenTemplates = { templatesOpen = true },
        onOpenMetric = onOpenMetric,
        onLongPressMetric = { metric -> actionMetricId = metric.id },
        onQuickRecord = { metricId ->
            quickMetricId = metricId
            quickOpen = true
        },
        onUndo = {
            prompt?.onUndo?.invoke()
        },
        onDismissPrompt = { prompt = null },
    )

    if (templatesOpen) {
        TemplateSheet(
            existingNames = state.metrics.map { it.name }.toSet(),
            onDismiss = { templatesOpen = false },
            onCreate = viewModel::createFromPresets,
            onCreated = { result ->
                templatesOpen = false
                prompt = UndoPrompt(System.nanoTime(), result.bannerText())
            },
            onCreateCustom = {
                templatesOpen = false
                onCreateMetric()
            },
        )
    }

    if (quickOpen) {
        if (state.recordableMetrics.isEmpty()) {
            LaunchedEffect(Unit) { quickOpen = false }
        } else {
            QuickRecordSheet(
                metrics = state.recordableMetrics,
                today = state.today,
                initialMetricId = quickMetricId ?: state.lastMetricId,
                measurementsByMetric = state.measurementsByMetric,
                onDismiss = { quickOpen = false },
                onSave = viewModel::save,
                onSaved = { change ->
                    quickOpen = false
                    val metric = state.recordableMetrics.firstOrNull { it.id == change.saved.metricId }
                    val message = if (metric == null) {
                        "听见了 · 已记录"
                    } else {
                        "听见了 · ${metric.name} ${NumberFormats.format(change.saved.value, metric.precision)} ${metric.unit}".trim()
                    }
                    prompt = UndoPrompt(System.nanoTime(), message) {
                        scope.launch {
                            viewModel.undo(change)
                            prompt = UndoPrompt(System.nanoTime(), "已撤销这次记录")
                        }
                    }
                },
            )
        }
    }

    val actionMetric = state.metrics.firstOrNull { it.id == actionMetricId }
    if (actionMetric != null) {
        val position = state.metrics.indexOf(actionMetric) + 1
        MetricActionSheet(
            metric = actionMetric,
            position = position,
            count = state.metrics.size,
            onDismiss = { actionMetricId = null },
            onMove = { move ->
                actionMetricId = null
                scope.launch { viewModel.move(actionMetric.id, move) }
            },
            onEdit = {
                actionMetricId = null
                onEditMetric(actionMetric.id)
            },
            onArchive = {
                actionMetricId = null
                scope.launch {
                    viewModel.setArchived(actionMetric.id, true)
                    prompt = UndoPrompt(System.nanoTime(), "已归档「${actionMetric.name}」") {
                        scope.launch {
                            viewModel.setArchived(actionMetric.id, false)
                            prompt = UndoPrompt(System.nanoTime(), "已恢复「${actionMetric.name}」")
                        }
                    }
                }
            },
        )
    }
}

@Composable
internal fun MetricsContent(
    state: HomeUiState,
    prompt: UndoPrompt?,
    onOpenSettings: () -> Unit,
    onCreateMetric: () -> Unit,
    onOpenTemplates: () -> Unit,
    onOpenMetric: (String) -> Unit,
    onLongPressMetric: (Metric) -> Unit,
    onQuickRecord: (String) -> Unit,
    onUndo: () -> Unit,
    onDismissPrompt: () -> Unit,
) {
    UndoPromptEffect(prompt = prompt, onDismiss = onDismissPrompt)

    // Non-saveable: a fresh instance marks a fresh entry into this screen, so
    // sparklines replay their wipe after returning from a detail page while
    // staying stable across recompositions and grid scrolling.
    val entryKey = remember { Any() }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val columns = when {
            maxWidth >= 720.dp -> 4
            maxWidth >= 600.dp -> 3
            else -> 2
        }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 1024.dp)
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = Spacing.screen)
                    .padding(top = Spacing.xs),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    HomeTopBar(onOpenSettings = onOpenSettings)

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(
                            top = 10.dp,
                            bottom = if (state.recordableMetrics.isNotEmpty()) 80.dp else Spacing.sm,
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                    when {
                        state.isLoading -> item(span = { GridItemSpan(maxLineSpan) }) {
                            LoadingState()
                        }

                        state.snapshots.isEmpty() -> {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                EmptyHero(onOpenTemplates = onOpenTemplates, onCreateMetric = onCreateMetric)
                            }
                            item(span = { GridItemSpan(maxLineSpan) }) { PrivacyCard() }
                            item(span = { GridItemSpan(maxLineSpan) }) { VersionFooter() }
                        }

                        else -> {
                            val featured = state.featuredSnapshot
                            val others = state.otherSnapshots
                            if (featured != null) {
                                item(span = { GridItemSpan(maxLineSpan) }, key = "featured-header") {
                                    SectionTitle(
                                        title = "最近记录",
                                        trailing = featured.latest?.let {
                                            DateFormats.stamp(it.date, it.measuredAt, state.today)
                                        } ?: "还没有记录",
                                        modifier = Modifier.animateItem(
                                            fadeInSpec = tween(Motion.standard),
                                            placementSpec = tween(Motion.standard),
                                            fadeOutSpec = tween(Motion.quick),
                                        ),
                                    )
                                }
                                item(span = { GridItemSpan(maxLineSpan) }, key = "hero") {
                                    HeroMetricCard(
                                        snapshot = featured,
                                        today = state.today,
                                        entryKey = entryKey,
                                        onClick = { onOpenMetric(featured.metric.id) },
                                        onLongClick = { onLongPressMetric(featured.metric) },
                                        modifier = Modifier.animateItem(
                                            fadeInSpec = tween(Motion.standard),
                                            placementSpec = tween(Motion.standard),
                                            fadeOutSpec = tween(Motion.quick),
                                        ),
                                    )
                                }
                            }
                            item(span = { GridItemSpan(maxLineSpan) }, key = "others-header") {
                                OthersHeader(
                                    count = others.size,
                                    onAdd = onOpenTemplates,
                                    modifier = Modifier.animateItem(
                                        fadeInSpec = tween(Motion.standard),
                                        placementSpec = tween(Motion.standard),
                                        fadeOutSpec = tween(Motion.quick),
                                    ),
                                )
                            }
                            items(others, key = { it.metric.id }) { snapshot ->
                                OtherMetricCard(
                                    snapshot = snapshot,
                                    today = state.today,
                                    entryKey = entryKey,
                                    onClick = { onOpenMetric(snapshot.metric.id) },
                                    onLongClick = { onLongPressMetric(snapshot.metric) },
                                    onQuickRecord = { onQuickRecord(snapshot.metric.id) },
                                    modifier = Modifier.animateItem(
                                        fadeInSpec = tween(Motion.standard),
                                        placementSpec = tween(Motion.standard),
                                        fadeOutSpec = tween(Motion.quick),
                                    ),
                                )
                            }
                        }
                    }
                }
            }

            BottomBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                prompt = prompt,
                hasMetrics = state.recordableMetrics.isNotEmpty(),
                onUndo = onUndo,
                onDismissPrompt = onDismissPrompt,
                onQuickRecord = {
                    val target = state.recordableMetrics.firstOrNull { it.id == state.lastMetricId }
                        ?: state.recordableMetrics.firstOrNull()
                    if (target != null) onQuickRecord(target.id)
                },
            )
        }
    }
}
}

@Composable
private fun HomeTopBar(onOpenSettings: () -> Unit) {
    val colors = BodyMeasureTheme.colors
    val showSlogan = LocalDensity.current.fontScale < 1.5f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "身语",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.6).sp,
                ),
                color = colors.ink,
                maxLines = 1,
                modifier = Modifier.semantics { heading() },
            )
            if (showSlogan) {
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "听见身体 · 看见变化",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.4.sp,
                    ),
                    color = colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.width(Spacing.sm))
        GlassIconButton(
            imageVector = AppIcons.Tune,
            contentDescription = "设置",
            onClick = onOpenSettings,
        )
    }
}

private val HeaderInset = 11.dp

@Composable
private fun SectionTitle(title: String, modifier: Modifier = Modifier, trailing: String? = null) {
    val colors = BodyMeasureTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = HeaderInset, end = HeaderInset, top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp,
            ),
            color = colors.inkSecondary,
        )
        Spacer(Modifier.weight(1f))
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelLarge,
                color = colors.muted,
            )
        }
    }
}

@Composable
private fun OthersHeader(count: Int, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BodyMeasureTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = HeaderInset, end = HeaderInset, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "全部指标",
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp,
            ),
            color = colors.inkSecondary,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "$count 项",
            style = MaterialTheme.typography.labelLarge,
            color = colors.muted,
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .border(1.dp, colors.line, CircleShape)
                .clickable(onClickLabel = "添加指标", onClick = onAdd)
                .semantics { contentDescription = "添加指标" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AppIcons.Plus,
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.size(13.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroMetricCard(
    snapshot: MetricSnapshot,
    today: LocalDate,
    entryKey: Any?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BodyMeasureTheme.colors
    val metric = snapshot.metric
    val accent = metricAccent(metric)
    val latest = snapshot.latest
    val shape = RoundedCornerShape(24.dp)
    val valueText = latest?.let { NumberFormats.format(it.value, metric.precision) } ?: "—"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.glassSurface)
            .accentGlow(accent, alpha = if (colors.isDark) 0.22f else 0.16f)
            .border(
                width = 1.dp,
                color = accent.copy(alpha = if (colors.isDark) 0.26f else 0.18f),
                shape = shape,
            )
            .combinedClickable(
                onClickLabel = "查看${metric.name}详情",
                onLongClickLabel = "更多操作",
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = Spacing.md, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AccentDot(accent = accent, size = 7.dp)
            Spacer(Modifier.width(7.dp))
            Text(
                text = metric.name,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = colors.inkSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            HeroTag(snapshot = snapshot, today = today, accent = accent)
        }

        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.Bottom) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Bottom,
            ) {
                AnimatedMetricValue(
                    text = valueText,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 38.sp,
                        lineHeight = 38.sp,
                        letterSpacing = (-1.7).sp,
                    ),
                    color = if (latest != null) colors.ink else colors.muted.copy(alpha = 0.45f),
                )
                if (latest != null && metric.unit.isNotBlank()) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = metric.unit,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }
            HeroDelta(snapshot = snapshot, accent = accent)
        }

        Spacer(Modifier.height(8.dp))

        if (snapshot.sparkline.size >= 2) {
            MetricSparkline(
                values = snapshot.sparkline,
                color = accent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp),
                entryKey = entryKey,
            )
        } else {
            Spacer(Modifier.height(30.dp))
        }

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = heroFootnote(snapshot = snapshot, today = today),
                style = MaterialTheme.typography.labelMedium,
                color = colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "查看趋势",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.inkSecondary,
            )
            Spacer(Modifier.width(2.dp))
            Icon(
                imageVector = AppIcons.ChevronRight,
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.size(11.dp),
            )
        }
    }
}

@Composable
private fun HeroTag(snapshot: MetricSnapshot, today: LocalDate, accent: Color) {
    val colors = BodyMeasureTheme.colors
    val latest = snapshot.latest
    val label = when {
        latest == null -> "待记录"
        snapshot.recordedToday -> "今天"
        else -> DateFormats.relativeDay(latest.date, today)
    }
    val textColor = lerp(accent, colors.ink, 0.32f)
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(accent.copy(alpha = if (colors.isDark) 0.16f else 0.09f))
            .border(1.dp, accent.copy(alpha = 0.24f), CircleShape)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = textColor,
            maxLines = 1,
        )
    }
}

@Composable
private fun HeroDelta(snapshot: MetricSnapshot, accent: Color) {
    val colors = BodyMeasureTheme.colors
    val metric = snapshot.metric
    snapshot.latest ?: return
    val delta = snapshot.delta
    val signed = delta?.let { NumberFormats.formatSigned(it.value, metric.precision) }
    val text = when {
        delta == null -> if (metric.kind == MetricKind.DERIVED) "第一条结果" else "第一条记录"
        metric.unit.isBlank() -> signed.orEmpty()
        else -> "$signed ${metric.unit}"
    }
    Row(
        modifier = Modifier.padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (signed != null) {
            DeltaArrow(signed = signed, accent = accent)
        }
        AnimatedMetricValue(
            text = text,
            style = if (delta == null) {
                MaterialTheme.typography.labelMedium
            } else {
                MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
            },
            color = if (delta == null) colors.muted else colors.inkSecondary,
        )
    }
}

@Composable
private fun heroFootnote(snapshot: MetricSnapshot, today: LocalDate): String {
    val metric = snapshot.metric
    val latest = snapshot.latest
    val delta = snapshot.delta
    val derived = metric.kind == MetricKind.DERIVED
    return when {
        latest == null -> if (derived) "关联指标有数据后，这里会自动更新" else "记下第一个数字，这里会出现轨迹"
        delta == null -> if (derived) "第一条结果" else "第一条记录"
        else -> "较${if (derived) "前次" else "上次"} ${DateFormats.displayDate(delta.fromDate, today)}"
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OtherMetricCard(
    snapshot: MetricSnapshot,
    today: LocalDate,
    entryKey: Any?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onQuickRecord: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BodyMeasureTheme.colors
    val metric = snapshot.metric
    val accent = metricAccent(metric)
    val latest = snapshot.latest
    val shape = RoundedCornerShape(22.dp)
    val showDate = LocalDensity.current.fontScale < 1.5f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(shape)
            .background(colors.glassSurface)
            .accentGlow(accent, alpha = if (colors.isDark) 0.14f else 0.09f)
            .border(1.dp, colors.line, shape)
            .combinedClickable(
                onClickLabel = "查看${metric.name}详情",
                onLongClickLabel = "更多操作",
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 13.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.heightIn(min = 26.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccentDot(accent = accent, size = 6.dp)
            Spacer(Modifier.width(6.dp))
            Text(
                text = metric.name,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = colors.inkSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (metric.kind == MetricKind.RAW) {
                MiniAddButton(name = metric.name, onClick = onQuickRecord)
            } else {
                Text(
                    text = "派生",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.mist,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = latest?.let { NumberFormats.format(it.value, metric.precision) } ?: "—",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 24.sp,
                    lineHeight = 25.sp,
                    letterSpacing = (-0.9).sp,
                ),
                color = if (latest != null) colors.ink else colors.muted.copy(alpha = 0.45f),
                maxLines = 1,
            )
            if (latest != null && metric.unit.isNotBlank()) {
                Spacer(Modifier.width(3.dp))
                Text(
                    text = metric.unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            SnapshotDelta(snapshot = snapshot, accent = accent)
            if (showDate) {
                Spacer(Modifier.weight(1f))
                Text(
                    text = latest?.let { DateFormats.displayDate(it.date, today) } ?: "—",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.muted,
                    maxLines = 1,
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp),
        ) {
            if (snapshot.sparkline.size >= 2) {
                MetricSparkline(
                    values = snapshot.sparkline,
                    color = accent,
                    modifier = Modifier.fillMaxSize(),
                    entryKey = entryKey,
                )
            }
        }
    }
}

@Composable
private fun SnapshotDelta(snapshot: MetricSnapshot, accent: Color) {
    val colors = BodyMeasureTheme.colors
    val metric = snapshot.metric
    val delta = snapshot.delta
    when {
        snapshot.latest == null -> Text(
            text = if (metric.kind == MetricKind.DERIVED) "等待数据" else "待记录",
            style = MaterialTheme.typography.labelMedium,
            color = colors.muted.copy(alpha = 0.75f),
            maxLines = 1,
        )

        delta == null -> Text(
            text = "首条",
            style = MaterialTheme.typography.labelMedium,
            color = colors.muted,
            maxLines = 1,
        )

        else -> {
            val signed = NumberFormats.formatSigned(delta.value, metric.precision)
            Row(verticalAlignment = Alignment.CenterVertically) {
                DeltaArrow(signed = signed, accent = accent, size = 10.dp)
                Text(
                    text = if (metric.unit.isBlank()) signed else "$signed ${metric.unit}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = colors.inkSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun DeltaArrow(signed: String, accent: Color, size: androidx.compose.ui.unit.Dp = 12.dp) {
    val colors = BodyMeasureTheme.colors
    val tint = lerp(accent, colors.ink, 0.2f)
    when {
        signed.startsWith("+") -> Icon(
            imageVector = AppIcons.ArrowUp,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(size)
                .padding(end = 2.dp),
        )

        signed.startsWith(NumberFormats.MINUS) -> Icon(
            imageVector = AppIcons.ArrowDown,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(size)
                .padding(end = 2.dp),
        )
    }
}

@Composable
private fun AccentDot(accent: Color, size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(accent),
    )
}

@Composable
private fun MiniAddButton(name: String, onClick: () -> Unit) {
    val colors = BodyMeasureTheme.colors
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .border(1.dp, colors.line, CircleShape)
            .clickable(onClickLabel = "记录$name", onClick = onClick)
            .semantics { contentDescription = "记录$name" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = AppIcons.Plus,
            contentDescription = null,
            tint = colors.muted,
            modifier = Modifier.size(11.dp),
        )
    }
}

@Composable
private fun BottomBar(
    modifier: Modifier = Modifier,
    prompt: UndoPrompt?,
    hasMetrics: Boolean,
    onUndo: () -> Unit,
    onDismissPrompt: () -> Unit,
    onQuickRecord: () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .padding(bottom = Spacing.xs),
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
            if (hasMetrics) {
                SoftActionButton(
                    text = "记下此刻",
                    onClick = onQuickRecord,
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    val colors = BodyMeasureTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BrandWaveMark(size = 64.dp)
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = "正在整理身体数据",
            style = MaterialTheme.typography.bodySmall,
            color = colors.muted.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun EmptyHero(onOpenTemplates: () -> Unit, onCreateMetric: () -> Unit) {
    val colors = BodyMeasureTheme.colors
    SoftGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.xl),
    ) {
        BrandWaveMark(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            size = 64.dp,
        )
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = "先听听身体的声音",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = "先从体重或腰围开始，之后随时添加更多指标。",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.lg))
        SoftActionButton(
            text = "从模板开始",
            onClick = onOpenTemplates,
            icon = AppIcons.Plus,
        )
        Spacer(Modifier.height(Spacing.xxs))
        TextButton(onClick = onCreateMetric, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "自定义指标",
                style = MaterialTheme.typography.labelLarge,
                color = colors.clay,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PrivacyCard() {
    val colors = BodyMeasureTheme.colors
    SoftGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(Spacing.lg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.clay.copy(alpha = if (colors.isDark) 0.16f else 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = AppIcons.Shield,
                    contentDescription = null,
                    tint = colors.clay,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(Spacing.sm))
            Column {
                Text(
                    text = "数据只留在这台设备",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.ink,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "没有账号，也不会上传任何内容。",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                )
            }
        }
        Spacer(Modifier.height(Spacing.md))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            SoftPill(text = "无网络权限", dotColor = colors.sage)
            SoftPill(text = "无云备份", dotColor = colors.sage)
            SoftPill(text = "无广告", dotColor = colors.sage)
        }
    }
}

@Composable
private fun VersionFooter() {
    Text(
        text = "身语 · v${BuildConfig.VERSION_NAME}",
        style = MaterialTheme.typography.labelMedium,
        color = BodyMeasureTheme.colors.muted.copy(alpha = 0.65f),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.xs),
    )
}

@Preview(name = "首页 · 浅色", widthDp = 360, heightDp = 900, showBackground = true)
@Composable
private fun MetricsScreenLightPreview() {
    BodyMeasureTheme(themeMode = ThemeMode.LIGHT) {
        SoftGradientBackground {
            MetricsContent(
                state = previewState(),
                prompt = null,
                onOpenSettings = {},
                onCreateMetric = {},
                onOpenTemplates = {},
                onOpenMetric = {},
                onLongPressMetric = {},
                onQuickRecord = {},
                onUndo = {},
                onDismissPrompt = {},
            )
        }
    }
}

@Preview(name = "首页 · 空态深色", widthDp = 360, heightDp = 900, showBackground = true)
@Composable
private fun MetricsScreenEmptyDarkPreview() {
    BodyMeasureTheme(themeMode = ThemeMode.DARK) {
        SoftGradientBackground {
            MetricsContent(
                state = HomeUiState(
                    today = LocalDate.of(2026, 10, 29),
                    isLoading = false,
                ),
                prompt = null,
                onOpenSettings = {},
                onCreateMetric = {},
                onOpenTemplates = {},
                onOpenMetric = {},
                onLongPressMetric = {},
                onQuickRecord = {},
                onUndo = {},
                onDismissPrompt = {},
            )
        }
    }
}

private fun previewState(): HomeUiState {
    val today = LocalDate.of(2026, 10, 29)
    val weight = previewMetric("weight", "体重", "kg", "weight", 0xFF3F7F72)
    val waist = previewMetric("waist", "腰围", "cm", "waist", 0xFFC96F4A)
    val chest = previewMetric("chest", "胸围", "cm", "chest", 0xFFB98A3E)
    val measurements = listOf(
        previewMeasurement(weight, today, 71.2),
        previewMeasurement(weight, today.minusDays(2), 71.6),
        previewMeasurement(weight, today.minusDays(6), 71.9),
        previewMeasurement(weight, today.minusDays(12), 72.4),
        previewMeasurement(weight, today.minusDays(20), 72.1),
        previewMeasurement(waist, today, 78.5),
        previewMeasurement(waist, today.minusDays(7), 79.1),
        previewMeasurement(waist, today.minusDays(18), 79.8),
        previewMeasurement(chest, today.minusDays(4), 96.0),
        previewMeasurement(chest, today.minusDays(16), 95.4),
    )
    val metrics = listOf(waist, weight, chest)
    return HomeUiState(
        today = today,
        snapshots = com.liangji.bodymeasure.domain.summary.MetricSummaries.build(metrics, measurements, today),
        measurementsByMetric = measurements.groupBy { it.metricId },
        lastMetricId = weight.id,
        isLoading = false,
    )
}

private fun previewMetric(id: String, name: String, unit: String, icon: String, color: Long) = Metric(
    id = id,
    name = name,
    kind = MetricKind.RAW,
    unit = unit,
    unitDimension = if (unit == "kg") UnitDimension.MASS else UnitDimension.LENGTH,
    precision = 1,
    colorArgb = color,
    iconKey = icon,
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

private fun previewMeasurement(metric: Metric, date: LocalDate, value: Double) =
    com.liangji.bodymeasure.domain.model.Measurement(
        id = "${metric.id}-$date",
        metricId = metric.id,
        date = date,
        measuredAt = 0,
        value = value,
        note = null,
        source = com.liangji.bodymeasure.domain.model.MeasurementSource.MANUAL,
        createdAt = 0,
        updatedAt = 0,
        deletedAt = null,
        originDeviceId = "preview",
    )
