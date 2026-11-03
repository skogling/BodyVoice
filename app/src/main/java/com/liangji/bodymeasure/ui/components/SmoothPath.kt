package com.liangji.bodymeasure.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

// ECharts' line smoothing: Catmull-Rom control points clamped into the box
// between each pair of neighbouring nodes. The clamp keeps every cubic span
// inside its endpoints' bounds, so the curve is round without overshooting or
// running backwards, no matter how uneven the spacing is.
internal fun smoothPath(
    points: List<Offset>,
    smooth: Float = 0.4f,
): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points.first().x, points.first().y)
    if (points.size == 1) return path
    if (points.size == 2) {
        path.lineTo(points.last().x, points.last().y)
        return path
    }

    var previousControlX = points.first().x
    var previousControlY = points.first().y
    for (index in 1 until points.size) {
        val previous = points[index - 1]
        val current = points[index]
        if (index == points.lastIndex) {
            path.cubicTo(
                previousControlX,
                previousControlY,
                current.x,
                current.y,
                current.x,
                current.y,
            )
            break
        }

        val next = points[index + 1]
        val spanX = next.x - previous.x
        val spanY = next.y - previous.y
        val previousLength = hypot(current.x - previous.x, current.y - previous.y)
        val nextLength = hypot(next.x - current.x, next.y - current.y)
        val ratio = nextLength / (previousLength + nextLength)

        var nextControlX = clamp(
            current.x + spanX * smooth * ratio,
            current.x,
            next.x,
        )
        var nextControlY = clamp(
            current.y + spanY * smooth * ratio,
            current.y,
            next.y,
        )
        val incomingControlX = clamp(
            current.x - (nextControlX - current.x) * previousLength / nextLength,
            previous.x,
            current.x,
        )
        val incomingControlY = clamp(
            current.y - (nextControlY - current.y) * previousLength / nextLength,
            previous.y,
            current.y,
        )
        nextControlX = current.x + (current.x - incomingControlX) * nextLength / previousLength
        nextControlY = current.y + (current.y - incomingControlY) * nextLength / previousLength

        path.cubicTo(
            previousControlX,
            previousControlY,
            incomingControlX,
            incomingControlY,
            current.x,
            current.y,
        )
        previousControlX = nextControlX
        previousControlY = nextControlY
    }
    return path
}

private fun clamp(value: Float, first: Float, second: Float): Float =
    value.coerceIn(min(first, second), max(first, second))
