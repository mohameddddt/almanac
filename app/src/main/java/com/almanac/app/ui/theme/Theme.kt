package com.almanac.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.almanac.app.R

/**
 * "Parchment & Ink" — warm paper, deep ink, and three earthy accents
 * (terracotta, sage, brass) that sit together like an old printed almanac.
 */
@Immutable
data class Palette(
    val paper: Color,
    val surface: Color,
    val card: Color,
    val ink: Color,
    val inkSoft: Color,
    val muted: Color,
    val line: Color,
    val terracotta: Color,
    val sage: Color,
    val brass: Color,
    val plum: Color,
    val slate: Color,
    val onAccent: Color,
    val isDark: Boolean,
) {
    /** Tints used for events and habits, in a fixed order. */
    val tints get() = listOf(terracotta, sage, brass, plum, slate)
    fun tint(i: Int) = tints[((i % tints.size) + tints.size) % tints.size]

    /** Mood scale from heavy to bright. */
    val moods get() = listOf(slate, plum, muted, sage, brass)
}

val LightPalette = Palette(
    paper = Color(0xFFF3EDE2),
    surface = Color(0xFFFAF6EE),
    card = Color(0xFFEBE3D4),
    ink = Color(0xFF22262A),
    inkSoft = Color(0xFF3D4247),
    muted = Color(0xFF8A8378),
    line = Color(0xFFDCD2C1),
    terracotta = Color(0xFFB8553A),
    sage = Color(0xFF6E8160),
    brass = Color(0xFFC0912F),
    plum = Color(0xFF7A5578),
    slate = Color(0xFF4F6A7E),
    onAccent = Color(0xFFFAF6EE),
    isDark = false,
)

val DarkPalette = Palette(
    paper = Color(0xFF14171A),
    surface = Color(0xFF1B1F23),
    card = Color(0xFF242A2F),
    ink = Color(0xFFEDE6D8),
    inkSoft = Color(0xFFC9C2B4),
    muted = Color(0xFF8C887F),
    line = Color(0xFF30363C),
    terracotta = Color(0xFFE07A5F),
    sage = Color(0xFF9DB08A),
    brass = Color(0xFFE2B65B),
    plum = Color(0xFFB58DB0),
    slate = Color(0xFF86A3B8),
    onAccent = Color(0xFF14171A),
    isDark = true,
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

object Fonts {
    val Display = FontFamily(Font(R.font.dm_serif_display))

    @OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
    private fun sans(weight: Int) = Font(
        R.font.dm_sans,
        weight = FontWeight(weight),
        variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
    )

    val Sans = FontFamily(sans(300), sans(400), sans(500), sans(600), sans(700))
}

private fun typography(p: Palette) = Typography(
    displayLarge = TextStyle(fontFamily = Fonts.Display, fontSize = 64.sp, lineHeight = 64.sp, letterSpacing = (-0.02).em, color = p.ink),
    displayMedium = TextStyle(fontFamily = Fonts.Display, fontSize = 44.sp, lineHeight = 48.sp, letterSpacing = (-0.01).em, color = p.ink),
    displaySmall = TextStyle(fontFamily = Fonts.Display, fontSize = 34.sp, lineHeight = 38.sp, color = p.ink),
    headlineMedium = TextStyle(fontFamily = Fonts.Display, fontSize = 26.sp, lineHeight = 30.sp, color = p.ink),
    headlineSmall = TextStyle(fontFamily = Fonts.Display, fontSize = 22.sp, lineHeight = 26.sp, color = p.ink),
    titleLarge = TextStyle(fontFamily = Fonts.Sans, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = p.ink),
    titleMedium = TextStyle(fontFamily = Fonts.Sans, fontWeight = FontWeight.Medium, fontSize = 16.sp, color = p.ink),
    titleSmall = TextStyle(fontFamily = Fonts.Sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = p.ink),
    bodyLarge = TextStyle(fontFamily = Fonts.Sans, fontSize = 16.sp, lineHeight = 23.sp, color = p.ink),
    bodyMedium = TextStyle(fontFamily = Fonts.Sans, fontSize = 14.sp, lineHeight = 20.sp, color = p.inkSoft),
    bodySmall = TextStyle(fontFamily = Fonts.Sans, fontSize = 12.sp, lineHeight = 16.sp, color = p.muted),
    labelLarge = TextStyle(fontFamily = Fonts.Sans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.02.em),
    labelMedium = TextStyle(fontFamily = Fonts.Sans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.16.em, color = p.muted),
    labelSmall = TextStyle(fontFamily = Fonts.Sans, fontWeight = FontWeight.Medium, fontSize = 10.sp, letterSpacing = 0.1.em, color = p.muted),
)

@Composable
fun AlmanacTheme(mode: Int, content: @Composable () -> Unit) {
    val dark = when (mode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val p = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) {
        darkColorScheme(
            primary = p.terracotta, onPrimary = p.onAccent,
            secondary = p.sage, onSecondary = p.onAccent,
            tertiary = p.brass, onTertiary = p.onAccent,
            background = p.paper, onBackground = p.ink,
            surface = p.surface, onSurface = p.ink,
            surfaceVariant = p.card, onSurfaceVariant = p.muted,
            surfaceContainerHigh = p.surface, surfaceContainerHighest = p.card,
            surfaceContainer = p.surface, surfaceContainerLow = p.surface,
            outline = p.line, outlineVariant = p.line,
        )
    } else {
        lightColorScheme(
            primary = p.terracotta, onPrimary = p.onAccent,
            secondary = p.sage, onSecondary = p.onAccent,
            tertiary = p.brass, onTertiary = p.onAccent,
            background = p.paper, onBackground = p.ink,
            surface = p.surface, onSurface = p.ink,
            surfaceVariant = p.card, onSurfaceVariant = p.muted,
            surfaceContainerHigh = p.surface, surfaceContainerHighest = p.card,
            surfaceContainer = p.surface, surfaceContainerLow = p.surface,
            outline = p.line, outlineVariant = p.line,
        )
    }
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, typography = typography(p), content = content)
    }
}
