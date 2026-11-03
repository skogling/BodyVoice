package com.liangji.bodymeasure.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush

@Composable
fun SoftGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = BodyMeasureTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(colors.glowPrimary, colors.glowPrimary.copy(alpha = 0f)),
                        center = Offset(size.width * 1.02f, -size.height * 0.04f),
                        radius = size.width * 1.15f,
                    ),
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(colors.glowSecondary, colors.glowSecondary.copy(alpha = 0f)),
                        center = Offset(-size.width * 0.12f, size.height * 1.02f),
                        radius = size.width,
                    ),
                )
            },
        content = content,
    )
}
