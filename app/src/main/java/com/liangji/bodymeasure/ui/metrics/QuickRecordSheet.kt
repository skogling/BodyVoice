package com.liangji.bodymeasure.ui.metrics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.liangji.bodymeasure.core.common.DateFormats
import com.liangji.bodymeasure.core.common.NumberFormats
import com.liangji.bodymeasure.core.designsystem.AppIcons
import com.liangji.bodymeasure.core.designsystem.BodyMeasureTheme
import com.liangji.bodymeasure.core.designsystem.Motion
import com.liangji.bodymeasure.core.designsystem.SoftActionButton
import com.liangji.bodymeasure.core.designsystem.Spacing
import com.liangji.bodymeasure.domain.model.Measurement
import com.liangji.bodymeasure.domain.model.MeasurementChange
import com.liangji.bodymeasure.domain.model.MeasurementInput
import com.liangji.bodymeasure.domain.model.MeasurementWriteResult
import com.liangji.bodymeasure.domain.model.Metric
import com.liangji.bodymeasure.domain.summary.AnomalyDetector
import com.liangji.bodymeasure.ui.components.metricAccent
import com.liangji.bodymeasure.ui.components.precisionHint
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.abs
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QuickRecordSheet(
    metrics: List<Metric>,
    today: LocalDate,
    initialMetricId: String?,
    measurementsByMetric: Map<String, List<Measurement>>,
    onDismiss: () -> Unit,
    onSave: suspend (MeasurementInput, Boolean) -> MeasurementWriteResult,
    onSaved: (MeasurementChange) -> Unit,
) {
    val colors = BodyMeasureTheme.colors
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val orderedMetrics = remember(metrics, measurementsByMetric) {
        metrics.sortedWith(
            compareByDescending<Metric> { metric ->
                measurementsByMetric[metric.id]?.maxOfOrNull { it.date }
            }.thenBy { it.sortOrder },
        )
    }

    var selectedId by rememberSaveable { mutableStateOf(initialMetricId) }
    val metric = orderedMetrics.firstOrNull { it.id == selectedId } ?: orderedMetrics.firstOrNull()
    var input by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var noteOpen by rememberSaveable { mutableStateOf(false) }
    var dateEpoch by rememberSaveable { mutableStateOf(today.toEpochDay()) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var pendingConflict by remember { mutableStateOf<Measurement?>(null) }
    var warning by remember { mutableStateOf<AnomalyDetector.Warning?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val date = LocalDate.ofEpochDay(dateEpoch)
    val parsed = NumberFormats.parse(input)
    val accent = metric?.let { metricAccent(it) } ?: colors.clay
    val existing = remember(metric, measurementsByMetric, date) {
        metric?.let { current ->
            measurementsByMetric[current.id].orEmpty().firstOrNull { it.date == date }
        }
    }
    val chipListState = rememberLazyListState()

    LaunchedEffect(Unit) {
        val index = orderedMetrics.indexOfFirst { it.id == metric?.id }
        if (index > 0) chipListState.scrollToItem(index)
    }

    fun pressKey(key: String) {
        if (saving) return
        val precision = metric?.precision ?: 2
        when (key) {
            "back" -> input = input.dropLast(1)
            "." -> {
                if (precision <= 0 || input.contains('.')) return
                input = (input.ifEmpty { "0" }) + "."
            }

            else -> {
                if (input.count { it.isDigit() } >= 6) return
                val decimals = input.substringAfter('.', "").length
                if (input.contains('.') && decimals >= precision) return
                input = if (input == "0") key else input + key
            }
        }
        error = null
    }

    fun submit(replaceExisting: Boolean, skipAnomaly: Boolean) {
        val current = metric ?: return
        val value = NumberFormats.parse(input)
        if (value == null) {
            error = "请输入有效数字"
            return
        }
        if (!skipAnomaly) {
            val history = measurementsByMetric[current.id].orEmpty().filter { it.date != date }
            val detected = AnomalyDetector.check(value, history)
            if (detected != null) {
                warning = detected
                return
            }
        }
        error = null
        saving = true
        scope.launch {
            val result = onSave(
                MeasurementInput(
                    metricId = current.id,
                    date = date,
                    value = value,
                    note = note.trim().takeIf { it.isNotEmpty() },
                ),
                replaceExisting,
            )
            saving = false
            when (result) {
                is MeasurementWriteResult.Saved -> onSaved(result.change)
                is MeasurementWriteResult.Conflict -> pendingConflict = result.existing
                MeasurementWriteResult.Invalid -> error = "这个数值无法保存"
                MeasurementWriteResult.NotFound -> error = "指标不存在或已删除"
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = { if (!saving) onDismiss() },
        sheetState = sheetState,
        containerColor = colors.sheet,
        scrimColor = Color.Black.copy(alpha = 0.36f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(width = 34.dp, height = 4.dp, color = colors.lineStrong)
        },
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 18.dp)
                .padding(bottom = 18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "记录数据",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.025).em,
                    ),
                    color = colors.ink,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { if (!saving) onDismiss() },
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(colors.glassSurface)
                            .border(1.dp, colors.line, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = AppIcons.Close,
                            contentDescription = "关闭",
                            tint = colors.inkSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            LazyRow(
                state = chipListState,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 2.dp),
                modifier = Modifier.selectableGroup(),
            ) {
                items(orderedMetrics, key = { it.id }) { item ->
                    MetricChip(
                        metric = item,
                        selected = item.id == metric?.id,
                        enabled = !saving,
                        onClick = {
                            if (item.id != metric?.id) {
                                selectedId = item.id
                                input = ""
                                note = ""
                                noteOpen = false
                                error = null
                            }
                        },
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val valueStyle = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 52.sp,
                        lineHeight = 52.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.055).em,
                        fontFeatureSettings = "tnum",
                    )
                    Text(
                        text = input.ifEmpty { "0" },
                        style = valueStyle,
                        color = if (input.isEmpty()) colors.muted.copy(alpha = 0.35f) else colors.ink,
                        maxLines = 1,
                        softWrap = false,
                    )
                    Spacer(Modifier.width(2.dp))
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(32.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(accent),
                    )
                    if (metric != null && metric.unit.isNotBlank()) {
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = metric.unit,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = colors.muted,
                        )
                    }
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    text = when {
                        existing != null -> replaceHint(existing, date, today)
                        metric != null -> precisionLine(metric)
                        else -> ""
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = colors.muted,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MetaPill(
                    icon = AppIcons.Calendar,
                    text = if (date == today) {
                        "今天 · ${DateFormats.shortDate(date)}"
                    } else {
                        DateFormats.header(date)
                    },
                    enabled = !saving,
                    accent = null,
                    onClick = { if (!saving) showDatePicker = true },
                )
                MetaPill(
                    icon = AppIcons.Edit,
                    text = if (note.isNotBlank() && !noteOpen) "已写备注" else "记录感受",
                    enabled = !saving,
                    accent = if (note.isNotBlank() && !noteOpen) accent else null,
                    onClick = { if (!saving) noteOpen = !noteOpen },
                )
            }

            AnimatedVisibility(
                visible = noteOpen,
                enter = expandVertically(tween(Motion.standard)) + fadeIn(tween(Motion.standard)),
                exit = shrinkVertically(tween(Motion.quick)) + fadeOut(tween(Motion.quick)),
            ) {
                Column {
                    Spacer(Modifier.height(Spacing.xs))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.ink.copy(alpha = 0.03f))
                            .border(1.dp, colors.line, RoundedCornerShape(12.dp))
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    ) {
                        if (note.isEmpty()) {
                            Text(
                                text = "例如：早晨空腹",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.muted.copy(alpha = 0.6f),
                            )
                        }
                        BasicTextField(
                            value = note,
                            onValueChange = { note = it.take(120) },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.ink),
                            cursorBrush = SolidColor(accent),
                            minLines = 2,
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = error != null,
                enter = fadeIn(tween(Motion.quick)),
                exit = fadeOut(tween(Motion.quick)),
            ) {
                if (error != null) {
                    Text(
                        text = error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.xs),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf(".", "0", "back"),
                ).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        row.forEach { key ->
                            KeypadKey(
                                key = key,
                                accent = accent,
                                enabled = !saving,
                                onPress = { pressKey(key) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            SoftActionButton(
                text = when {
                    saving -> "保存中…"
                    existing != null -> if (date == today) "替换今日记录" else "替换当日记录"
                    else -> "保存记录"
                },
                onClick = { submit(false, false) },
                enabled = parsed != null && !saving,
            )
        }
    }

    if (showDatePicker) {
        val selectableDates = remember(today) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis <= today.toEpochDay() * 86_400_000L

                override fun isSelectableYear(year: Int): Boolean = year <= today.year
            }
        }
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.toEpochDay() * 86_400_000L,
            selectableDates = selectableDates,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            dateEpoch = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                                .toEpochDay()
                        }
                        showDatePicker = false
                    },
                ) {
                    Text("确定", color = colors.clay)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消", color = colors.muted)
                }
            },
            shape = RoundedCornerShape(28.dp),
            colors = DatePickerDefaults.colors(containerColor = colors.sheet),
        ) {
            DatePicker(
                state = pickerState,
                colors = DatePickerDefaults.colors(containerColor = colors.sheet),
            )
        }
    }

    warning?.let { detected ->
        AlertDialog(
            onDismissRequest = { warning = null },
            containerColor = colors.sheet,
            shape = RoundedCornerShape(24.dp),
            titleContentColor = colors.ink,
            textContentColor = colors.inkSecondary,
            title = {
                Text("确认这次记录？", style = MaterialTheme.typography.headlineSmall)
            },
            text = {
                Text(
                    text = anomalyMessage(detected),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        warning = null
                        submit(replaceExisting = false, skipAnomaly = true)
                    },
                ) {
                    Text("仍然保存", color = colors.clay)
                }
            },
            dismissButton = {
                TextButton(onClick = { warning = null }) {
                    Text("再检查一下", color = colors.muted)
                }
            },
        )
    }

    pendingConflict?.let { existingRecord ->
        val current = metric
        AlertDialog(
            onDismissRequest = { pendingConflict = null },
            containerColor = colors.sheet,
            shape = RoundedCornerShape(24.dp),
            titleContentColor = colors.ink,
            textContentColor = colors.inkSecondary,
            title = {
                Text("这一天已有记录", style = MaterialTheme.typography.headlineSmall)
            },
            text = {
                Text(
                    text = buildString {
                        append(DateFormats.displayDate(existingRecord.date, today))
                        append(" 已记录 ")
                        if (current != null) {
                            append(valueWithUnit(existingRecord.value, current))
                        } else {
                            append(existingRecord.value)
                        }
                        append("。要用这次的值替换吗？")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingConflict = null
                        submit(replaceExisting = true, skipAnomaly = true)
                    },
                ) {
                    Text("替换", color = colors.clay)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingConflict = null }) {
                    Text("取消", color = colors.muted)
                }
            },
        )
    }
}

