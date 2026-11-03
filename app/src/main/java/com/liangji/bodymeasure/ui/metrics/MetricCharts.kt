package com.liangji.bodymeasure.ui.metrics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.liangji.bodymeasure.core.common.DateFormats
import com.liangji.bodymeasure.core.common.NumberFormats
import com.liangji.bodymeasure.core.designsystem.AppIcons
import com.liangji.bodymeasure.core.designsystem.BodyMeasureTheme
import com.liangji.bodymeasure.core.designsystem.Motion
import com.liangji.bodymeasure.core.designsystem.SoftGlassSurface
import com.liangji.bodymeasure.core.designsystem.Spacing
import com.liangji.bodymeasure.domain.model.Measurement
import com.liangji.bodymeasure.domain.model.Metric
import com.liangji.bodymeasure.domain.summary.CalendarMonth
import com.liangji.bodymeasure.domain.summary.ChartRange
import com.liangji.bodymeasure.domain.summary.ChartStatistics
import com.liangji.bodymeasure.domain.summary.ComparisonSpan
import com.liangji.bodymeasure.domain.summary.DistributionChart
import com.liangji.bodymeasure.domain.summary.PeriodComparison
import com.liangji.bodymeasure.domain.summary.PeriodStats
import com.liangji.bodymeasure.domain.summary.TrendChart
import com.liangji.bodymeasure.ui.components.metricAccent
import com.liangji.bodymeasure.ui.components.smoothPath
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private enum class ChartView(val label: String) {
    TREND("趋势"),
    DISTRIBUTION("分布"),
    CALENDAR("日历"),
    COMPARISON("对比"),
}

