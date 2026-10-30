package org.gradpath.ai

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import gradpathai.app.shared.generated.resources.Res
import gradpathai.app.shared.generated.resources.hero_academic_bg
import kotlinx.coroutines.launch
import org.gradpath.ai.ui.components.*
import org.gradpath.ai.viewmodel.FeedViewModel
import org.jetbrains.compose.resources.painterResource

/**
 * -----------------------------------------------------------------------------
 * HomeScreen — The Executive Academic Dashboard
 *
 * Central command hub for GradPath AI:
 *   1. Academic Hero Banner with dynamic slide intelligence
 *   2. Telemetry Stat Bar (Cohorts, Top Match, Active Watchdogs, Deadlines)
 *   3. Recent Opportunity Alerts with Seamless Deep-Linking to Feed Workstation
 * -----------------------------------------------------------------------------
 */
@Composable
fun HomeScreen(
    isServerConnected: Boolean,
    feedViewModel: FeedViewModel = viewModel { FeedViewModel() },
    listState: LazyListState = rememberLazyListState(),
    onNavigateToFeed: (selectedOpportunityId: String?) -> Unit,
    onNavigateToStudio: ((String) -> Unit)? = null,
    onNavigateToWatchdogs: (() -> Unit)? = null,
    onNavigateToProfile: (() -> Unit)? = null,
    onNavigateToDiary: (() -> Unit)? = null,
    onNavigateToNotifications: (() -> Unit)? = null
) {
    val colors = GradPathTheme.colors
    val uriHandler = LocalUriHandler.current

    val feedItems by feedViewModel.feedItems.collectAsState()
    val isLoading by feedViewModel.isLoading.collectAsState()
    val bookmarkedIds by feedViewModel.bookmarkedIds.collectAsState()

    LaunchedEffect(Unit) {
        feedViewModel.fetchFeed(isServerConnected)
    }

    LaunchedEffect(isServerConnected) {
        if (isServerConnected) {
            feedViewModel.fetchFeed(isServerConnected)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
    ) {
        val isMobile = maxWidth < 768.dp
        val opportunityCardWidth = if (isMobile) (maxWidth - 52.dp).coerceIn(280.dp, 340.dp) else 330.dp

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // -------------------------------------------------------------
            // 1. HERO SECTION WITH DYNAMIC SLIDER INTELLIGENCE
            // -------------------------------------------------------------
            item {
                HomeHeroSectionWithSliderCards(
                    isMobile = isMobile,
                    feedItemsCount = feedItems.size,
                    onNavigateToFeed = onNavigateToFeed,
                    onNavigateToStudio = onNavigateToStudio,
                    onNavigateToWatchdogs = onNavigateToWatchdogs,
                    onNavigateToProfile = onNavigateToProfile,
                    onNavigateToDiary = onNavigateToDiary
                )
            }

            // -------------------------------------------------------------
            // 2. TELEMETRY STAT BAR & QUICK ACCESS WORKSTATION HUB
            // -------------------------------------------------------------
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (isMobile) 16.dp else 28.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Telemetry 4-Stat Overview
                    if (isMobile) {
                        GCard(
                            modifier = Modifier.fillMaxWidth(),
                            padding = PaddingValues(GradPathSpace.md)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    TelemetryStatItem(
                                        value = "${feedItems.size.coerceAtLeast(42)}",
                                        label = "Fellowships",
                                        sub = "Verified Calls",
                                        valueColor = GradPathThemeTokens.palette.textPrimary
                                    )
                                    TelemetryStatItem(
                                        value = "94%",
                                        label = "Top Calibration",
                                        sub = "MIT CSAIL",
                                        valueColor = GradPathThemeTokens.palette.academicBlue
                                    )
                                }
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(GradPathThemeTokens.palette.borderSoft))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    TelemetryStatItem(
                                        value = "6",
                                        label = "Active Portals",
                                        sub = "DAAD, NSF, MIT",
                                        valueColor = GradPathThemeTokens.palette.textPrimary
                                    )
                                    TelemetryStatItem(
                                        value = "8 Days",
                                        label = "Next Deadline",
                                        sub = "Gates Cambridge",
                                        valueColor = GradPathThemeTokens.palette.alertAmber
                                    )
                                }
                            }
                        }
                    } else {
                        // Desktop 4-stat single card row
                        GCard(
                            modifier = Modifier.fillMaxWidth(),
                            padding = PaddingValues(vertical = GradPathSpace.md, horizontal = GradPathSpace.md)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TelemetryStatItem(
                                    value = "${feedItems.size.coerceAtLeast(42)}",
                                    label = "Verified Fellowships",
                                    sub = "Across 18 Global Institutions",
                                    valueColor = GradPathThemeTokens.palette.textPrimary
                                )
                                Box(modifier = Modifier.width(1.dp).height(40.dp).background(GradPathThemeTokens.palette.borderSoft))
                                TelemetryStatItem(
                                    value = "94%",
                                    label = "Top Match Calibration",
                                    sub = "MIT CSAIL Intelligence",
                                    valueColor = GradPathThemeTokens.palette.academicBlue
                                )
                                Box(modifier = Modifier.width(1.dp).height(40.dp).background(GradPathThemeTokens.palette.borderSoft))
                                TelemetryStatItem(
                                    value = "6",
                                    label = "Active Portals",
                                    sub = "DAAD, NSF, MIT, Cambridge",
                                    valueColor = GradPathThemeTokens.palette.textPrimary
                                )
                                Box(modifier = Modifier.width(1.dp).height(40.dp).background(GradPathThemeTokens.palette.borderSoft))
                                TelemetryStatItem(
                                    value = "8 Days",
                                    label = "Next Priority Deadline",
                                    sub = "Gates Cambridge Trust",
                                    valueColor = GradPathThemeTokens.palette.alertAmber
                                )
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 3. RECENT OPPORTUNITIES STREAM PREVIEW (DEEP-LINK TO FEED)
            // -------------------------------------------------------------
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (isMobile) 16.dp else 28.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Opportunities",
                            fontSize = if (isMobile) 18.sp else 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface
                        )

                        // Prominent View All in Feed Action Button
                        Button(
                            onClick = { onNavigateToFeed(null) },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceLow),
                            border = BorderStroke(1.dp, colors.academicBlue.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                text = "View all",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.academicBlue
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "View Feed",
                                tint = colors.academicBlue,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    // Display top 6 Recent Opportunities as a Horizontal Sliding List
                    val previewItems = feedItems.take(6)
                    val cardWidth = opportunityCardWidth

                    if (isLoading && previewItems.isEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(3) {
                                GSkeletonOpportunityCard(
                                    modifier = Modifier.width(cardWidth),
                                    isCompact = true
                                )
                            }
                        }
                    } else if (previewItems.isEmpty()) {
                        GCard(
                            modifier = Modifier.fillMaxWidth().padding(vertical = GradPathSpace.md),
                            padding = PaddingValues(GradPathSpace.xl)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.School,
                                    contentDescription = "Empty",
                                    tint = GradPathThemeTokens.palette.textMuted,
                                    modifier = Modifier.size(40.dp)
                                )
                                VGap(GradPathSpace.sm)
                                Text(
                                    text = "Connecting to Autonomous Sweepers...",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GradPathThemeTokens.palette.textPrimary
                                )
                                VGap(GradPathSpace.xs)
                                Text(
                                    text = "Sweeper will populate doctoral and fellowship opportunities shortly",
                                    fontSize = 12.sp,
                                    color = GradPathThemeTokens.palette.textMuted
                                )
                            }
                        }
                    } else {
                        // Horizontal Sliding List of Compact Opportunity Cards
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(previewItems) { item ->
                                AcademicOpportunityCard(
                                    opportunity = item,
                                    isBookmarked = bookmarkedIds.contains(item.id),
                                    onBookmarkToggle = { feedViewModel.toggleBookmark(item.id) },
                                    isCompact = true,
                                    modifier = Modifier.width(cardWidth),
                                    onClick = {
                                        feedViewModel.selectItem(item)
                                        onNavigateToFeed(item.id)
                                    }
                                )
                            }
                        }
                    }

                    // Bottom Callout to explore full Feed Workstation
                    GCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        radius = GradPathRadii.rL,
                        padding = PaddingValues(GradPathSpace.lg),
                        onClick = { onNavigateToFeed(null) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.academicBlue.copy(alpha = 0.12f))
                                        .border(1.dp, colors.academicBlue.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.DynamicFeed,
                                        contentDescription = "Feed Workstation",
                                        tint = colors.academicBlue,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Explore All Opportunities",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.onSurface
                                    )
                                }
                            }

                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Enter Feed",
                                tint = colors.academicBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * -----------------------------------------------------------------------------
 * Data & View for Hero Slider Cards
 * -----------------------------------------------------------------------------
 */
