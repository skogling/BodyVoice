package com.liangji.bodymeasure.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun SoftGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    contentPadding: PaddingValues = PaddingValues(Spacing.lg),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = BodyMeasureTheme.colors
    val clickableModifier = if (onClick != null) {
        Modifier
            .clip(shape)
            .clickable(onClick = onClick)
    } else {
        Modifier
    }
    val highlight = Brush.horizontalGradient(
        listOf(
            Color.Transparent,
            Color.White.copy(alpha = if (colors.isDark) 0.12f else 0.7f),
            Color.Transparent,
        ),
    )
    Column(
        modifier = modifier
            .then(clickableModifier)
            .clip(shape)
            .background(colors.glassSurface)
            .border(1.dp, colors.line, shape)
            .drawWithContent {
                drawContent()
                drawLine(
                    brush = highlight,
                    start = Offset(18.dp.toPx(), 1.dp.toPx()),
                    end = Offset(size.width - 18.dp.toPx(), 1.dp.toPx()),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun GlassIconButton(
    imageVector: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BodyMeasureTheme.colors
    val interaction = remember { MutableInteractionSource() }
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(44.dp)
            .pressScale(interaction),
        interactionSource = interaction,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(colors.glassSurface)
                .border(1.dp, colors.line, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = colors.inkSecondary,
                modifier = Modifier.size(17.dp),
            )
        }
    }
}

/** Warm radial wash used on the metric hero and grid cards. */
fun Modifier.accentGlow(accent: Color, alpha: Float): Modifier = drawBehind {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(accent.copy(alpha = alpha), Color.Transparent),
            center = Offset(size.width * 0.96f, 0f),
            radius = size.width * 1.2f,
        ),
    )
}

@Composable
fun GlassDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(BodyMeasureTheme.colors.line),
    )
}
