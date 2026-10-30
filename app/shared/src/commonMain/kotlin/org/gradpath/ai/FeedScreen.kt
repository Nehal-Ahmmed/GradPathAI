package org.gradpath.ai

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import gradpathai.app.shared.generated.resources.Res
import gradpathai.app.shared.generated.resources.hero_academic_bg
import kotlinx.coroutines.launch
import org.gradpath.ai.viewmodel.FeedViewModel
import org.jetbrains.compose.resources.painterResource
import org.gradpath.ai.ui.components.*

// Featured fellowship data structure for carousel
data class FeaturedFellowship(
    val id: String,
    val institution: String,
    val mon: String,
    val title: String,
    val funding: String,
    val deadline: String,
    val matchScore: String,
    val url: String
)

val FEATURED_FELLOWSHIPS = listOf(
    FeaturedFellowship(
        id = "mit-csail-feat",
        institution = "MIT · CSAIL Intelligence Group",
        mon = "M",
        title = "Generative Intelligence Doctoral Fellowship",
        funding = "$52,000/yr + 100% Tuition Waiver",
        deadline = "Dec 15, 2026",
        matchScore = "94%",
        url = "https://www.eecs.mit.edu/academics/graduate-programs/admission-process/"
    ),
    FeaturedFellowship(
        id = "max-planck-feat",
        institution = "Max Planck Institute for Informatics",
        mon = "MP",
        title = "Neuro-Symbolic Reasoning Research Grant",
        funding = "Full TVöD E13 Stipend (100% Funded)",
        deadline = "Jan 10, 2027",
        matchScore = "92%",
        url = "https://www.mpi-inf.mpg.de/"
    ),
    FeaturedFellowship(
        id = "cambridge-gates-feat",
        institution = "University of Cambridge · Gates Trust",
        mon = "G",
        title = "Gates Cambridge International PhD Fellowship",
        funding = "Full Tuition + £20,000/yr Living Allowance",
        deadline = "Dec 03, 2026",
        matchScore = "91%",
        url = "https://www.gatescambridge.org/"
    ),
    FeaturedFellowship(
        id = "stanford-biox-feat",
        institution = "Stanford University · Bio-X Program",
        mon = "S",
        title = "Graduate Fellowship in Computational Biology & AI",
        funding = "$45,000/yr + Full Health & Tuition",
        deadline = "Jan 05, 2027",
        matchScore = "89%",
        url = "https://biox.stanford.edu/"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    isServerConnected: Boolean,
    onNavigateToStudio: ((String) -> Unit)? = null,
    onNavigateToWatchdogs: (() -> Unit)? = null,
    onNavigateToProfile: (() -> Unit)? = null,
    onNavigateToDiary: (() -> Unit)? = null,
    onNavigateToNotifications: (() -> Unit)? = null,
    viewModel: FeedViewModel = viewModel { FeedViewModel() }
) {
    val colors = GradPathTheme.colors
    val uriHandler = LocalUriHandler.current

    val feedItems by viewModel.feedItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedItem by viewModel.selectedItem.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTagFilter by viewModel.selectedTagFilter.collectAsState()
    val selectedDegreeFilter by viewModel.selectedDegreeFilter.collectAsState()
    val bookmarkedIds by viewModel.bookmarkedIds.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    var activeDetailItem by remember { mutableStateOf<WatchdogOpportunity?>(null) }
    var showFilterOptions by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val pullOffset = remember { Animatable(0f) }
    val refreshThresholdPx = with(density) { 72.dp.toPx() }
    val maxPullPx = with(density) { 130.dp.toPx() }
    val listState = rememberLazyListState()

    LaunchedEffect(isLoading) {
        if (!isLoading && pullOffset.value > 0f) {
            pullOffset.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    val nestedScrollConnection = remember(isLoading) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0 && pullOffset.value > 0f) {
                    val newOffset = (pullOffset.value + available.y).coerceAtLeast(0f)
                    val consumed = newOffset - pullOffset.value
                    coroutineScope.launch { pullOffset.snapTo(newOffset) }
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val isAtTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                if (available.y > 0 && isAtTop && source == NestedScrollSource.UserInput) {
                    val tension = (1f - (pullOffset.value / maxPullPx).coerceIn(0f, 0.85f)) * 0.45f
                    val delta = available.y * tension
                    val newOffset = (pullOffset.value + delta).coerceAtMost(maxPullPx)
                    coroutineScope.launch { pullOffset.snapTo(newOffset) }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pullOffset.value > 0f) {
                    if (pullOffset.value >= refreshThresholdPx && !isLoading) {
                        viewModel.fetchFeed(isServerConnected)
                        pullOffset.animateTo(
                            targetValue = with(density) { 46.dp.toPx() },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    } else {
                        pullOffset.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchFeed(isServerConnected)
    }

    LaunchedEffect(isServerConnected) {
        if (isServerConnected) {
            viewModel.fetchFeed(isServerConnected)
        }
    }

    // Filter Logic
    val filteredFeed = remember(feedItems, searchQuery, selectedTagFilter, selectedDegreeFilter) {
        feedItems.filter { item ->
            val matchesSearch = item.title.contains(searchQuery, ignoreCase = true) ||
                    item.university.contains(searchQuery, ignoreCase = true) ||
                    item.country.contains(searchQuery, ignoreCase = true) ||
                    item.summary.contains(searchQuery, ignoreCase = true)

            val matchesTag = when (selectedTagFilter) {
                "TOP_200" -> item.pillar == ResearchPillar.TOP_200_ADMISSION || item.tags.contains("#Top200Admissions")
                "SCHOLARSHIP" -> item.pillar == ResearchPillar.SCHOLARSHIP_FUNDING || item.tags.contains("#ScholarshipAlert")
                "CAREER_INTEL" -> item.pillar == ResearchPillar.COUNTRY_CAREER_INTELLIGENCE || item.tags.contains("#CountryCareerIntel")
                "ACTION_PLAN" -> item.pillar == ResearchPillar.CAREER_ACTION_PLAN || item.tags.contains("#CareerActionPlan")
                "PHD" -> item.tags.contains("#PhDOpening")
                "DEADLINE" -> item.tags.contains("#DeadlineCountdown")
                "POLICY" -> item.tags.contains("#PolicyUpdate")
                else -> true
            }

            val matchesDegree = when (selectedDegreeFilter) {
                "PHD" -> item.degreeTrack.contains("Ph.D.", ignoreCase = true) || item.degreeTrack.equals("Both", ignoreCase = true)
                "MASTERS" -> item.degreeTrack.contains("Master", ignoreCase = true) || item.degreeTrack.equals("Both", ignoreCase = true)
                else -> true
            }

            matchesSearch && matchesTag && matchesDegree
        }
    }

    LaunchedEffect(filteredFeed) {
        if (filteredFeed.isNotEmpty()) {
            val currentSelected = selectedItem
            if (currentSelected == null || filteredFeed.none { it.id == currentSelected.id }) {
                viewModel.selectItem(filteredFeed.first())
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(colors.canvas)) {
        val screenMaxWidth = maxWidth
        val isMobile = screenMaxWidth < 768.dp
        val currentDetail = activeDetailItem

        if (currentDetail != null) {
            OpportunityDetailScreen(
                item = currentDetail,
                isBookmarked = bookmarkedIds.contains(currentDetail.id),
                onBack = { activeDetailItem = null },
                onBookmark = { viewModel.toggleBookmark(currentDetail.id) },
                onOpenUrl = { uriHandler.openUri(it) },
                onDiscussInStudio = {
                    val title = currentDetail.title
                    val uni = currentDetail.university
                    activeDetailItem = null
                    onNavigateToStudio?.invoke("Tell me about the $title at $uni")
                },
                isMobile = isMobile
            )
        } else {
            val containerModifier = if (isMobile) {
                Modifier.fillMaxSize().nestedScroll(nestedScrollConnection)
            } else {
                Modifier.fillMaxSize()
            }

            Box(modifier = containerModifier) {
                // Pull to Reload Bouncing Indicator (Only for Mobile / Android)
                val showPullIndicator = isMobile && (pullOffset.value > 8f || (isLoading && pullOffset.value > 0f))
                if (showPullIndicator) {
                    val isReady = pullOffset.value >= refreshThresholdPx
                    val progress = (pullOffset.value / refreshThresholdPx).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .graphicsLayer {
                                alpha = if (isLoading) 1f else progress
                                translationY = (pullOffset.value * 0.28f).coerceAtMost(36.dp.toPx())
                            },
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(colors.surfaceLow)
                                .border(1.dp, if (isReady || isLoading) colors.academicBlue else colors.outline, RoundedCornerShape(20.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(13.dp),
                                    strokeWidth = 2.dp,
                                    color = colors.academicBlue
                                )
                                Text(
                                    text = "Reloading feed...",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.academicBlue,
                                    fontFamily = FontFamily.Monospace
                                )
                            } else {
                                Icon(
                                    imageVector = if (isReady) Icons.Default.Refresh else Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = if (isReady) colors.academicBlue else colors.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isReady) "Release to reload" else "Pull to reload",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isReady) colors.academicBlue else colors.onSurfaceVariant,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationY = if (isMobile) pullOffset.value else 0f
                        },
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                // -------------------------------------------------------------
                // 1. SEARCH BAY & COLLAPSIBLE FILTERS
                // -------------------------------------------------------------
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = if (isMobile) 16.dp else 28.dp,
                                end = if (isMobile) 16.dp else 28.dp,
                                top = if (isMobile) 14.dp else 20.dp
                            ),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Search Bar + Filter Toggle Button
                        val hasActiveFilter = selectedDegreeFilter != "ALL" || selectedTagFilter != "ALL"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GSearchBar(
                                query = searchQuery,
                                onQueryChange = { viewModel.setSearchQuery(it) },
                                onClear = { viewModel.setSearchQuery("") },
                                placeholder = if (isMobile) "Search lab, professor, university..." else "Search by research lab, professor, university, or keywords (e.g. 'Embodied AI', 'Max Planck')...",
                                modifier = Modifier.weight(1f)
                            )

                            // Filter Button to Show and Hide Filter Options
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(GradPathRadii.rPill))
                                    .background(if (showFilterOptions) colors.academicBlue else GradPathThemeTokens.palette.card)
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            if (showFilterOptions) colors.academicBlue else GradPathThemeTokens.palette.border
                                        ),
                                        RoundedCornerShape(GradPathRadii.rPill)
                                    )
                                    .clickable { showFilterOptions = !showFilterOptions },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Toggle Filters",
                                    tint = if (showFilterOptions) Color.White else colors.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                                if (hasActiveFilter) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (showFilterOptions) Color.White else colors.academicBlue)
                                    )
                                }
                            }
                        }

                        // Collapsible Filter Options (Animated Visibility)
                        AnimatedVisibility(
                            visible = showFilterOptions,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Degree track pills
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    GChip(
                                        label = "All Degrees",
                                        tone = if (selectedDegreeFilter == "ALL") GChipTone.Brand else GChipTone.Neutral,
                                        onClick = { viewModel.setSelectedDegreeFilter("ALL") }
                                    )
                                    GChip(
                                        label = "Ph.D.",
                                        tone = if (selectedDegreeFilter == "PHD") GChipTone.Brand else GChipTone.Neutral,
                                        onClick = { viewModel.setSelectedDegreeFilter("PHD") }
                                    )
                                    GChip(
                                        label = "Master's",
                                        tone = if (selectedDegreeFilter == "MASTERS") GChipTone.Brand else GChipTone.Neutral,
                                        onClick = { viewModel.setSelectedDegreeFilter("MASTERS") }
                                    )
                                }

                                // Tag Filter Row
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    item {
                                        GChip(
                                            label = "All Feed (${feedItems.size})",
                                            tone = if (selectedTagFilter == "ALL") GChipTone.Brand else GChipTone.Neutral,
                                            onClick = { viewModel.setSelectedTagFilter("ALL") }
                                        )
                                    }
                                    item {
                                        GChip(
                                            label = "🏛️ Top 200 Admissions",
                                            tone = if (selectedTagFilter == "TOP_200") GChipTone.Brand else GChipTone.Neutral,
                                            onClick = { viewModel.setSelectedTagFilter("TOP_200") }
                                        )
                                    }
                                    item {
                                        GChip(
                                            label = "💰 Full Funding",
                                            tone = if (selectedTagFilter == "SCHOLARSHIP") GChipTone.Brand else GChipTone.Neutral,
                                            onClick = { viewModel.setSelectedTagFilter("SCHOLARSHIP") }
                                        )
                                    }
                                    item {
                                        GChip(
                                            label = "🌐 Visa & Career Intel",
                                            tone = if (selectedTagFilter == "CAREER_INTEL") GChipTone.Brand else GChipTone.Neutral,
                                            onClick = { viewModel.setSelectedTagFilter("CAREER_INTEL") }
                                        )
                                    }
                                    item {
                                        GChip(
                                            label = "🎯 Action Roadmap",
                                            tone = if (selectedTagFilter == "ACTION_PLAN") GChipTone.Brand else GChipTone.Neutral,
                                            onClick = { viewModel.setSelectedTagFilter("ACTION_PLAN") }
                                        )
                                    }
                                    item {
                                        GChip(
                                            label = "🔬 Ph.D. Openings",
                                            tone = if (selectedTagFilter == "PHD") GChipTone.Brand else GChipTone.Neutral,
                                            onClick = { viewModel.setSelectedTagFilter("PHD") }
                                        )
                                    }
                                    item {
                                        GChip(
                                            label = "⏳ Deadlines",
                                            tone = if (selectedTagFilter == "DEADLINE") GChipTone.Brand else GChipTone.Neutral,
                                            onClick = { viewModel.setSelectedTagFilter("DEADLINE") }
                                        )
                                    }
                                }
                            }
                        }

                        // Toast Feedback
                        AnimatedVisibility(visible = toastMessage != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colors.academicBlue.copy(alpha = 0.15f))
                                    .border(1.dp, colors.academicBlue.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(toastMessage ?: "", fontSize = 12.sp, color = colors.academicBlue)
                                IconButton(onClick = { viewModel.dismissToast() }, modifier = Modifier.size(18.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = colors.academicBlue, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }

                // -------------------------------------------------------------
                // 2. MAIN CONTENT: Split-Screen View (Desktop: 58% List / 42% Detail)
                // -------------------------------------------------------------
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = if (isMobile) 16.dp else 28.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Opportunities (${filteredFeed.size})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onSurface
                            )

                            // Refresh button exclusively for desktop application
                            if (!isMobile) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.surfaceLow)
                                        .border(1.dp, colors.outline, RoundedCornerShape(8.dp))
                                        .clickable(enabled = !isLoading) {
                                            viewModel.fetchFeed(isServerConnected)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(13.dp),
                                            strokeWidth = 2.dp,
                                            color = colors.academicBlue
                                        )
                                        Text(
                                            text = "Refreshing...",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.academicBlue
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Refresh Feed",
                                            tint = colors.academicBlue,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Refresh",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.onSurface
                                        )
                                    }
                                }
                            }
                        }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (filteredFeed.isEmpty() && !isLoading) {
                        GCard(
                            modifier = Modifier.fillMaxWidth().padding(vertical = GradPathSpace.xl),
                            padding = PaddingValues(GradPathSpace.xxl)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.SearchOff, contentDescription = "Empty", tint = GradPathThemeTokens.palette.textMuted, modifier = Modifier.size(44.dp))
                                VGap(GradPathSpace.sm)
                                Text("No opportunities matching criteria", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GradPathThemeTokens.palette.textPrimary)
                                VGap(GradPathSpace.xs)
                                Text("Try clearing your search query or selecting 'All Degrees'", fontSize = 13.sp, color = GradPathThemeTokens.palette.textMuted)
                            }
                        }
                    } else if (isMobile) {
                        // Mobile Single-Column Stream (Clicking card opens full Detail Screen)
                        if (isLoading && filteredFeed.isEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                repeat(3) {
                                    GSkeletonOpportunityCard()
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                filteredFeed.forEach { item ->
                                    OpportunityCard(
                                        item = item,
                                        isSelected = selectedItem?.id == item.id,
                                        isBookmarked = bookmarkedIds.contains(item.id),
                                        onSelect = {
                                            viewModel.selectItem(item)
                                            activeDetailItem = item
                                        },
                                        onBookmark = { viewModel.toggleBookmark(item.id) }
                                    )
                                }
                            }
                        }
                    } else {
                        // Desktop Responsive Grid: 2 columns on tablet/medium, 3 columns on wide desktop
                        val columns = if (screenMaxWidth >= 1200.dp) 3 else 2

                        if (isLoading && filteredFeed.isEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                repeat(2) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        repeat(columns) {
                                            Box(modifier = Modifier.weight(1f)) {
                                                GSkeletonOpportunityCard()
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            val chunkedFeed = remember(filteredFeed, columns) { filteredFeed.chunked(columns) }
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                chunkedFeed.forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        rowItems.forEach { item ->
                                            Box(modifier = Modifier.weight(1f)) {
                                                OpportunityCard(
                                                    item = item,
                                                    isSelected = selectedItem?.id == item.id,
                                                    isBookmarked = bookmarkedIds.contains(item.id),
                                                    onSelect = {
                                                        viewModel.selectItem(item)
                                                        activeDetailItem = item
                                                    },
                                                    onBookmark = { viewModel.toggleBookmark(item.id) }
                                                )
                                            }
                                        }
                                        val emptySlots = columns - rowItems.size
                                        repeat(emptySlots) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}
}

@Composable
fun TelemetryStatItem(value: String, label: String, sub: String, valueColor: Color) {
    val palette = GradPathThemeTokens.palette
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = GradPathSpace.sm)
    ) {
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            letterSpacing = (-0.5).sp
        )
        VGap(GradPathSpace.xxs)
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.textPrimary
        )
        Text(
            text = sub,
            fontSize = 10.sp,
            color = palette.textMuted
        )
    }
}

