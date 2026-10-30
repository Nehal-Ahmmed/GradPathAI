package org.gradpath.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.gradpath.ai.FundingType
import org.gradpath.ai.ResearchPillar
import org.gradpath.ai.SourceConfidence
import org.gradpath.ai.WatchdogOpportunity

/**
 * -----------------------------------------------------------------------------
 * AcademicOpportunityCard — Premium Boxed Opportunity Card
 * Faithfully adapting BrainSton's [CourseItemCard] & [ArticleCard] standard.
 *
 * Features:
 *   - Aspect ratio hero banner with institutional monogram fallback & scrim overlay
 *   - Overlapping PI/Faculty avatar stack with circular border cutouts
 *   - 5-slot role-based funding and deadline badges
 *   - Verified source shield indicator
 *   - Integrated spring-back [gPressScale] feedback
 * -----------------------------------------------------------------------------
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AcademicOpportunityCard(
    opportunity: WatchdogOpportunity,
    isBookmarked: Boolean,
    onBookmarkToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    onDiscussInStudio: (() -> Unit)? = null,
    onOpenUrl: ((String) -> Unit)? = null
) {
    val palette = GradPathThemeTokens.palette

    GCard(
        modifier = modifier.fillMaxWidth(),
        radius = GradPathRadii.rL,
        hasBorder = true,
        hasShadow = true,
        padding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // -------------------------------------------------------------
            // 1. BANNER & OVERLAYS
            // -------------------------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 7.2f)
                    .clip(RoundedCornerShape(topStart = GradPathRadii.rL, topEnd = GradPathRadii.rL))
                    .background(palette.heroGradient)
            ) {
                // Diagonal texture lines or university initials monogram
                Box(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = opportunity.university.take(3).uppercase(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = Color.White.copy(alpha = 0.22f)
                        )
                        VGap(GradPathSpace.xs)
                        Text(
                            text = opportunity.university,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = GradPathSpace.lg)
                        )
                    }
                }

                // Top-Left Source Confidence Pill
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(GradPathSpace.md)
                ) {
                    val isVerified = opportunity.confidence == SourceConfidence.VERIFIED_OFFICIAL
                    GChip(
                        label = if (isVerified) "Verified Official" else "Needs Review",
                        tone = if (isVerified) GChipTone.Ok else GChipTone.Warn,
                        dense = true,
                        leadingIcon = {
                            Icon(
                                imageVector = if (isVerified) Icons.Default.Verified else Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = if (isVerified) palette.onVerifiedSoft else palette.onAlertSoft
                            )
                        }
                    )
                }

                // Top-Right Bookmark Button
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(GradPathSpace.md)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(palette.card.copy(alpha = 0.92f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onBookmarkToggle
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) palette.savedCoral else palette.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // 2. BODY CONTENT
            // -------------------------------------------------------------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(GradPathSpace.lg)
            ) {
                // Degree track, Pillar & tags row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(GradPathSpace.xs)
                ) {
                    val (pillarLabel, pillarTone) = when (opportunity.pillar) {
                        ResearchPillar.TOP_200_ADMISSION -> "🏛️ Top 200" to GChipTone.Brand
                        ResearchPillar.SCHOLARSHIP_FUNDING -> "💰 Funding" to GChipTone.Ok
                        ResearchPillar.COUNTRY_CAREER_INTELLIGENCE -> "🌐 Career & Visa" to GChipTone.Warn
                        ResearchPillar.CAREER_ACTION_PLAN -> "🎯 Action Plan" to GChipTone.Soft
                    }
                    GChip(
                        label = pillarLabel,
                        tone = pillarTone,
                        dense = true
                    )

                    GChip(
                        label = opportunity.degreeTrack,
                        tone = GChipTone.Ghost,
                        dense = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = palette.textSecondary
                            )
                        }
                    )

                    if (opportunity.matchScore > 0) {
                        GChip(
                            label = "${opportunity.matchScore}% Match",
                            tone = GChipTone.Ghost,
                            dense = true
                        )
                    }

                    opportunity.tags.take(1).forEach { tag ->
                        GChip(
                            label = tag.removePrefix("#"),
                            tone = GChipTone.Neutral,
                            dense = true
                        )
                    }
                }

                VGap(GradPathSpace.md)

                // Title
                Text(
                    text = opportunity.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    lineHeight = 22.sp,
                    letterSpacing = (-0.2).sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                VGap(GradPathSpace.xs)

                // University & Location Kicker
                GKicker(
                    text = "${opportunity.university} · ${opportunity.country}"
                )

                VGap(GradPathSpace.md)

                // Research Summary Excerpt
                Text(
                    text = opportunity.summary,
                    fontSize = 13.5.sp,
                    color = palette.textSecondary,
                    lineHeight = 19.sp,
                    maxLines = if (isCompact) 2 else 3,
                    overflow = TextOverflow.Ellipsis
                )

                if (!isCompact) {
                    VGap(GradPathSpace.lg)

                    // PI / Mentor Row with Overlapping Avatars
                    _PIMentorBlock(
                        institution = opportunity.university,
                        source = opportunity.sourceName
                    )

                    VGap(GradPathSpace.lg)

                    // Divider line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(palette.borderSoft)
                    )

                    VGap(GradPathSpace.md)

                    // -------------------------------------------------------------
                    // 3. FOOTER TELEMETRY & ACTIONS
                    // -------------------------------------------------------------
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Funding Tag
                        val fundingLabel = when (opportunity.fundingType) {
                            FundingType.FULL_FUNDING -> "100% Fully Funded"
                            FundingType.TA_RA -> "Graduate TA/RA"
                            FundingType.TUITION_WAIVER -> "Tuition Waiver"
                            FundingType.PARTIAL -> "Partial Grant"
                            FundingType.INDUSTRY_FUNDED -> "Industry Sponsored"
                        }

                        GChip(
                            label = fundingLabel,
                            tone = GChipTone.Soft,
                            dense = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Paid,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = palette.academicBlue
                                )
                            }
                        )

                        HGap(GradPathSpace.xs)

                        // Deadline Badge
                        GChip(
                            label = opportunity.deadlineDate,
                            tone = GChipTone.Warn,
                            dense = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = palette.onAlertSoft
                                )
                            }
                        )

                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * Institutional PI Mentor Block with overlapping avatar cuts.
 * Adapting BrainSton's [_MentorBlock] & [_MentorAvatars].
 */
@Composable
private fun _PIMentorBlock(
    institution: String,
    source: String
) {
    val palette = GradPathThemeTokens.palette

    Row(verticalAlignment = Alignment.CenterVertically) {
        // Overlapping Avatars Stack
        Box(modifier = Modifier.width(62.dp).height(36.dp)) {
            GAvatar(
                name = institution,
                size = 34.dp,
                hasRing = true,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            GAvatar(
                name = source,
                size = 34.dp,
                hasRing = true,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        HGap(GradPathSpace.sm)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$institution Faculty & PI Group",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            VGap(GradPathSpace.xxs)
            Text(
                text = "Tracked via $source",
                fontSize = 11.5.sp,
                color = palette.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
