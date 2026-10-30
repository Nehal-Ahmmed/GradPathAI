package org.gradpath.ai

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import org.gradpath.ai.ui.components.*
import org.gradpath.ai.viewmodel.DiaryViewModel

enum class DiaryTab(val label: String) {
    UNIVERSITIES("Target Universities"),
    PROFESSOR_OUTREACH("Professor Outreach")
}

/**
 * -----------------------------------------------------------------------------
 * DiaryScreen — Focused, Student-Friendly Academic Diary Workstation
 *
 * Redesigned for maximum clarity, focus, and elegance:
 *   1. Zero Header Clutter: Compact header with direct "+ Add" action & clean tabs
 *   2. Conflict-Free Cards: Full-width university headlines without badge truncation
 *   3. Single-Line Standard Search: Guaranteed single-line placeholder without wrapping
 *   4. Interactive Modal Bottom Sheets: Deep notes, policies, and AI emails on demand
 *   5. Collision-Free Action Button: Safe bottom scrolling margin avoiding card overlap
 * -----------------------------------------------------------------------------
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(
    isServerConnected: Boolean,
    viewModel: DiaryViewModel = viewModel { DiaryViewModel() }
) {
    val palette = GradPathThemeTokens.palette
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current

    val diary by viewModel.diary.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val selectedTierFilter by viewModel.selectedTierFilter.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val showAddUniDialog by viewModel.showAddUniDialog.collectAsState()
    val showAddOutreachDialog by viewModel.showAddOutreachDialog.collectAsState()
    val showExportModal by viewModel.showExportModal.collectAsState()
    val exportContent by viewModel.exportContent.collectAsState()
    val coldEmailModalData by viewModel.coldEmailModalData.collectAsState()
    val isGeneratingEmail by viewModel.isGeneratingEmail.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showFilterRow by remember { mutableStateOf(false) }
    var selectedOutreachStatusFilter by remember { mutableStateOf("ALL") }
    var showExportDropdown by remember { mutableStateOf(false) }

    // Bottom Sheet active models
    var sheetUniversity by remember { mutableStateOf<WatchlistUniversity?>(null) }
    var sheetOutreach by remember { mutableStateOf<ProfessorOutreach?>(null) }

    LaunchedEffect(isServerConnected) {
        if (isServerConnected) {
            viewModel.fetchDiary(isServerConnected)
        }
    }

    // Seed fallback if server or local diary is empty
    val displayUniversities = remember(diary.universities) {
        if (diary.universities.isEmpty()) DiaryDefaults.SEED_UNIVERSITIES else diary.universities
    }
    val displayOutreach = remember(diary.outreach) {
        if (diary.outreach.isEmpty()) DiaryDefaults.SEED_OUTREACH else diary.outreach
    }

    // Filtered lists
    val filteredUniversities = remember(displayUniversities, selectedTierFilter, searchQuery) {
        displayUniversities.filter { uni ->
            val matchesTier = when (selectedTierFilter) {
                "DREAM" -> uni.tier == UniversityTier.DREAM
                "TARGET" -> uni.tier == UniversityTier.TARGET
                "SAFE" -> uni.tier == UniversityTier.SAFE
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                uni.name.contains(searchQuery, ignoreCase = true) ||
                uni.country.contains(searchQuery, ignoreCase = true) ||
                uni.programName.contains(searchQuery, ignoreCase = true) ||
                uni.deadline.contains(searchQuery, ignoreCase = true)

            matchesTier && matchesSearch
        }
    }

    val filteredOutreach = remember(displayOutreach, selectedOutreachStatusFilter, searchQuery) {
        displayOutreach.filter { prof ->
            val matchesStatus = when (selectedOutreachStatusFilter) {
                "NOT_CONTACTED" -> prof.status == OutreachStatus.NOT_CONTACTED
                "EMAIL_SENT" -> prof.status == OutreachStatus.EMAIL_SENT
                "FOLLOW_UP_SENT" -> prof.status == OutreachStatus.FOLLOW_UP_SENT
                "INTERVIEW_SCHEDULED" -> prof.status == OutreachStatus.INTERVIEW_SCHEDULED
                "POSITIVE_REPLY" -> prof.status == OutreachStatus.POSITIVE_REPLY
                "REJECTED" -> prof.status == OutreachStatus.REJECTED
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                prof.professorName.contains(searchQuery, ignoreCase = true) ||
                prof.university.contains(searchQuery, ignoreCase = true) ||
                prof.department.contains(searchQuery, ignoreCase = true) ||
                prof.researchArea.contains(searchQuery, ignoreCase = true)

            matchesStatus && matchesSearch
        }
    }

    val dreamCount = remember(displayUniversities) { displayUniversities.count { it.tier == UniversityTier.DREAM } }
    val targetCount = remember(displayUniversities) { displayUniversities.count { it.tier == UniversityTier.TARGET } }
    val safeCount = remember(displayUniversities) { displayUniversities.count { it.tier == UniversityTier.SAFE } }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(palette.canvas)) {
        val isMobile = maxWidth < 768.dp

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = if (isMobile) GradPathSpace.lg else GradPathSpace.xxl,
                    top = if (isMobile) GradPathSpace.sm else GradPathSpace.lg,
                    end = if (isMobile) GradPathSpace.lg else GradPathSpace.xxl,
                    bottom = if (isMobile) 88.dp else GradPathSpace.xxl
                ),
                verticalArrangement = Arrangement.spacedBy(GradPathSpace.md)
            ) {
                // -------------------------------------------------------------
                // 1. CLEAN, FOCUSED TOP HEADER ("Application Diary" ONLY)
                // -------------------------------------------------------------
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(GradPathSpace.xs)) {
                        Text(
                            text = "Application Diary",
                            fontSize = if (isMobile) 22.sp else 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary,
                            letterSpacing = (-0.4).sp
                        )

                        // Optional Toast message
                        AnimatedVisibility(visible = toastMessage != null) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(GradPathRadii.rS))
                                    .background(palette.academicBlueSoft)
                                    .border(1.dp, palette.academicBlueBorder, RoundedCornerShape(GradPathRadii.rS))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        toastMessage ?: "",
                                        fontSize = 12.sp,
                                        color = palette.onAcademicBlueSoft,
                                        fontWeight = FontWeight.Medium
                                    )
                                    IconButton(
                                        onClick = { viewModel.dismissToast() },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, tint = palette.academicBlue, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // -------------------------------------------------------------
                // 2. SIMPLE, CLEAN SEGMENTED TABS (CONCISE LABELS)
                // -------------------------------------------------------------
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(GradPathRadii.rPill))
                            .background(palette.surfaceLow)
                            .border(1.dp, palette.borderSoft, RoundedCornerShape(GradPathRadii.rPill))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CleanTabItem(
                            label = "🏛️ Universities (${displayUniversities.size})",
                            isSelected = activeTab == DiaryTab.UNIVERSITIES,
                            onClick = {
                                viewModel.setActiveTab(DiaryTab.UNIVERSITIES)
                                searchQuery = ""
                            },
                            modifier = Modifier.weight(1f),
                            palette = palette
                        )
                        CleanTabItem(
                            label = "👨‍🏫 Faculty (${displayOutreach.size})",
                            isSelected = activeTab == DiaryTab.PROFESSOR_OUTREACH,
                            onClick = {
                                viewModel.setActiveTab(DiaryTab.PROFESSOR_OUTREACH)
                                searchQuery = ""
                            },
                            modifier = Modifier.weight(1f),
                            palette = palette
                        )
                    }
                }

                // -------------------------------------------------------------
                // 3. SINGLE-LINE STANDARD SEARCH BAR & FILTER ROW
                // -------------------------------------------------------------
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(GradPathSpace.xs)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(GradPathSpace.xs)
                        ) {
                            DiarySearchBar(
                                query = searchQuery,
                                onQueryChange = { searchQuery = it },
                                placeholder = if (activeTab == DiaryTab.UNIVERSITIES) "Search universities or programs..." else "Search faculty or research topics...",
                                onClear = { searchQuery = "" },
                                palette = palette,
                                modifier = Modifier.weight(1f)
                            )

                            // Filter toggle button
                            val hasFilter = if (activeTab == DiaryTab.UNIVERSITIES) selectedTierFilter != "ALL" else selectedOutreachStatusFilter != "ALL"

                            IconButton(
                                onClick = { showFilterRow = !showFilterRow },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(GradPathRadii.rPill))
                                    .background(if (showFilterRow || hasFilter) palette.academicBlueSoft else palette.card)
                                    .border(
                                        1.dp,
                                        if (showFilterRow || hasFilter) palette.academicBlue else palette.borderSoft,
                                        RoundedCornerShape(GradPathRadii.rPill)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Filter",
                                    tint = if (showFilterRow || hasFilter) palette.academicBlue else palette.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Collapsible category filter pills
                        AnimatedVisibility(visible = showFilterRow) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(GradPathSpace.xs),
                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                            ) {
                                if (activeTab == DiaryTab.UNIVERSITIES) {
                                    item {
                                        GChip(
                                            label = "All (${displayUniversities.size})",
                                            tone = if (selectedTierFilter == "ALL") GChipTone.Brand else GChipTone.Neutral,
                                            onClick = { viewModel.setSelectedTierFilter("ALL") }
                                        )
                                    }
                                    item {
                                        GChip(
                                            label = "🟣 Dream ($dreamCount)",
                                            tone = if (selectedTierFilter == "DREAM") GChipTone.Brand else GChipTone.Neutral,
                                            onClick = { viewModel.setSelectedTierFilter("DREAM") }
                                        )
                                    }
                                    item {
                                        GChip(
                                            label = "🔵 Target ($targetCount)",
                                            tone = if (selectedTierFilter == "TARGET") GChipTone.Brand else GChipTone.Neutral,
                                            onClick = { viewModel.setSelectedTierFilter("TARGET") }
                                        )
                                    }
                                    item {
                                        GChip(
                                            label = "🟢 Safe ($safeCount)",
                                            tone = if (selectedTierFilter == "SAFE") GChipTone.Brand else GChipTone.Neutral,
                                            onClick = { viewModel.setSelectedTierFilter("SAFE") }
                                        )
                                    }
                                } else {
                                    item {
                                        GChip(
                                            label = "All (${displayOutreach.size})",
                                            tone = if (selectedOutreachStatusFilter == "ALL") GChipTone.Brand else GChipTone.Neutral,
                                            onClick = { selectedOutreachStatusFilter = "ALL" }
                                        )
                                    }
                                    item {
                                        GChip(
                                            label = "✉️ Sent",
                                            tone = if (selectedOutreachStatusFilter == "EMAIL_SENT") GChipTone.Brand else GChipTone.Neutral,
                                            onClick = { selectedOutreachStatusFilter = "EMAIL_SENT" }
                                        )
                                    }
                                    item {
                                        GChip(
                                            label = "⏰ Follow-up",
                                            tone = if (selectedOutreachStatusFilter == "FOLLOW_UP_SENT") GChipTone.Brand else GChipTone.Neutral,
                                            onClick = { selectedOutreachStatusFilter = "FOLLOW_UP_SENT" }
                                        )
                                    }
                                    item {
                                        GChip(
                                            label = "💬 Positive",
                                            tone = if (selectedOutreachStatusFilter == "POSITIVE_REPLY") GChipTone.Brand else GChipTone.Neutral,
                                            onClick = { selectedOutreachStatusFilter = "POSITIVE_REPLY" }
                                        )
                                    }
                                    item {
                                        GChip(
                                            label = "🤝 Interview",
                                            tone = if (selectedOutreachStatusFilter == "INTERVIEW_SCHEDULED") GChipTone.Brand else GChipTone.Neutral,
                                            onClick = { selectedOutreachStatusFilter = "INTERVIEW_SCHEDULED" }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // -------------------------------------------------------------
                // 4. MAIN CARDS LIST (WELL-ORGANIZED, CONFLICT-FREE)
                // -------------------------------------------------------------
                if (activeTab == DiaryTab.UNIVERSITIES) {
                    if (filteredUniversities.isEmpty()) {
                        item {
                            EmptyStatePlate(
                                icon = Icons.Default.School,
                                title = if (searchQuery.isNotBlank() || selectedTierFilter != "ALL") "No matching universities" else "Watchlist is empty",
                                subtitle = "Track target graduate programs, funding opportunities, and upcoming deadlines.",
                                actionText = if (searchQuery.isNotBlank() || selectedTierFilter != "ALL") "Clear Filters" else "+ Add Target University",
                                onAction = {
                                    if (searchQuery.isNotBlank() || selectedTierFilter != "ALL") {
                                        searchQuery = ""
                                        viewModel.setSelectedTierFilter("ALL")
                                    } else {
                                        viewModel.setShowAddUniDialog(true)
                                    }
                                },
                                palette = palette
                            )
                        }
                    } else {
                        items(filteredUniversities, key = { it.id }) { uni ->
                            FocusedUniversityCard(
                                uni = uni,
                                palette = palette,
                                onClick = { sheetUniversity = uni }
                            )
                        }
                    }
                } else {
                    if (filteredOutreach.isEmpty()) {
                        item {
                            EmptyStatePlate(
                                icon = Icons.Default.PersonSearch,
                                title = if (searchQuery.isNotBlank() || selectedOutreachStatusFilter != "ALL") "No matching faculty" else "No outreach logged",
                                subtitle = "Record professor outreach, align research papers, and generate AI cold emails.",
                                actionText = if (searchQuery.isNotBlank() || selectedOutreachStatusFilter != "ALL") "Clear Filters" else "+ Log Faculty Outreach",
                                onAction = {
                                    if (searchQuery.isNotBlank() || selectedOutreachStatusFilter != "ALL") {
                                        searchQuery = ""
                                        selectedOutreachStatusFilter = "ALL"
                                    } else {
                                        viewModel.setShowAddOutreachDialog(true)
                                    }
                                },
                                palette = palette
                            )
                        }
                    } else {
                        items(filteredOutreach, key = { it.id }) { outreach ->
                            FocusedProfessorCard(
                                outreach = outreach,
                                palette = palette,
                                onClick = { sheetOutreach = outreach },
                                onQuickEmail = {
                                    viewModel.generateColdEmail(
                                        ColdEmailRequest(
                                            professorName = outreach.professorName,
                                            university = outreach.university,
                                            researchArea = outreach.researchArea,
                                            studentSpecificInterest = outreach.paperAlignment
                                        ),
                                        isServerConnected
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 5. PROPERLY POSITIONED FLOATING ACTION BUTTON
            // -------------------------------------------------------------
            FloatingActionButton(
                onClick = {
                    if (activeTab == DiaryTab.UNIVERSITIES) viewModel.setShowAddUniDialog(true)
                    else viewModel.setShowAddOutreachDialog(true)
                },
                containerColor = palette.academicBlue,
                contentColor = palette.onAcademicBlue,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = GradPathSpace.lg,
                        bottom = GradPathSpace.lg
                    )
                    .size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (activeTab == DiaryTab.UNIVERSITIES) "Add University" else "Log Outreach",
                    tint = palette.onAcademicBlue,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }

    // -------------------------------------------------------------------------
    // BOTTOM SHEETS: DETAILED DOSSIERS WITHOUT MAIN SCREEN CLUTTER
    // -------------------------------------------------------------------------

    // University Dossier Bottom Sheet
    if (sheetUniversity != null) {
        val uni = sheetUniversity!!
        ModalBottomSheet(
            onDismissRequest = { sheetUniversity = null },
            containerColor = palette.card,
            scrimColor = Color.Black.copy(alpha = 0.5f),
            shape = RoundedCornerShape(topStart = GradPathRadii.rXl, topEnd = GradPathRadii.rXl)
        ) {
            UniversityDossierSheetContent(
                uni = uni,
                palette = palette,
                onClose = { sheetUniversity = null },
                onOpenUrl = {
                    if (uni.officialUrl.isNotBlank()) {
                        uriHandler.openUri(uni.officialUrl)
                    }
                },
                onDelete = {
                    viewModel.deleteUniversity(uni.id, isServerConnected)
                    sheetUniversity = null
                }
            )
        }
    }

    // Professor Outreach Dossier Bottom Sheet
    if (sheetOutreach != null) {
        val outreach = sheetOutreach!!
        ModalBottomSheet(
            onDismissRequest = { sheetOutreach = null },
            containerColor = palette.card,
            scrimColor = Color.Black.copy(alpha = 0.5f),
            shape = RoundedCornerShape(topStart = GradPathRadii.rXl, topEnd = GradPathRadii.rXl)
        ) {
            ProfessorDossierSheetContent(
                outreach = outreach,
                palette = palette,
                onClose = { sheetOutreach = null },
                onGenerateEmail = {
                    viewModel.generateColdEmail(
                        ColdEmailRequest(
                            professorName = outreach.professorName,
                            university = outreach.university,
                            researchArea = outreach.researchArea,
                            studentSpecificInterest = outreach.paperAlignment
                        ),
                        isServerConnected
                    )
                },
                onDelete = {
                    viewModel.deleteOutreach(outreach.id, isServerConnected)
                    sheetOutreach = null
                },
                onCopyEmail = {
                    clipboardManager.setText(AnnotatedString(outreach.email))
                }
            )
        }
    }

    // -------------------------------------------------------------------------
    // MODAL DIALOGS: ADD UNIVERSITY & LOG OUTREACH
    // -------------------------------------------------------------------------

    if (showAddUniDialog) {
        AddUniversityDialog(
            palette = palette,
            onDismiss = { viewModel.setShowAddUniDialog(false) },
            onConfirm = { viewModel.saveUniversity(it, isServerConnected) }
        )
    }

    if (showAddOutreachDialog) {
        AddOutreachDialog(
            palette = palette,
            onDismiss = { viewModel.setShowAddOutreachDialog(false) },
            onConfirm = { viewModel.saveOutreach(it, isServerConnected) }
        )
    }

    // Export Modal Dialog
    if (showExportModal != null) {
        ExportDossierModal(
            title = "Exported ${showExportModal}",
            content = exportContent,
            palette = palette,
            onDismiss = { viewModel.closeExportModal() },
            onCopy = {
                clipboardManager.setText(AnnotatedString(exportContent))
                viewModel.closeExportModal()
            }
        )
    }

    // AI Cold Email Draft Modal
    if (coldEmailModalData != null) {
        ColdEmailDraftModal(
            email = coldEmailModalData!!,
            palette = palette,
            onDismiss = { viewModel.closeColdEmailModal() },
            onCopy = {
                val fullDraft = "Subject: ${coldEmailModalData!!.subject}\n\n${coldEmailModalData!!.body}"
                clipboardManager.setText(AnnotatedString(fullDraft))
                viewModel.closeColdEmailModal()
            }
        )
    }
}

// =============================================================================
// SUB-COMPONENTS: SEARCH BAR, CARDS & DOSSIER SHEETS
// =============================================================================

/**
 * Standard Single-Line Search Bar (Never wraps placeholder into 2 lines)
 */