@Composable
internal fun MetricChartSection(
    metric: Metric,
    measurements: List<Measurement>,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    val colors = BodyMeasureTheme.colors
    val accent = metricAccent(metric)
    val daily = remember(measurements) { ChartStatistics.dailyValues(measurements) }
    var viewIndex by rememberSaveable { mutableIntStateOf(0) }
    var rangeIndex by rememberSaveable { mutableIntStateOf(ChartRange.ONE_YEAR.ordinal) }
    var spanIndex by rememberSaveable { mutableIntStateOf(1) }
    val view = ChartView.entries[viewIndex]
    val range = ChartRange.entries[rangeIndex]
    val span = ComparisonSpan.entries[spanIndex]

    val currentMonth = YearMonth.from(today)
    val earliestMonth = daily.firstOrNull()?.date?.let { YearMonth.from(it) }
    val latestMonth = daily.lastOrNull()?.date?.let { YearMonth.from(it) }
        ?.coerceAtMost(currentMonth)
    val anchorOffset = remember(daily, today) {
        if (latestMonth == null) 0 else ChronoUnit.MONTHS
            .between(currentMonth, latestMonth)
            .toInt()
    }
    var monthOffset by rememberSaveable(daily.firstOrNull()?.date) {
        mutableIntStateOf(anchorOffset)
    }
    val displayMonth = remember(currentMonth, monthOffset) {
        currentMonth.plusMonths(monthOffset.toLong())
    }

    val trend = remember(daily, range, today) { ChartStatistics.trend(daily, range, today) }
    val ranged = remember(daily, range, today) {
        ChartStatistics.withinRange(daily, range, today)
    }
    val distribution = remember(ranged) { ChartStatistics.distribution(ranged) }
    val calendar = remember(daily, displayMonth, today) {
        ChartStatistics.calendar(daily, displayMonth, today)
    }
    val comparison = remember(daily, span, today) {
        ChartStatistics.comparison(daily, today, span)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        ViewTabs(
            selected = view,
            onSelect = { viewIndex = it.ordinal },
        )
        Spacer(Modifier.height(14.dp))
        SegmentedRow(
            options = ChartRange.entries.toList(),
            selected = range,
            label = { it.label },
            onSelect = { rangeIndex = ChartRange.entries.indexOf(it) },
        )
        Spacer(Modifier.height(12.dp))
        SoftGlassSurface(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = 13.dp,
                bottom = 10.dp,
            ),
        ) {
            ChartHead(
                title = view.label,
                badge = when (view) {
                    ChartView.TREND -> "7 日均线"
                    ChartView.DISTRIBUTION -> null
                    ChartView.CALENDAR -> null
                    ChartView.COMPARISON -> null
                },
            )
            Spacer(Modifier.height(2.dp))
            when (view) {
                ChartView.TREND -> TrendView(
                    chart = trend,
                    metric = metric,
                    accent = accent,
                    today = today,
                )

                ChartView.DISTRIBUTION -> DistributionView(
                    chart = distribution,
                    metric = metric,
                    accent = accent,
                )

                ChartView.CALENDAR -> CalendarView(
                    month = calendar,
                    metric = metric,
                    accent = accent,
                    today = today,
                    canPrevious = earliestMonth != null && displayMonth > earliestMonth,
                    canNext = displayMonth < currentMonth,
                    onPrevious = { monthOffset-- },
                    onNext = { monthOffset++ },
                )

                ChartView.COMPARISON -> ComparisonView(
                    comparison = comparison,
                    metric = metric,
                    accent = accent,
                    span = span,
                    onSpanChange = { spanIndex = ComparisonSpan.entries.indexOf(it) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        RangeStats(chart = trend, metric = metric)
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun ChartHead(title: String, badge: String?) {
    val colors = BodyMeasureTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = colors.inkSecondary,
        )
        Spacer(Modifier.weight(1f))
        if (badge != null) {
            Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall,
                color = colors.muted,
                maxLines = 1,
                modifier = Modifier
                    .clip(CircleShape)
                    .border(1.dp, colors.line, CircleShape)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}

@Composable
private fun <T> SegmentedRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    val colors = BodyMeasureTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(colors.glassSurface)
            .border(1.dp, colors.line, CircleShape)
            .padding(3.dp)
            .selectableGroup(),
    ) {
        options.forEach { option ->
            val active = option == selected
            val background by animateColorAsState(
                targetValue = if (active) colors.ink else Color.Transparent,
                animationSpec = tween(Motion.quick),
                label = "segmentBackground",
            )
            val content by animateColorAsState(
                targetValue = if (active) colors.background else colors.muted,
                animationSpec = tween(Motion.quick),
                label = "segmentContent",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(background)
                    .selectable(
                        selected = active,
                        role = Role.RadioButton,
                        onClick = { onSelect(option) },
                    )
                    .heightIn(min = 32.dp)
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label(option),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = content,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun ViewTabs(selected: ChartView, onSelect: (ChartView) -> Unit) {
    val colors = BodyMeasureTheme.colors
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.line),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            ChartView.entries.forEach { view ->
                val active = view == selected
                Column(
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .selectable(
                            selected = active,
                            role = Role.Tab,
                            onClick = { onSelect(view) },
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = view.label,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = if (active) colors.ink else colors.muted,
                        maxLines = 1,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (active) colors.ink else Color.Transparent),
                    )
                }
            }
        }
    }
}

@Composable
private fun RangeStats(chart: TrendChart, metric: Metric) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatCard(
            label = "最低",
            value = chart.minPoint?.value,
            metric = metric,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = "平均",
            value = chart.meanValue,
            metric = metric,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = "最高",
            value = chart.maxPoint?.value,
            metric = metric,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: Double?,
    metric: Metric,
    modifier: Modifier = Modifier,
) {
    val colors = BodyMeasureTheme.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colors.glassSurface)
            .border(1.dp, colors.line, RoundedCornerShape(16.dp))
            .padding(horizontal = 11.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.muted,
            maxLines = 1,
        )
        Spacer(Modifier.height(5.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value?.let {
                    NumberFormats.format(it, metric.precision)
                } ?: "—",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = if (value != null) colors.ink else colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (value != null && metric.unit.isNotBlank()) {
                Spacer(Modifier.width(2.dp))
                Text(
                    text = metric.unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.muted,
                    modifier = Modifier.padding(bottom = 1.dp),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun ChartEmpty(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = BodyMeasureTheme.colors.ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = BodyMeasureTheme.colors.muted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TrendView(
    chart: TrendChart,
    metric: Metric,
    accent: Color,
    today: LocalDate,
) {
    if (chart.isEmpty) {
        ChartEmpty(
            title = "这段时间还没有记录",
            subtitle = "换个时间范围看看，或先记一笔。",
        )
        return
    }
    TrendCanvas(chart = chart, metric = metric, accent = accent, today = today)
}

@Composable
private fun TrendCanvas(
    chart: TrendChart,
    metric: Metric,
    accent: Color,
    today: LocalDate,
) {
    val colors = BodyMeasureTheme.colors
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val axisStyle = MaterialTheme.typography.labelSmall.copy(
        color = colors.muted.copy(alpha = 0.8f),
    )
    val tooltipDateStyle = MaterialTheme.typography.labelSmall.copy(color = colors.muted)
    val tooltipValueStyle = MaterialTheme.typography.titleSmall.copy(
        color = colors.ink,
        fontWeight = FontWeight.SemiBold,
    )
    val height: Dp = (176f * density.fontScale.coerceIn(1f, 1.35f)).dp
    val touchLeft = 34.dp
    var selected by remember(chart) { mutableStateOf<Int?>(null) }
    var revealed by rememberSaveable(chart) { mutableStateOf(false) }
    val reveal = remember { Animatable(if (revealed) 1f else 0f) }
    LaunchedEffect(chart) {
        if (revealed) {
            reveal.snapTo(1f)
        } else {
            reveal.snapTo(0f)
            reveal.animateTo(1f, tween(Motion.chartDraw, easing = FastOutSlowInEasing))
            revealed = true
        }
    }
    val description = "趋势图，${chart.count} 天记录，最新 " +
        valueWithUnit(chart.latestValue ?: 0.0, metric) +
        "，平均 " + valueWithUnit(chart.meanValue ?: 0.0, metric)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .pointerInput(chart) {
                detectTapGestures { offset ->
                    selected = chart.nearestIndex(
                        x = offset.x,
                        left = touchLeft.toPx(),
                        right = size.width - 10.dp.toPx(),
                    )
                }
            }
            .pointerInput(chart) {
                detectDragGestures(
                    onDragStart = { offset ->
                        selected = chart.nearestIndex(
                            x = offset.x,
                            left = touchLeft.toPx(),
                            right = size.width - 10.dp.toPx(),
                        )
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        selected = chart.nearestIndex(
                            x = change.position.x,
                            left = touchLeft.toPx(),
                            right = size.width - 10.dp.toPx(),
                        )
                    },
                )
            }
            .semantics { contentDescription = description },
    ) {
        val values = chart.points.map { it.value } + chart.averagePoints.map { it.value }
        val rawMin = values.min()
        val rawMax = values.max()
        val span = rawMax - rawMin
        val padding = if (span < 1e-9) max(abs(rawMin) * 0.02, 0.5) else span * 0.14
        val low = rawMin - padding
        val high = rawMax + padding
        val range = (high - low).takeIf { it > 1e-9 } ?: 1.0

        val topGap = 18.dp.toPx()
        val axisGap = 6.dp.toPx()
        val yValues = listOf(high, (high + low) / 2, low)
        val yTexts = yValues.map { axisLabel(it) }
        val yLayouts = yTexts.map {
            measurer.measure(AnnotatedString(it), axisStyle)
        }
        val yLabelWidth = yLayouts.maxOf { it.size.width }
        val xLabelHeight = measurer.measure(AnnotatedString("10月30日"), axisStyle).size.height
        val plotLeft = 2.dp.toPx() + yLabelWidth + axisGap
        val plotRight = size.width - 10.dp.toPx()
        val plotTop = topGap
        val plotBottom = size.height - xLabelHeight - 8.dp.toPx()
        if (plotRight <= plotLeft || plotBottom <= plotTop) return@Canvas

        fun yOf(value: Double): Float =
            plotTop + ((high - value) / range).toFloat() * (plotBottom - plotTop)

        val firstDay = chart.points.first().date.toEpochDay().toDouble()
        val lastDay = chart.points.last().date.toEpochDay().toDouble()
        fun xOf(date: LocalDate): Float {
            if (chart.points.size == 1) return (plotLeft + plotRight) / 2f
            val fraction = ((date.toEpochDay() - firstDay) / (lastDay - firstDay)).toFloat()
            return plotLeft + fraction * (plotRight - plotLeft)
        }

        yLayouts.forEachIndexed { index, layout ->
            val y = plotTop + (plotBottom - plotTop) * index / 2f
            drawLine(
                color = colors.line,
                start = Offset(plotLeft, y),
                end = Offset(plotRight, y),
                strokeWidth = 1.dp.toPx(),
            )
            val duplicated = index == 1 && (yTexts[1] == yTexts[0] || yTexts[1] == yTexts[2])
            if (!duplicated) {
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(
                        x = 2.dp.toPx(),
                        y = y - layout.size.height / 2f,
                    ),
                )
            }
        }

        val points = chart.points.map { Offset(xOf(it.date), yOf(it.value)) }
        clipRect(right = size.width * reveal.value) {
            if (points.size > 1) {
                val nodeIndices = points.indices.toList()
                // The drawn nodes are the curve's control points: every dot sits on
                // the line, and each span stays inside its endpoints' box.
                val shapePoints = nodeIndices.map { points[it] }
                val line = smoothPath(shapePoints)
                val fill = Path().apply {
                    addPath(line)
                    lineTo(shapePoints.last().x, plotBottom)
                    lineTo(shapePoints.first().x, plotBottom)
                    close()
                }
                drawPath(
                    path = fill,
                    brush = Brush.verticalGradient(
                        colors = listOf(accent.copy(alpha = 0.20f), Color.Transparent),
                        startY = plotTop,
                        endY = plotBottom,
                    ),
                )
                if (chart.averagePoints.size > 1) {
                    val averagePoints = nodeIndices.mapNotNull { index ->
                        chart.averagePoints.getOrNull(index)?.let {
                            Offset(xOf(it.date), yOf(it.value))
                        }
                    }
                    if (averagePoints.size > 1) {
                        val average = smoothPath(averagePoints)
                        drawPath(
                            path = average,
                            color = accent.copy(alpha = 0.42f),
                            style = Stroke(
                                width = 1.2.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(5.dp.toPx(), 4.dp.toPx()),
                                ),
                            ),
                        )
                    }
                }
                drawPath(
                    path = line,
                    color = accent,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )

                val nodeRadius = 3.2.dp.toPx()
                val ring = 1.dp.toPx()
                nodeIndices.forEach { index ->
                    if (index == points.lastIndex) return@forEach
                    val point = points[index]
                    drawCircle(
                        color = colors.glassSurface,
                        radius = nodeRadius + ring,
                        center = point,
                    )
                    drawCircle(color = accent, radius = nodeRadius, center = point)
                }
            }

            val lastPoint = points.last()
            if (selected == null) {
                drawLine(
                    color = accent.copy(alpha = 0.25f),
                    start = lastPoint,
                    end = Offset(lastPoint.x, plotBottom),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(3.dp.toPx(), 4.dp.toPx()),
                    ),
                )
                drawCircle(
                    color = accent.copy(alpha = 0.12f),
                    radius = 9.dp.toPx(),
                    center = lastPoint,
                )
                drawCircle(
                    color = colors.glassSurface,
                    radius = 5.5.dp.toPx(),
                    center = lastPoint,
                )
                drawCircle(color = accent, radius = 3.6.dp.toPx(), center = lastPoint)
            }
        }

        drawXLabels(
            measurer = measurer,
            style = axisStyle,
            chart = chart,
            plotLeft = plotLeft,
            plotRight = plotRight,
            baseline = plotBottom,
            yOffset = 4.dp.toPx(),
        )

        selected?.let { index ->
            val point = chart.points.getOrNull(index) ?: return@let
            val x = xOf(point.date)
            val y = yOf(point.value)
            drawLine(
                color = accent.copy(alpha = 0.30f),
                start = Offset(x, plotTop),
                end = Offset(x, plotBottom),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(3.dp.toPx(), 4.dp.toPx()),
                ),
            )
            drawCircle(
                color = accent.copy(alpha = 0.12f),
                radius = 9.dp.toPx(),
                center = Offset(x, y),
            )
            drawCircle(
                color = colors.glassSurface,
                radius = 6.dp.toPx(),
                center = Offset(x, y),
            )
            drawCircle(
                color = accent,
                radius = 3.8.dp.toPx(),
                center = Offset(x, y),
            )
            val dateText = DateFormats.shortDate(point.date) +
                if (point.date == today) " · 今天" else ""
            val valueText = valueWithUnit(point.value, metric)
            val dateLayout = measurer.measure(AnnotatedString(dateText), tooltipDateStyle)
            val valueLayout = measurer.measure(AnnotatedString(valueText), tooltipValueStyle)
            val padH = 10.dp.toPx()
            val padV = 7.dp.toPx()
            val gap = 1.dp.toPx()
            val bubbleWidth = max(dateLayout.size.width, valueLayout.size.width) + padH * 2
            val bubbleHeight = dateLayout.size.height + valueLayout.size.height +
                padV * 2 + gap
            val bubbleX = (x - bubbleWidth / 2f).coerceIn(0f, size.width - bubbleWidth)
            val above = y - bubbleHeight - 8.dp.toPx()
            val bubbleY = if (above >= 0f) {
                above
            } else {
                (y + 8.dp.toPx()).coerceAtMost(size.height - bubbleHeight)
            }
            val corner = CornerRadius(12.dp.toPx())
            drawRoundRect(
                color = colors.sheet,
                topLeft = Offset(bubbleX, bubbleY),
                size = Size(bubbleWidth, bubbleHeight),
                cornerRadius = corner,
            )
            drawRoundRect(
                color = colors.line,
                topLeft = Offset(bubbleX, bubbleY),
                size = Size(bubbleWidth, bubbleHeight),
                cornerRadius = corner,
                style = Stroke(width = 1.dp.toPx()),
            )
            drawText(
                textLayoutResult = dateLayout,
                topLeft = Offset(bubbleX + padH, bubbleY + padV),
            )
            drawText(
                textLayoutResult = valueLayout,
                topLeft = Offset(
                    bubbleX + padH,
                    bubbleY + padV + dateLayout.size.height + gap,
                ),
            )
        }
    }
}

private fun axisLabel(value: Double): String = NumberFormats.format(value, 0)

private fun TrendChart.nearestIndex(
    x: Float,
    left: Float,
    right: Float,
): Int? {
    if (points.isEmpty()) return null
    if (points.size == 1) return 0
    val plotWidth = (right - left).coerceAtLeast(1f)
    val fraction = ((x - left) / plotWidth).coerceIn(0f, 1f)
    val first = points.first().date.toEpochDay()
    val last = points.last().date.toEpochDay()
    val target = first + (last - first) * fraction.toDouble()
    return points.indices.minByOrNull {
        abs(points[it].date.toEpochDay() - target)
    }
}

private fun DrawScope.drawXLabels(
    measurer: TextMeasurer,
    style: TextStyle,
    chart: TrendChart,
    plotLeft: Float,
    plotRight: Float,
    baseline: Float,
    yOffset: Float,
) {
    val points = chart.points
    if (points.isEmpty()) return
    if (points.size == 1) {
        val layout = measurer.measure(
            AnnotatedString(DateFormats.shortDate(points.first().date)),
            style,
        )
        drawText(
            textLayoutResult = layout,
            topLeft = Offset(
                x = (plotLeft + plotRight) / 2f - layout.size.width / 2f,
                y = baseline + yOffset,
            ),
        )
        return
    }
    val firstLayout = measurer.measure(
        AnnotatedString(DateFormats.shortDate(points.first().date)),
        style,
    )
    val lastLayout = measurer.measure(
        AnnotatedString(DateFormats.shortDate(points.last().date)),
        style,
    )
    drawText(firstLayout, topLeft = Offset(plotLeft, baseline + yOffset))
    drawText(
        lastLayout,
        topLeft = Offset(plotRight - lastLayout.size.width, baseline + yOffset),
    )
    val middleIndex = points.size / 2
    val middleLayout = measurer.measure(
        AnnotatedString(DateFormats.shortDate(points[middleIndex].date)),
        style,
    )
    val middleX = (plotLeft + plotRight) / 2f - middleLayout.size.width / 2f
    val gap = 12.dp.toPx()
    if (middleX > plotLeft + firstLayout.size.width + gap &&
        middleX + middleLayout.size.width < plotRight - lastLayout.size.width - gap
    ) {
        drawText(middleLayout, topLeft = Offset(middleX, baseline + yOffset))
    }
}

@Composable
private fun DistributionView(
    chart: DistributionChart?,
    metric: Metric,
    accent: Color,
) {
    val colors = BodyMeasureTheme.colors
    when {
        chart == null -> ChartEmpty(
            title = "这段时间还没有记录",
            subtitle = "换个时间范围看看，或先记一笔。",
        )

        chart.count < 2 -> ChartEmpty(
            title = "再多记一次，就能看出分布",
            subtitle = "记录越多，分布越说明问题。",
        )

        else -> {
            DistributionCanvas(chart = chart, metric = metric, accent = accent)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "中位数",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.muted,
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = valueWithUnit(chart.median, metric),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = colors.ink,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "共 ${chart.count} 次记录",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.muted.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun DistributionCanvas(
    chart: DistributionChart,
    metric: Metric,
    accent: Color,
) {
    val colors = BodyMeasureTheme.colors
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val axisStyle = MaterialTheme.typography.labelSmall.copy(
        color = colors.muted.copy(alpha = 0.8f),
    )
    val height: Dp = (128f * density.fontScale.coerceIn(1f, 1.35f)).dp
    val description = "分布图，${chart.count} 次记录，平均 " +
        valueWithUnit(chart.mean, metric) +
        "，最低 " + valueWithUnit(chart.minimum, metric) +
        "，最高 " + valueWithUnit(chart.maximum, metric)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .semantics { contentDescription = description },
    ) {
        val bins = chart.bins
        if (bins.isEmpty()) return@Canvas
        val xLabelHeight = measurer.measure(AnnotatedString("000"),
            axisStyle).size.height
        val plotLeft = 2.dp.toPx()
        val plotRight = size.width - 2.dp.toPx()
        val plotTop = 16.dp.toPx()
        val plotBottom = size.height - xLabelHeight - 8.dp.toPx()
        if (plotRight <= plotLeft || plotBottom <= plotTop) return@Canvas
        val plotWidth = plotRight - plotLeft
        val plotHeight = plotBottom - plotTop
        val gap = 6.dp.toPx()
        val slot = (plotWidth - gap * (bins.size - 1)) / bins.size
        if (slot <= 0f) return@Canvas
        val maxCount = max(1, bins.maxOf { it.count })

        drawLine(
            color = colors.line,
            start = Offset(plotLeft, plotBottom),
            end = Offset(plotRight, plotBottom),
            strokeWidth = 1.dp.toPx(),
        )

        bins.forEachIndexed { index, bin ->
            val left = plotLeft + index * (slot + gap)
            if (bin.count > 0) {
                val barHeight = max(2.dp.toPx(), plotHeight * bin.count / maxCount)
                val alpha = 0.3f + 0.5f * (bin.count.toFloat() / maxCount)
                drawRoundRect(
                    color = accent.copy(alpha = alpha),
                    topLeft = Offset(left, plotBottom - barHeight),
                    size = Size(slot, barHeight),
                    cornerRadius = CornerRadius(min(5.dp.toPx(), slot / 3f)),
                )
                val countLayout = measurer.measure(
                    AnnotatedString(bin.count.toString()),
                    axisStyle,
                )
                if (countLayout.size.width + 4.dp.toPx() <= slot) {
                    drawText(
                        textLayoutResult = countLayout,
                        topLeft = Offset(
                            x = left + slot / 2f - countLayout.size.width / 2f,
                            y = plotBottom - barHeight - countLayout.size.height - 2.dp.toPx(),
                        ),
                    )
                }
            } else {
                drawLine(
                    color = colors.lineStrong.copy(alpha = 0.5f),
                    start = Offset(left, plotBottom),
                    end = Offset(left + slot, plotBottom),
                    strokeWidth = 2.dp.toPx(),
                )
            }
        }

        val domainStart = bins.first().lower
        val domainEnd = bins.last().upper
        val domainSpan = (domainEnd - domainStart).takeIf { it > 1e-9 }
        if (domainSpan != null) {
            val meanX = plotLeft +
                ((chart.mean - domainStart) / domainSpan).toFloat() * plotWidth
            drawLine(
                color = colors.inkSecondary.copy(alpha = 0.32f),
                start = Offset(meanX, plotTop),
                end = Offset(meanX, plotBottom),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(4.dp.toPx(), 4.dp.toPx()),
                ),
            )
        }

        val labelY = plotBottom + 4.dp.toPx()
        if (bins.size == 1) {
            val layout = measurer.measure(
                AnnotatedString(axisLabel(bins.first().lower)),
                axisStyle,
            )
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    x = (plotLeft + plotRight) / 2f - layout.size.width / 2f,
                    y = labelY,
                ),
            )
        } else {
            val firstText = axisLabel(bins.first().lower)
            val lastText = axisLabel(bins.last().upper)
            val firstLayout = measurer.measure(AnnotatedString(firstText), axisStyle)
            val lastLayout = measurer.measure(AnnotatedString(lastText), axisStyle)
            drawText(firstLayout, topLeft = Offset(plotLeft, labelY))
            drawText(
                lastLayout,
                topLeft = Offset(plotRight - lastLayout.size.width, labelY),
            )
            val middle = bins.size / 2
            val middleText = axisLabel(bins[middle].lower)
            val middleLayout = measurer.measure(AnnotatedString(middleText), axisStyle)
            val boundaryX = plotLeft + middle * (slot + gap) - gap / 2f
            val middleX = boundaryX - middleLayout.size.width / 2f
            val minGap = 10.dp.toPx()
            if (middleText != firstText &&
                middleText != lastText &&
                middleX > plotLeft + firstLayout.size.width + minGap &&
                middleX + middleLayout.size.width <
                plotRight - lastLayout.size.width - minGap
            ) {
                drawText(middleLayout, topLeft = Offset(middleX, labelY))
            }
        }
    }
}

@Composable
private fun CalendarView(
    month: CalendarMonth,
    metric: Metric,
    accent: Color,
    today: LocalDate,
    canPrevious: Boolean,
    canNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val colors = BodyMeasureTheme.colors
    val fontScale = LocalDensity.current.fontScale
    val cells = month.weeks.flatten().filterNotNull()
    val defaultDate = cells.lastOrNull { it.value != null }?.date
    var pickedIso by rememberSaveable(month.month.year, month.month.monthValue) {
        mutableStateOf<String?>(null)
    }
    val picked = pickedIso?.let { LocalDate.parse(it) }
        ?.takeIf { YearMonth.from(it) == month.month }
    val selectedDate = picked ?: defaultDate
    val selectedValue = selectedDate?.let { date ->
        cells.firstOrNull { it.date == date }?.value
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonthButton(
            previous = true,
            enabled = canPrevious,
            onClick = onPrevious,
        )
        Text(
            text = DateFormats.monthTitle(month.month),
            style = MaterialTheme.typography.titleMedium,
            color = colors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        MonthButton(
            previous = false,
            enabled = canNext,
            onClick = onNext,
        )
    }
    Spacer(Modifier.height(3.dp))
    Text(
        text = if (month.count > 0) {
            "本月 ${month.count} 条 · 平均 " +
                valueWithUnit(month.meanValue ?: 0.0, metric)
        } else {
            "本月还没有记录"
        },
        style = MaterialTheme.typography.labelSmall,
        color = colors.muted,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(12.dp))
    Row(modifier = Modifier.fillMaxWidth()) {
        listOf("一", "二", "三", "四", "五", "六", "日").forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.muted.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
    Spacer(Modifier.height(4.dp))
    month.weeks.dropLastWhile { week -> week.all { it == null } }.forEach { week ->
        Row(modifier = Modifier.fillMaxWidth()) {
            week.forEach { cell ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .padding(2.dp),
                ) {
                    if (cell != null) {
                        CalendarDay(
                            cell = cell,
                            month = month,
                            metric = metric,
                            accent = accent,
                            today = today,
                            selected = cell.date == selectedDate,
                            showTodayDot = fontScale < 1.5f,
                            onClick = { pickedIso = cell.date.toString() },
                        )
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.ink.copy(alpha = if (colors.isDark) 0.06f else 0.035f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = selectedDate?.let { DateFormats.header(it) } ?: "点选一天查看记录",
            style = MaterialTheme.typography.bodySmall,
            color = colors.muted,
            modifier = Modifier.weight(1f),
        )
        if (selectedDate != null) {
            Text(
                text = if (selectedValue != null) {
                    valueWithUnit(selectedValue, metric)
                } else {
                    "没有记录"
                },
                style = MaterialTheme.typography.titleSmall,
                color = if (selectedValue != null) colors.ink else colors.muted,
            )
        }
    }
}

@Composable
private fun CalendarDay(
    cell: com.liangji.bodymeasure.domain.summary.CalendarCell,
    month: CalendarMonth,
    metric: Metric,
    accent: Color,
    today: LocalDate,
    selected: Boolean,
    showTodayDot: Boolean,
    onClick: () -> Unit,
) {
    val colors = BodyMeasureTheme.colors
    val shape = RoundedCornerShape(10.dp)
    val future = cell.date.isAfter(today)
    val value = cell.value
    val description = buildString {
        append(DateFormats.shortDate(cell.date))
        append("，")
        when {
            future -> append("还没到")
            value == null -> append("没有记录")
            else -> {
                append(NumberFormats.format(value, metric.precision))
                if (metric.unit.isNotBlank()) {
                    append(" ")
                    append(metric.unit)
                }
            }
        }
    }
    val intensity = if (value != null) {
        val minimum = month.minimum ?: value
        val maximum = month.maximum ?: value
        val span = maximum - minimum
        val fraction = if (span < 1e-9) 0.45f else ((value - minimum) / span).toFloat()
        (if (colors.isDark) 0.17f else 0.10f) + 0.28f * fraction
    } else {
        0f
    }
    val textColor = when {
        future -> colors.muted.copy(alpha = 0.4f)
        value != null -> colors.ink
        else -> colors.muted.copy(alpha = 0.8f)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .then(
                if (value != null) {
                    Modifier.background(accent.copy(alpha = intensity))
                } else {
                    Modifier
                },
            )
            .then(
                if (selected) Modifier.border(1.5.dp, accent, shape) else Modifier,
            )
            .semantics(mergeDescendants = true) {
                contentDescription = description
                if (selected) {
                    this.selected = true
                }
            }
            .clickable(
                onClickLabel = "查看当天记录",
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = cell.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
                maxLines = 1,
            )
            if (cell.date == today && showTodayDot) {
                Spacer(Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .clip(CircleShape)
                        .background(accent),
                )
            }
        }
    }
}

@Composable
private fun MonthButton(
    previous: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = BodyMeasureTheme.colors
    val label = if (previous) "上个月" else "下个月"
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .border(1.dp, colors.line, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AppIcons.ChevronRight,
                contentDescription = label,
                tint = if (enabled) colors.inkSecondary else colors.muted.copy(alpha = 0.3f),
                modifier = Modifier
                    .size(14.dp)
                    .rotate(if (previous) 180f else 0f),
            )
        }
    }
}

@Composable
private fun ComparisonView(
    comparison: PeriodComparison,
    metric: Metric,
    accent: Color,
    span: ComparisonSpan,
    onSpanChange: (ComparisonSpan) -> Unit,
) {
    val colors = BodyMeasureTheme.colors
    SegmentedRow(
        options = ComparisonSpan.entries.toList(),
        selected = span,
        label = { it.label },
        onSelect = onSpanChange,
    )
    Spacer(Modifier.height(12.dp))
    val current = comparison.current
    val previous = comparison.previous
    if (current.count == 0 && previous.count == 0) {
        ChartEmpty(
            title = "最近 ${span.days} 天还没有记录",
            subtitle = "记几次之后，这里会和上一个周期做对比。",
        )
        return
    }

    val change = comparison.averageChange
    if (change != null) {
        Text(
            text = "平均变化",
            style = MaterialTheme.typography.labelSmall,
            color = colors.muted,
        )
        Spacer(Modifier.height(1.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (change > 0) {
                Icon(
                    imageVector = AppIcons.ArrowUp,
                    contentDescription = null,
                    tint = colors.inkSecondary,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(2.dp))
            } else if (change < 0) {
                Icon(
                    imageVector = AppIcons.ArrowDown,
                    contentDescription = null,
                    tint = colors.inkSecondary,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(2.dp))
            }
            Text(
                text = signedWithUnit(change, metric),
                style = MaterialTheme.typography.titleLarge,
                color = colors.ink,
            )
            comparison.averageChangeRatio?.let { ratio ->
                Spacer(Modifier.width(6.dp))
                Text(
                    text = NumberFormats.formatPercent(ratio),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.muted,
                )
            }
        }
    } else {
        Text(
            text = if (current.count == 0) {
                "最近 ${span.days} 天还没有记录"
            } else {
                "上一个周期还没有记录，暂时无法对比"
            },
            style = MaterialTheme.typography.bodySmall,
            color = colors.muted,
        )
    }
    Spacer(Modifier.height(12.dp))
    val maxAverage = max(current.average ?: 0.0, previous.average ?: 0.0)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        PeriodBar(
            stats = current,
            label = "最近 ${span.days} 天",
            metric = metric,
            accent = accent,
            maxAverage = maxAverage,
            highlighted = true,
            modifier = Modifier.weight(1f),
        )
        PeriodBar(
            stats = previous,
            label = "之前 ${span.days} 天",
            metric = metric,
            accent = accent,
            maxAverage = maxAverage,
            highlighted = false,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PeriodBar(
    stats: PeriodStats,
    label: String,
    metric: Metric,
    accent: Color,
    maxAverage: Double,
    highlighted: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = BodyMeasureTheme.colors
    val chartHeight = 118.dp
    Column(modifier = modifier) {
        Text(
            text = stats.average?.let {
                NumberFormats.format(it, metric.precision)
            } ?: "—",
            style = MaterialTheme.typography.titleLarge,
            color = if (stats.average != null) colors.ink else colors.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "平均",
            style = MaterialTheme.typography.labelSmall,
            color = colors.muted.copy(alpha = 0.75f),
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.line),
            )
            val average = stats.average
            if (average != null && maxAverage > 0.0) {
                val fraction = (average / maxAverage).toFloat().coerceIn(0.06f, 1f)
                val shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.62f)
                        .height(chartHeight * fraction)
                        .clip(shape)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = if (highlighted) {
                                    listOf(
                                        accent.copy(alpha = 0.82f),
                                        accent.copy(alpha = 0.42f),
                                    )
                                } else {
                                    listOf(
                                        accent.copy(alpha = 0.34f),
                                        accent.copy(alpha = 0.14f),
                                    )
                                },
                            ),
                            shape = shape,
                        ),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (highlighted) colors.ink else colors.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${stats.count} 次记录",
            style = MaterialTheme.typography.labelSmall,
            color = colors.muted.copy(alpha = 0.7f),
            maxLines = 1,
        )
    }
}

private fun valueWithUnit(value: Double, metric: Metric): String {
    val text = NumberFormats.format(value, metric.precision)
    return if (metric.unit.isBlank()) text else "$text ${metric.unit}"
}

private fun signedWithUnit(value: Double, metric: Metric): String {
    val text = NumberFormats.formatSigned(value, metric.precision)
    return if (metric.unit.isBlank()) text else "$text ${metric.unit}"
}
