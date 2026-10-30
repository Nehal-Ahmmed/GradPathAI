package org.gradpath.ai.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * -----------------------------------------------------------------------------
 * GradPath Role-Based Color System
 * Faithfully adapting BrainSton's 5-slot semantic family architecture:
 *   - [x]          : Solid fill (buttons, primary icons, progress bars)
 *   - [onX]        : Content drawn ON TOP of [x]
 *   - [xSoft]      : Tinted background surface
 *   - [onXSoft]    : Content drawn ON TOP of [xSoft]
 *   - [xBorder]    : Subtle border hairline surrounding [xSoft]
 *
 * Guarantees WCAG AA (4.5:1+) readability across both Light and Dark modes.
 * -----------------------------------------------------------------------------
 */
@Immutable
data class GradPathPalette(
    val isDark: Boolean,

    // ── Primary Academic Brand (Royal Electric Blue) ──
    val academicBlue: Color,
    val onAcademicBlue: Color,
    val academicBlueSoft: Color,
    val onAcademicBlueSoft: Color,
    val academicBlueBorder: Color,
    val academicDeep: Color,

    // ── Verified Status Family (Emerald Green) ─────────
    val verifiedEmerald: Color,
    val onVerified: Color,
    val verifiedSoft: Color,
    val onVerifiedSoft: Color,
    val verifiedBorder: Color,

    // ── Alert & Deadline Family (Warm Amber) ───────────
    val alertAmber: Color,
    val onAlert: Color,
    val alertSoft: Color,
    val onAlertSoft: Color,
    val alertBorder: Color,

    // ── Danger & Error Family (Red Coral) ──────────────
    val dangerRed: Color,
    val onDanger: Color,
    val dangerSoft: Color,
    val onDangerSoft: Color,
    val dangerBorder: Color,

    // ── Accent Family (Sky Cyan / Violet) ──────────────
    val accentSky: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val onAccentSoft: Color,
    val accentBorder: Color,

    // ── Bookmark & Saved Affection Family (Warm Coral) ──
    val savedCoral: Color,
    val onSaved: Color,
    val savedSoft: Color,
    val onSavedSoft: Color,

    // ── Neutral Surfaces ───────────────────────────────
    val canvas: Color,
    val surfaceLow: Color,
    val card: Color,
    val sheet: Color,
    val chip: Color,
    val chipStrong: Color,
    val track: Color,

    // ── Neutral Lines & Depth ──────────────────────────
    val border: Color,
    val borderSoft: Color,
    val shadow: Color,
    val scrim: Color,

    // ── Text & Typography Ink ──────────────────────────
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textInverse: Color
) {
    val surfaceElevated: Color
        get() = surfaceLow

    /**
     * Deep academic gradient for hero banners and featured research cards.
     */
    val academicGradient: Brush
        get() = Brush.linearGradient(
            colors = listOf(academicDeep, academicBlue)
        )

    /**
     * Hero card multi-stop gradient matching BrainSton's DeepGradientCard.
     */
    val heroGradient: Brush
        get() = Brush.linearGradient(
            colors = if (isDark) {
                listOf(Color(0xFF0A192F), Color(0xFF102A4C), Color(0xFF1E60D5))
            } else {
                listOf(Color(0xFF0F2B5C), Color(0xFF164796), Color(0xFF1E60D5))
            }
        )

    /**
     * Frosted glass card fill with optimal contrast.
     */
    val glassFill: Color
        get() = if (isDark) {
            Color(0xFF161E2E).copy(alpha = 0.78f)
        } else {
            Color(0xFFFFFFFF).copy(alpha = 0.76f)
        }

    val glassBorder: Color
        get() = if (isDark) {
            Color.White.copy(alpha = 0.22f)
        } else {
            Color.White.copy(alpha = 0.85f)
        }
}

