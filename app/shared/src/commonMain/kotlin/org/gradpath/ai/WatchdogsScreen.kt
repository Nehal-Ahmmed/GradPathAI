package org.gradpath.ai

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.gradpath.ai.ui.components.*
import org.gradpath.ai.viewmodel.WatchdogsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchdogsScreen(
    isServerConnected: Boolean,
    viewModel: WatchdogsViewModel = viewModel { WatchdogsViewModel() }
) {
    val palette = GradPathThemeTokens.palette
    val uriHandler = LocalUriHandler.current

    val sources by viewModel.sources.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSweeping by viewModel.isSweeping.collectAsState()
    val sweepStatusMessage by viewModel.sweepStatusMessage.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsState()
    val showAddDialog by viewModel.showAddDialog.collectAsState()
    var showTelemetrySheet by remember { mutableStateOf(false) }
    var showFilterRow by remember { mutableStateOf(false) }

    LaunchedEffect(isServerConnected) {
        if (isServerConnected) {
            viewModel.fetchSources(isServerConnected)
        }
    }

    // Default sources if server is empty or offline
    val displaySources = if (sources.isEmpty()) WatchdogRegistry.DEFAULT_SOURCES else sources

    val filteredSources = remember(displaySources, searchQuery, selectedCategoryFilter) {
        displaySources.filter { source ->
            val matchesSearch = source.name.contains(searchQuery, ignoreCase = true) ||
                    source.country.contains(searchQuery, ignoreCase = true) ||
                    source.portalUrl.contains(searchQuery, ignoreCase = true)

            val matchesCategory = when (selectedCategoryFilter) {
                "SCHOLARSHIP" -> source.category == OpportunityCategory.SCHOLARSHIP
                "PHD" -> source.category == OpportunityCategory.PHD_OPENING
                "DEADLINE" -> source.category == OpportunityCategory.DEADLINE_ALERT
                "POLICY" -> source.category == OpportunityCategory.POLICY_UPDATE
                else -> true
            }

            matchesSearch && matchesCategory
        }
    }

    var activeMobileSource by remember { mutableStateOf<MonitoredSource?>(null) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(palette.canvas)) {
        val isMobile = maxWidth < 768.dp

        if (isMobile && activeMobileSource != null) {
            WatchdogDetailScreen(
                source = activeMobileSource!!,
                isServerConnected = isServerConnected,
                isSweeping = isSweeping,
                onBack = { activeMobileSource = null },
                onToggleEnabled = {
                    viewModel.toggleSource(activeMobileSource!!, isServerConnected)
                    activeMobileSource = activeMobileSource!!.copy(isEnabled = !activeMobileSource!!.isEnabled)
                },
                onTriggerSweep = { viewModel.triggerSweep(isServerConnected) },
                onOpenUrl = { uriHandler.openUri(it) }
            )
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = if (isMobile) GradPathSpace.lg else GradPathSpace.xxl,
                        top = if (isMobile) GradPathSpace.md else GradPathSpace.lg,
                        end = if (isMobile) GradPathSpace.lg else GradPathSpace.xxl,
                        bottom = if (isMobile) 90.dp else GradPathSpace.xxl
                    ),
                    verticalArrangement = Arrangement.spacedBy(GradPathSpace.md)
                ) {
                    // -------------------------------------------------------------
                    // 1. DESKTOP SUB-HEADER & RADAR TELEMETRY HERO BANNER
                    // -------------------------------------------------------------
                    if (!isMobile) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(GradPathSpace.md)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "University Portals & Radars",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textPrimary
                                    )

                                    GButton(
                                        label = "Add Portal",
                                        onClick = { viewModel.setShowAddDialog(true) },
                                        variant = GBtnVariant.Secondary,
                                        size = GBtnSize.SM,
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                tint = palette.academicBlue,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    )
                                }

                                // OS Task Scheduler Telemetry Banner
                                GCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    padding = PaddingValues(GradPathSpace.md)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(GradPathSpace.sm)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(GradPathRadii.rXs))
                                                    .background(palette.surfaceLow)
                                                    .border(1.dp, palette.borderSoft, RoundedCornerShape(GradPathRadii.rXs))
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = "OS Task Scheduler: Daily 8:00 AM",
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = palette.textPrimary
                                                )
                                            }
                                            Text("●", fontSize = 8.sp, color = palette.borderSoft)
                                            Text(
                                                text = "Command: daily_sweep.ps1",
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = palette.textMuted
                                            )
                                            Text("●", fontSize = 8.sp, color = palette.borderSoft)
                                            Text(
                                                text = "Last Run: Today 8:00 AM (ExitCode: 0)",
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.SemiBold,
                                                color = palette.verifiedEmerald
                                            )
                                        }

                                        GButton(
                                            label = if (isSweeping) "Sweeping..." else "Trigger Autonomous Sweep Now",
                                            onClick = { viewModel.triggerSweep(isServerConnected) },
                                            enabled = isServerConnected && !isSweeping,
                                            variant = GBtnVariant.Primary,
                                            size = GBtnSize.SM,
                                            leadingIcon = {
                                                if (isSweeping) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(13.dp),
                                                        color = palette.onAcademicBlue,
                                                        strokeWidth = 2.dp
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.Bolt,
                                                        contentDescription = null,
                                                        tint = palette.onAcademicBlue,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }

                                // Sweep output message feedback
                                AnimatedVisibility(visible = sweepStatusMessage != null) {
                                    GCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = palette.academicBlueSoft,
                                        padding = PaddingValues(GradPathSpace.md)
                                    ) {
                                        Text(
                                            text = sweepStatusMessage ?: "",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = palette.onAcademicBlueSoft
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // -------------------------------------------------------------
                    // 2. SEARCH & COLLAPSIBLE CATEGORY FILTER ROW
                    // -------------------------------------------------------------
                    item {
                        if (isMobile) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(GradPathSpace.xs)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(GradPathSpace.xs)
                                ) {
                                    GSearchBar(
                                        query = searchQuery,
                                        onQueryChange = { viewModel.setSearchQuery(it) },
                                        onClear = { viewModel.setSearchQuery("") },
                                        placeholder = "Filter monitored portals...",
                                        modifier = Modifier.weight(1f)
                                    )

                                    // Filter Toggle Button directly to the right of search bar
                                    IconButton(
                                        onClick = { showFilterRow = !showFilterRow },
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(GradPathRadii.rM))
                                            .background(if (showFilterRow || selectedCategoryFilter != "ALL") palette.academicBlueSoft else palette.surfaceLow)
                                            .border(
                                                1.dp,
                                                if (showFilterRow || selectedCategoryFilter != "ALL") palette.academicBlue else palette.borderSoft,
                                                RoundedCornerShape(GradPathRadii.rM)
                                            )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = "Filter",
                                            tint = if (showFilterRow || selectedCategoryFilter != "ALL") palette.academicBlue else palette.textSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                // Filter options appear only when filter button is clicked
                                AnimatedVisibility(visible = showFilterRow) {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(GradPathSpace.xs),
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                    ) {
                                        item {
                                            GChip(
                                                label = "All (${displaySources.size})",
                                                tone = if (selectedCategoryFilter == "ALL") GChipTone.Brand else GChipTone.Neutral,
                                                onClick = { viewModel.setSelectedCategoryFilter("ALL") }
                                            )
                                        }
                                        item {
                                            GChip(
                                                label = "🎓 Scholarships",
                                                tone = if (selectedCategoryFilter == "SCHOLARSHIP") GChipTone.Brand else GChipTone.Neutral,
                                                onClick = { viewModel.setSelectedCategoryFilter("SCHOLARSHIP") }
                                            )
                                        }
                                        item {
                                            GChip(
                                                label = "🔬 Ph.D. Openings",
                                                tone = if (selectedCategoryFilter == "PHD") GChipTone.Brand else GChipTone.Neutral,
                                                onClick = { viewModel.setSelectedCategoryFilter("PHD") }
                                            )
                                        }
                                        item {
                                            GChip(
                                                label = "⏳ Deadlines",
                                                tone = if (selectedCategoryFilter == "DEADLINE") GChipTone.Brand else GChipTone.Neutral,
                                                onClick = { viewModel.setSelectedCategoryFilter("DEADLINE") }
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(GradPathSpace.sm)
                            ) {
                                GSearchBar(
                                    query = searchQuery,
                                    onQueryChange = { viewModel.setSearchQuery(it) },
                                    onClear = { viewModel.setSearchQuery("") },
                                    placeholder = "Filter monitored portals by name, domain, or country...",
                                    modifier = Modifier.weight(1f)
                                )

                                LazyRow(horizontalArrangement = Arrangement.spacedBy(GradPathSpace.xs)) {
                                    item {
                                        GChip(
                                            label = "All (${displaySources.size})",
                                            tone = if (selectedCategoryFilter == "ALL") GChipTone.Brand else GChipTone.Neutral,
                                            onClick = { viewModel.setSelectedCategoryFilter("ALL") }
                                        )
                                    }
                                    item {
                                        GChip(
                                            label = "🎓 Scholarships",
                                            tone = if (selectedCategoryFilter == "SCHOLARSHIP") GChipTone.Brand else GChipTone.Neutral,
                                            onClick = { viewModel.setSelectedCategoryFilter("SCHOLARSHIP") }
                                        )
                                    }
                                    item {
                                        GChip(
                                            label = "🔬 Ph.D. Openings",
                                            tone = if (selectedCategoryFilter == "PHD") GChipTone.Brand else GChipTone.Neutral,
                                            onClick = { viewModel.setSelectedCategoryFilter("PHD") }
                                        )
                                    }
                                    item {
                                        GChip(
                                            label = "⏳ Deadlines",
                                            tone = if (selectedCategoryFilter == "DEADLINE") GChipTone.Brand else GChipTone.Neutral,
                                            onClick = { viewModel.setSelectedCategoryFilter("DEADLINE") }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // -------------------------------------------------------------
                    // 3. MONITORED PORTALS CARDS DECK
                    // -------------------------------------------------------------
                    items(filteredSources, key = { it.id }) { source ->
                        WatchdogPortalCard(
                            source = source,
                            onToggleEnabled = { viewModel.toggleSource(source, isServerConnected) },
                            onOpenUrl = { uriHandler.openUri(source.portalUrl) },
                            onClick = { if (isMobile) activeMobileSource = source }
                        )
                    }
                }

                // Floating Action Button for Adding Portals (Positioned neatly at bottom-right corner)
                FloatingActionButton(
                    onClick = { viewModel.setShowAddDialog(true) },
                    containerColor = palette.academicBlue,
                    contentColor = Color.White,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = GradPathSpace.lg, bottom = GradPathSpace.lg)
                        .size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Portal",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }

    // Add Source Dialog
    if (showAddDialog) {
        AddSourceDialog(
            onDismiss = { viewModel.setShowAddDialog(false) },
            onConfirm = { newSource ->
                viewModel.addSource(newSource, isServerConnected)
            }
        )
    }

    // Interactive Telemetry Bottom Sheet (BrainSton Sheet Pattern)
    if (showTelemetrySheet) {
        ModalBottomSheet(
            onDismissRequest = { showTelemetrySheet = false },
            containerColor = palette.card,
            scrimColor = Color.Black.copy(alpha = 0.5f),
            shape = RoundedCornerShape(topStart = GradPathRadii.rXl, topEnd = GradPathRadii.rXl)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = GradPathSpace.lg)
                    .padding(bottom = GradPathSpace.xxl),
                verticalArrangement = Arrangement.spacedBy(GradPathSpace.md)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(GradPathSpace.xs)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(palette.verifiedEmerald)
                        )
                        Text(
                            text = "AUTONOMOUS RADAR ACTIVE · SYNC #8824",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = palette.verifiedEmerald
                        )
                    }
                    GCountBadge(
                        text = "${filteredSources.size} Radars",
                        color = palette.surfaceLow,
                        textColor = palette.textSecondary
                    )
                }

                Text(
                    text = "${filteredSources.count { it.isEnabled }} Radars Active · All Healthy",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )

                Text(
                    text = "Last sweep completed 14m ago across US (.gov), German (daad.de), UK and Commonwealth portals. 14 new circulars captured today.",
                    fontSize = 13.sp,
                    color = palette.textSecondary,
                    lineHeight = 19.sp
                )

                VGap(GradPathSpace.xs)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GradPathSpace.sm)
                ) {
                    GButton(
                        label = if (isSweeping) "Sweeping..." else "Force Sweep All",
                        onClick = { viewModel.triggerSweep(isServerConnected) },
                        variant = GBtnVariant.Primary,
                        size = GBtnSize.MD,
                        enabled = isServerConnected && !isSweeping,
                        leadingIcon = {
                            if (isSweeping) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(13.dp),
                                    color = palette.onAcademicBlue,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = palette.onAcademicBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    GButton(
                        label = "Add Portal +",
                        onClick = {
                            showTelemetrySheet = false
                            viewModel.setShowAddDialog(true)
                        },
                        variant = GBtnVariant.Secondary,
                        size = GBtnSize.MD,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = palette.academicBlue,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                AnimatedVisibility(visible = sweepStatusMessage != null) {
                    GCard(
                        modifier = Modifier.fillMaxWidth(),
                        color = palette.academicBlueSoft,
                        padding = PaddingValues(GradPathSpace.md)
                    ) {
                        Text(
                            text = sweepStatusMessage ?: "",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = palette.onAcademicBlueSoft
                        )
                    }
                }
            }
        }
    }
}

/**
 * -----------------------------------------------------------------------------
 * Monitored Watchdog Portal Card (BrainSton Boxed Card Pattern)
 * -----------------------------------------------------------------------------
 */
@Composable
fun WatchdogPortalCard(
    source: MonitoredSource,
    onToggleEnabled: () -> Unit,
    onOpenUrl: () -> Unit,
    onClick: () -> Unit = {}
) {
    val palette = GradPathThemeTokens.palette
    val confidence = WatchdogRegistry.evaluateConfidence(source.portalUrl)

    GCard(
        modifier = Modifier.fillMaxWidth(),
        padding = PaddingValues(GradPathSpace.lg),
        onClick = onClick
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(GradPathSpace.md)) {
            // Header: Avatar + Title & Country/Category on left, clean chevron on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(GradPathSpace.sm)
                ) {
                    GAvatar(
                        name = source.name,
                        size = 38.dp,
                        hasRing = true
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = source.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(GradPathSpace.xs)
                        ) {
                            Text(source.country, fontSize = 11.sp, color = palette.textSecondary)
                            Text("●", fontSize = 6.sp, color = palette.borderSoft)
                            Text(
                                text = source.category.label,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = palette.academicBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Open Dossier",
                    tint = palette.textMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Description / notes
            if (source.notes.isNotBlank()) {
                Text(
                    text = source.notes,
                    fontSize = 12.sp,
                    color = palette.textSecondary,
                    lineHeight = 17.sp
                )
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(palette.borderSoft))

            // Footer URL + Last Swept Telemetry
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenUrl() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(GradPathSpace.xs)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Visit Portal",
                        tint = palette.academicBlue,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = source.portalUrl,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = palette.academicBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(GradPathSpace.sm))

                Text(
                    text = "Swept: ${source.lastSweptAt ?: "Today 8:00 AM"}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = palette.textMuted,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * -----------------------------------------------------------------------------
 * Add Monitored Source Dialog
 * -----------------------------------------------------------------------------
 */
@Composable
fun AddSourceDialog(
    onDismiss: () -> Unit,
    onConfirm: (MonitoredSource) -> Unit
) {
    val palette = GradPathThemeTokens.palette

    var name by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("USA") }
    var url by remember { mutableStateOf("https://") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Monitored Watchdog Portal",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(GradPathSpace.sm)) {
                GInput(
                    value = name,
                    onValueChange = { name = it },
                    label = "Institution / Portal Name",
                    placeholder = "e.g. Stanford Bio-X Fellowships",
                    modifier = Modifier.fillMaxWidth()
                )
                GInput(
                    value = country,
                    onValueChange = { country = it },
                    label = "Country",
                    placeholder = "e.g. USA, Germany, UK",
                    modifier = Modifier.fillMaxWidth()
                )
                GInput(
                    value = url,
                    onValueChange = { url = it },
                    label = "Official Portal URL (.edu / .gov / .de)",
                    placeholder = "https://...",
                    modifier = Modifier.fillMaxWidth()
                )
                GInput(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "Monitoring Focus / Lab keywords",
                    placeholder = "e.g. Neural engineering, full stipend",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            GButton(
                label = "Add Watchdog",
                onClick = {
                    if (name.isNotBlank() && url.isNotBlank()) {
                        val newSource = MonitoredSource(
                            id = "custom-${System.currentTimeMillis()}",
                            name = name,
                            country = country,
                            portalUrl = url,
                            notes = notes,
                            category = OpportunityCategory.SCHOLARSHIP
                        )
                        onConfirm(newSource)
                    }
                },
                variant = GBtnVariant.Primary,
                size = GBtnSize.SM
            )
        },
        dismissButton = {
            GButton(
                label = "Cancel",
                onClick = onDismiss,
                variant = GBtnVariant.Ghost,
                size = GBtnSize.SM
            )
        },
        containerColor = palette.card,
        shape = RoundedCornerShape(GradPathRadii.rL)
    )
}

/**
 * -----------------------------------------------------------------------------
 * Mobile Dedicated Watchdog Detail Screen (Full Page on Android Mobile)
 * -----------------------------------------------------------------------------
 */
@Composable
fun WatchdogDetailScreen(
    source: MonitoredSource,
    isServerConnected: Boolean,
    isSweeping: Boolean,
    onBack: () -> Unit,
    onToggleEnabled: () -> Unit,
    onTriggerSweep: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    val palette = GradPathThemeTokens.palette
    val confidence = WatchdogRegistry.evaluateConfidence(source.portalUrl)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.canvas)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.surfaceLow)
                .padding(horizontal = GradPathSpace.md, vertical = GradPathSpace.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GradPathSpace.sm)
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
                    text = "Portal Telemetry Dossier",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Text(
                    text = source.name,
                    fontSize = 11.sp,
                    color = palette.textSecondary
                )
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(palette.borderSoft))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(GradPathSpace.lg),
            verticalArrangement = Arrangement.spacedBy(GradPathSpace.lg)
        ) {
            // Header Card with status
            GCard(
                modifier = Modifier.fillMaxWidth(),
                padding = PaddingValues(GradPathSpace.lg)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(GradPathSpace.md)) {
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
                                name = source.name,
                                size = 42.dp,
                                hasRing = true
                            )
                            Column {
                                Text(source.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                Text("${source.country} · ${source.category.label}", fontSize = 11.sp, color = palette.textSecondary)
                            }
                        }

                        // Confidence Seal
                        GChip(
                            label = confidence.label,
                            tone = if (confidence == SourceConfidence.VERIFIED_OFFICIAL) GChipTone.Ok else GChipTone.Warn,
                            dense = true,
                            leadingIcon = if (confidence == SourceConfidence.VERIFIED_OFFICIAL) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = palette.verifiedEmerald,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            } else null
                        )
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(palette.borderSoft))

                    // Enable Switch Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Autonomous Monitoring", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = palette.textPrimary)
                            Text(if (source.isEnabled) "Actively swept daily at 8:00 AM" else "Portal monitoring paused", fontSize = 11.sp, color = palette.textSecondary)
                        }
                        Switch(
                            checked = source.isEnabled,
                            onCheckedChange = { onToggleEnabled() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = palette.onAcademicBlue,
                                checkedTrackColor = palette.academicBlue,
                                uncheckedThumbColor = palette.textMuted,
                                uncheckedTrackColor = palette.track
                            )
                        )
                    }
                }
            }

            // Telemetry & Details Card
            GCard(
                modifier = Modifier.fillMaxWidth(),
                color = palette.surfaceLow,
                padding = PaddingValues(GradPathSpace.lg)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(GradPathSpace.sm)) {
                    GKicker("TELEMETRY & CREDENTIALS")

                    if (source.notes.isNotBlank()) {
                        VGap(GradPathSpace.xxs)
                        Text(source.notes, fontSize = 12.sp, color = palette.textPrimary, lineHeight = 18.sp)
                    }

                    VGap(GradPathSpace.xs)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Category", fontSize = 11.sp, color = palette.textSecondary)
                        Text(source.category.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = palette.academicBlue)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Last Swept", fontSize = 11.sp, color = palette.textSecondary)
                        Text(source.lastSweptAt ?: "Today 8:00 AM", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = palette.textPrimary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Portal ID", fontSize = 11.sp, color = palette.textSecondary)
                        Text(source.id, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = palette.textMuted)
                    }
                }
            }

            // -------------------------------------------------------------
            // CAPTURED CIRCULARS & LAB OPENINGS STREAM
            // -------------------------------------------------------------
            val circulars = remember(source.id) { getCapturedCircularsForSource(source) }

            Column(verticalArrangement = Arrangement.spacedBy(GradPathSpace.sm)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        GKicker("AUTONOMOUS CRAWLER HARVEST")
                        Text(
                            text = "Captured Circulars (${circulars.size})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                    }
                    GChip(
                        label = "Swept Today",
                        tone = GChipTone.Ok,
                        dense = true
                    )
                }

                if (circulars.isEmpty()) {
                    GCard(
                        modifier = Modifier.fillMaxWidth(),
                        color = palette.surfaceLow,
                        padding = PaddingValues(GradPathSpace.lg)
                    ) {
                        Text(
                            text = "No circulars captured today. Autonomous sweeper scheduled for 8:00 AM.",
                            fontSize = 12.sp,
                            color = palette.textMuted
                        )
                    }
                } else {
                    circulars.forEach { circ ->
                        GCard(
                            modifier = Modifier.fillMaxWidth(),
                            padding = PaddingValues(GradPathSpace.md)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(GradPathSpace.xs)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = circ.title,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.textPrimary
                                        )
                                        Text(
                                            text = circ.program,
                                            fontSize = 11.sp,
                                            color = palette.textSecondary
                                        )
                                    }

                                    GChip(
                                        label = circ.matchScore,
                                        tone = GChipTone.Brand,
                                        dense = true
                                    )
                                }

                                VGap(GradPathSpace.xxs)

                                // Funding and deadline ribbons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(GradPathSpace.xs),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(GradPathRadii.rXs))
                                            .background(palette.verifiedSoft)
                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = circ.funding,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = palette.onVerifiedSoft
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(GradPathRadii.rXs))
                                            .background(palette.alertSoft)
                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "⏳ ${circ.deadline}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = palette.onAlertSoft
                                        )
                                    }

                                    Spacer(modifier = Modifier.weight(1f))

                                    GChip(
                                        label = circ.degreeTrack,
                                        tone = GChipTone.Neutral,
                                        dense = true
                                    )
                                }

                                VGap(GradPathSpace.xxs)

                                // Direct Apply CTA
                                GButton(
                                    label = "Apply / Open Official Circular",
                                    onClick = { onOpenUrl(circ.url) },
                                    variant = GBtnVariant.Soft,
                                    size = GBtnSize.SM,
                                    fullWidth = true,
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                            contentDescription = null,
                                            tint = palette.academicBlue,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Action Buttons
            Column(verticalArrangement = Arrangement.spacedBy(GradPathSpace.sm)) {
                GButton(
                    label = "Visit Official Portal in Browser",
                    onClick = { onOpenUrl(source.portalUrl) },
                    variant = GBtnVariant.Primary,
                    size = GBtnSize.MD,
                    fullWidth = true,
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = palette.onAcademicBlue, modifier = Modifier.size(14.dp))
                    }
                )

                GButton(
                    label = if (isSweeping) "Sweeping..." else "Trigger Immediate Sweep",
                    onClick = onTriggerSweep,
                    enabled = isServerConnected && !isSweeping,
                    variant = GBtnVariant.Secondary,
                    size = GBtnSize.MD,
                    fullWidth = true,
                    leadingIcon = {
                        if (isSweeping) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = palette.academicBlue, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = palette.academicBlue, modifier = Modifier.size(14.dp))
                        }
                    }
                )
            }
        }
    }
}

