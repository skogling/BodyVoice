package com.liangji.bodymeasure.core.designsystem

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

@Immutable
data class ExtendedColors(
    val isDark: Boolean,
    val background: Color,
    val glassSurface: Color,
    val sheet: Color,
    val ink: Color,
    val inkSecondary: Color,
    val muted: Color,
    val line: Color,
    val lineStrong: Color,
    val glowPrimary: Color,
    val glowSecondary: Color,
    val clay: Color,
    val teal: Color,
    val ochre: Color,
    val indigo: Color,
    val plum: Color,
    val sage: Color,
    val mist: Color,
)

/** Content color for clay-accented fills; keeps text above 4.5:1 in both themes. */
val ExtendedColors.onAccent: Color
    get() = if (isDark) background else ink

/** Lifts stored light-theme metric colors for legibility on the dark background. */
fun ExtendedColors.metricColor(argb: Long): Color {
    val base = Color(argb)
    return if (isDark) lerp(base, Color.White, 0.26f) else base
}

internal val LightExtendedColors = ExtendedColors(
    isDark = false,
    background = Color(0xFFF7F4EF),
    glassSurface = Color(0xB8FFFFFF),
    sheet = Color(0xFFFBF9F5),
    ink = Color(0xFF1C1B18),
    inkSecondary = Color(0xFF4A463F),
    muted = Color(0xFF6F6A63),
    line = Color(0x171C1B18),
    lineStrong = Color(0x291C1B18),
    glowPrimary = Color(0x2EC96F4A),
    glowSecondary = Color(0x1F3F7F72),
    clay = Color(0xFFC96F4A),
    teal = Color(0xFF3F7F72),
    ochre = Color(0xFFB98A3E),
    indigo = Color(0xFF5D6FA8),
    plum = Color(0xFF8A6478),
    sage = Color(0xFF6F8A63),
    mist = Color(0xFF55706B),
)

internal val DarkExtendedColors = ExtendedColors(
    isDark = true,
    background = Color(0xFF0E0F11),
    glassSurface = Color(0xC71A1B1E),
    sheet = Color(0xFF17181C),
    ink = Color(0xFFF3EFE8),
    inkSecondary = Color(0xFFCFC8BD),
    muted = Color(0xFFA9A39A),
    line = Color(0x1AFFFFFF),
    lineStrong = Color(0x2EFFFFFF),
    glowPrimary = Color(0x1AE08A64),
    glowSecondary = Color(0x145FA898),
    clay = Color(0xFFE08A64),
    teal = Color(0xFF5FA898),
    ochre = Color(0xFFD2A55A),
    indigo = Color(0xFF8193CC),
    plum = Color(0xFFB1849A),
    sage = Color(0xFF8FAE7E),
    mist = Color(0xFFA7C6C0),
)

internal val LightColorScheme = lightColorScheme(
    primary = LightExtendedColors.clay,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF3DED3),
    onPrimaryContainer = LightExtendedColors.ink,
    secondary = LightExtendedColors.teal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE9E4),
    onSecondaryContainer = LightExtendedColors.ink,
    tertiary = LightExtendedColors.ochre,
    onTertiary = Color.White,
    background = LightExtendedColors.background,
    onBackground = LightExtendedColors.ink,
    surface = LightExtendedColors.background,
    onSurface = LightExtendedColors.ink,
    surfaceVariant = Color(0xFFEFEAE1),
    onSurfaceVariant = LightExtendedColors.inkSecondary,
    outline = Color(0xFF8A857C),
    outlineVariant = LightExtendedColors.lineStrong,
    error = Color(0xFFB3261E),
    onError = Color.White,
)

internal val DarkColorScheme = darkColorScheme(
    primary = DarkExtendedColors.clay,
    onPrimary = Color(0xFF24120B),
    primaryContainer = Color(0xFF4A2A1D),
    onPrimaryContainer = DarkExtendedColors.ink,
    secondary = DarkExtendedColors.teal,
    onSecondary = Color(0xFF0B1F1A),
    secondaryContainer = Color(0xFF243B35),
    onSecondaryContainer = DarkExtendedColors.ink,
    tertiary = DarkExtendedColors.ochre,
    onTertiary = Color(0xFF24180A),
    background = DarkExtendedColors.background,
    onBackground = DarkExtendedColors.ink,
    surface = DarkExtendedColors.background,
    onSurface = DarkExtendedColors.ink,
    surfaceVariant = Color(0xFF1E1F23),
    onSurfaceVariant = DarkExtendedColors.inkSecondary,
    outline = Color(0xFF6F6A63),
    outlineVariant = DarkExtendedColors.lineStrong,
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
)