@Composable
private fun DiarySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    onClear: () -> Unit,
    palette: GradPathPalette,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(GradPathRadii.rPill))
            .background(palette.card)
            .border(BorderStroke(1.dp, palette.borderSoft), RoundedCornerShape(GradPathRadii.rPill))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = palette.textMuted,
            modifier = Modifier.size(18.dp)
        )
        HGap(8.dp)
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = placeholder,
                    color = palette.textMuted,
                    fontSize = 13.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = palette.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                ),
                cursorBrush = SolidColor(palette.academicBlue),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (query.isNotEmpty()) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear search",
                    tint = palette.textMuted,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun CleanTabItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    palette: GradPathPalette
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(GradPathRadii.rPill))
            .background(if (isSelected) palette.card else Color.Transparent)
            .then(
                if (isSelected) Modifier.border(1.dp, palette.borderSoft, RoundedCornerShape(GradPathRadii.rPill))
                else Modifier
            )
            .clickable { onClick() }
            .padding(vertical = 9.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) palette.textPrimary else palette.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * -----------------------------------------------------------------------------
 * FocusedUniversityCard — Clean, Conflict-Free University Card
 *
 * Solves:
 *   - Title & Badge conflict eliminated (Title has full width)
 *   - Concise Tier badge (🟣 Dream / 🔵 Target / 🟢 Safe)
 *   - Metadata organized neatly into a pill row
 *   - Tapping opens comprehensive Bottom Sheet
 * -----------------------------------------------------------------------------
 */
