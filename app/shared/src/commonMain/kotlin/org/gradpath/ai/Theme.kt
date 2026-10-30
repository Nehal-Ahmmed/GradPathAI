package org.gradpath.ai

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.gradpath.ai.ui.components.GradPathDarkPaletteStandard
import org.gradpath.ai.ui.components.GradPathLightPaletteStandard
import org.gradpath.ai.ui.components.LocalGradPathPalette

// Backward-compatible tokens for existing usages
val GradPathBluePrimary = Color(0xFF1E60D5)  // Royal Electric Blue
val GradPathCyanAccent  = Color(0xFF38BDF8)  // Sky Cyan
val GradPathDarkSurface = Color(0xFF121417)  // Deep Dark Background
val GradPathCardSurface = Color(0xFF1E2228)  // Elevated Card Background
val GradPathCardBorder  = Color(0xFF2E3540)  // Subtle Card Border
val GradPathTextPrimary = Color(0xFFF1F5F9)  // Crisp Text
val GradPathTextSecondary = Color(0xFF94A3B8) // Muted Text
val GradPathSuccessGreen = Color(0xFF10B981) // Engine Connected Green
val GradPathAlertAmber  = Color(0xFFF59E0B)  // Deadline Warning Amber

@Immutable
data class GradPathColors(
    val canvas: Color,
    val surfaceLow: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val surfaceHighest: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val faintText: Color,
    val outline: Color,
    val outlineVariant: Color,
    val lineDashed: Color,
    val lineStrong: Color,
    val primary: Color,
    val onPrimary: Color,
    val academicBlue: Color,
    val academicBlueGlow: Color,
    val verifiedEmerald: Color,
    val alertAmber: Color,
    val cardBackground: Color,
    val cardBorder: Color,
    val isDark: Boolean
)

val GradPathLightPalette = GradPathColors(
    canvas = Color(0xFFFFFFFF),
    surfaceLow = Color(0xFFF8F9FA),
    surface = Color(0xFFF3F3F3),
    surfaceHigh = Color(0xFFEBEBEB),
    surfaceHighest = Color(0xFFE2E2E2),
    onSurface = Color(0xFF191917),
    onSurfaceVariant = Color(0xA6191917), // 65% opacity
    faintText = Color(0x59191917),        // 35% opacity
    outline = Color(0x33191917),          // 20% opacity
    outlineVariant = Color(0x1F191917),   // 12% opacity
    lineDashed = Color(0x33191917),
    lineStrong = Color(0x66191917),
    primary = Color(0xFF191917),
    onPrimary = Color(0xFFFFFFFF),
    academicBlue = Color(0xFF1E60D5),
    academicBlueGlow = Color(0xFF38BDF8),
    verifiedEmerald = Color(0xFF059669),
    alertAmber = Color(0xFFD97706),
    cardBackground = Color(0xFFFFFFFF),
    cardBorder = Color(0x26191917),
    isDark = false
)

val GradPathDarkPalette = GradPathColors(
    canvas = Color(0xFF121417),
    surfaceLow = Color(0xFF181B22),
    surface = Color(0xFF1E222A),
    surfaceHigh = Color(0xFF242933),
    surfaceHighest = Color(0xFF2C323E),
    onSurface = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xA6F1F5F9), // 65% opacity
    faintText = Color(0x59F1F5F9),        // 35% opacity
    outline = Color(0x29FFFFFF),          // 16% opacity
    outlineVariant = Color(0x1FFFFFFF),   // 12% opacity
    lineDashed = Color(0x29FFFFFF),
    lineStrong = Color(0x4DFFFFFF),
    primary = Color(0xFFF1F5F9),
    onPrimary = Color(0xFF121417),
    academicBlue = Color(0xFF38BDF8),
    academicBlueGlow = Color(0xFF38BDF8),
    verifiedEmerald = Color(0xFF10B981),
    alertAmber = Color(0xFFF59E0B),
    cardBackground = Color(0xFF1E222A),
    cardBorder = Color(0x29FFFFFF),
    isDark = true
)