@Composable
fun FilterChipPill(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val colors = GradPathTheme.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) colors.surfaceHigh else colors.surfaceLow)
            .border(1.dp, if (isSelected) colors.academicBlue else colors.outline, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) colors.academicBlue else colors.onSurfaceVariant
        )
    }
}

@Composable
fun TagPill(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val colors = GradPathTheme.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) colors.academicBlue.copy(alpha = 0.15f) else colors.surfaceLow)
            .border(1.dp, if (isSelected) colors.academicBlue else colors.outline, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (isSelected) colors.academicBlue else colors.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun FeaturedFellowshipCard(
    feat: FeaturedFellowship,
    onClick: () -> Unit
) {
    val colors = GradPathTheme.colors

    Box(
        modifier = Modifier
            .width(280.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surfaceLow)
            .border(1.dp, colors.outline, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.surfaceHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(feat.mon, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
                    }
                    Text(
                        text = feat.institution,
                        fontSize = 10.sp,
                        color = colors.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.academicBlue.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(feat.matchScore, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = colors.academicBlue, fontWeight = FontWeight.Bold)
                }
            }

            Text(
                text = feat.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
                lineHeight = 18.sp,
                maxLines = 2
            )

            Box(modifier = Modifier.fillMaxWidth().dashedHairlineBottom(color = colors.lineDashed))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(feat.deadline, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = colors.alertAmber)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Apply", fontSize = 10.sp, color = colors.academicBlue, fontWeight = FontWeight.SemiBold)
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Open", tint = colors.academicBlue, modifier = Modifier.size(11.dp))
                }
            }
        }
    }
}