@Composable
fun FocusedUniversityCard(
    uni: WatchlistUniversity,
    palette: GradPathPalette,
    onClick: () -> Unit
) {
    val tierColor = when (uni.tier) {
        UniversityTier.DREAM -> Color(0xFF8B5CF6)
        UniversityTier.TARGET -> palette.academicBlue
        UniversityTier.SAFE -> palette.verifiedEmerald
    }

    val tierShortLabel = when (uni.tier) {
        UniversityTier.DREAM -> "🟣 Dream"
        UniversityTier.TARGET -> "🔵 Target"
        UniversityTier.SAFE -> "🟢 Safe"
    }

    GCard(
        modifier = Modifier.fillMaxWidth(),
        padding = PaddingValues(GradPathSpace.lg),
        hasShadow = true,
        onClick = onClick
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Row 1: Full-Width Title Area (No badge crowding!)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Initial square badge
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(GradPathRadii.rM))
                        .background(tierColor.copy(alpha = 0.12f))
                        .border(1.dp, tierColor.copy(alpha = 0.25f), RoundedCornerShape(GradPathRadii.rM)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uni.name.firstOrNull()?.toString() ?: "U",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = tierColor
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = uni.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = uni.programName,
                        fontSize = 12.sp,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = palette.textMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Row 2: Metadata Pills (Clean, uncrowded)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tier Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(GradPathRadii.rPill))
                        .background(tierColor.copy(alpha = 0.12f))
                        .border(1.dp, tierColor.copy(alpha = 0.3f), RoundedCornerShape(GradPathRadii.rPill))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = tierShortLabel,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = tierColor
                    )
                }

                // Country Tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(GradPathRadii.rPill))
                        .background(palette.surfaceLow)
                        .border(1.dp, palette.borderSoft, RoundedCornerShape(GradPathRadii.rPill))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = getCountryFormatted(uni.country),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = palette.textSecondary
                    )
                }

                // Degree Track Tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(GradPathRadii.rPill))
                        .background(palette.surfaceLow)
                        .border(1.dp, palette.borderSoft, RoundedCornerShape(GradPathRadii.rPill))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = uni.degreeTrack,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = palette.textSecondary
                    )
                }
            }

            // Row 3: Key Telemetry Strip (Funding & Deadline)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(GradPathRadii.rS))
                    .background(palette.surfaceLow)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Verified,
                        contentDescription = null,
                        tint = palette.verifiedEmerald,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = uni.fundingStatus,
                        fontSize = 11.sp,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = "Deadline",
                        tint = palette.alertAmber,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = uni.deadline,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.alertAmber
                    )
                }
            }
        }
    }
}