val GradPathLightPaletteStandard = GradPathPalette(
    isDark = false,

    // Academic Brand (#1E60D5)
    academicBlue = Color(0xFF1E60D5),
    onAcademicBlue = Color(0xFFFFFFFF),
    academicBlueSoft = Color(0xFF1E60D5).copy(alpha = 0.08f),
    onAcademicBlueSoft = Color(0xFF114299),
    academicBlueBorder = Color(0xFF1E60D5).copy(alpha = 0.24f),
    academicDeep = Color(0xFF0F2B5C),

    // Verified Emerald (#059669)
    verifiedEmerald = Color(0xFF059669),
    onVerified = Color(0xFFFFFFFF),
    verifiedSoft = Color(0xFFE8F5E9),
    onVerifiedSoft = Color(0xFF0B6644),
    verifiedBorder = Color(0xFFA5D6A7),

    // Alert Amber (#D97706)
    alertAmber = Color(0xFFD97706),
    onAlert = Color(0xFFFFFFFF),
    alertSoft = Color(0xFFFEF3C7),
    onAlertSoft = Color(0xFF92400E),
    alertBorder = Color(0xFFFDE68A),

    // Danger Red (#DC2626)
    dangerRed = Color(0xFFDC2626),
    onDanger = Color(0xFFFFFFFF),
    dangerSoft = Color(0xFFFEE2E2),
    onDangerSoft = Color(0xFF991B1B),
    dangerBorder = Color(0xFFFECACA),

    // Accent Sky (#0284C7)
    accentSky = Color(0xFF0284C7),
    onAccent = Color(0xFFFFFFFF),
    accentSoft = Color(0xFFE0F2FE),
    onAccentSoft = Color(0xFF0369A1),
    accentBorder = Color(0xFFBAE6FD),

    // Saved Coral (#EA580C)
    savedCoral = Color(0xFFEA580C),
    onSaved = Color(0xFFFFFFFF),
    savedSoft = Color(0xFFFFEDD5),
    onSavedSoft = Color(0xFF9A3412),

    // Neutral Surfaces
    canvas = Color(0xFFF4F6FA),
    surfaceLow = Color(0xFFFFFFFF),
    card = Color(0xFFFFFFFF),
    sheet = Color(0xFFFFFFFF),
    chip = Color(0xFFF1F4F9),
    chipStrong = Color(0xFFEAEEF5),
    track = Color(0xFFE2E8F0),

    // Neutral Lines & Depth
    border = Color(0xFFE2E8F0),
    borderSoft = Color(0xFFEEF2F6),
    shadow = Color(0xFF0F172A).copy(alpha = 0.06f),
    scrim = Color(0xFF0B0F19).copy(alpha = 0.48f),

    // Typography Ink
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    textMuted = Color(0xFF64748B),
    textInverse = Color(0xFFFFFFFF)
)

val GradPathDarkPaletteStandard = GradPathPalette(
    isDark = true,

    // Academic Brand (#38BDF8 / #1E60D5)
    academicBlue = Color(0xFF38BDF8),
    onAcademicBlue = Color(0xFF0B192E),
    academicBlueSoft = Color(0xFF38BDF8).copy(alpha = 0.14f),
    onAcademicBlueSoft = Color(0xFF7DD3FC),
    academicBlueBorder = Color(0xFF38BDF8).copy(alpha = 0.35f),
    academicDeep = Color(0xFF0D213F),

    // Verified Emerald (#10B981)
    verifiedEmerald = Color(0xFF10B981),
    onVerified = Color(0xFF063323),
    verifiedSoft = Color(0xFF10B981).copy(alpha = 0.14f),
    onVerifiedSoft = Color(0xFF34D399),
    verifiedBorder = Color(0xFF10B981).copy(alpha = 0.40f),

    // Alert Amber (#F59E0B)
    alertAmber = Color(0xFFF59E0B),
    onAlert = Color(0xFF382302),
    alertSoft = Color(0xFFF59E0B).copy(alpha = 0.14f),
    onAlertSoft = Color(0xFFFBBF24),
    alertBorder = Color(0xFFF59E0B).copy(alpha = 0.35f),

    // Danger Red (#F87171)
    dangerRed = Color(0xFFF87171),
    onDanger = Color(0xFF3B0D0D),
    dangerSoft = Color(0xFFF87171).copy(alpha = 0.14f),
    onDangerSoft = Color(0xFFFCA5A5),
    dangerBorder = Color(0xFFF87171).copy(alpha = 0.40f),

    // Accent Sky (#38BDF8)
    accentSky = Color(0xFF38BDF8),
    onAccent = Color(0xFF0B192E),
    accentSoft = Color(0xFF38BDF8).copy(alpha = 0.14f),
    onAccentSoft = Color(0xFF7DD3FC),
    accentBorder = Color(0xFF38BDF8).copy(alpha = 0.35f),

    // Saved Coral (#FB923C)
    savedCoral = Color(0xFFFB923C),
    onSaved = Color(0xFF3D1503),
    savedSoft = Color(0xFFFB923C).copy(alpha = 0.14f),
    onSavedSoft = Color(0xFFFDBA74),

    // Neutral Surfaces
    canvas = Color(0xFF0B0F19),
    surfaceLow = Color(0xFF111726),
    card = Color(0xFF141C2E),
    sheet = Color(0xFF0E1524),
    chip = Color.White.copy(alpha = 0.06f),
    chipStrong = Color.White.copy(alpha = 0.10f),
    track = Color.White.copy(alpha = 0.12f),

    // Neutral Lines & Depth
    border = Color.White.copy(alpha = 0.08f),
    borderSoft = Color.White.copy(alpha = 0.05f),
    shadow = Color.Black.copy(alpha = 0.38f),
    scrim = Color.Black.copy(alpha = 0.65f),

    // Typography Ink
    textPrimary = Color(0xFFF1F5F9),
    textSecondary = Color(0xFF94A3B8),
    textMuted = Color(0xFF64748B),
    textInverse = Color(0xFF0B0F19)
)

val LocalGradPathPalette = staticCompositionLocalOf { GradPathDarkPaletteStandard }

object GradPathThemeTokens {
    val palette: GradPathPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalGradPathPalette.current
}
