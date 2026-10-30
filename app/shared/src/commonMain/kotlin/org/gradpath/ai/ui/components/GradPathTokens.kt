package org.gradpath.ai.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * -----------------------------------------------------------------------------
 * GradPath Space Scale (Strict 4px Grid)
 * Derived from BrainSton's AppSpace standard.
 * -----------------------------------------------------------------------------
 */
object GradPathSpace {
    /** 2.dp — Hairline separation, tight icon-to-badge spacing */
    val xxs: Dp = 2.dp

    /** 4.dp — Tight pairing inside a single component */
    val xs: Dp = 4.dp

    /** 8.dp — Standard spacing between related items in a row or column */
    val sm: Dp = 8.dp

    /** 12.dp — Default gap between fields, cards, and list elements */
    val md: Dp = 12.dp

    /** 16.dp — Standard card padding, section body gaps, and gutters */
    val lg: Dp = 16.dp

    /** 20.dp — Generous card padding and gap above section headers */
    val xl: Dp = 20.dp

    /** 24.dp — Spacing between major standalone sections */
    val xxl: Dp = 24.dp

    /** 32.dp — Page top/bottom breathing room and empty state spacing */
    val xxxl: Dp = 32.dp
}

/**
 * Vertical spacer with explicit Dp measurement.
 */
@Composable
fun VGap(size: Dp, modifier: Modifier = Modifier) {
    Spacer(modifier = modifier.height(size))
}

/**
 * Horizontal spacer with explicit Dp measurement.
 */
@Composable
fun HGap(size: Dp, modifier: Modifier = Modifier) {
    Spacer(modifier = modifier.width(size))
}

/**
 * -----------------------------------------------------------------------------
 * GradPath Radii Scale
 * Derived from BrainSton's AppTokens radii hierarchy.
 * -----------------------------------------------------------------------------
 */
object GradPathRadii {
    /** 6.dp — Micro badges, small tags, and status dots */
    val rXs: Dp = 6.dp

    /** 10.dp — Compact buttons, thumbnail image corners, and chips */
    val rS: Dp = 10.dp

    /** 12.dp — Standard inputs, small cards, and icon tiles */
    val rMd: Dp = 12.dp

    /** 14.dp — Workhorse radius: search bars, form fields, and action plates */
    val rM: Dp = 14.dp

    /** 18.dp — Standard cards, frosted glass tiles, and modal dialogs */
    val rL: Dp = 18.dp

    /** 22.dp — Hero panels, elevated feature banners, and sheet headers */
    val rXl: Dp = 22.dp

    /** 999.dp — Stadium pills, circular avatars, and floating navigation docks */
    val rPill: Dp = 999.dp
}
