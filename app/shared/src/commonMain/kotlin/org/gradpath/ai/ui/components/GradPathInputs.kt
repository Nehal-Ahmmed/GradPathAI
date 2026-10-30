package org.gradpath.ai.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * -----------------------------------------------------------------------------
 * GSearchBar — Pill Search Bar
 * Adapting BrainSton's [BSearchBar].
 * -----------------------------------------------------------------------------
 */
@Composable
fun GSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search fellowship, PI, lab, university...",
    onClear: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val palette = GradPathThemeTokens.palette
    val shape = RoundedCornerShape(GradPathRadii.rPill)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(palette.card)
            .border(BorderStroke(1.dp, palette.border), shape)
            .padding(horizontal = GradPathSpace.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = palette.textMuted,
            modifier = Modifier.size(20.dp)
        )
        HGap(GradPathSpace.sm)

        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = placeholder,
                    color = palette.textMuted,
                    fontSize = 14.sp
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = palette.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                ),
                cursorBrush = SolidColor(palette.academicBlue),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (query.isNotEmpty() && onClear != null) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear",
                    tint = palette.textMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (trailing != null) {
            trailing()
        }
    }
}

/**
 * Labelled Input Field adapting BrainSton's [BInput].
 */
@Composable
fun GInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "",
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true
) {
    val palette = GradPathThemeTokens.palette
    val shape = RoundedCornerShape(GradPathRadii.rM)

    Column(modifier = modifier.fillMaxWidth()) {
        if (label != null) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = palette.textSecondary
            )
            VGap(GradPathSpace.xs)
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, fontSize = 14.sp, color = palette.textMuted) },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            singleLine = singleLine,
            shape = shape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = palette.card,
                unfocusedContainerColor = palette.card,
                focusedBorderColor = palette.academicBlue,
                unfocusedBorderColor = palette.border,
                focusedTextColor = palette.textPrimary,
                unfocusedTextColor = palette.textPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Segmented Pill Tabs adapting BrainSton's [BTabPill].
 */
@Composable
fun GTabPill(
    tabs: List<String>,
    selectedIndex: Int,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = GradPathThemeTokens.palette
    val outerShape = RoundedCornerShape(GradPathRadii.rPill)

    Box(
        modifier = modifier
            .clip(outerShape)
            .background(palette.chip)
            .padding(GradPathSpace.xs)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = index == selectedIndex
                val pillShape = RoundedCornerShape(GradPathRadii.rPill)

                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) palette.card else Color.Transparent,
                    animationSpec = tween(160, easing = FastOutSlowInEasing),
                    label = "TabPillBg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) palette.textPrimary else palette.textMuted,
                    animationSpec = tween(160, easing = FastOutSlowInEasing),
                    label = "TabPillText"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .then(
                            if (isSelected) {
                                Modifier.shadow(2.dp, pillShape)
                            } else {
                                Modifier
                            }
                        )
                        .clip(pillShape)
                        .background(bgColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelectTab(index) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        fontSize = 12.5.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = textColor
                    )
                }
            }
        }
    }
}
