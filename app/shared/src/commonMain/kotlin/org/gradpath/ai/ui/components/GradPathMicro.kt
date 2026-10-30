package org.gradpath.ai.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale

/**
 * -----------------------------------------------------------------------------
 * GPressScale — Bouncy Spring-Back Touch Feedback
 * Faithfully adapting BrainSton's [_PressScale] mechanism.
 *
 * Provides a crisp, tactile 0.94x press-in that springs back with subtle bounce
 * instead of a flat rectangular ripple.
 * -----------------------------------------------------------------------------
 */

@Composable
fun Modifier.gPressScale(
    enabled: Boolean = true,
    pressedScale: Float = 0.94f,
    onClick: (() -> Unit)? = null
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1.0f,
        animationSpec = if (isPressed) {
            tween(durationMillis = 90)
        } else {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        },
        label = "GPressScale"
    )

    return this
        .scale(scale)
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null, // Disable default intrusive square ripple
                    enabled = enabled,
                    onClick = onClick
                )
            } else {
                Modifier
            }
        )
}

@Composable
fun GPressScale(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    pressedScale: Float = 0.94f,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.gPressScale(
            enabled = enabled,
            pressedScale = pressedScale,
            onClick = onClick
        )
    ) {
        content()
    }
}