@Composable
fun OpportunityCard(
    item: WatchdogOpportunity,
    isSelected: Boolean,
    isBookmarked: Boolean,
    onSelect: () -> Unit,
    onBookmark: () -> Unit
) {
    AcademicOpportunityCard(
        opportunity = item,
        isBookmarked = isBookmarked,
        onBookmarkToggle = onBookmark,
        onClick = onSelect
    )
}

@Composable
fun DossierDetailPane(
    item: WatchdogOpportunity,
    onOpenUrl: (String) -> Unit,
    onDiscussInStudio: () -> Unit
) {
    val palette = GradPathThemeTokens.palette

    GCard(
        modifier = Modifier.fillMaxWidth(),
        padding = PaddingValues(GradPathSpace.lg)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(GradPathSpace.md)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(GradPathSpace.sm)
                ) {
                    GAvatar(
                        name = item.university,
                        size = 36.dp
                    )
                    Column {
                        Text(item.university, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                        Text("${item.country} · ${item.degreeTrack}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = palette.textSecondary)
                    }
                }

                if (item.confidence == SourceConfidence.VERIFIED_OFFICIAL) {
                    GChip(
                        label = "Verified Official",
                        tone = GChipTone.Ok,
                        leadingIcon = {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = palette.verifiedEmerald, modifier = Modifier.size(12.dp))
                        }
                    )
                }
            }

            Text(
                text = item.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
                lineHeight = 22.sp
            )

            // Divider
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(palette.borderSoft))

            // Funding & Deadline Info Grid
            GCard(
                modifier = Modifier.fillMaxWidth(),
                color = palette.surfaceLow,
                padding = PaddingValues(GradPathSpace.md)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        GKicker("FUNDING TYPE")
                        VGap(GradPathSpace.xxs)
                        Text(item.fundingType.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = palette.textPrimary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        GKicker("DEADLINE")
                        VGap(GradPathSpace.xxs)
                        Text(item.deadlineDate, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = palette.alertAmber, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Summary dossier
            Column(verticalArrangement = Arrangement.spacedBy(GradPathSpace.xs)) {
                GKicker("DOSSIER SUMMARY")
                Text(
                    text = item.summary,
                    fontSize = 12.sp,
                    color = palette.textSecondary,
                    lineHeight = 18.sp
                )
            }

            // Action Buttons
            Column(verticalArrangement = Arrangement.spacedBy(GradPathSpace.sm)) {
                GButton(
                    label = "Open Official Circular",
                    onClick = { onOpenUrl(item.officialUrl) },
                    variant = GBtnVariant.Primary,
                    size = GBtnSize.MD,
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = palette.onAcademicBlue, modifier = Modifier.size(14.dp))
                    },
                    fullWidth = true
                )

                GButton(
                    label = "Discuss Strategy in Research Studio",
                    onClick = onDiscussInStudio,
                    variant = GBtnVariant.Secondary,
                    size = GBtnSize.MD,
                    leadingIcon = {
                        Icon(Icons.Default.SmartToy, contentDescription = null, tint = palette.academicBlue, modifier = Modifier.size(14.dp))
                    },
                    fullWidth = true
                )
            }
        }
    }
}

