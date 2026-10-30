package org.gradpath.ai.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
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
 * GButton Variants & Sizes
 * Adapting BrainSton's [BBtnVariant] and [BBtnSize] system.
 * -----------------------------------------------------------------------------
 */
enum class GBtnVariant {
    Primary,   // Solid Academic Blue
    Secondary, // Clean Card Surface with Hairline Border
    Ghost,     // Transparent with Hover Sheen
    Soft,      // Tinted Soft Background
    Danger,    // Solid Danger Red
    Dark       // High-contrast Ink surface (Dark in light mode, Light in dark mode)
}

enum class GBtnSize(
    val height: Dp,
    val fontSize: Int,
    val horizontalPadding: Dp,
    val iconSize: Dp
) {
    XS(height = 28.dp, fontSize = 11, horizontalPadding = 10.dp, iconSize = 13.dp),
    SM(height = 34.dp, fontSize = 12, horizontalPadding = 14.dp, iconSize = 15.dp),
    MD(height = 42.dp, fontSize = 13, horizontalPadding = 18.dp, iconSize = 17.dp),
    LG(height = 48.dp, fontSize = 14, horizontalPadding = 22.dp, iconSize = 19.dp)
}

/**
 * Enterprise Stadium Pill Button with tactile bouncy press feedback.
 */
@Composable
fun GButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: GBtnVariant = GBtnVariant.Primary,
    size: GBtnSize = GBtnSize.MD,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    fullWidth: Boolean = false,
    enabled: Boolean = true
) {
    val palette = GradPathThemeTokens.palette
    val shape = RoundedCornerShape(GradPathRadii.rPill)

    val (bgColor, fgColor, borderStroke) = when (variant) {
        GBtnVariant.Primary -> Triple(
            palette.academicBlue,
            palette.onAcademicBlue,
            null
        )
        GBtnVariant.Secondary -> Triple(
            palette.card,
            palette.textPrimary,
            BorderStroke(1.dp, palette.border)
        )
        GBtnVariant.Ghost -> Triple(
            Color.Transparent,
            palette.textPrimary,
            null
        )
        GBtnVariant.Soft -> Triple(
            palette.academicBlueSoft,
            palette.onAcademicBlueSoft,
            BorderStroke(1.dp, palette.academicBlueBorder)
        )
        GBtnVariant.Danger -> Triple(
            palette.dangerRed,
            palette.onDanger,
            null
        )
        GBtnVariant.Dark -> Triple(
            palette.textPrimary,
            palette.textInverse,
            null
        )
    }

    Box(
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .height(size.height)
            .gPressScale(enabled = enabled, onClick = onClick)
            .clip(shape)
            .background(if (enabled) bgColor else bgColor.copy(alpha = 0.5f))
            .then(
                if (borderStroke != null) {
                    Modifier.border(borderStroke, shape)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = size.horizontalPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                HGap(GradPathSpace.xs)
            }
            Text(
                text = label,
                fontSize = size.fontSize.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) fgColor else fgColor.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (trailingIcon != null) {
                HGap(GradPathSpace.xs)
                trailingIcon()
            }
        }
    }
}

/**
 * Secondary Navigation Link Pill adapting BrainSton's [BSeeAll].
 *
 * Uses ink styling ([GBtnVariant.Dark]) so it sits beside headings
 * without competing with the screen's main CTA.
 */
@Composable
fun GSeeAll(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hasChevron: Boolean = true,
    variant: GBtnVariant = GBtnVariant.Dark
) {
    val palette = GradPathThemeTokens.palette

    GButton(
        label = label,
        onClick = onClick,
        modifier = modifier,
        variant = variant,
        size = GBtnSize.XS,
        trailingIcon = if (hasChevron) {
            {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
            }
        } else null
    )
}
