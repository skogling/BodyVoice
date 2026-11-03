package com.liangji.bodymeasure.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import com.liangji.bodymeasure.core.designsystem.Motion
import kotlin.math.abs

@Composable
fun MetricSparkline(
    values: List<Double>,
    color: Color,
    modifier: Modifier = Modifier,
    entryKey: Any? = null,
) {
    if (values.isEmpty()) return
    val series = remember(values) { condensed(values) }
    // rememberSaveable so scrolling the grid away and back does not replay the
    // wipe; a changed series (new record) or a new screen entry does.
    var revealed by rememberSaveable(values, entryKey) { mutableStateOf(false) }
    val reveal = remember { Animatable(if (revealed) 1f else 0f) }
    LaunchedEffect(values, entryKey) {
        if (revealed) {
            reveal.snapTo(1f)
        } else {
            reveal.snapTo(0f)
            reveal.animateTo(1f, tween(Motion.chartDraw, easing = FastOutSlowInEasing))
            revealed = true
        }
    }
    Canvas(
        modifier = modifier.drawWithContent {
            clipRect(right = size.width * reveal.value) {
                this@drawWithContent.drawContent()
            }
        },
    ) {
        val pad = 3.dp.toPx()
        val width = size.width - pad * 2
        val height = size.height - pad * 2
        if (width <= 0f || height <= 0f) return@Canvas

        if (series.size == 1) {
            drawCircle(
                color = color.copy(alpha = 0.85f),
                radius = 2.5.dp.toPx(),
                center = Offset(pad + width, pad + height * yFraction(series.last(), series)),
            )
            return@Canvas
        }

        val points = series.mapIndexed { index, value ->
            Offset(
                x = pad + width * index / (series.size - 1),
                y = pad + height * yFraction(value, series),
            )
        }
        val line = smoothPath(points)
        val fill = Path().apply {
            addPath(line)
            lineTo(points.last().x, size.height)
            lineTo(points.first().x, size.height)
            close()
        }
        drawPath(
            path = fill,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.16f), Color.Transparent),
                startY = 0f,
                endY = size.height,
            ),
        )
        drawPath(
            path = line,
            color = color,
            style = Stroke(
                width = 1.8.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

private const val MAX_SPARKLINE_POINTS = 14

// Bucket averages keep the thumbnail's silhouette while dropping day-to-day
// jitter; the newest reading stays exact so the line ends on the latest value.
private fun condensed(values: List<Double>): List<Double> {
    if (values.size <= MAX_SPARKLINE_POINTS) return values
    val last = values.lastIndex
    return List(MAX_SPARKLINE_POINTS) { index ->
        when (index) {
            0 -> values.first()
            MAX_SPARKLINE_POINTS - 1 -> values.last()
            else -> {
                val start = index * last / (MAX_SPARKLINE_POINTS - 1)
                val end = ((index + 1) * last / (MAX_SPARKLINE_POINTS - 1))
                    .coerceAtLeast(start + 1)
                values.subList(start, end).average()
            }
        }
    }
}

private fun yFraction(value: Double, values: List<Double>): Float {
    val min = values.min()
    val max = values.max()
    val span = max - min
    return if (abs(span) < 1e-12) 0.5f else (1.0 - (value - min) / span).toFloat()
}
