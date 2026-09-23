package com.openmgmt.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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
private val PaperSurface2 = Color(0xFFEEF0EA)
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
private val Danger = Color(0xFF8D302B)
private val DangerSoft = Color(0xFFF0D1CE)
private val Warn = Color(0xFFB5731F)
private val Ok = Color(0xFF3F7A4D)

private val BoardBg = Color(0xFF0D100E)
private val BoardSurface = Color(0xFF151916)
private val BoardSurface2 = Color(0xFF1D231E)
private val BoardLine = Color(0xFF2C322D)
private val BoardText = Color(0xFFE6EBE3)
private val BoardTextMuted = Color(0xFF8E988F)

private val LightColors = lightColorScheme(
    primary = DeepGreen,
    onPrimary = Color(0xFFEEF1E9),
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
    surfaceContainerLow = PaperSurface2,
    outline = PaperLine,
    outlineVariant = PaperLine,
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
    background = BoardBg,
    onBackground = BoardText,
    surface = BoardSurface,
    onSurface = BoardText,
    surfaceVariant = BoardSurface2,
    onSurfaceVariant = BoardTextMuted,
    surfaceContainerLow = BoardSurface2,
    outline = BoardLine,
    outlineVariant = BoardLine,
    error = Color(0xFFE08079),
    errorContainer = Color(0xFF3A1F1D),
)

private val OpenMgmtShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(9.dp),
    large = RoundedCornerShape(9.dp),
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
    val onBackground = Color(0xFFEEF1E9)
    val muted = Color(0xFF9AA39A)
    val activeBackground = Charcoal2
    val brandMarkBackground = Lime
    val brandMarkText = LimeInk
    val statusDot = Ok
}

@Composable
fun OpenMgmtTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = OpenMgmtShapes,
        typography = OpenMgmtTypography,
        content = content,
    )
}