@Composable
private fun MetricChip(
    metric: Metric,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = BodyMeasureTheme.colors
    val accent = metricAccent(metric)
    val background by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = if (colors.isDark) 0.16f else 0.1f) else Color.Transparent,
        animationSpec = tween(Motion.quick),
        label = "metricChipBackground",
    )
    val outline by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.28f) else colors.line,
        animationSpec = tween(Motion.quick),
        label = "metricChipOutline",
    )
    val content by animateColorAsState(
        targetValue = if (selected) lerp(accent, colors.ink, 0.32f) else colors.muted,
        animationSpec = tween(Motion.quick),
        label = "metricChipContent",
    )
    Box(
        modifier = Modifier
            .heightIn(min = 34.dp)
            .clip(CircleShape)
            .background(background)
            .border(1.dp, outline, CircleShape)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = 13.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = metric.name,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = content,
            maxLines = 1,
        )
    }
}

@Composable
private fun MetaPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    enabled: Boolean,
    accent: Color?,
    onClick: () -> Unit,
) {
    val colors = BodyMeasureTheme.colors
    val border = accent?.copy(alpha = 0.3f) ?: colors.line
    val content = accent?.let { lerp(it, colors.ink, 0.32f) } ?: colors.inkSecondary
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .border(1.dp, border, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(13.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = content,
            maxLines = 1,
        )
    }
}

