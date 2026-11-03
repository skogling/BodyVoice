package com.liangji.bodymeasure.ui.metrics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.liangji.bodymeasure.core.designsystem.AppIcons
import com.liangji.bodymeasure.core.designsystem.BodyMeasureTheme
import com.liangji.bodymeasure.core.designsystem.GlassIconButton
import com.liangji.bodymeasure.core.designsystem.ScreenColumn
import com.liangji.bodymeasure.core.designsystem.SoftGlassSurface
import com.liangji.bodymeasure.core.designsystem.SoftGradientBackground
import com.liangji.bodymeasure.core.designsystem.Spacing
import com.liangji.bodymeasure.domain.model.Metric
import com.liangji.bodymeasure.domain.model.MetricKind
import com.liangji.bodymeasure.domain.model.MetricPalette
import com.liangji.bodymeasure.domain.model.PairingPolicy
import com.liangji.bodymeasure.domain.model.ThemeMode
import com.liangji.bodymeasure.domain.model.UnitDimension
import com.liangji.bodymeasure.ui.components.MetricBadge
import com.liangji.bodymeasure.ui.components.precisionHint

@Composable
fun ArchivedMetricsScreen(
    onBack: () -> Unit,
    onOpenMetric: (String) -> Unit,
    viewModel: ArchivedMetricsViewModel = hiltViewModel(),
) {
    val metrics by viewModel.metrics.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<Metric?>(null) }

    ArchivedMetricsContent(
        metrics = metrics,
        onBack = onBack,
        onOpenMetric = onOpenMetric,
        onRestore = viewModel::restore,
        onDeleteRequest = { pendingDelete = it },
    )

    if (message != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissMessage,
            containerColor = BodyMeasureTheme.colors.sheet,
            shape = RoundedCornerShape(24.dp),
            titleContentColor = BodyMeasureTheme.colors.ink,
            textContentColor = BodyMeasureTheme.colors.inkSecondary,
            title = {
                Text(
                    text = "暂时不能删除",
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Text(
                    text = message.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissMessage) {
                    Text("知道了", color = BodyMeasureTheme.colors.clay)
                }
            },
        )
    }

    pendingDelete?.let { metric ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = BodyMeasureTheme.colors.sheet,
            shape = RoundedCornerShape(24.dp),
            titleContentColor = BodyMeasureTheme.colors.ink,
            textContentColor = BodyMeasureTheme.colors.inkSecondary,
            title = {
                Text(
                    text = "删除「${metric.name}」？",
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Text(
                    text = "指标会从所有列表中消失，相关记录也不再显示。此操作不可撤销。",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        viewModel.delete(metric.id)
                    },
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("取消", color = BodyMeasureTheme.colors.muted)
                }
            },
        )
    }
}

@Composable
internal fun ArchivedMetricsContent(
    metrics: List<Metric>,
    onBack: () -> Unit,
    onOpenMetric: (String) -> Unit,
    onRestore: (String) -> Unit,
    onDeleteRequest: (Metric) -> Unit,
) {
    val colors = BodyMeasureTheme.colors
    ScreenColumn(
        modifier = Modifier.padding(horizontal = Spacing.screen),
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
                text = "已归档指标",
                style = MaterialTheme.typography.headlineLarge,
                color = colors.ink,
            )
        }

        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = "归档的指标不会出现在首页，记录仍然完整保留。",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.muted,
        )

        if (metrics.isEmpty()) {
            EmptyArchive(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(top = Spacing.md, bottom = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(metrics, key = { it.id }) { metric ->
                    ArchivedMetricCard(
                        metric = metric,
                        onOpen = { onOpenMetric(metric.id) },
                        onRestore = { onRestore(metric.id) },
                        onDelete = { onDeleteRequest(metric) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ArchivedMetricCard(
    metric: Metric,
    onOpen: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = BodyMeasureTheme.colors
    SoftGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        onClick = onOpen,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MetricBadge(metric = metric, size = 40.dp)
            Spacer(Modifier.width(Spacing.sm))
            Column(Modifier.weight(1f)) {
                Text(
                    text = metric.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    text = buildString {
                        append(if (metric.unit.isBlank()) "不设单位" else metric.unit)
                        append(" · 精确到 ")
                        append(precisionHint(metric.precision))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(
                onClick = onDelete,
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.heightIn(min = 40.dp),
            ) {
                Text(
                    text = "删除",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.muted,
                )
            }
            TextButton(
                onClick = onRestore,
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.heightIn(min = 40.dp),
            ) {
                Text(
                    text = "恢复到首页",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.clay,
                )
            }
        }
    }
}

@Composable
private fun EmptyArchive(modifier: Modifier = Modifier) {
    val colors = BodyMeasureTheme.colors
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(colors.ink.copy(alpha = if (colors.isDark) 0.06f else 0.04f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = AppIcons.Archive,
                    contentDescription = null,
                    tint = colors.muted,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = "还没有归档的指标",
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink,
            )
            Spacer(Modifier.height(Spacing.xxs))
            Text(
                text = "在首页长按指标卡片，选择「归档」，\n就能把它收在这里。",
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = Spacing.xl),
            )
        }
    }
}

@Preview(name = "已归档 · 浅色", widthDp = 360, heightDp = 900, showBackground = true)
@Composable
private fun ArchivedLightPreview() {
    BodyMeasureTheme(themeMode = ThemeMode.LIGHT) {
        SoftGradientBackground {
            ArchivedMetricsContent(
                metrics = listOf(
                    previewArchivedMetric("体重", "kg", MetricPalette.CLAY, "weight"),
                    previewArchivedMetric("胸围", "cm", MetricPalette.TEAL, "chest"),
                ),
                onBack = {},
                onOpenMetric = {},
                onRestore = {},
                onDeleteRequest = {},
            )
        }
    }
}

private fun previewArchivedMetric(
    name: String,
    unit: String,
    colorArgb: Long,
    iconKey: String,
): Metric = Metric(
    id = "archived-$name",
    name = name,
    kind = MetricKind.RAW,
    unit = unit,
    unitDimension = if (unit == "kg") UnitDimension.MASS else UnitDimension.LENGTH,
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
    isArchived = true,
    createdAt = 0,
    updatedAt = 0,
    deletedAt = null,
    originDeviceId = "preview",
)

@Preview(name = "已归档 · 空态深色", widthDp = 360, heightDp = 900, showBackground = true)
@Composable
private fun ArchivedEmptyDarkPreview() {
    BodyMeasureTheme(themeMode = ThemeMode.DARK) {
        SoftGradientBackground {
            ArchivedMetricsContent(
                metrics = emptyList(),
                onBack = {},
                onOpenMetric = {},
                onRestore = {},
                onDeleteRequest = {},
            )
        }
    }
}
