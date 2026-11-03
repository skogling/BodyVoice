package com.liangji.bodymeasure.core.designsystem

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SoftPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    busy: Boolean = false,
    containerColor: Color? = null,
    contentColor: Color? = null,
    minHeight: Dp = 50.dp,
    labelStyle: TextStyle? = null,
) {
    val colors = BodyMeasureTheme.colors
    val container = containerColor ?: colors.ink
    val content = contentColor ?: colors.background
    val interaction = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = minHeight)
            .pressScale(interaction),
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = container.copy(alpha = 0.08f),
            disabledContentColor = colors.muted,
        ),
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
        interactionSource = interaction,
    ) {
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                color = LocalContentColor.current,
                strokeWidth = 1.6.dp,
            )
            Spacer(Modifier.width(7.dp))
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(7.dp))
        }
        AnimatedContent(
            targetState = text,
            transitionSpec = {
                (fadeIn(tween(Motion.quick)) +
                    slideInVertically(tween(Motion.quick)) { it / 3 })
                    .togetherWith(
                        fadeOut(tween(Motion.quick / 2)) +
                            slideOutVertically(tween(Motion.quick)) { -it / 3 }
                    )
            },
            contentAlignment = Alignment.Center,
            label = "buttonLabel",
        ) { label ->
            Text(
                text = label,
                style = labelStyle ?: MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
            )
        }
    }
}

@Composable
fun SoftActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    busy: Boolean = false,
) {
    val colors = BodyMeasureTheme.colors
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        SoftPrimaryButton(
            text = text,
            onClick = onClick,
            enabled = enabled,
            icon = icon,
            busy = busy,
            modifier = Modifier.width(264.dp),
            containerColor = colors.mist,
            contentColor = colors.background,
            minHeight = 48.dp,
            labelStyle = MaterialTheme.typography.labelLarge.copy(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

@Composable
internal fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.97f,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = if (pressed) tween(Motion.quick) else Motion.spring,
        label = "pressScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
fun SoftPill(
    text: String,
    modifier: Modifier = Modifier,
    dotColor: Color? = null,
) {
    val colors = BodyMeasureTheme.colors
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(colors.ink.copy(alpha = if (colors.isDark) 0.08f else 0.05f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dotColor != null) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = colors.muted,
        )
    }
}
