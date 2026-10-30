package org.gradpath.ai.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * -----------------------------------------------------------------------------
 * GradPath Hardware-Accelerated Shimmer System
 * Adapting BrainSton's [Shimmer] and [Bone] architecture.
 *
 * Provides a single coordinated infinite light sweep across all placeholder shapes.
 * -----------------------------------------------------------------------------
 */

val LocalGradPathShimmerBrush = staticCompositionLocalOf<Brush?> { null }

@Composable
fun rememberGradPathShimmerBrush(): Brush {
    val palette = GradPathThemeTokens.palette
    val isDark = palette.isDark

    val baseColor = if (isDark) Color(0xFF1E2838) else Color(0xFFE2E8F0)
    val highlightColor = if (isDark) Color(0xFF2E3E56) else Color(0xFFF1F5F9)

    val transition = rememberInfiniteTransition(label = "GradPathShimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = -800f,
        targetValue = 1600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "GradPathShimmerTranslate"
    )

    return Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(translateAnim - 400f, 0f),
        end = Offset(translateAnim + 400f, 0f)
    )
}

/**
 * Wraps content with a synchronized shimmer brush.
 */
@Composable
fun GShimmerScope(content: @Composable () -> Unit) {
    val brush = rememberGradPathShimmerBrush()
    CompositionLocalProvider(LocalGradPathShimmerBrush provides brush) {
        content()
    }
}

/**
 * Placeholder Bone shapes that draw the shimmering sweep.
 */
object GBone {

    @Composable
    fun Box(
        modifier: Modifier = Modifier,
        width: Dp? = null,
        height: Dp? = null,
        cornerRadius: Dp = GradPathRadii.rMd
    ) {
        val brush = LocalGradPathShimmerBrush.current ?: rememberGradPathShimmerBrush()
        val mod = modifier
            .then(if (width != null) Modifier.width(width) else Modifier)
            .then(if (height != null) Modifier.height(height) else Modifier)
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush)

        androidx.compose.foundation.layout.Box(modifier = mod)
    }

    @Composable
    fun Line(
        modifier: Modifier = Modifier,
        widthFraction: Float = 1.0f,
        height: Dp = 12.dp,
        cornerRadius: Dp = GradPathRadii.rXs
    ) {
        val brush = LocalGradPathShimmerBrush.current ?: rememberGradPathShimmerBrush()
        androidx.compose.foundation.layout.Box(
            modifier = modifier
                .fillMaxWidth(widthFraction)
                .height(height)
                .clip(RoundedCornerShape(cornerRadius))
                .background(brush)
        )
    }

    @Composable
    fun Circle(
        modifier: Modifier = Modifier,
        size: Dp = 38.dp
    ) {
        val brush = LocalGradPathShimmerBrush.current ?: rememberGradPathShimmerBrush()
        androidx.compose.foundation.layout.Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(brush)
        )
    }
}

/**
 * Prebuilt composite skeleton placeholder for an Opportunity Card.
 */
@Composable
fun GSkeletonOpportunityCard(
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    val palette = GradPathThemeTokens.palette

    GShimmerScope {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(GradPathRadii.rL))
                .background(palette.card)
        ) {
            Column {
                // Banner bone
                GBone.Box(
                    modifier = Modifier.fillMaxWidth().height(if (isCompact) 110.dp else 160.dp),
                    cornerRadius = 0.dp
                )

                // Body content
                Column(
                    modifier = Modifier.padding(GradPathSpace.lg)
                ) {
                    // Tag chip bone
                    GBone.Box(width = 110.dp, height = 24.dp, cornerRadius = GradPathRadii.rPill)
                    VGap(GradPathSpace.md)

                    // Title lines
                    GBone.Line(widthFraction = 0.85f, height = 18.dp)
                    VGap(GradPathSpace.xs)
                    GBone.Line(widthFraction = 0.55f, height = 18.dp)
                    VGap(GradPathSpace.md)

                    // Summary line
                    GBone.Line(widthFraction = 1.0f, height = 12.dp)
                    VGap(GradPathSpace.xs)
                    GBone.Line(widthFraction = 0.70f, height = 12.dp)

                    if (!isCompact) {
                        VGap(GradPathSpace.lg)

                        // PI Mentor Row
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GBone.Circle(size = 36.dp)
                            HGap(GradPathSpace.sm)
                            Column {
                                GBone.Line(widthFraction = 0.40f, height = 14.dp)
                                VGap(GradPathSpace.xs)
                                GBone.Line(widthFraction = 0.25f, height = 10.dp)
                            }
                        }
                        VGap(GradPathSpace.md)

                        // Footer funding & deadline bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GBone.Box(width = 140.dp, height = 20.dp, cornerRadius = GradPathRadii.rXs)
                            Spacer(modifier = Modifier.weight(1f))
                            GBone.Box(width = 80.dp, height = 20.dp, cornerRadius = GradPathRadii.rXs)
                        }
                    }
                }
            }
        }
    }
}
