package org.gradpath.ai.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * -----------------------------------------------------------------------------
 * GCard — Soft Surface Card with Hairline Sheen
 * Faithfully adapting BrainSton's [BCard] architecture.
 *
 * Provides a clean surface, subtle hairline border, optional lift shadow,
 * and integrated bouncy [gPressScale] feedback when [onClick] is provided.
 * -----------------------------------------------------------------------------
 */
@Composable
fun GCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(GradPathSpace.lg),
    color: Color? = null,
    radius: Dp = GradPathRadii.rL,
    hasBorder: Boolean = true,
    hasShadow: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val palette = GradPathThemeTokens.palette
    val shape = RoundedCornerShape(radius)
    val cardBg = color ?: palette.card
    val borderStroke = if (hasBorder) BorderStroke(1.dp, palette.borderSoft) else null

    val shadowModifier = if (hasShadow) {
        Modifier.shadow(
            elevation = if (palette.isDark) 6.dp else 4.dp,
            shape = shape,
            ambientColor = palette.shadow,
            spotColor = palette.shadow
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(shadowModifier)
            .then(
                if (onClick != null) {
                    Modifier.gPressScale(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(cardBg)
            .then(
                if (borderStroke != null) {
                    Modifier.border(borderStroke, shape)
                } else {
                    Modifier
                }
            )
            .padding(padding)
    ) {
        content()
    }
}

/**
 * Frosted Glass Card adapting BrainSton's [GlassCard].
 * Uses a high-alpha surface with a light sheen border.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    radius: Dp = GradPathRadii.rL,
    padding: PaddingValues = PaddingValues(GradPathSpace.lg),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val palette = GradPathThemeTokens.palette
    val shape = RoundedCornerShape(radius)

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
            .background(palette.glassFill)
            .border(BorderStroke(1.dp, palette.glassBorder), shape)
            .padding(padding)
    ) {
        content()
    }
}

/**
 * Deep Academic Gradient Card adapting BrainSton's [DeepGradientCard].
 * Used for high-impact highlighted calls to action and priority research tasks.
 */
@Composable
fun DeepGradientCard(
    modifier: Modifier = Modifier,
    radius: Dp = GradPathRadii.rL,
    gradient: Brush? = null,
    padding: PaddingValues = PaddingValues(GradPathSpace.lg),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val palette = GradPathThemeTokens.palette
    val shape = RoundedCornerShape(radius)
    val brush = gradient ?: palette.heroGradient

    Box(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = shape,
                ambientColor = palette.shadow,
                spotColor = palette.shadow
            )
            .then(
                if (onClick != null) {
                    Modifier.gPressScale(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(brush)
            .padding(padding)
    ) {
        content()
    }
}

/**
 * Amber Alert Card adapting BrainSton's [AmberCard].
 * Used for urgent deadline warnings and priority admission notices.
 */
@Composable
fun AmberAlertCard(
    modifier: Modifier = Modifier,
    radius: Dp = GradPathRadii.rL,
    padding: PaddingValues = PaddingValues(GradPathSpace.lg),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val palette = GradPathThemeTokens.palette
    val shape = RoundedCornerShape(radius)

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
            .background(palette.alertSoft)
            .border(BorderStroke(1.dp, palette.alertBorder), shape)
            .padding(padding)
    ) {
        content()
    }
}
