package org.gradpath.ai.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * -----------------------------------------------------------------------------
 * GChip Semantic Tones
 * Adapting BrainSton's [BChipTone] system.
 * -----------------------------------------------------------------------------
 */
enum class GChipTone {
    Neutral, // Inactive pill on card surface
    Brand,   // Solid Academic Blue
    Soft,    // Tinted Academic Blue surface
    Coral,   // Tinted Coral/Saved surface
    Warn,    // Tinted Amber/Deadline surface
    Ok,      // Tinted Emerald/Verified surface
    Ghost    // Transparent with subtle border hairline
}

/**
 * Enterprise Pill Chip / Tag adapting BrainSton's [BChip].
 */
@Composable
fun GChip(
    label: String,
    modifier: Modifier = Modifier,
    tone: GChipTone = GChipTone.Neutral,
    active: Boolean = false,
    dense: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val palette = GradPathThemeTokens.palette
    val shape = RoundedCornerShape(GradPathRadii.rPill)

    val (bgColor, fgColor, borderStroke) = when (tone) {
        GChipTone.Brand -> Triple(
            palette.academicBlue,
            palette.onAcademicBlue,
            null
        )
        GChipTone.Soft -> Triple(
            palette.academicBlueSoft,
            palette.onAcademicBlueSoft,
            BorderStroke(1.dp, palette.academicBlueBorder)
        )
        GChipTone.Coral -> Triple(
            palette.savedSoft,
            palette.onSavedSoft,
            null
        )
        GChipTone.Warn -> Triple(
            palette.alertSoft,
            palette.onAlertSoft,
            BorderStroke(1.dp, palette.alertBorder)
        )
        GChipTone.Ok -> Triple(
            palette.verifiedSoft,
            palette.onVerifiedSoft,
            BorderStroke(1.dp, palette.verifiedBorder)
        )
        GChipTone.Ghost -> Triple(
            Color.Transparent,
            palette.textSecondary,
            BorderStroke(1.dp, palette.border)
        )
        GChipTone.Neutral -> if (active) {
            Triple(
                palette.academicBlue,
                palette.onAcademicBlue,
                null
            )
        } else {
            Triple(
                palette.chip,
                palette.textSecondary,
                BorderStroke(1.dp, palette.borderSoft)
            )
        }
    }

    val padH = if (dense) GradPathSpace.sm else GradPathSpace.md
    val padV = if (dense) 3.dp else 5.dp
    val fontSize = if (dense) 11.sp else 12.5.sp

    Box(
        modifier = modifier
            .then(
                if (onClick != null) {
                    Modifier.gPressScale(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(bgColor)
            .then(
                if (borderStroke != null) {
                    Modifier.border(borderStroke, shape)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = padH, vertical = padV),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                leadingIcon()
                HGap(GradPathSpace.xs)
            }
            Text(
                text = label,
                fontSize = fontSize,
                fontWeight = FontWeight.Medium,
                color = fgColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Notification / Live Count Badge adapting BrainSton's [_CountBadge].
 */
@Composable
fun GCountBadge(
    count: Int,
    modifier: Modifier = Modifier,
    color: Color? = null,
    textColor: Color? = null
) {
    if (count <= 0) return

    val palette = GradPathThemeTokens.palette
    val bg = color ?: palette.alertAmber
    val fg = textColor ?: palette.onAlert

    Box(
        modifier = modifier
            .height(18.dp)
            .clip(RoundedCornerShape(GradPathRadii.rPill))
            .background(bg)
            .border(BorderStroke(1.5.dp, palette.card), RoundedCornerShape(GradPathRadii.rPill))
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 99) "99+" else "$count",
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = fg,
            lineHeight = 10.sp
        )
    }
}

/**
 * Text badge overload (e.g. for badges like "88", "6", "NEW")
 */
@Composable
fun GCountBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color? = null,
    textColor: Color? = null
) {
    if (text.isBlank()) return

    val palette = GradPathThemeTokens.palette
    val bg = color ?: palette.alertAmber
    val fg = textColor ?: palette.onAlert

    Box(
        modifier = modifier
            .height(18.dp)
            .clip(RoundedCornerShape(GradPathRadii.rPill))
            .background(bg)
            .border(BorderStroke(1.5.dp, palette.card), RoundedCornerShape(GradPathRadii.rPill))
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = fg,
            lineHeight = 10.sp
        )
    }
}
