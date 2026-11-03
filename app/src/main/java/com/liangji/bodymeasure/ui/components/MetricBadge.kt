package com.liangji.bodymeasure.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liangji.bodymeasure.core.designsystem.BodyMeasureTheme
import com.liangji.bodymeasure.core.designsystem.metricColor
import com.liangji.bodymeasure.domain.model.Metric
import com.liangji.bodymeasure.domain.model.MetricIconKeys

fun metricGlyph(iconKey: String, name: String): String = when (iconKey) {
    MetricIconKeys.WEIGHT -> "重"
    MetricIconKeys.CHEST -> "胸"
    MetricIconKeys.WAIST -> "腰"
    MetricIconKeys.HIP -> "臀"
    MetricIconKeys.ARM -> "臂"
    MetricIconKeys.THIGH -> "腿"
    MetricIconKeys.CALF -> "小"
    MetricIconKeys.HEIGHT -> "高"
    else -> name.trim().takeIf { it.isNotEmpty() }?.take(1) ?: "量"
}

@Composable
fun metricAccent(metric: Metric): Color = BodyMeasureTheme.colors.metricColor(metric.colorArgb)

@Composable
fun MetricBadge(
    metric: Metric,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    glyphSize: TextUnit = 17.sp,
) {
    val colors = BodyMeasureTheme.colors
    val accent = metricAccent(metric)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.34f))
            .background(accent.copy(alpha = if (colors.isDark) 0.18f else 0.1f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = metricGlyph(metric.iconKey, metric.name),
            style = TextStyle(
                fontSize = glyphSize,
                lineHeight = glyphSize,
                fontWeight = FontWeight.Medium,
            ),
            color = accent,
        )
    }
}