/**
 * -----------------------------------------------------------------------------
 * Dedicated Opportunity Detail Screen (Full Page on Mobile & Desktop)
 * -----------------------------------------------------------------------------
 */
@Composable
fun OpportunityDetailScreen(
    item: WatchdogOpportunity,
    isBookmarked: Boolean,
    onBack: () -> Unit,
    onBookmark: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onDiscussInStudio: () -> Unit,
    isMobile: Boolean
) {
    val palette = GradPathThemeTokens.palette

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.canvas)
    ) {
        // Top App Bar / Navigation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.surfaceLow)
                .padding(
                    horizontal = if (isMobile) GradPathSpace.md else GradPathSpace.xl,
                    vertical = GradPathSpace.sm
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(GradPathSpace.sm),
                modifier = Modifier.weight(1f)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = palette.academicBlue
                    )
                }
                Column {
                    Text(
                        text = if (isMobile) "Opportunity Dossier" else "Back to Opportunities",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        modifier = Modifier.clickable { onBack() }
                    )
                    Text(
                        text = "${item.university} · ${item.degreeTrack}",
                        fontSize = 11.sp,
                        color = palette.textSecondary,
                        maxLines = 1
                    )
                }
            }

            IconButton(onClick = onBookmark) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = if (isBookmarked) palette.academicBlue else palette.textMuted
                )
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(palette.borderSoft))

        // Dossier Content
        if (isMobile) {
            // Mobile Single-Column Scroll
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(GradPathSpace.lg),
                verticalArrangement = Arrangement.spacedBy(GradPathSpace.lg)
            ) {
                DossierDetailPane(
                    item = item,
                    onOpenUrl = onOpenUrl,
                    onDiscussInStudio = onDiscussInStudio
                )
            }
        } else {
            // Desktop 2-Column Dossier with Max Content Width
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 1200.dp)
                        .padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Left Column (62%): Dossier Overview, Criteria, and PI Group
                    Column(
                        modifier = Modifier.weight(0.62f),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        GCard(
                            modifier = Modifier.fillMaxWidth(),
                            padding = PaddingValues(24.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        GAvatar(name = item.university, size = 44.dp)
                                        Column {
                                            Text(
                                                text = item.university,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = palette.textPrimary
                                            )
                                            Text(
                                                text = "${item.country} · ${item.degreeTrack}",
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = palette.textSecondary
                                            )
                                        }
                                    }

                                    if (item.confidence == SourceConfidence.VERIFIED_OFFICIAL) {
                                        GChip(
                                            label = "Verified Official",
                                            tone = GChipTone.Ok,
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Verified,
                                                    contentDescription = null,
                                                    tint = palette.verifiedEmerald,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        )
                                    }
                                }

                                Text(
                                    text = item.title,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary,
                                    lineHeight = 28.sp
                                )

                                // Tags
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    GChip(label = item.pillar.label, tone = GChipTone.Brand)
                                    GChip(label = "${item.matchScore}% Match", tone = GChipTone.Neutral)
                                }

                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(palette.borderSoft))

                                // Summary
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    GKicker("DOSSIER SUMMARY & RESEARCH SCOPE")
                                    Text(
                                        text = item.summary,
                                        fontSize = 14.sp,
                                        color = palette.textSecondary,
                                        lineHeight = 22.sp
                                    )
                                }

                                // Admission Criteria if available
                                val criteria = item.admissionCriteria
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    GKicker("ADMISSION & ELIGIBILITY CRITERIA")
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        GCard(
                                            modifier = Modifier.weight(1f),
                                            color = palette.surfaceLow,
                                            padding = PaddingValues(12.dp)
                                        ) {
                                            Column {
                                                GKicker("MIN CGPA")
                                                VGap(GradPathSpace.xxs)
                                                Text(criteria.minCgpa, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                            }
                                        }
                                        GCard(
                                            modifier = Modifier.weight(1f),
                                            color = palette.surfaceLow,
                                            padding = PaddingValues(12.dp)
                                        ) {
                                            Column {
                                                GKicker("GRE POLICY")
                                                VGap(GradPathSpace.xxs)
                                                Text(criteria.grePolicy, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                            }
                                        }
                                        GCard(
                                            modifier = Modifier.weight(1f),
                                            color = palette.surfaceLow,
                                            padding = PaddingValues(12.dp)
                                        ) {
                                            Column {
                                                GKicker("IELTS / TOEFL")
                                                VGap(GradPathSpace.xxs)
                                                Text(criteria.ieltsMin, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                            }
                                        }
                                    }
                                }

                                if (item.principalInvestigators.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        GKicker("PRINCIPAL INVESTIGATORS / LAB")
                                        item.principalInvestigators.forEach { pi ->
                                            GCard(
                                                modifier = Modifier.fillMaxWidth(),
                                                color = palette.surfaceLow,
                                                padding = PaddingValues(12.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(pi.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                                        Text(pi.lab, fontSize = 11.sp, color = palette.textSecondary)
                                                    }
                                                    GChip(label = pi.status, tone = GChipTone.Neutral)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Right Column (38%): Funding, Deadline & Primary Actions
                    Column(
                        modifier = Modifier.weight(0.38f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        GCard(
                            modifier = Modifier.fillMaxWidth(),
                            padding = PaddingValues(20.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column {
                                    GKicker("FUNDING DETAILS")
                                    VGap(GradPathSpace.xs)
                                    Text(
                                        text = item.fundingType.label,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textPrimary
                                    )
                                    VGap(GradPathSpace.xxs)
                                    Text(
                                        text = item.fundingBreakdown,
                                        fontSize = 12.sp,
                                        color = palette.textSecondary
                                    )
                                }

                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(palette.borderSoft))

                                Column {
                                    GKicker("APPLICATION DEADLINE")
                                    VGap(GradPathSpace.xs)
                                    Text(
                                        text = item.deadlineDate,
                                        fontSize = 15.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.alertAmber
                                    )
                                }

                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(palette.borderSoft))

                                // Actions
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    GButton(
                                        label = "Open Official Circular",
                                        onClick = { onOpenUrl(item.officialUrl) },
                                        variant = GBtnVariant.Primary,
                                        size = GBtnSize.MD,
                                        leadingIcon = {
                                            Icon(
                                                Icons.AutoMirrored.Filled.OpenInNew,
                                                contentDescription = null,
                                                tint = palette.onAcademicBlue,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        },
                                        fullWidth = true
                                    )

                                    GButton(
                                        label = "Discuss Strategy in Research Studio",
                                        onClick = onDiscussInStudio,
                                        variant = GBtnVariant.Secondary,
                                        size = GBtnSize.MD,
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.SmartToy,
                                                contentDescription = null,
                                                tint = palette.academicBlue,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        },
                                        fullWidth = true
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * -----------------------------------------------------------------------------
 * Quick Access Squircle Item (BrainSton 48dp squircle touch tile with bounce)
 * -----------------------------------------------------------------------------
 */
@Composable
fun QuickAccessSquircleItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    val palette = GradPathThemeTokens.palette
    GPressScale(
        onClick = onClick,
        modifier = Modifier.width(88.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = GradPathSpace.xs)
        ) {
            Box {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(GradPathRadii.rM))
                        .background(palette.surfaceLow)
                        .border(1.dp, palette.borderSoft, RoundedCornerShape(GradPathRadii.rM)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = palette.academicBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
                if (badgeText != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-4).dp)
                    ) {
                        GCountBadge(text = badgeText)
                    }
                }
            }
            VGap(GradPathSpace.xs)
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}




