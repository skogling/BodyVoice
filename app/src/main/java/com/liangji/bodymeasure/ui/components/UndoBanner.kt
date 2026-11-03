package com.liangji.bodymeasure.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.liangji.bodymeasure.core.designsystem.BodyMeasureTheme
import com.liangji.bodymeasure.core.designsystem.Spacing
import kotlinx.coroutines.delay

internal data class UndoPrompt(
    val id: Long,
    val message: String,
    val onUndo: (() -> Unit)? = null,
)

@Composable
internal fun UndoPromptEffect(prompt: UndoPrompt?, onDismiss: () -> Unit) {
    LaunchedEffect(prompt?.id) {
        val current = prompt ?: return@LaunchedEffect
        delay(if (current.onUndo != null) 5_000 else 2_600)
        onDismiss()
    }
}

@Composable
internal fun UndoBanner(
    prompt: UndoPrompt,
    onUndo: () -> Unit,
) {
    val colors = BodyMeasureTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.glassSurface)
            .border(1.dp, colors.line, shape)
            .padding(start = Spacing.md, end = Spacing.xxs)
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (prompt.onUndo != null) colors.sage else colors.muted),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = prompt.message,
            style = MaterialTheme.typography.titleSmall,
            color = colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (prompt.onUndo != null) {
            TextButton(onClick = onUndo) {
                Text("撤销", color = colors.clay, style = MaterialTheme.typography.labelLarge)
            }
        } else {
            Spacer(Modifier.width(Spacing.sm))
        }
    }
}