/**
 * -----------------------------------------------------------------------------
 * FocusedProfessorCard — Clean Faculty Outreach Card
 * -----------------------------------------------------------------------------
 */
@Composable
fun FocusedProfessorCard(
    outreach: ProfessorOutreach,
    palette: GradPathPalette,
    onClick: () -> Unit,
    onQuickEmail: () -> Unit
) {
    val statusColor = when (outreach.status) {
        OutreachStatus.NOT_CONTACTED -> palette.textSecondary
        OutreachStatus.EMAIL_SENT -> palette.academicBlue
        OutreachStatus.FOLLOW_UP_SENT -> palette.alertAmber
        OutreachStatus.INTERVIEW_SCHEDULED -> palette.verifiedEmerald
        OutreachStatus.POSITIVE_REPLY -> palette.accentSky
        OutreachStatus.REJECTED -> palette.dangerRed
    }

    GCard(
        modifier = Modifier.fillMaxWidth(),
        padding = PaddingValues(GradPathSpace.lg),
        hasShadow = true,
        onClick = onClick
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Row 1: Avatar + Full Name + University
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GAvatar(name = outreach.professorName, size = 38.dp)

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = outreach.professorName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = outreach.university,
                        fontSize = 12.sp,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = palette.textMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Row 2: Status Tag & Research Focus
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(GradPathRadii.rPill))
                        .background(statusColor.copy(alpha = 0.12f))
                        .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(GradPathRadii.rPill))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = outreach.status.label,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(GradPathRadii.rPill))
                        .background(palette.surfaceLow)
                        .border(1.dp, palette.borderSoft, RoundedCornerShape(GradPathRadii.rPill))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .weight(1f, fill = false)
                ) {
                    Text(
                        text = outreach.researchArea,
                        fontSize = 10.sp,
                        color = palette.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Row 3: Follow-Up & Action Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(GradPathRadii.rS))
                    .background(palette.surfaceLow)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        tint = if (outreach.followUpDueDate != null) palette.alertAmber else palette.textMuted,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = if (outreach.followUpDueDate != null) "Follow-Up: ${outreach.followUpDueDate}" else "Ready to contact",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (outreach.followUpDueDate != null) palette.alertAmber else palette.textMuted
                    )
                }

                GButton(
                    label = "AI Cold Email",
                    onClick = onQuickEmail,
                    variant = GBtnVariant.Soft,
                    size = GBtnSize.XS,
                    leadingIcon = {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = palette.academicBlue,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                )
            }
        }
    }
}

