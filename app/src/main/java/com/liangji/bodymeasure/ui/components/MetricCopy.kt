package com.liangji.bodymeasure.ui.components

internal fun precisionHint(precision: Int): String =
    if (precision <= 0) "整数" else "0." + "0".repeat(precision - 1) + "1"
