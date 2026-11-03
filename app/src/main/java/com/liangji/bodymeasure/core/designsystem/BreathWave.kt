package com.liangji.bodymeasure.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The brand mark: a breath wave that draws itself in and settles on a listening dot. */
@Composable
fun BreathWave(
    modifier: Modifier = Modifier,
    color: Color = BodyMeasureTheme.colors.mist,
    strokeWidth: Dp = 2.5.dp,
    animate: Boolean = true,
) {
    val progress = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (animate) {
            progress.animateTo(1f, tween(Motion.chartDraw, easing = FastOutSlowInEasing))
        }
    }
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        val wave = Path().apply {
            moveTo(w * 0.03f, h * 0.60f)
            cubicTo(w * 0.19f, h * 0.60f, w * 0.23f, h * 0.14f, w * 0.39f, h * 0.14f)
            cubicTo(w * 0.55f, h * 0.14f, w * 0.59f, h * 0.86f, w * 0.75f, h * 0.86f)
            cubicTo(w * 0.87f, h * 0.86f, w * 0.90f, h * 0.60f, w * 0.95f, h * 0.60f)
        }
        clipRect(right = w * progress.value) {
            drawPath(
                path = wave,
                color = color,
                style = Stroke(
                    width = strokeWidth.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
            drawCircle(
                color = color,
                radius = strokeWidth.toPx() * 1.3f,
                center = Offset(w * 0.95f, h * 0.60f),
            )
        }
    }
}

@Composable
fun BrandWaveMark(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
) {
    val colors = BodyMeasureTheme.colors
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.mist.copy(alpha = if (colors.isDark) 0.18f else 0.10f)),
        contentAlignment = Alignment.Center,
    ) {
        BreathWave(
            modifier = Modifier.size(width = size * 0.58f, height = size * 0.34f),
        )
    }
}