data class HeroSliderCardData(
    val id: String,
    val eyebrow: String,
    val badge: String,
    val title: String,
    val description: String,
    val stat1Value: String,
    val stat1Label: String,
    val stat1Color: Color? = null,
    val stat2Value: String,
    val stat2Label: String,
    val stat2Color: Color? = null,
    val stat3Value: String,
    val stat3Label: String,
    val stat3Color: Color? = null,
    val primaryButtonText: String,
    val primaryButtonIcon: ImageVector,
    val onPrimaryClick: () -> Unit,
    val secondaryButtonText: String,
    val secondaryButtonIcon: ImageVector,
    val onSecondaryClick: () -> Unit
)

@Composable
private fun HeroSliderCardView(
    card: HeroSliderCardData,
    cardWidth: Dp
) {
    val palette = GradPathThemeTokens.palette

    GCard(
        radius = GradPathRadii.rXl,
        hasBorder = true,
        hasShadow = true,
        padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        modifier = Modifier
            .width(cardWidth)
            .heightIn(min = 188.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Eyebrow and Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GKicker(
                    text = card.eyebrow,
                    color = palette.academicBlue
                )
                GChip(
                    label = card.badge,
                    tone = GChipTone.Neutral,
                    dense = true
                )
            }

            // Description / pitch
            Text(
                text = card.description,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = palette.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // 3-Metric Mini Ledger
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(GradPathRadii.rS))
                    .background(palette.surfaceLow)
                    .border(BorderStroke(1.dp, palette.borderSoft), RoundedCornerShape(GradPathRadii.rS))
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = card.stat1Value,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = card.stat1Color ?: palette.textPrimary
                    )
                    Text(
                        text = card.stat1Label,
                        fontSize = 9.sp,
                        color = palette.textMuted
                    )
                }
                Box(modifier = Modifier.width(1.dp).height(20.dp).background(palette.borderSoft))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = card.stat2Value,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = card.stat2Color ?: palette.academicBlue
                    )
                    Text(
                        text = card.stat2Label,
                        fontSize = 9.sp,
                        color = palette.textMuted
                    )
                }
                Box(modifier = Modifier.width(1.dp).height(20.dp).background(palette.borderSoft))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = card.stat3Value,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = card.stat3Color ?: palette.alertAmber
                    )
                    Text(
                        text = card.stat3Label,
                        fontSize = 9.sp,
                        color = palette.textMuted
                    )
                }
            }

            // Dual Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GButton(
                    label = card.primaryButtonText,
                    onClick = card.onPrimaryClick,
                    variant = GBtnVariant.Primary,
                    size = GBtnSize.SM,
                    leadingIcon = {
                        Icon(
                            imageVector = card.primaryButtonIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )

                GButton(
                    label = card.secondaryButtonText,
                    onClick = card.onSecondaryClick,
                    variant = GBtnVariant.Secondary,
                    size = GBtnSize.SM,
                    leadingIcon = {
                        Icon(
                            imageVector = card.secondaryButtonIcon,
                            contentDescription = null,
                            tint = palette.academicBlue,
                            modifier = Modifier.size(13.dp)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * -----------------------------------------------------------------------------
 * Hero Section with Static Background & Floating Intelligence Cards
 * -----------------------------------------------------------------------------
 */
@Composable
private fun HomeHeroSectionWithSliderCards(
    isMobile: Boolean,
    feedItemsCount: Int,
    onNavigateToFeed: (String?) -> Unit,
    onNavigateToStudio: ((String) -> Unit)?,
    onNavigateToWatchdogs: (() -> Unit)?,
    onNavigateToProfile: (() -> Unit)?,
    onNavigateToDiary: (() -> Unit)?
) {
    val colors = GradPathTheme.colors
    val uriHandler = LocalUriHandler.current
    val sliderListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val currentSlideIndex by remember {
        derivedStateOf {
            sliderListState.firstVisibleItemIndex.coerceIn(0, 3)
        }
    }

    val cards = remember(feedItemsCount) {
        listOf(
            HeroSliderCardData(
                id = "candidate-intelligence",
                eyebrow = "WELCOME TO GRADPATH AI",
                badge = "GRA-884 · 94% MATCH",
                title = "Autonomous Academic Radar & Cohorts",
                description = "Access 42 active cohorts, track 6 watchdog channels, and calibrate your doctoral research applications.",
                stat1Value = "${feedItemsCount.coerceAtLeast(42)}",
                stat1Label = "Cohorts",
                stat2Value = "6",
                stat2Label = "Portals",
                stat2Color = Color(0xFF10B981),
                stat3Value = "8 Days",
                stat3Label = "Next Deadline",
                stat3Color = Color(0xFFF59E0B),
                primaryButtonText = "Explore Feed",
                primaryButtonIcon = Icons.Default.DynamicFeed,
                onPrimaryClick = { onNavigateToFeed(null) },
                secondaryButtonText = "AI Studio",
                secondaryButtonIcon = Icons.Default.SmartToy,
                onSecondaryClick = { onNavigateToStudio?.invoke("Help me find doctoral fellowships aligned with my profile") }
            ),
            HeroSliderCardData(
                id = "featured-fellowship",
                eyebrow = "TOP FELLOWSHIP MATCH",
                badge = "MIT CSAIL · 94% SYNERGY",
                title = "Generative Intelligence Doctoral Fellowship",
                description = "Full tuition waiver + $52,000/yr stipend. Calibrated for neuro-symbolic reasoning and foundation AI labs.",
                stat1Value = "$52k/yr",
                stat1Label = "Stipend",
                stat1Color = Color(0xFF38BDF8),
                stat2Value = "Dec 15",
                stat2Label = "Deadline",
                stat2Color = Color(0xFFF59E0B),
                stat3Value = "100%",
                stat3Label = "Tuition",
                stat3Color = Color(0xFF10B981),
                primaryButtonText = "View Call",
                primaryButtonIcon = Icons.Default.School,
                onPrimaryClick = { uriHandler.openUri("https://www.eecs.mit.edu/academics/graduate-programs/admission-process/") },
                secondaryButtonText = "Draft SOP",
                secondaryButtonIcon = Icons.Default.AutoAwesome,
                onSecondaryClick = { onNavigateToStudio?.invoke("Draft a research statement and SOP for MIT CSAIL Generative Intelligence Fellowship") }
            ),
            HeroSliderCardData(
                id = "daily-sweeper",
                eyebrow = "DAILY SWEEPER TELEMETRY",
                badge = "14 NEW CIRCULARS",
                title = "Global PhD & Fellowship Openings",
                description = "Automated sweep completed across MIT CSAIL, Max Planck, Cambridge Gates Trust, and Stanford Bio-X.",
                stat1Value = "14",
                stat1Label = "Circulars",
                stat1Color = Color(0xFF10B981),
                stat2Value = "4 Labs",
                stat2Label = "Global PIs",
                stat2Color = Color(0xFF38BDF8),
                stat3Value = "8:00 AM",
                stat3Label = "Sweep Cron",
                stat3Color = Color(0xFF94A3B8),
                primaryButtonText = "Portals",
                primaryButtonIcon = Icons.Default.TravelExplore,
                onPrimaryClick = { onNavigateToWatchdogs?.invoke() },
                secondaryButtonText = "Radar Config",
                secondaryButtonIcon = Icons.Default.Tune,
                onSecondaryClick = { onNavigateToWatchdogs?.invoke() }
            ),
            HeroSliderCardData(
                id = "readiness-audit",
                eyebrow = "PORTFOLIO READINESS AUDIT",
                badge = "SCORE: 88 / 100",
                title = "Doctoral Application Profile Calibrated",
                description = "Calibrated against your publication citations, GPA, and research statement. 7 faculty outreach threads active.",
                stat1Value = "88/100",
                stat1Label = "Readiness",
                stat1Color = Color(0xFF10B981),
                stat2Value = "7",
                stat2Label = "PI Threads",
                stat2Color = Color(0xFF38BDF8),
                stat3Value = "2",
                stat3Label = "In Review",
                stat3Color = Color(0xFFF59E0B),
                primaryButtonText = "Audit Score",
                primaryButtonIcon = Icons.Default.Verified,
                onPrimaryClick = { onNavigateToProfile?.invoke() },
                secondaryButtonText = "Diary Notes",
                secondaryButtonIcon = Icons.AutoMirrored.Filled.MenuBook,
                onSecondaryClick = { onNavigateToDiary?.invoke() }
            )
        )
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        // 1. Static Image Background Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isMobile) 330.dp else 345.dp)
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(Color(0xFF0F172A))
        ) {
            Image(
                painter = painterResource(Res.drawable.hero_academic_bg),
                contentDescription = "GradPath AI Academic Hero Background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // High-contrast gradient overlay ensuring readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0F172A).copy(alpha = 0.45f),
                                Color(0xFF0B1120).copy(alpha = 0.82f)
                            )
                        )
                    )
            )
        }

        // 2. Foreground content: Header, typography, and overlapping slider cards
        Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = if (isMobile) 16.dp else 28.dp,
                        end = if (isMobile) 16.dp else 28.dp,
                        top = if (isMobile) 14.dp else 24.dp,
                        bottom = 12.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Top Header Row (Mobile only: Desktop has top docked DesktopTopNavBar)
                if (isMobile) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Profile Avatar
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onNavigateToProfile?.invoke() }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .border(1.5.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Profile",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // AI Studio Pill Button
                        Button(
                            onClick = { onNavigateToStudio?.invoke("Open AI Studio") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E60D5),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AI Studio",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = "Welcome to GradPath AI",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f),
                    letterSpacing = 0.4.sp
                )

                Text(
                    text = buildAnnotatedString {
                        append("Where Scholarly Ambition\n")
                        withStyle(
                            SpanStyle(
                                fontStyle = FontStyle.Italic,
                                color = Color(0xFF38BDF8)
                            )
                        ) {
                            append("Meets")
                        }
                        append(" Machine Intelligence.")
                    },
                    fontSize = if (isMobile) 23.sp else 34.sp,
                    lineHeight = if (isMobile) 29.sp else 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = (-0.3).sp
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onNavigateToWatchdogs?.invoke() }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Explore Academic Lab Radar",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Floating Slider Cards Carousel
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val cardWidth = if (isMobile) (maxWidth - 44.dp).coerceIn(295.dp, 360.dp) else 340.dp

                LazyRow(
                    state = sliderListState,
                    contentPadding = PaddingValues(horizontal = if (isMobile) 16.dp else 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(cards) { card ->
                        HeroSliderCardView(
                            card = card,
                            cardWidth = cardWidth
                        )
                    }
                }
            }

            // 4. Pagination Dots Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                cards.forEachIndexed { index, _ ->
                    val isActive = currentSlideIndex == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(6.dp)
                            .width(if (isActive) 22.dp else 6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isActive) colors.academicBlue else colors.outline.copy(alpha = 0.5f))
                            .clickable {
                                coroutineScope.launch {
                                    sliderListState.animateScrollToItem(index)
                                }
                            }
                    )
                }
            }
        }
    }
}