/**
 * -----------------------------------------------------------------------------
 * University Dossier Bottom Sheet Content
 * -----------------------------------------------------------------------------
 */
@Composable
private fun UniversityDossierSheetContent(
    uni: WatchlistUniversity,
    palette: GradPathPalette,
    onClose: () -> Unit,
    onOpenUrl: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GradPathSpace.lg)
            .padding(bottom = GradPathSpace.xxl),
        verticalArrangement = Arrangement.spacedBy(GradPathSpace.md)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                GKicker(text = "TARGET DOSSIER", color = palette.academicBlue)
                Text(
                    text = uni.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Text(
                    text = "${uni.country} · ${uni.programName}",
                    fontSize = 12.sp,
                    color = palette.textSecondary
                )
            }
            IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = palette.textMuted, modifier = Modifier.size(18.dp))
            }
        }

        // Admission & Funding Matrix Card
        GCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(GradPathSpace.md)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Application Deadline", fontSize = 11.sp, color = palette.textMuted)
                        VGap(2.dp)
                        Text(uni.deadline, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = palette.alertAmber)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Target Tier", fontSize = 11.sp, color = palette.textMuted)
                        VGap(2.dp)
                        Text(uni.tier.label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = palette.academicBlue)
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(palette.borderSoft))

                Column {
                    Text("Funding & Fellowship Policy", fontSize = 11.sp, color = palette.textMuted)
                    VGap(2.dp)
                    Text(uni.fundingStatus, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = palette.verifiedEmerald)
                }
            }
        }

        // Student Strategy & Notes
        GCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(GradPathSpace.md)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                GKicker(text = "CANDIDATE NOTES & LAB PREFERENCES", color = palette.academicBlue)
                Text(
                    text = if (uni.notes.isNotBlank()) uni.notes else "No personal notes recorded yet.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = palette.textPrimary
                )
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GradPathSpace.sm)
        ) {
            if (uni.officialUrl.isNotBlank()) {
                GButton(
                    label = "Department Portal",
                    onClick = onOpenUrl,
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
                    modifier = Modifier.weight(1f)
                )
            }

            GButton(
                label = "Remove",
                onClick = onDelete,
                variant = GBtnVariant.Secondary,
                size = GBtnSize.MD,
                leadingIcon = {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = palette.dangerRed,
                        modifier = Modifier.size(14.dp)
                    )
                },
                modifier = Modifier.weight(0.6f)
            )
        }
    }
}

