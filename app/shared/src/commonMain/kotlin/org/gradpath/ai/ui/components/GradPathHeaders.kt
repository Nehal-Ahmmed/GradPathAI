package org.gradpath.ai.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * -----------------------------------------------------------------------------
 * GKicker — Mono Uppercase Eyebrow Label
 * Adapting BrainSton's [BKicker].
 * -----------------------------------------------------------------------------
 */
@Composable
fun GKicker(
    text: String,
    modifier: Modifier = Modifier,
    color: Color? = null
) {
    val palette = GradPathThemeTokens.palette
    val fg = color ?: palette.textMuted

    Text(
        text = text.uppercase(),
        modifier = modifier,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 1.1.sp,
        color = fg,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

/**
 * Section Heading with optional kicker and trailing action.
 * Adapting BrainSton's [BSectionHead].
 */
@Composable
fun GSectionHead(
    title: String,
    modifier: Modifier = Modifier,
    kicker: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val palette = GradPathThemeTokens.palette

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (kicker != null) {
                GKicker(text = kicker)
                VGap(GradPathSpace.xs)
            }
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
                letterSpacing = (-0.3).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (actionLabel != null && onAction != null) {
            GSeeAll(label = actionLabel, onClick = onAction)
        }
    }
}

/**
 * Standardized Page Header Block.
 * Adapting BrainSton's [BPageHeader].
 */
@Composable
fun GPageHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String = "",
    centered: Boolean = false,
    trailing: (@Composable () -> Unit)? = null
) {
    val palette = GradPathThemeTokens.palette

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start
    ) {
        if (trailing != null && !centered) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.textPrimary,
                    letterSpacing = (-0.4).sp,
                    modifier = Modifier.weight(1f)
                )
                HGap(GradPathSpace.md)
                trailing()
            }
        } else {
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = palette.textPrimary,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                letterSpacing = (-0.4).sp
            )
        }

        if (subtitle.isNotEmpty()) {
            VGap(GradPathSpace.xs)
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = palette.textSecondary,
                lineHeight = 20.sp,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start
            )
        }
    }
}

/**
 * Deterministic Initial Avatar adapting BrainSton's [BAvatar].
 */
private val AVATAR_PALETTE = listOf(
    Color(0xFF0F766E), // Teal
    Color(0xFF1D4ED8), // Blue
    Color(0xFFB45309), // Amber
    Color(0xFF6D28D9), // Violet
    Color(0xFF047857), // Emerald
    Color(0xFFBE123C)  // Crimson
)

@Composable
fun GAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    hasRing: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val palette = GradPathThemeTokens.palette
    val clean = name.trim().ifEmpty { "?" }
    val parts = clean.split(Regex("\\s+"))
    val initials = parts.take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
    val charCode = clean.first().code + if (clean.length > 1) clean[1].code else 0
    val bg = AVATAR_PALETTE[charCode % AVATAR_PALETTE.size]

    val clickMod = if (onClick != null) {
        Modifier.gPressScale(onClick = onClick)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(clickMod)
            .size(size)
            .clip(CircleShape)
            .background(bg)
            .then(
                if (hasRing) {
                    Modifier.border(2.dp, palette.card, CircleShape)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            fontSize = (size.value * 0.38f).sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

/**
 * Diagonal Stripe Canvas Painter Fallback adapting BrainSton's [BStripePlaceholder].
 * Renders an elegant textured canvas placeholder when an image is missing.
 */
@Composable
fun GStripePlaceholder(
    modifier: Modifier = Modifier,
    height: Dp = 150.dp,
    label: String? = null,
    radius: Dp = GradPathRadii.rL
) {
    val palette = GradPathThemeTokens.palette
    val isDark = palette.isDark

    val (bg1, bg2, ink) = if (isDark) {
        Triple(Color(0xFF141F30), Color(0xFF1A293E), Color(0xFF64748B))
    } else {
        Triple(Color(0xFFE2E8F0), Color(0xFFCBD5E1), Color(0xFF475569))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(radius))
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRect(color = bg1)
            val stripeWidth = 16f
            val diagonalSpan = size.width + size.height

            var x = -size.height
            while (x < size.width) {
                val path = Path().apply {
                    moveTo(x, 0f)
                    lineTo(x + stripeWidth, 0f)
                    lineTo(x + stripeWidth + size.height, size.height)
                    lineTo(x + size.height, size.height)
                    close()
                }
                drawPath(path, color = bg2)
                x += stripeWidth * 2
            }
        }

        if (label != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(GradPathSpace.sm)
            ) {
                GKicker(text = label, color = ink)
            }
        }
    }
}
