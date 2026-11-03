package com.liangji.bodymeasure.ui.metrics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liangji.bodymeasure.core.common.DateFormats
import com.liangji.bodymeasure.core.common.NumberFormats
import com.liangji.bodymeasure.core.designsystem.BodyMeasureTheme
import com.liangji.bodymeasure.core.designsystem.SoftActionButton
import com.liangji.bodymeasure.core.designsystem.Spacing
import com.liangji.bodymeasure.domain.model.Measurement
import com.liangji.bodymeasure.domain.model.MeasurementChange
import com.liangji.bodymeasure.domain.model.MeasurementWriteResult
import com.liangji.bodymeasure.domain.model.Metric
import com.liangji.bodymeasure.ui.components.precisionHint
import java.time.LocalDate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MeasurementEditSheet(
    measurement: Measurement,
    metric: Metric,
    today: LocalDate,
    onDismiss: () -> Unit,
    onSave: suspend (value: Double, note: String?) -> MeasurementWriteResult,
    onSaved: (MeasurementChange) -> Unit,
    onDelete: () -> Unit,
) {
    val colors = BodyMeasureTheme.colors
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var input by rememberSaveable {
        mutableStateOf(NumberFormats.format(measurement.value, metric.precision))
    }
    var note by rememberSaveable { mutableStateOf(measurement.note.orEmpty()) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val parsed = NumberFormats.parse(input)
    val trimmedNote = note.trim().takeIf { it.isNotEmpty() }
    val changed = parsed != null && (parsed != measurement.value || trimmedNote != measurement.note)
    val canSave = changed && !saving

    fun submit() {
        val value = parsed ?: return
        if (!canSave) return
        saving = true
        error = null
        scope.launch {
            when (val result = onSave(value, trimmedNote)) {
                is MeasurementWriteResult.Saved -> {
                    saving = false
                    onSaved(result.change)
                }

                is MeasurementWriteResult.Conflict -> {
                    saving = false
                    error = "这一天已经有一条记录"
                }

                MeasurementWriteResult.Invalid -> {
                    saving = false
                    error = "这个数值无法保存"
                }

                MeasurementWriteResult.NotFound -> {
                    saving = false
                    error = "记录不存在或已被删除"
                }
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
                        text = "编辑记录",
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.ink,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${DateFormats.header(measurement.date)} · ${metric.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted,
                    )
                }
                TextButton(
                    onClick = onDismiss,
                    enabled = !saving,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = Spacing.sm,
                    ),
                ) {
                    Text(
                        text = "取消",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.muted,
                    )
                }
            }

            Spacer(Modifier.height(Spacing.md))

            ValueField(
                input = input,
                onInputChange = {
                    input = it.take(10)
                    error = null
                },
                unit = metric.unit,
                precision = metric.precision,
                onSubmit = { submit() },
            )

            Spacer(Modifier.height(Spacing.md))
            Text(
                text = "记录感受",
                style = MaterialTheme.typography.labelMedium,
                color = colors.muted,
            )
            Spacer(Modifier.height(Spacing.xs))
            NoteField(
                note = note,
                onNoteChange = { note = it.take(120) },
            )

            if (error != null) {
                Text(
                    text = error!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = Spacing.xs, start = Spacing.xxs),
                )
            }

            Spacer(Modifier.height(18.dp))

            SoftActionButton(
                text = if (saving) "保存中…" else "保存修改",
                onClick = { submit() },
                enabled = canSave,
            )

            TextButton(
                onClick = onDelete,
                enabled = !saving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "删除这条记录",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun ValueField(
    input: String,
    onInputChange: (String) -> Unit,
    unit: String,
    precision: Int,
    onSubmit: () -> Unit,
) {
    val colors = BodyMeasureTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.ink.copy(alpha = if (colors.isDark) 0.06f else 0.035f))
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Box(Modifier.weight(1f)) {
                val valueStyle = MaterialTheme.typography.displayMedium.copy(
                    fontSize = 40.sp,
                    lineHeight = 44.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.ink,
                )
                if (input.isEmpty()) {
                    Text(
                        text = "0.0",
                        style = valueStyle,
                        color = colors.muted.copy(alpha = 0.35f),
                    )
                }
                BasicTextField(
                    value = input,
                    onValueChange = onInputChange,
                    textStyle = valueStyle,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                    singleLine = true,
                    cursorBrush = SolidColor(colors.clay),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (unit.isNotBlank()) {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.muted,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "精确到 ${precisionHint(precision)}",
            style = MaterialTheme.typography.labelSmall,
            color = colors.muted,
        )
    }
}

@Composable
private fun NoteField(
    note: String,
    onNoteChange: (String) -> Unit,
) {
    val colors = BodyMeasureTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.ink.copy(alpha = 0.04f))
            .border(1.dp, colors.line, RoundedCornerShape(14.dp))
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
            onValueChange = onNoteChange,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.ink),
            cursorBrush = SolidColor(colors.clay),
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