/**
 * -----------------------------------------------------------------------------
 * Professor Outreach Dossier Bottom Sheet Content
 * -----------------------------------------------------------------------------
 */
@Composable
private fun ProfessorDossierSheetContent(
    outreach: ProfessorOutreach,
    palette: GradPathPalette,
    onClose: () -> Unit,
    onGenerateEmail: () -> Unit,
    onDelete: () -> Unit,
    onCopyEmail: () -> Unit
) {
    var copied by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GradPathSpace.lg)
            .padding(bottom = GradPathSpace.xxl),
        verticalArrangement = Arrangement.spacedBy(GradPathSpace.md)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                GAvatar(name = outreach.professorName, size = 44.dp)
                Column {
                    Text(
                        text = outreach.professorName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "${outreach.university} · ${outreach.department}",
                        fontSize = 12.sp,
                        color = palette.textSecondary
                    )
                }
            }
            IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = palette.textMuted, modifier = Modifier.size(18.dp))
            }
        }

        // Email Contact Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(GradPathRadii.rS))
                .background(palette.surfaceLow)
                .border(1.dp, palette.borderSoft, RoundedCornerShape(GradPathRadii.rS))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Email, contentDescription = null, tint = palette.academicBlue, modifier = Modifier.size(15.dp))
                Text(outreach.email, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = palette.textPrimary)
            }
            IconButton(
                onClick = {
                    onCopyEmail()
                    copied = true
                },
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = if (copied) palette.verifiedEmerald else palette.textMuted,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Research & Alignment Card
        GCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(GradPathSpace.md)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GKicker(text = "RESEARCH TOPIC & PAPER ALIGNMENT", color = palette.academicBlue)
                Text(
                    text = outreach.researchArea,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textPrimary
                )
                if (outreach.paperAlignment.isNotBlank()) {
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(palette.borderSoft))
                    Text(
                        text = outreach.paperAlignment,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = palette.textSecondary
                    )
                }
            }
        }

        // Actions: Generate AI Cold Email & Delete
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GradPathSpace.sm)
        ) {
            GButton(
                label = "Generate AI Cold Email",
                onClick = onGenerateEmail,
                variant = GBtnVariant.Primary,
                size = GBtnSize.MD,
                leadingIcon = {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = palette.onAcademicBlue, modifier = Modifier.size(15.dp))
                },
                modifier = Modifier.weight(1f)
            )

            GButton(
                label = "Remove",
                onClick = onDelete,
                variant = GBtnVariant.Secondary,
                size = GBtnSize.MD,
                leadingIcon = {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = palette.dangerRed, modifier = Modifier.size(14.dp))
                },
                modifier = Modifier.weight(0.6f)
            )
        }
    }
}

/**
 * -----------------------------------------------------------------------------
 * Clean Empty State Plate
 * -----------------------------------------------------------------------------
 */
@Composable
private fun EmptyStatePlate(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    actionText: String,
    onAction: () -> Unit,
    palette: GradPathPalette
) {
    GCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = GradPathSpace.lg),
        padding = PaddingValues(GradPathSpace.xl)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(GradPathSpace.sm)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(palette.surfaceLow),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = palette.academicBlue, modifier = Modifier.size(24.dp))
            }

            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = palette.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            VGap(4.dp)

            GButton(
                label = actionText,
                onClick = onAction,
                variant = GBtnVariant.Secondary,
                size = GBtnSize.SM
            )
        }
    }
}

