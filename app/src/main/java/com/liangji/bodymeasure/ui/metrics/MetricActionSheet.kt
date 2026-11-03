package com.liangji.bodymeasure.ui.metrics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.liangji.bodymeasure.core.designsystem.AppIcons
import com.liangji.bodymeasure.core.designsystem.BodyMeasureTheme
import com.liangji.bodymeasure.core.designsystem.GlassDivider
import com.liangji.bodymeasure.core.designsystem.Spacing
import com.liangji.bodymeasure.domain.model.Metric
import com.liangji.bodymeasure.domain.model.MetricMove
import com.liangji.bodymeasure.ui.components.MetricBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MetricActionSheet(
    metric: Metric,
    position: Int,
    count: Int,
    onDismiss: () -> Unit,
    onMove: (MetricMove) -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
) {
    val colors = BodyMeasureTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.sheet,
        scrimColor = Color.Black.copy(alpha = 0.36f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(width = 36.dp, height = 4.dp, color = colors.lineStrong)
        },
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.md),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.xxs, bottom = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MetricBadge(metric = metric, size = 40.dp)
                Spacer(Modifier.width(Spacing.sm))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = metric.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.ink,
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = buildString {
                            if (metric.unit.isNotBlank()) {
                                append(metric.unit)
                                append(" · ")
                            }
                            append("第 $position 项，共 $count 项")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted,
                    )
                }
            }

            ActionRow(
                icon = AppIcons.ArrowToTop,
                title = "置顶",
                enabled = position > 1,
                onClick = { onMove(MetricMove.TOP) },
            )
            ActionRow(
                icon = AppIcons.ArrowUp,
                title = "上移",
                enabled = position > 1,
                onClick = { onMove(MetricMove.UP) },
            )
            ActionRow(
                icon = AppIcons.ArrowDown,
                title = "下移",
                enabled = position < count,
                onClick = { onMove(MetricMove.DOWN) },
            )

            Spacer(Modifier.padding(vertical = Spacing.xxs))
            GlassDivider()
            Spacer(Modifier.padding(vertical = Spacing.xxs))

            ActionRow(
                icon = AppIcons.Edit,
                title = "编辑指标",
                onClick = onEdit,
            )
            ActionRow(
                icon = AppIcons.Archive,
                title = "归档",
                accent = colors.clay,
                hint = "可在设置中恢复",
                onClick = onArchive,
            )
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    accent: Color? = null,
    hint: String? = null,
) {
    val colors = BodyMeasureTheme.colors
    val content = when {
        !enabled -> colors.muted.copy(alpha = 0.4f)
        accent != null -> accent
        else -> colors.ink
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 13.dp, horizontal = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(19.dp),
        )
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = content,
            modifier = Modifier.weight(1f),
        )
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.labelSmall,
                color = colors.muted,
            )
        }
    }
}