/**
 * -----------------------------------------------------------------------------
 * Captured Circular Item & Provider
 * -----------------------------------------------------------------------------
 */
data class PortalOpportunityItem(
    val id: String,
    val title: String,
    val program: String,
    val funding: String,
    val deadline: String,
    val degreeTrack: String,
    val matchScore: String,
    val url: String
)

private fun getCapturedCircularsForSource(source: MonitoredSource): List<PortalOpportunityItem> {
    return when (source.id) {
        "daad-germany" -> listOf(
            PortalOpportunityItem(
                id = "daad-circ-1",
                title = "DAAD Doctoral Research Grants in Germany",
                program = "German Academic Exchange Service (DAAD) · Nationwide",
                funding = "TV-L E13 Contract (100%) + €1,300/mo",
                deadline = "Nov 15, 2026",
                degreeTrack = "Ph.D.",
                matchScore = "94% Match",
                url = source.portalUrl
            ),
            PortalOpportunityItem(
                id = "daad-circ-2",
                title = "DAAD Helmut-Schmidt-Programme (PPGG)",
                program = "Public Policy & Good Governance Masters",
                funding = "Full Tuition Waiver + €934/mo Allowance",
                deadline = "Dec 31, 2026",
                degreeTrack = "Master's",
                matchScore = "89% Match",
                url = source.portalUrl
            ),
            PortalOpportunityItem(
                id = "daad-circ-3",
                title = "DAAD Postdoctoral Researchers Experience (PRIME)",
                program = "International Research Mobility · German Universities",
                funding = "TV-L E13 Salary Scale + Reintegration Grant",
                deadline = "Jan 15, 2027",
                degreeTrack = "Postdoc",
                matchScore = "91% Match",
                url = source.portalUrl
            )
        )
        "us-nsf-grfp" -> listOf(
            PortalOpportunityItem(
                id = "nsf-circ-1",
                title = "NSF Graduate Research Fellowship Program (GRFP)",
                program = "National Science Foundation Directorate for STEM",
                funding = "$37,000/yr Annual Stipend + $16,000 Tuition",
                deadline = "Oct 24, 2026",
                degreeTrack = "Ph.D.",
                matchScore = "96% Match",
                url = source.portalUrl
            ),
            PortalOpportunityItem(
                id = "nsf-circ-2",
                title = "NSF CISE Expeditions in AI Foundations & Computing",
                program = "CISE Directorate · US Tier-1 Universities",
                funding = "$42,000/yr Graduate Research Appointment",
                deadline = "Dec 05, 2026",
                degreeTrack = "Ph.D.",
                matchScore = "92% Match",
                url = source.portalUrl
            )
        )
        "fulbright-intl" -> listOf(
            PortalOpportunityItem(
                id = "fulbright-circ-1",
                title = "Fulbright Foreign Student Graduate Scholarship (USA)",
                program = "US Department of State / IIE · All Disciplines",
                funding = "Full Tuition, Living Stipend & J-1 Visa",
                deadline = "Jan 15, 2027",
                degreeTrack = "Master's / Ph.D.",
                matchScore = "95% Match",
                url = source.portalUrl
            ),
            PortalOpportunityItem(
                id = "fulbright-circ-2",
                title = "Fulbright-Hays Doctoral Dissertation Research Abroad",
                program = "International Education Directorate",
                funding = "$34,000 Research Travel & Living Allowance",
                deadline = "Feb 01, 2027",
                degreeTrack = "Ph.D.",
                matchScore = "90% Match",
                url = source.portalUrl
            )
        )
        "mit-eecs" -> listOf(
            PortalOpportunityItem(
                id = "mit-circ-1",
                title = "MIT CSAIL Generative Intelligence Doctoral Fellowship",
                program = "MIT Computer Science & AI Lab (CSAIL)",
                funding = "$52,000/yr Stipend + 100% Tuition Waiver",
                deadline = "Dec 15, 2026",
                degreeTrack = "Ph.D.",
                matchScore = "94% Match",
                url = source.portalUrl
            ),
            PortalOpportunityItem(
                id = "mit-circ-2",
                title = "MIT EECS Graduate Research Assistantship (RA)",
                program = "EECS Department · Quantum & AI Labs",
                funding = "$48,500/yr + Full Health Insurance",
                deadline = "Jan 05, 2027",
                degreeTrack = "Ph.D.",
                matchScore = "88% Match",
                url = source.portalUrl
            )
        )
        "cmu-scs" -> listOf(
            PortalOpportunityItem(
                id = "cmu-circ-1",
                title = "CMU Language Technologies Institute (LTI) Fellowship",
                program = "Carnegie Mellon School of Computer Science",
                funding = "$46,000/yr RA Contract + Full Health & Tuition",
                deadline = "Dec 10, 2026",
                degreeTrack = "Ph.D.",
                matchScore = "93% Match",
                url = source.portalUrl
            ),
            PortalOpportunityItem(
                id = "cmu-circ-2",
                title = "CMU Robotics Institute (RI) Doctoral Assistantship",
                program = "Autonomous Systems & Manipulation Lab",
                funding = "$47,000/yr + 100% Tuition Waiver",
                deadline = "Dec 12, 2026",
                degreeTrack = "Ph.D.",
                matchScore = "91% Match",
                url = source.portalUrl
            )
        )
        "stanford-cs" -> listOf(
            PortalOpportunityItem(
                id = "stanford-circ-1",
                title = "Stanford Computer Science & Bio-X Graduate Fellowship",
                program = "Stanford University Department of CS",
                funding = "$45,000/yr + Full Health & Tuition Waiver",
                deadline = "Jan 05, 2027",
                degreeTrack = "Ph.D.",
                matchScore = "89% Match",
                url = source.portalUrl
            )
        )
        else -> listOf(
            PortalOpportunityItem(
                id = "${source.id}-circ-1",
                title = "${source.name} Doctoral Research Call",
                program = "${source.country} Academic Directorate",
                funding = "Full Tuition Waiver + Living Allowance",
                deadline = "Rolling Admissions 2026-2027",
                degreeTrack = "Ph.D.",
                matchScore = "88% Match",
                url = source.portalUrl
            )
        )
    }
}