// =============================================================================
// MODAL DIALOGS: ADD UNIVERSITY & LOG OUTREACH
// =============================================================================

@Composable
private fun AddUniversityDialog(
    palette: GradPathPalette,
    onDismiss: () -> Unit,
    onConfirm: (WatchlistUniversity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("USA") }
    var program by remember { mutableStateOf("Ph.D. in Computer Science") }
    var deadline by remember { mutableStateOf("December 15, 2026") }
    var funding by remember { mutableStateOf("100% Guaranteed RA/TA + Full Tuition") }
    var selectedTier by remember { mutableStateOf(UniversityTier.TARGET) }

    Dialog(onDismissRequest = onDismiss) {
        GCard(
            modifier = Modifier.fillMaxWidth().padding(GradPathSpace.xs),
            padding = PaddingValues(GradPathSpace.lg)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(GradPathSpace.sm)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        GKicker(text = "TARGET PORTFOLIO", color = palette.academicBlue)
                        Text("Add Target University", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = palette.textMuted, modifier = Modifier.size(16.dp))
                    }
                }

                StyledInputField(label = "University Name", value = name, onValueChange = { name = it }, placeholder = "e.g. Carnegie Mellon University", palette = palette)
                StyledInputField(label = "Country", value = country, onValueChange = { country = it }, placeholder = "e.g. USA, Germany, UK", palette = palette)
                StyledInputField(label = "Degree & Program", value = program, onValueChange = { program = it }, placeholder = "e.g. Ph.D. in Language Technologies", palette = palette)
                StyledInputField(label = "Application Deadline", value = deadline, onValueChange = { deadline = it }, placeholder = "e.g. December 15, 2026", palette = palette)
                StyledInputField(label = "Funding Guarantee", value = funding, onValueChange = { funding = it }, placeholder = "e.g. Full Funding / Fellowship", palette = palette)

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Target Tier", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = palette.textSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(GradPathSpace.xs)) {
                        UniversityTier.entries.forEach { tier ->
                            val isSel = selectedTier == tier
                            val tierColor = when (tier) {
                                UniversityTier.DREAM -> Color(0xFF8B5CF6)
                                UniversityTier.TARGET -> palette.academicBlue
                                UniversityTier.SAFE -> palette.verifiedEmerald
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(GradPathRadii.rPill))
                                    .background(if (isSel) tierColor.copy(alpha = 0.15f) else palette.surfaceLow)
                                    .border(1.dp, if (isSel) tierColor else palette.borderSoft, RoundedCornerShape(GradPathRadii.rPill))
                                    .clickable { selectedTier = tier }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = tier.name,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) tierColor else palette.textSecondary
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(GradPathSpace.sm)
                ) {
                    GButton(
                        label = "Cancel",
                        onClick = onDismiss,
                        variant = GBtnVariant.Secondary,
                        size = GBtnSize.MD,
                        modifier = Modifier.weight(1f)
                    )
                    GButton(
                        label = "Save University",
                        onClick = {
                            if (name.isNotBlank()) {
                                onConfirm(
                                    WatchlistUniversity(
                                        id = "uni-${System.currentTimeMillis()}",
                                        name = name.trim(),
                                        country = country.trim(),
                                        programName = program.trim(),
                                        deadline = deadline.trim(),
                                        fundingStatus = funding.trim(),
                                        tier = selectedTier
                                    )
                                )
                            }
                        },
                        variant = GBtnVariant.Primary,
                        size = GBtnSize.MD,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddOutreachDialog(
    palette: GradPathPalette,
    onDismiss: () -> Unit,
    onConfirm: (ProfessorOutreach) -> Unit
) {
    var profName by remember { mutableStateOf("") }
    var uni by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var paper by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        GCard(
            modifier = Modifier.fillMaxWidth().padding(GradPathSpace.xs),
            padding = PaddingValues(GradPathSpace.lg)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(GradPathSpace.sm)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        GKicker(text = "FACULTY CADENCE", color = palette.academicBlue)
                        Text("Log Professor Outreach", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = palette.textMuted, modifier = Modifier.size(16.dp))
                    }
                }

                StyledInputField(label = "Professor Full Name", value = profName, onValueChange = { profName = it }, placeholder = "e.g. Prof. Graham Neubig", palette = palette)
                StyledInputField(label = "University / Lab", value = uni, onValueChange = { uni = it }, placeholder = "e.g. CMU Language Technologies", palette = palette)
                StyledInputField(label = "Email Address", value = email, onValueChange = { email = it }, placeholder = "e.g. professor@cmu.edu", palette = palette)
                StyledInputField(label = "Research Area", value = area, onValueChange = { area = it }, placeholder = "e.g. NLP, Code Generation, LLMs", palette = palette)
                StyledInputField(label = "Recent Paper / Alignment", value = paper, onValueChange = { paper = it }, placeholder = "e.g. Code Agent Evaluation & Reasoning", palette = palette)

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(GradPathSpace.sm)
                ) {
                    GButton(
                        label = "Cancel",
                        onClick = onDismiss,
                        variant = GBtnVariant.Secondary,
                        size = GBtnSize.MD,
                        modifier = Modifier.weight(1f)
                    )
                    GButton(
                        label = "Log Outreach",
                        onClick = {
                            if (profName.isNotBlank() && email.isNotBlank()) {
                                onConfirm(
                                    ProfessorOutreach(
                                        id = "prof-${System.currentTimeMillis()}",
                                        professorName = profName.trim(),
                                        university = uni.trim(),
                                        email = email.trim(),
                                        researchArea = area.trim(),
                                        paperAlignment = paper.trim(),
                                        status = OutreachStatus.NOT_CONTACTED
                                    )
                                )
                            }
                        },
                        variant = GBtnVariant.Primary,
                        size = GBtnSize.MD,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportDossierModal(
    title: String,
    content: String,
    palette: GradPathPalette,
    onDismiss: () -> Unit,
    onCopy: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        GCard(
            modifier = Modifier.fillMaxWidth().padding(GradPathSpace.xs),
            padding = PaddingValues(GradPathSpace.lg)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(GradPathSpace.md)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        GKicker(text = "EXPORT WORKSPACE", color = palette.academicBlue)
                        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = palette.textMuted, modifier = Modifier.size(16.dp))
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .clip(RoundedCornerShape(GradPathRadii.rS))
                        .background(palette.surfaceLow)
                        .border(1.dp, palette.borderSoft, RoundedCornerShape(GradPathRadii.rS))
                        .padding(GradPathSpace.md)
                        .verticalScroll(rememberScrollState())
                ) {
                    SelectionContainer {
                        Text(
                            text = content,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = palette.textPrimary
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GradPathSpace.sm)
                ) {
                    GButton(
                        label = "Close",
                        onClick = onDismiss,
                        variant = GBtnVariant.Secondary,
                        size = GBtnSize.MD,
                        modifier = Modifier.weight(1f)
                    )
                    GButton(
                        label = "Copy to Clipboard",
                        onClick = onCopy,
                        variant = GBtnVariant.Primary,
                        size = GBtnSize.MD,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ColdEmailDraftModal(
    email: ColdEmailResponse,
    palette: GradPathPalette,
    onDismiss: () -> Unit,
    onCopy: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        GCard(
            modifier = Modifier.fillMaxWidth().padding(GradPathSpace.xs),
            padding = PaddingValues(GradPathSpace.lg)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(GradPathSpace.sm)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = palette.academicBlue, modifier = Modifier.size(18.dp))
                        Column {
                            GKicker(text = "AI GENERATED DRAFT", color = palette.academicBlue)
                            Text("Personalized Cold Email", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = palette.textMuted, modifier = Modifier.size(16.dp))
                    }
                }

                Text(
                    text = "Subject: ${email.subject}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.academicBlue
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .clip(RoundedCornerShape(GradPathRadii.rS))
                        .background(palette.surfaceLow)
                        .border(1.dp, palette.borderSoft, RoundedCornerShape(GradPathRadii.rS))
                        .padding(GradPathSpace.md)
                        .verticalScroll(rememberScrollState())
                ) {
                    SelectionContainer {
                        Text(
                            text = email.body,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = palette.textPrimary
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GradPathSpace.sm)
                ) {
                    GButton(
                        label = "Close",
                        onClick = onDismiss,
                        variant = GBtnVariant.Secondary,
                        size = GBtnSize.MD,
                        modifier = Modifier.weight(1f)
                    )
                    GButton(
                        label = "Copy Email Draft",
                        onClick = onCopy,
                        variant = GBtnVariant.Primary,
                        size = GBtnSize.MD,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StyledInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    palette: GradPathPalette
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = palette.textSecondary)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(GradPathRadii.rS))
                .background(palette.surfaceLow)
                .border(1.dp, palette.borderSoft, RoundedCornerShape(GradPathRadii.rS))
                .padding(horizontal = 12.dp, vertical = 9.dp)
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    fontSize = 12.sp,
                    color = palette.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = palette.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                ),
                cursorBrush = SolidColor(palette.academicBlue),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun getCountryFormatted(country: String): String {
    val clean = country.trim()
    return when {
        clean.contains("USA", ignoreCase = true) || clean.contains("United States", ignoreCase = true) -> "🇺🇸 USA"
        clean.contains("Germany", ignoreCase = true) || clean.contains("Deutschland", ignoreCase = true) -> "🇩🇪 Germany"
        clean.contains("UK", ignoreCase = true) || clean.contains("United Kingdom", ignoreCase = true) || clean.contains("Britain", ignoreCase = true) -> "🇬🇧 UK"
        clean.contains("Canada", ignoreCase = true) -> "🇨🇦 Canada"
        clean.contains("Switzerland", ignoreCase = true) -> "🇨🇭 Switzerland"
        clean.contains("Sweden", ignoreCase = true) -> "🇸🇪 Sweden"
        clean.contains("Australia", ignoreCase = true) -> "🇦🇺 Australia"
        clean.contains("Japan", ignoreCase = true) -> "🇯🇵 Japan"
        clean.contains("Singapore", ignoreCase = true) -> "🇸🇬 Singapore"
        clean.contains("France", ignoreCase = true) -> "🇫🇷 France"
        clean.contains("Netherlands", ignoreCase = true) -> "🇳🇱 Netherlands"
        else -> "🌐 $clean"
    }
}
