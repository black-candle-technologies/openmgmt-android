package com.openmgmt.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * OpenMGMT theme, ported from the desktop app (apps/desktop/ui/styles.css).
 *
 * Light: warm paper background (#f3f2ed), deep-green primary (#294f37),
 * lime accent (#c9ef6a), charcoal drawer (#171c17).
 * Dark: the desktop "board" palette (#0d100e family).
 */

private val Paper = Color(0xFFF3F2ED)
private val PaperSurface = Color(0xFFFAFAF7)
private val PaperSurface2 = Color(0xFFF0EFE8)
private val PaperLine = Color(0xFFDDE0D7)
private val Ink = Color(0xFF20251F)
private val InkSoft = Color(0xFF45503F)
private val InkMuted = Color(0xFF6C756A)
private val DeepGreen = Color(0xFF294F37)
private val DeepGreenDark = Color(0xFF1D3B29)
private val Charcoal = Color(0xFF171C17)
private val Charcoal2 = Color(0xFF20271F)
private val Lime = Color(0xFFC9EF6A)
private val LimeInk = Color(0xFF182017)
private val Danger = Color(0xFFC03527)
private val DangerSoft = Color(0xFFF6D9D6)
private val Warn = Color(0xFFB07C1F)
private val Ok = Color(0xFF2E7D43)

private val BoardBg = Color(0xFF0D100E)
private val BoardSurface = Color(0xFF151916)
private val BoardSurface2 = Color(0xFF1D231E)
private val BoardLine = Color(0xFF2C322D)
private val BoardText = Color(0xFFE6EBE3)
private val BoardTextMuted = Color(0xFF8E988F)

private val LightColors = lightColorScheme(
    primary = DeepGreen,
    onPrimary = Color.White,
    primaryContainer = DeepGreenDark,
    onPrimaryContainer = Color(0xFFEEF1E9),
    secondary = InkSoft,
    onSecondary = Color(0xFFEEF1E9),
    tertiary = Lime,
    onTertiary = LimeInk,
    background = Paper,
    onBackground = Ink,
    surface = PaperSurface,
    onSurface = Ink,
    surfaceVariant = PaperSurface2,
    onSurfaceVariant = InkSoft,
    secondaryContainer = Color(0xFFDDE8D3),
    onSecondaryContainer = DeepGreenDark,
    tertiaryContainer = Color(0xFFE4F3BF),
    onTertiaryContainer = Color(0xFF2F4A12),
    surfaceContainerLowest = PaperSurface,
    surfaceContainerLow = PaperSurface,
    surfaceContainer = PaperSurface,
    surfaceContainerHigh = PaperSurface,
    surfaceContainerHighest = PaperSurface2,
    outline = Color(0xFFC8CDC2),
    outlineVariant = PaperLine,
    inverseSurface = Charcoal,
    inverseOnSurface = Color(0xFFEEF1E9),
    inversePrimary = Lime,
    error = Danger,
    onError = Color.White,
    errorContainer = DangerSoft,
    onErrorContainer = Danger,
)

private val DarkColors = darkColorScheme(
    primary = Lime,
    onPrimary = LimeInk,
    primaryContainer = DeepGreen,
    onPrimaryContainer = BoardText,
    secondary = BoardTextMuted,
    onSecondary = BoardBg,
    secondaryContainer = Color(0xFF26312A),
    onSecondaryContainer = Color(0xFFD6E8C0),
    tertiary = Lime,
    onTertiary = LimeInk,
    tertiaryContainer = Color(0xFF2A3517),
    onTertiaryContainer = Lime,
    background = BoardBg,
    onBackground = BoardText,
    surface = BoardSurface,
    onSurface = BoardText,
    surfaceVariant = BoardSurface2,
    onSurfaceVariant = Color(0xFFC3CCC2),
    surfaceContainerLowest = BoardBg,
    surfaceContainerLow = BoardSurface,
    surfaceContainer = BoardSurface,
    surfaceContainerHigh = BoardSurface2,
    surfaceContainerHighest = Color(0xFF232A24),
    outline = Color(0xFF4A524B),
    outlineVariant = BoardLine,
    inverseSurface = BoardText,
    inverseOnSurface = BoardBg,
    inversePrimary = DeepGreen,
    error = Color(0xFFE5484D),
    onError = Color(0xFF2A0F0D),
    errorContainer = Color(0xFF3A1713),
    onErrorContainer = Color(0xFFF2B8B3),
)

/**
 * Status colors Material's scheme has no slot for: the desktop "warn"
 * amber used for blocked / waiting work.
 */
data class StatusColors(
    val caution: Color,
    val cautionContainer: Color,
    val onCautionContainer: Color,
)

private val LightStatusColors = StatusColors(
    caution = Warn,
    cautionContainer = Color(0xFFF8ECD2),
    onCautionContainer = Color(0xFF6E420C),
)

private val DarkStatusColors = StatusColors(
    caution = Color(0xFFE0A52E),
    cautionContainer = Color(0xFF3A2C17),
    onCautionContainer = Color(0xFFEBC18A),
)

val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }

private val OpenMgmtShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

private val OpenMgmtTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 1.2.sp,
    ),
)

/** Charcoal drawer colors, matching the desktop sidebar. */
object DrawerColors {
    val background = Charcoal
    val onBackground = Color(0xFFE6EBE3)
    val muted = Color(0xFF8E988F)
    val activeBackground = Charcoal2
    val brandMarkBackground = Lime
    val brandMarkText = LimeInk
    val statusDot = Ok
}

object UrgencyColors {
    val overdue = Color(0xFFE5484D)
    val now = Color(0xFF4D8DF6)
    val next = Color(0xFF46C46B)
    val due = Color(0xFFE0A52E)
    val waiting = Color(0xFFE07B39)
    val later = Color(0xFF6B8299)
    val done = Color(0xFF9B7AD6)
}

@Composable
fun agentPillColors(state: String): Pair<Color, Color> {
    val colors = when (state) {
        "running" -> Color(0xFFDCEAD2) to Color(0xFF294F37)
        "needs_input" -> Color(0xFFF8ECD2) to Color(0xFF7C5A17)
        "pr_opened" -> Color(0xFFD9E4F7) to Color(0xFF2F4D8A)
        "done" -> Color(0xFFE7DDF2) to Color(0xFF5B4390)
        "failed" -> Color(0xFFF6D9D6) to Color(0xFF8D302B)
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    return if (isSystemInDarkTheme() && state != "dispatched") colors.second to colors.first else colors
}

@Composable
fun OpenMgmtTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalStatusColors provides if (darkTheme) DarkStatusColors else LightStatusColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            shapes = OpenMgmtShapes,
            typography = OpenMgmtTypography,
            content = content,
        )
    }
}
