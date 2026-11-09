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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.almanac.app.R

/**
 * Print edition: uncoated paper, black ink and a single signal colour.
 * Everything else — events, rituals — is set in a small family of
 * process inks, the way a printed almanac would mark them.
 */
@Immutable
data class Palette(
    val paper: Color,
    val shade: Color,
    val ink: Color,
    val inkSoft: Color,
    val muted: Color,
    val rule: Color,
    val signal: Color,
    val cobalt: Color,
    val ochre: Color,
    val green: Color,
    val isDark: Boolean,
) {
    /** Marker inks for events and rituals, in a fixed order. */
    val inks get() = listOf(signal, cobalt, ochre, green, ink)
    fun inkAt(i: Int) = inks[((i % inks.size) + inks.size) % inks.size]

    /** Mood is set as ink density: 1 is a faint wash, 5 is solid. */
    fun mood(level: Int): Color = ink.copy(alpha = listOf(0.14f, 0.32f, 0.52f, 0.74f, 1f)[level.coerceIn(0, 4)])
}

val LightPalette = Palette(
    paper = Color(0xFFF3F2EE),
    shade = Color(0xFFE8E6E0),
    ink = Color(0xFF121212),
    inkSoft = Color(0xFF3A3A37),
    muted = Color(0xFF7A7974),
    rule = Color(0xFFCFCDC6),
    signal = Color(0xFFE0401B),
    cobalt = Color(0xFF2546B8),
    ochre = Color(0xFFC9920F),
    green = Color(0xFF2F7A4D),
    isDark = false,
)

val DarkPalette = Palette(
    paper = Color(0xFF111111),
    shade = Color(0xFF1C1C1B),
    ink = Color(0xFFECEAE4),
    inkSoft = Color(0xFFC4C2BB),
    muted = Color(0xFF85837D),
    rule = Color(0xFF2E2E2C),
    signal = Color(0xFFFF5A33),
    cobalt = Color(0xFF7D95FF),
    ochre = Color(0xFFF0BA3A),
    green = Color(0xFF5DB07F),
    isDark = true,
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

object Fonts {
    /** Headlines and big numerals. */
    val Condensed = FontFamily(
        Font(R.font.plex_condensed_semibold, FontWeight.SemiBold),
        Font(R.font.plex_condensed_bold, FontWeight.Bold),
    )

    /** Running text. */
    val Sans = FontFamily(
        Font(R.font.plex_sans_regular, FontWeight.Normal),
        Font(R.font.plex_sans_medium, FontWeight.Medium),
        Font(R.font.plex_sans_semibold, FontWeight.SemiBold),
    )

    /** Times, counts, labels — anything tabular. */
    val Mono = FontFamily(
        Font(R.font.plex_mono_regular, FontWeight.Normal),
        Font(R.font.plex_mono_medium, FontWeight.Medium),
    )
}

/** Extra styles that Material's scale has no names for. */
object Type {
    val numeral = TextStyle(fontFamily = Fonts.Condensed, fontWeight = FontWeight.Bold, fontSize = 112.sp, lineHeight = 104.sp, letterSpacing = (-0.03).em)
    val numeralMid = TextStyle(fontFamily = Fonts.Condensed, fontWeight = FontWeight.Bold, fontSize = 64.sp, lineHeight = 62.sp, letterSpacing = (-0.02).em)
    val data = TextStyle(fontFamily = Fonts.Mono, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp)
    val tag = TextStyle(fontFamily = Fonts.Mono, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.06.em)
}

private fun typography(p: Palette) = Typography(
    displayLarge = TextStyle(fontFamily = Fonts.Condensed, fontWeight = FontWeight.Bold, fontSize = 60.sp, lineHeight = 58.sp, letterSpacing = (-0.02).em, color = p.ink),
    displayMedium = TextStyle(fontFamily = Fonts.Condensed, fontWeight = FontWeight.Bold, fontSize = 46.sp, lineHeight = 46.sp, letterSpacing = (-0.01).em, color = p.ink),
    displaySmall = TextStyle(fontFamily = Fonts.Condensed, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 38.sp, color = p.ink),
    headlineMedium = TextStyle(fontFamily = Fonts.Condensed, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 32.sp, color = p.ink),
    headlineSmall = TextStyle(fontFamily = Fonts.Condensed, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 27.sp, color = p.ink),
    titleLarge = TextStyle(fontFamily = Fonts.Sans, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp, color = p.ink),
    titleMedium = TextStyle(fontFamily = Fonts.Sans, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp, color = p.ink),
    titleSmall = TextStyle(fontFamily = Fonts.Sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, color = p.ink),
    bodyLarge = TextStyle(fontFamily = Fonts.Sans, fontSize = 16.sp, lineHeight = 23.sp, color = p.ink),
    bodyMedium = TextStyle(fontFamily = Fonts.Sans, fontSize = 14.sp, lineHeight = 20.sp, color = p.inkSoft),
    bodySmall = TextStyle(fontFamily = Fonts.Sans, fontSize = 12.sp, lineHeight = 16.sp, color = p.muted),
    labelLarge = TextStyle(fontFamily = Fonts.Mono, fontWeight = FontWeight.Medium, fontSize = 13.sp, letterSpacing = 0.04.em),
    labelMedium = TextStyle(fontFamily = Fonts.Mono, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.06.em, color = p.muted),
    labelSmall = TextStyle(fontFamily = Fonts.Mono, fontSize = 10.sp, letterSpacing = 0.04.em, color = p.muted),
)

@Composable
fun AlmanacTheme(mode: Int, content: @Composable () -> Unit) {
    val dark = when (mode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val p = if (dark) DarkPalette else LightPalette
    val base = if (dark) darkColorScheme() else lightColorScheme()
    val scheme = base.copy(
        primary = p.ink, onPrimary = p.paper,
        secondary = p.signal, onSecondary = p.paper,
        tertiary = p.signal, onTertiary = p.paper,
        background = p.paper, onBackground = p.ink,
        surface = p.paper, onSurface = p.ink,
        surfaceVariant = p.shade, onSurfaceVariant = p.muted,
        surfaceContainerLowest = p.paper, surfaceContainerLow = p.paper,
        surfaceContainer = p.paper, surfaceContainerHigh = p.paper, surfaceContainerHighest = p.shade,
        outline = p.ink, outlineVariant = p.rule,
    )
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, typography = typography(p), content = content)
    }
}
