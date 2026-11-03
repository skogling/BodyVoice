package com.liangji.bodymeasure.ui.metrics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liangji.bodymeasure.core.designsystem.AppIcons
import com.liangji.bodymeasure.core.designsystem.BodyMeasureTheme
import com.liangji.bodymeasure.core.designsystem.Motion
import com.liangji.bodymeasure.core.designsystem.SoftActionButton
import com.liangji.bodymeasure.core.designsystem.Spacing
import com.liangji.bodymeasure.core.designsystem.metricColor
import com.liangji.bodymeasure.domain.model.MetricPreset
import com.liangji.bodymeasure.domain.model.MetricPresets
import com.liangji.bodymeasure.ui.components.metricGlyph
import com.liangji.bodymeasure.ui.components.precisionHint
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TemplateSheet(
    existingNames: Set<String>,
    onDismiss: () -> Unit,
    onCreate: suspend (List<MetricPreset>) -> TemplateResult,
    onCreated: (TemplateResult) -> Unit,
    onCreateCustom: () -> Unit,
) {
    val colors = BodyMeasureTheme.colors
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val presets = MetricPresets.all
    val normalizedExisting = remember(existingNames) {
        existingNames.map { it.trim().lowercase() }.toSet()
    }

    var selectedKeys by remember {
        mutableStateOf(
            listOf("weight", "waist").filter { key ->
                presets.firstOrNull { it.key == key }?.name?.trim()?.lowercase() !in normalizedExisting
            },
        )
    }
    var creating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun toggle(key: String) {
        if (creating) return
        selectedKeys = if (key in selectedKeys) selectedKeys - key else selectedKeys + key
        error = null
    }

    ModalBottomSheet(
        onDismissRequest = { if (!creating) onDismiss() },
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
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.md),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.xxs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "从模板开始",
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.ink,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "选择常记录的部位，稍后还能改单位与精度。",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted,
                    )
                }
                TextButton(
                    onClick = onDismiss,
                    enabled = !creating,
                    contentPadding = PaddingValues(horizontal = Spacing.sm),
                ) {
                    Text(
                        text = "取消",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.muted,
                    )
                }
            }

            Spacer(Modifier.height(Spacing.md))

            presets.chunked(2).forEach { rowPresets ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    rowPresets.forEach { preset ->
                        TemplateTile(
                            preset = preset,
                            selected = preset.key in selectedKeys,
                            exists = preset.name.trim().lowercase() in normalizedExisting,
                            enabled = !creating,
                            onClick = { toggle(preset.key) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowPresets.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(Spacing.xs))
            }

            Spacer(Modifier.height(Spacing.xxs))

            if (error != null) {
                Text(
                    text = error!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = Spacing.xs, start = Spacing.xxs),
                )
            }

            SoftActionButton(
                text = when {
                    creating -> "创建中…"
                    selectedKeys.isEmpty() -> "请选择指标"
                    else -> "创建 ${selectedKeys.size} 个指标"
                },
                onClick = {
                    val chosen = presets.filter { it.key in selectedKeys }
                    if (chosen.isEmpty() || creating) return@SoftActionButton
                    creating = true
                    error = null
                    scope.launch {
                        val result = onCreate(chosen)
                        creating = false
                        if (result.created == 0) {
                            error = "这些指标都已经在列表里了"
                        } else {
                            onCreated(result)
                        }
                    }
                },
                enabled = selectedKeys.isNotEmpty() && !creating,
            )

            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "已存在的同名指标会自动跳过",
                style = MaterialTheme.typography.labelSmall,
                color = colors.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(Spacing.xxs))
            TextButton(
                onClick = onCreateCustom,
                enabled = !creating,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "没有合适的？自定义指标",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.clay,
                )
            }
        }
    }
}

@Composable
private fun TemplateTile(
    preset: MetricPreset,
    selected: Boolean,
    exists: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BodyMeasureTheme.colors
    val accent = colors.metricColor(preset.colorArgb)
    val shape = RoundedCornerShape(16.dp)
    val background by animateColorAsState(
        targetValue = when {
            selected -> accent.copy(alpha = if (colors.isDark) 0.16f else 0.1f)
            else -> colors.ink.copy(alpha = if (colors.isDark) 0.04f else 0.025f)
        },
        animationSpec = tween(Motion.quick),
        label = "templateTileBackground",
    )
    val outline by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.55f) else colors.line,
        animationSpec = tween(Motion.quick),
        label = "templateTileOutline",
    )

    Row(
        modifier = modifier
            .heightIn(min = 64.dp)
            .clip(shape)
            .background(background)
            .border(1.dp, outline, shape)
            .then(if (enabled && !exists) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = Spacing.sm, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (exists) {
                        colors.ink.copy(alpha = 0.05f)
                    } else {
                        accent.copy(alpha = if (colors.isDark) 0.18f else 0.1f)
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = metricGlyph(preset.iconKey, preset.name),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                color = if (exists) colors.muted else accent,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = preset.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (exists) colors.muted else colors.ink,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = if (exists) "已存在" else "${preset.unit} · ${precisionHint(preset.precision)}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
            )
        }
        if (selected && !exists) {
            Icon(
                imageVector = AppIcons.Check,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(15.dp),
            )
        }
    }
}

internal fun TemplateResult.bannerText(): String = when {
    created == 0 -> "没有新增指标"
    skipped.isEmpty() -> "已创建 $created 个指标"
    else -> "已创建 $created 个，跳过 ${skipped.size} 个同名指标"
}