@Composable
private fun KeypadKey(
    key: String,
    accent: Color,
    enabled: Boolean,
    onPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BodyMeasureTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val background by animateColorAsState(
        targetValue = if (pressed && enabled) {
            accent.copy(alpha = if (colors.isDark) 0.14f else 0.08f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(Motion.quick),
        label = "keypadPressed",
    )
    val tag = when (key) {
        "." -> "QuickRecordKey-dot"
        "back" -> "QuickRecordKey-back"
        else -> "QuickRecordKey-$key"
    }
    val description = when (key) {
        "." -> "小数点"
        "back" -> "删除"
        else -> null
    }
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .testTag(tag)
            .semantics { if (description != null) contentDescription = description }
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onPress,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (key == "back") {
            Icon(
                imageVector = AppIcons.Backspace,
                contentDescription = null,
                tint = colors.inkSecondary,
                modifier = Modifier.size(19.dp),
            )
        } else {
            Text(
                text = key,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    fontFeatureSettings = "tnum",
                ),
                color = colors.ink,
            )
        }
    }
}

private fun replaceHint(existing: Measurement, date: LocalDate, today: LocalDate): String {
    val whenText = if (date == today) {
        DateFormats.timeOfDay(existing.measuredAt)?.let { "今天 $it" } ?: "今天"
    } else {
        DateFormats.shortDate(date)
    }
    return "$whenText 已记录 · 保存将替换"
}

private fun precisionLine(metric: Metric): String {
    val hint = precisionHint(metric.precision)
    return if (metric.unit.isBlank()) "精确到$hint" else "精确到 $hint ${metric.unit}"
}

private fun valueWithUnit(value: Double, metric: Metric): String {
    val formatted = NumberFormats.format(value, metric.precision)
    return if (metric.unit.isBlank()) formatted else "$formatted ${metric.unit}"
}

private fun anomalyMessage(warning: AnomalyDetector.Warning): String {
    val multiple = warning.multipleOfMedian
    return when {
        warning.isFarFromMedian && multiple != null && multiple > 1.0 ->
            "这次是近期中位数的 ${NumberFormats.format(multiple, 1)} 倍，确认没有输错吗？"

        warning.isFarFromMedian && multiple != null ->
            "这次只有近期中位数的 ${NumberFormats.format(multiple * 100.0, 0)}%，确认没有输错吗？"

        warning.changeRatio != null ->
            "比上次${if (warning.changeRatio > 0) "高" else "低"} " +
                NumberFormats.formatPercent(abs(warning.changeRatio), 0).removePrefix("+") +
                "，确认保存吗？"

        else -> "这次记录和之前差别较大，确认保存吗？"
    }
}