val LocalGradPathColors = staticCompositionLocalOf { GradPathDarkPalette }

object GradPathTheme {
    val colors: GradPathColors
        @Composable
        get() = LocalGradPathColors.current

    val isDark: Boolean
        @Composable
        get() = LocalGradPathColors.current.isDark
}

private val LightM3Colors = lightColorScheme(
    primary = Color(0xFF191917),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF1E60D5),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF333330),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE5E7EB),
    onSecondaryContainer = Color(0xFF191917),
    tertiary = Color(0xFF0F766E),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF191917),
    surface = Color(0xFFF8F9FA),
    onSurface = Color(0xFF191917),
    surfaceVariant = Color(0xFFF3F3F3),
    onSurfaceVariant = Color(0xFF474741),
    outline = Color(0x33191917)
)

private val DarkM3Colors = darkColorScheme(
    primary = Color(0xFFF1F5F9),
    onPrimary = Color(0xFF121417),
    primaryContainer = Color(0xFF1E60D5),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF121417),
    secondaryContainer = Color(0xFF1E222A),
    onSecondaryContainer = Color(0xFFF1F5F9),
    tertiary = Color(0xFF10B981),
    onTertiary = Color(0xFF121417),
    background = Color(0xFF121417),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF1E222A),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF242933),
    onSurfaceVariant = Color(0xA6F1F5F9),
    outline = Color(0x29FFFFFF)
)

@Composable
fun GradPathTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) GradPathDarkPalette else GradPathLightPalette
    val rolePalette = if (darkTheme) GradPathDarkPaletteStandard else GradPathLightPaletteStandard
    val m3Colors = if (darkTheme) DarkM3Colors else LightM3Colors

    CompositionLocalProvider(
        LocalGradPathColors provides colors,
        LocalGradPathPalette provides rolePalette
    ) {
        MaterialTheme(
            colorScheme = m3Colors,
            content = content
        )
    }
}

/**
 * Editorial Dashed Hairline Modifiers matching DESIGN.md
 */
fun Modifier.dashedBorder(
    width: Dp = 1.dp,
    color: Color,
    dashLength: Dp = 4.dp,
    gapLength: Dp = 4.dp,
    cornerRadius: Dp = 0.dp
): Modifier = this.drawBehind {
    val strokeWidth = width.toPx()
    val dashPx = dashLength.toPx()
    val gapPx = gapLength.toPx()
    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashPx, gapPx), 0f)

    if (cornerRadius > 0.dp) {
        val cornerPx = cornerRadius.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
            size = Size(size.width - strokeWidth, size.height - strokeWidth),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerPx, cornerPx),
            style = Stroke(width = strokeWidth, pathEffect = pathEffect)
        )
    } else {
        drawRect(
            color = color,
            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
            size = Size(size.width - strokeWidth, size.height - strokeWidth),
            style = Stroke(width = strokeWidth, pathEffect = pathEffect)
        )
    }
}

fun Modifier.dashedHairlineBottom(
    color: Color,
    width: Dp = 1.dp,
    dashLength: Dp = 4.dp,
    gapLength: Dp = 4.dp
): Modifier = this.drawBehind {
    val strokeWidth = width.toPx()
    val dashPx = dashLength.toPx()
    val gapPx = gapLength.toPx()
    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashPx, gapPx), 0f)
    val y = size.height - strokeWidth / 2
    drawLine(
        color = color,
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = strokeWidth,
        pathEffect = pathEffect
    )
}

fun Modifier.dashedHairlineTop(
    color: Color,
    width: Dp = 1.dp,
    dashLength: Dp = 4.dp,
    gapLength: Dp = 4.dp
): Modifier = this.drawBehind {
    val strokeWidth = width.toPx()
    val dashPx = dashLength.toPx()
    val gapPx = gapLength.toPx()
    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashPx, gapPx), 0f)
    val y = strokeWidth / 2
    drawLine(
        color = color,
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = strokeWidth,
        pathEffect = pathEffect
    )
}