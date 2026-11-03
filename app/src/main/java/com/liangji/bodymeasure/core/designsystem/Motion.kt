package com.liangji.bodymeasure.core.designsystem

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

object Motion {
    const val quick = 180
    const val standard = 240
    const val chartDraw = 400

    val spring = spring<Float>(
        dampingRatio = 0.82f,
        stiffness = Spring.StiffnessMediumLow,
    )
}
