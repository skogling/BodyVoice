package com.liangji.bodymeasure.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.liangji.bodymeasure.core.designsystem.Motion

@Composable
fun AnimatedMetricValue(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.CenterStart,
) {
    AnimatedContent(
        targetState = text,
        modifier = modifier,
        transitionSpec = {
            (fadeIn(tween(Motion.standard, delayMillis = 30)) +
                slideInVertically(tween(Motion.standard)) { it / 4 })
                .togetherWith(
                    fadeOut(tween(Motion.quick)) +
                        slideOutVertically(tween(Motion.standard)) { -it / 4 }
                )
        },
        contentAlignment = contentAlignment,
        label = "metricValue",
    ) { value ->
        Text(
            text = value,
            style = style,
            color = color,
            maxLines = 1,
        )
    }
}
