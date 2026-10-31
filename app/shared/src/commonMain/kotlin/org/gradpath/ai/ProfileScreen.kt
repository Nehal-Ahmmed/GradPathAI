package org.gradpath.ai

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.gradpath.ai.ui.components.*
import org.gradpath.ai.viewmodel.ProfileViewModel

/**
 * -----------------------------------------------------------------------------
 * ProfileScreen — Dual Mode Architecture (Observe Mode vs. Edit Mode)
 *
 * 1. OBSERVE MODE (Default):
 *    - No form inputs, zero editing clutter.
 *    - To-the-point modular summary cards categorized cleanly.
 *    - High-density readability without cognitive overload.
 *
 * 2. EDIT MODE:
 *    - Dedicated editing environment with explicit Save / Cancel.
 *    - Well-organized form controls for credentials, scores, and preferences.
 * -----------------------------------------------------------------------------
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    isServerConnected: Boolean,
    viewModel: ProfileViewModel = viewModel { ProfileViewModel() }
) {
    val palette = GradPathThemeTokens.palette
    val clipboardManager = LocalClipboardManager.current

    val profile by viewModel.profile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val auditResult by viewModel.auditResult.collectAsState()
    val isAuditing by viewModel.isAuditing.collectAsState()
    val showDocAuditor by viewModel.showDocAuditor.collectAsState()

    // Route to fullscreen Document Auditor if requested
    if (showDocAuditor) {
        DocumentAuditScreen(
            isServerConnected = isServerConnected,
            onBack = { viewModel.setShowDocAuditor(false) }
        )
        return
    }

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    val scoreBreakdown = remember(profile) {
        ProfileScoringEngine.calculateScore(profile)
    }

    // Mode state: 0 = Observe Mode, 1 = Edit Mode
    var isEditMode by remember { mutableStateOf(false) }
    var editDraft by remember(profile) { mutableStateOf(profile) }
    var copiedToast by remember { mutableStateOf(false) }

    LaunchedEffect(copiedToast) {
        if (copiedToast) {
            kotlinx.coroutines.delay(2000)
            copiedToast = false
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val pullOffset = remember { Animatable(0f) }
    val refreshThresholdPx = with(density) { 72.dp.toPx() }
    val maxPullPx = with(density) { 130.dp.toPx() }
    val scrollState = rememberScrollState()

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
                val isAtTop = scrollState.value == 0
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
                        viewModel.refreshProfile()
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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.canvas)
    ) {
        val isMobile = maxWidth < 768.dp
        val horizontalPadding = if (isMobile) GradPathSpace.md else GradPathSpace.xxl

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isMobile) Modifier.nestedScroll(nestedScrollConnection) else Modifier)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(0, pullOffset.value.roundToInt()) }
                    .verticalScroll(scrollState)
                    .padding(
                        start = horizontalPadding,
                        end = horizontalPadding,
                        top = GradPathSpace.md,
                        bottom = if (isMobile) 96.dp else GradPathSpace.xxxl
                    ),
                verticalArrangement = Arrangement.spacedBy(GradPathSpace.md)
            ) {
                // =================================================================
                // 1. TOP CANDIDATE IDENTITY HEADER
                // =================================================================
                ProfileCandidateHeader(
                    profile = if (isEditMode) editDraft else profile,
                    scoreBreakdown = if (isEditMode) ProfileScoringEngine.calculateScore(editDraft) else scoreBreakdown,
                    isEditMode = isEditMode,
                    isMobile = isMobile,
                    isLoading = isLoading,
                    onRefresh = { viewModel.refreshProfile() },
                    onToggleEditMode = {
                        if (!isEditMode) {
                            editDraft = profile
                            isEditMode = true
                        } else {
                            viewModel.updateProfile(editDraft)
                            isEditMode = false
                        }
                    },
                    onOpenDocAuditor = { viewModel.setShowDocAuditor(true) },
                    onRunAudit = { viewModel.runAudit(isServerConnected) },
                    isAuditing = isAuditing,
                    isServerConnected = isServerConnected
                )

            // =================================================================
            // 2. MAIN CONTENT: OBSERVE MODE vs. EDIT MODE
            // =================================================================
            AnimatedContent(
                targetState = isEditMode,
                transitionSpec = {
                    fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(120))
                },
                label = "ProfileModeTransition"
            ) { inEditMode ->
                if (inEditMode) {
                    // ──────── EDIT MODE ────────
                    ProfileEditModeView(
                        draft = editDraft,
                        onDraftChange = { editDraft = it },
                        onSave = {
                            viewModel.updateProfile(editDraft)
                            isEditMode = false
                        },
                        onCancel = {
                            editDraft = profile
                            isEditMode = false
                        },
                        isMobile = isMobile
                    )
                } else {
                    // ──────── OBSERVE MODE ────────
                    ProfileObserveModeView(
                        profile = profile,
                        scoreBreakdown = scoreBreakdown,
                        auditResult = auditResult,
                        isAuditing = isAuditing,
                        isServerConnected = isServerConnected,
                        isMobile = isMobile,
                        onEditClick = {
                            editDraft = profile
                            isEditMode = true
                        },
                        onRunAudit = { viewModel.runAudit(isServerConnected) },
                        onOpenDocAuditor = { viewModel.setShowDocAuditor(true) },
                        onCopyAuditResult = { text ->
                            clipboardManager.setText(AnnotatedString(text))
                            copiedToast = true
                        },
                        copiedToast = copiedToast
                    )
                }
            }
        }

        // Bouncing Pull-to-Refresh Indicator Badge on Mobile
        if (isMobile && (pullOffset.value > 0f || isLoading)) {
            val isReady = pullOffset.value >= refreshThresholdPx
            val progress = (pullOffset.value / refreshThresholdPx).coerceIn(0f, 1f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(0, (pullOffset.value * 0.6f - 38.dp.toPx()).roundToInt().coerceAtLeast(8.dp.toPx().roundToInt())) }
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(GradPathRadii.rPill),
                    color = palette.card,
                    border = BorderStroke(1.dp, if (isReady || isLoading) palette.academicBlue else palette.borderSoft),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(13.dp),
                                strokeWidth = 2.dp,
                                color = palette.academicBlue
                            )
                            Text(
                                text = "Syncing with Cloud Firestore...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = palette.academicBlue
                            )
                        } else {
                            Icon(
                                imageVector = if (isReady) Icons.Default.Refresh else Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = if (isReady) palette.academicBlue else palette.textMuted,
                                modifier = Modifier
                                    .size(14.dp)
                                    .rotate(progress * 180f)
                            )
                            Text(
                                text = if (isReady) "Release to reload Firestore" else "Pull to sync Firestore",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isReady) palette.academicBlue else palette.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
}

// =============================================================================
// HEADER & MODE SELECTOR
// =============================================================================

@Composable
private fun ProfileCandidateHeader(
    profile: UserProfile,
    scoreBreakdown: ScoreBreakdown,
    isEditMode: Boolean,
    isMobile: Boolean,
    onToggleEditMode: () -> Unit,
    onOpenDocAuditor: () -> Unit,
    onRunAudit: () -> Unit,
    onRefresh: () -> Unit,
    isLoading: Boolean,
    isAuditing: Boolean,
    isServerConnected: Boolean
) {
    val palette = GradPathThemeTokens.palette
    val candidateName = profile.fullName.ifBlank { "Candidate Profile" }
    val degreeTrackLabel = if (profile.target.targetDegree == TargetDegree.PHD) "Direct Ph.D. Track" else "Funded Master's Track"
    val institutionDisplay = profile.academic.institution.ifBlank { "Institution Not Set" }
    val majorDisplay = profile.academic.major.ifBlank { "Major Not Set" }

    GCard(
        modifier = Modifier.fillMaxWidth(),
        radius = GradPathRadii.rL,
        padding = PaddingValues(GradPathSpace.md)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(GradPathSpace.sm)) {
            // Identity Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GAvatar(
                    name = candidateName,
                    size = if (isMobile) 46.dp else 52.dp,
                    hasRing = true
                )
                HGap(GradPathSpace.md)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(GradPathSpace.sm)
                    ) {
                        Text(
                            text = candidateName,
                            fontSize = if (isMobile) 17.sp else 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary,
                            letterSpacing = (-0.3).sp
                        )
                        GChip(
                            label = if (isEditMode) "Editing" else "Verified Dossier",
                            tone = if (isEditMode) GChipTone.Brand else GChipTone.Ok,
                            dense = true
                        )
                    }
                    Text(
                        text = "$majorDisplay · $institutionDisplay",
                        fontSize = 12.sp,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Target: ${profile.target.targetTerm} · $degreeTrackLabel",
                        fontSize = 11.sp,
                        color = palette.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Quick Actions & Readiness Pill
            if (isMobile) {
                Column(verticalArrangement = Arrangement.spacedBy(GradPathSpace.sm)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ScoreBadgePill(score = scoreBreakdown.totalScore, isMobile = true)
                        Surface(
                            onClick = onRefresh,
                            enabled = !isLoading,
                            shape = RoundedCornerShape(GradPathRadii.rPill),
                            color = palette.chip,
                            border = BorderStroke(1.dp, palette.borderSoft),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(11.dp),
                                        strokeWidth = 1.8.dp,
                                        color = palette.academicBlue
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Sync from Firestore",
                                        tint = palette.textSecondary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Text(
                                    text = if (isLoading) "Syncing..." else "Sync Cloud",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isLoading) palette.academicBlue else palette.textSecondary
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(GradPathSpace.sm)
                    ) {
                        GButton(
                            label = if (isEditMode) "Save Changes" else "Edit Profile",
                            onClick = onToggleEditMode,
                            variant = if (isEditMode) GBtnVariant.Primary else GBtnVariant.Secondary,
                            size = GBtnSize.SM,
                            modifier = Modifier.weight(1f),
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isEditMode) Icons.Default.Check else Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        )
                        GButton(
                            label = "SOP/CV Auditor",
                            onClick = onOpenDocAuditor,
                            variant = GBtnVariant.Secondary,
                            size = GBtnSize.SM,
                            modifier = Modifier.weight(1f),
                            leadingIcon = {
                                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(13.dp))
                            }
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ScoreBadgePill(score = scoreBreakdown.totalScore, isMobile = false)
                    Row(horizontalArrangement = Arrangement.spacedBy(GradPathSpace.md)) {
                        GButton(
                            label = if (isLoading) "Syncing..." else "Sync Cloud",
                            onClick = onRefresh,
                            enabled = !isLoading,
                            variant = GBtnVariant.Soft,
                            size = GBtnSize.SM,
                            leadingIcon = {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(13.dp),
                                        strokeWidth = 2.dp,
                                        color = palette.academicBlue
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Sync from Firestore",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        )
                        GButton(
                            label = if (isEditMode) "Save Changes" else "Edit Profile",
                            onClick = onToggleEditMode,
                            variant = if (isEditMode) GBtnVariant.Primary else GBtnVariant.Secondary,
                            size = GBtnSize.SM,
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isEditMode) Icons.Default.Check else Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        )
                        GButton(
                            label = "SOP & CV Auditor",
                            onClick = onOpenDocAuditor,
                            variant = GBtnVariant.Secondary,
                            size = GBtnSize.SM,
                            leadingIcon = {
                                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        )
                        GButton(
                            label = if (isAuditing) "Auditing..." else "Trigger AI Audit",
                            onClick = onRunAudit,
                            enabled = isServerConnected && !isAuditing,
                            variant = GBtnVariant.Soft,
                            size = GBtnSize.SM,
                            leadingIcon = {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// OBSERVE MODE VIEW (Clutter-Free, To-the-Point Summary)
// =============================================================================

@Composable
private fun ProfileObserveModeView(
    profile: UserProfile,
    scoreBreakdown: ScoreBreakdown,
    auditResult: String,
    isAuditing: Boolean,
    isServerConnected: Boolean,
    isMobile: Boolean,
    onEditClick: () -> Unit,
    onRunAudit: () -> Unit,
    onOpenDocAuditor: () -> Unit,
    onCopyAuditResult: (String) -> Unit,
    copiedToast: Boolean
) {
    val palette = GradPathThemeTokens.palette

    Column(verticalArrangement = Arrangement.spacedBy(GradPathSpace.md)) {

        // 1. READINESS SCORE & 4-QUADRANT SUMMARY
        GCard(
            modifier = Modifier.fillMaxWidth(),
            radius = GradPathRadii.rL,
            padding = PaddingValues(GradPathSpace.md)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(GradPathSpace.md)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        GKicker(text = "ADMISSION READINESS")
                        Text(
                            text = if (profile.target.targetDegree == TargetDegree.PHD) "Direct Ph.D. Calibration" else "Funded Master's Calibration",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(GradPathRadii.rPill))
                            .background(palette.academicBlueSoft)
                            .padding(horizontal = 9.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${scoreBreakdown.totalScore}/100",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = palette.academicBlue
                        )
                    }
                }

                // Dial & 4-Metric Progress Bars
                if (isMobile) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(GradPathSpace.md)
                    ) {
                        RadialReadinessDial(score = scoreBreakdown.totalScore, size = 96.dp)

                        Column(verticalArrangement = Arrangement.spacedBy(GradPathSpace.sm), modifier = Modifier.fillMaxWidth()) {
                            ScoreMetricBar(label = "Research Experience", score = scoreBreakdown.researchScore, max = scoreBreakdown.researchMax, icon = Icons.Default.Science)
                            ScoreMetricBar(label = "Academic CGPA", score = scoreBreakdown.academicScore, max = scoreBreakdown.academicMax, icon = Icons.Default.School)
                            ScoreMetricBar(label = "Test Benchmarks", score = scoreBreakdown.testScore, max = scoreBreakdown.testMax, icon = Icons.Default.QueryStats)
                            ScoreMetricBar(label = "Dossier Preparation", score = scoreBreakdown.prepScore, max = scoreBreakdown.prepMax, icon = Icons.Default.FolderOpen)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(GradPathSpace.xl)
                    ) {
                        Box(modifier = Modifier.weight(0.35f), contentAlignment = Alignment.Center) {
                            RadialReadinessDial(score = scoreBreakdown.totalScore, size = 114.dp)
                        }
                        Column(modifier = Modifier.weight(0.65f), verticalArrangement = Arrangement.spacedBy(GradPathSpace.sm)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(GradPathSpace.md)) {
                                ScoreMetricBar(label = "Research Experience", score = scoreBreakdown.researchScore, max = scoreBreakdown.researchMax, icon = Icons.Default.Science, modifier = Modifier.weight(1f))
                                ScoreMetricBar(label = "Academic CGPA", score = scoreBreakdown.academicScore, max = scoreBreakdown.academicMax, icon = Icons.Default.School, modifier = Modifier.weight(1f))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(GradPathSpace.md)) {
                                ScoreMetricBar(label = "Test Benchmarks", score = scoreBreakdown.testScore, max = scoreBreakdown.testMax, icon = Icons.Default.QueryStats, modifier = Modifier.weight(1f))
                                ScoreMetricBar(label = "Dossier Preparation", score = scoreBreakdown.prepScore, max = scoreBreakdown.prepMax, icon = Icons.Default.FolderOpen, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // 2. ACADEMIC & TEST BENCHMARKS (Clean Grid Tiles)
        GCard(
            modifier = Modifier.fillMaxWidth(),
            radius = GradPathRadii.rL,
            padding = PaddingValues(GradPathSpace.md)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(GradPathSpace.sm)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        GKicker(text = "ACADEMICS & TESTS")
                        Text(
                            text = "Degree Record & Examination Scores",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                    }
                    Text(
                        text = "Edit ↗",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.academicBlue,
                        modifier = Modifier
                            .clickable(onClick = onEditClick)
                            .padding(4.dp)
                    )
                }

                VGap(GradPathSpace.xs)

                // 4 Academic Tiles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GradPathSpace.sm)
                ) {
                    SummaryDataTile(label = "CGPA", value = if (profile.academic.gpa > 0) "${profile.academic.gpa}/4.0" else "Not Set", modifier = Modifier.weight(1f))
                    SummaryDataTile(label = "MAJOR", value = profile.academic.major.ifBlank { "Not Set" }, modifier = Modifier.weight(1.3f))
                    SummaryDataTile(label = "GRAD YEAR", value = "${profile.academic.graduationYear}", modifier = Modifier.weight(1f))
                }

                // 3 Test Tiles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GradPathSpace.sm)
                ) {
                    SummaryDataTile(label = "IELTS/TOEFL", value = if (profile.tests.ieltsOverall > 0) "${profile.tests.ieltsOverall}" else "N/A", modifier = Modifier.weight(1f))
                    SummaryDataTile(label = "GRE QUANT", value = if (profile.tests.greQuant > 0) "${profile.tests.greQuant}/170" else "N/A", modifier = Modifier.weight(1f))
                    SummaryDataTile(label = "GRE VERBAL", value = if (profile.tests.greVerbal > 0) "${profile.tests.greVerbal}/170" else "N/A", modifier = Modifier.weight(1f))
                }
            }
        }

        // 3. RESEARCH OUTPUT & PUBLICATION FOOTPRINT
        GCard(
            modifier = Modifier.fillMaxWidth(),
            radius = GradPathRadii.rL,
            padding = PaddingValues(GradPathSpace.md)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(GradPathSpace.sm)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        GKicker(text = "RESEARCH PORTFOLIO")
                        Text(
                            text = "Publications & Research Footprint",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                    }
                    Text(
                        text = "Edit ↗",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.academicBlue,
                        modifier = Modifier
                            .clickable(onClick = onEditClick)
                            .padding(4.dp)
                    )
                }

                VGap(GradPathSpace.xs)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GradPathSpace.sm)
                ) {
                    SummaryDataTile(label = "CONFERENCES", value = "${profile.research.conferencePapers}", modifier = Modifier.weight(1f))
                    SummaryDataTile(label = "JOURNALS", value = "${profile.research.journalPapers}", modifier = Modifier.weight(1f))
                    SummaryDataTile(label = "CITATIONS", value = "${profile.research.citations}", modifier = Modifier.weight(1f))
                    SummaryDataTile(label = "PREPRINTS", value = if (profile.research.hasPreprint) "Active" else "None", modifier = Modifier.weight(1f))
                }

                if (profile.research.researchAreas.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(GradPathRadii.rS))
                            .background(palette.surfaceLow)
                            .padding(horizontal = GradPathSpace.md, vertical = GradPathSpace.sm)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(text = "RESEARCH DOMAINS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = palette.textMuted, fontFamily = FontFamily.Monospace)
                            Text(text = profile.research.researchAreas, fontSize = 12.sp, color = palette.textPrimary)
                        }
                    }
                }
            }
        }

        // 4. DOSSIER CHECKLIST & TARGET GOALS SUMMARY
        GCard(
            modifier = Modifier.fillMaxWidth(),
            radius = GradPathRadii.rL,
            padding = PaddingValues(GradPathSpace.md)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(GradPathSpace.sm)) {
                GKicker(text = "APPLICATION MATERIALS & GOALS")
                Text(
                    text = "Dossier Readiness & Target Intake",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )

                VGap(GradPathSpace.xs)

                // 3 Materials Status Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GradPathSpace.sm)
                ) {
                    MaterialStatusBadge(label = "SOP Draft", isReady = profile.prep.sopReady, modifier = Modifier.weight(1f))
                    MaterialStatusBadge(label = "Academic CV", isReady = profile.prep.cvReady, modifier = Modifier.weight(1f))
                    MaterialStatusBadge(label = "3 LORs", isReady = profile.prep.lorCount >= 3, detail = "${profile.prep.lorCount}/3", modifier = Modifier.weight(1f))
                }

                // Target Goals strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GradPathRadii.rS))
                        .background(palette.surfaceLow)
                        .padding(horizontal = GradPathSpace.md, vertical = GradPathSpace.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "TARGET CYCLE & REGIONS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = palette.textMuted, fontFamily = FontFamily.Monospace)
                        Text(
                            text = "${profile.target.targetTerm} · ${profile.target.targetCountries.joinToString(", ")}",
                            fontSize = 12.sp,
                            color = palette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (profile.target.fullFundingMandatory) {
                        GChip(label = "100% Funding", tone = GChipTone.Ok, dense = true)
                    }
                }
            }
        }

        // 5. AI GAP AUDIT CONSOLE
        GCard(
            modifier = Modifier.fillMaxWidth(),
            radius = GradPathRadii.rL,
            padding = PaddingValues(GradPathSpace.md)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(GradPathSpace.sm)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(GradPathSpace.sm),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(GradPathRadii.rS))
                                .background(palette.academicBlueSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = palette.academicBlue, modifier = Modifier.size(16.dp))
                        }
                        Column {
                            GKicker(text = "MACHINE INTELLIGENCE")
                            Text(
                                text = "Antigravity AI Gap Audit",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                        }
                    }

                    if (auditResult.isNotBlank()) {
                        GButton(
                            label = if (copiedToast) "Copied!" else "Copy",
                            onClick = { onCopyAuditResult(auditResult) },
                            variant = GBtnVariant.Ghost,
                            size = GBtnSize.XS,
                            leadingIcon = {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                            }
                        )
                    }
                }

                GButton(
                    label = if (isAuditing) "Evaluating Dossier with Antigravity AI..." else "Run Diagnostic Admission Audit",
                    onClick = onRunAudit,
                    enabled = isServerConnected && !isAuditing,
                    variant = GBtnVariant.Primary,
                    size = GBtnSize.MD,
                    fullWidth = true,
                    leadingIcon = {
                        if (isAuditing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = palette.onAcademicBlue, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                )

                if (auditResult.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(GradPathRadii.rM))
                            .background(palette.surfaceLow)
                            .border(1.dp, palette.borderSoft, RoundedCornerShape(GradPathRadii.rM))
                            .padding(GradPathSpace.md)
                    ) {
                        MarkdownText(
                            markdown = auditResult,
                            textColor = palette.textPrimary,
                            fontSize = 12.5.sp,
                            lineHeight = 19.sp
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// EDIT MODE VIEW (Focused, Dedicated Form Controls with Save/Cancel)
// =============================================================================

@Composable
private fun ProfileEditModeView(
    draft: UserProfile,
    onDraftChange: (UserProfile) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    isMobile: Boolean
) {
    val palette = GradPathThemeTokens.palette

    Column(verticalArrangement = Arrangement.spacedBy(GradPathSpace.md)) {
        // Sticky/Top Action Bar for Save & Cancel
        GCard(
            modifier = Modifier.fillMaxWidth(),
            radius = GradPathRadii.rL,
            padding = PaddingValues(GradPathSpace.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Edit Academic Profile",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "Adjust metrics to recalculate your real-time readiness score",
                        fontSize = 11.5.sp,
                        color = palette.textMuted
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(GradPathSpace.sm)) {
                    GButton(
                        label = "Cancel",
                        onClick = onCancel,
                        variant = GBtnVariant.Ghost,
                        size = GBtnSize.SM
                    )
                    GButton(
                        label = "Save",
                        onClick = onSave,
                        variant = GBtnVariant.Primary,
                        size = GBtnSize.SM,
                        leadingIcon = {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    )
                }
            }
        }

        // 0. CANDIDATE IDENTITY
        GCard(
            modifier = Modifier.fillMaxWidth(),
            radius = GradPathRadii.rL,
            padding = PaddingValues(GradPathSpace.md)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(GradPathSpace.sm)) {
                GKicker(text = "CANDIDATE IDENTITY")
                Text(
                    text = "Personal & Dossier Information",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )

                ProfileInputField(
                    value = draft.fullName,
                    onValueChange = { onDraftChange(draft.copy(fullName = it)) },
                    label = "Candidate Full Name",
                    placeholder = "e.g. Nehal Ahmmed"
                )
            }
        }

        // 1. APPLICATION DEGREE TRACK
        GCard(
            modifier = Modifier.fillMaxWidth(),
            radius = GradPathRadii.rL,
            padding = PaddingValues(GradPathSpace.md)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(GradPathSpace.sm)) {
                GKicker(text = "EVALUATION PROTOCOL")
                Text(
                    text = "Select Application Degree Track",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )

                val isPhd = draft.target.targetDegree == TargetDegree.PHD

                if (isMobile) {
                    Column(verticalArrangement = Arrangement.spacedBy(GradPathSpace.sm)) {
                        TrackOptionCard(
                            title = "Direct Ph.D. Research Track",
                            weights = "40% Research · 30% CGPA · 15% SOP/LOR · 15% Tests",
                            isSelected = isPhd,
                            onClick = {
                                if (!isPhd) onDraftChange(draft.copy(target = draft.target.copy(targetDegree = TargetDegree.PHD)))
                            }
                        )
                        TrackOptionCard(
                            title = "Funded Master's Track",
                            weights = "35% CGPA · 25% Tests · 20% Research · 20% SOP/LOR",
                            isSelected = !isPhd,
                            onClick = {
                                if (isPhd) onDraftChange(draft.copy(target = draft.target.copy(targetDegree = TargetDegree.MASTERS)))
                            }
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(GradPathSpace.md)
                    ) {
                        TrackOptionCard(
                            title = "Direct Ph.D. Research Track",
                            weights = "40% Research · 30% CGPA · 15% SOP/LOR · 15% Tests",
                            isSelected = isPhd,
                            onClick = {
                                if (!isPhd) onDraftChange(draft.copy(target = draft.target.copy(targetDegree = TargetDegree.PHD)))
                            },
                            modifier = Modifier.weight(1f)
                        )
                        TrackOptionCard(
                            title = "Funded Master's Track",
                            weights = "35% CGPA · 25% Tests · 20% Research · 20% SOP/LOR",
                            isSelected = !isPhd,
                            onClick = {
                                if (isPhd) onDraftChange(draft.copy(target = draft.target.copy(targetDegree = TargetDegree.MASTERS)))
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 2. UNDERGRADUATE ACADEMIC RECORD
        GCard(
            modifier = Modifier.fillMaxWidth(),
            radius = GradPathRadii.rL,
            padding = PaddingValues(GradPathSpace.md)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(GradPathSpace.sm)) {
                GKicker(text = "ACADEMIC FOUNDATION")
                Text(
                    text = "Edit Undergraduate Record",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )

                if (isMobile) {
                    Column(verticalArrangement = Arrangement.spacedBy(GradPathSpace.sm)) {
                        ProfileInputField(
                            value = if (draft.academic.gpa > 0) draft.academic.gpa.toString() else "",
                            onValueChange = {
                                val g = it.toDoubleOrNull() ?: 0.0
                                onDraftChange(draft.copy(academic = draft.academic.copy(gpa = g)))
                            },
                            label = "Undergraduate CGPA (Scale 4.0)",
                            placeholder = "e.g. 3.88",
                            keyboardType = KeyboardType.Decimal
                        )
                        ProfileInputField(
                            value = draft.academic.major,
                            onValueChange = { onDraftChange(draft.copy(academic = draft.academic.copy(major = it))) },
                            label = "Major / Department",
                            placeholder = "e.g. Computer Science & Engineering"
                        )
                        ProfileInputField(
                            value = draft.academic.institution,
                            onValueChange = { onDraftChange(draft.copy(academic = draft.academic.copy(institution = it))) },
                            label = "University / Institution",
                            placeholder = "e.g. University of Dhaka"
                        )
                        ProfileInputField(
                            value = draft.academic.graduationYear.toString(),
                            onValueChange = {
                                val yr = it.toIntOrNull() ?: 2026
                                onDraftChange(draft.copy(academic = draft.academic.copy(graduationYear = yr)))
                            },
                            label = "Graduation Year",
                            placeholder = "2026",
                            keyboardType = KeyboardType.Number
                        )
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(GradPathSpace.md)) {
                        ProfileInputField(
                            value = if (draft.academic.gpa > 0) draft.academic.gpa.toString() else "",
                            onValueChange = {
                                val g = it.toDoubleOrNull() ?: 0.0
                                onDraftChange(draft.copy(academic = draft.academic.copy(gpa = g)))
                            },
                            label = "Undergraduate CGPA (Scale 4.0)",
                            placeholder = "e.g. 3.88",
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                        ProfileInputField(
                            value = draft.academic.major,
                            onValueChange = { onDraftChange(draft.copy(academic = draft.academic.copy(major = it))) },
                            label = "Major / Department",
                            placeholder = "e.g. Computer Science & Engineering",
                            modifier = Modifier.weight(1.5f)
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(GradPathSpace.md)) {
                        ProfileInputField(
                            value = draft.academic.institution,
                            onValueChange = { onDraftChange(draft.copy(academic = draft.academic.copy(institution = it))) },
                            label = "University / Institution",
                            placeholder = "e.g. University of Dhaka",
                            modifier = Modifier.weight(2f)
                        )
                        ProfileInputField(
                            value = draft.academic.graduationYear.toString(),
                            onValueChange = {
                                val yr = it.toIntOrNull() ?: 2026
                                onDraftChange(draft.copy(academic = draft.academic.copy(graduationYear = yr)))
                            },
                            label = "Graduation Year",
                            placeholder = "2026",
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 3. STANDARDIZED TEST SCORES
        GCard(
            modifier = Modifier.fillMaxWidth(),
            radius = GradPathRadii.rL,
            padding = PaddingValues(GradPathSpace.md)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(GradPathSpace.sm)) {
                GKicker(text = "TEST BENCHMARKS")
                Text(
                    text = "Edit Language & GRE Scores",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )

                if (isMobile) {
                    Column(verticalArrangement = Arrangement.spacedBy(GradPathSpace.sm)) {
                        ProfileInputField(
                            value = if (draft.tests.ieltsOverall > 0) draft.tests.ieltsOverall.toString() else "",
                            onValueChange = {
                                val s = it.toDoubleOrNull() ?: 0.0
                                onDraftChange(draft.copy(tests = draft.tests.copy(ieltsOverall = s)))
                            },
                            label = "IELTS / TOEFL Overall",
                            placeholder = "e.g. 8.0",
                            keyboardType = KeyboardType.Decimal
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(GradPathSpace.sm)) {
                            ProfileInputField(
                                value = if (draft.tests.greQuant > 0) draft.tests.greQuant.toString() else "",
                                onValueChange = {
                                    val q = it.toIntOrNull() ?: 0
                                    onDraftChange(draft.copy(tests = draft.tests.copy(greQuant = q)))
                                },
                                label = "GRE Quant (170)",
                                placeholder = "168",
                                keyboardType = KeyboardType.Number,
                                modifier = Modifier.weight(1f)
                            )
                            ProfileInputField(
                                value = if (draft.tests.greVerbal > 0) draft.tests.greVerbal.toString() else "",
                                onValueChange = {
                                    val v = it.toIntOrNull() ?: 0
                                    onDraftChange(draft.copy(tests = draft.tests.copy(greVerbal = v)))
                                },
                                label = "GRE Verbal (170)",
                                placeholder = "158",
                                keyboardType = KeyboardType.Number,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(GradPathSpace.md)) {
                        ProfileInputField(
                            value = if (draft.tests.ieltsOverall > 0) draft.tests.ieltsOverall.toString() else "",
                            onValueChange = {
                                val s = it.toDoubleOrNull() ?: 0.0
                                onDraftChange(draft.copy(tests = draft.tests.copy(ieltsOverall = s)))
                            },
                            label = "IELTS / TOEFL Overall Score",
                            placeholder = "e.g. 8.0",
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1.2f)
                        )
                        ProfileInputField(
                            value = if (draft.tests.greQuant > 0) draft.tests.greQuant.toString() else "",
                            onValueChange = {
                                val q = it.toIntOrNull() ?: 0
                                onDraftChange(draft.copy(tests = draft.tests.copy(greQuant = q)))
                            },
                            label = "GRE Quantitative (Max 170)",
                            placeholder = "168",
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f)
                        )
                        ProfileInputField(
                            value = if (draft.tests.greVerbal > 0) draft.tests.greVerbal.toString() else "",
                            onValueChange = {
                                val v = it.toIntOrNull() ?: 0
                                onDraftChange(draft.copy(tests = draft.tests.copy(greVerbal = v)))
                            },
                            label = "GRE Verbal (Max 170)",
                            placeholder = "158",
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 4. RESEARCH & PUBLICATIONS
        GCard(
            modifier = Modifier.fillMaxWidth(),
            radius = GradPathRadii.rL,
            padding = PaddingValues(GradPathSpace.md)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(GradPathSpace.sm)) {
                GKicker(text = "RESEARCH OUTPUT")
                Text(
                    text = "Edit Publications & Citations",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GradPathSpace.sm)
                ) {
                    PublicationCounterCard(
                        title = "Conferences",
                        count = draft.research.conferencePapers,
                        onIncrement = {
                            onDraftChange(draft.copy(research = draft.research.copy(conferencePapers = draft.research.conferencePapers + 1)))
                        },
                        onDecrement = {
                            if (draft.research.conferencePapers > 0) {
                                onDraftChange(draft.copy(research = draft.research.copy(conferencePapers = draft.research.conferencePapers - 1)))
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    PublicationCounterCard(
                        title = "Journals",
                        count = draft.research.journalPapers,
                        onIncrement = {
                            onDraftChange(draft.copy(research = draft.research.copy(journalPapers = draft.research.journalPapers + 1)))
                        },
                        onDecrement = {
                            if (draft.research.journalPapers > 0) {
                                onDraftChange(draft.copy(research = draft.research.copy(journalPapers = draft.research.journalPapers - 1)))
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    PublicationCounterCard(
                        title = "Citations",
                        count = draft.research.citations,
                        onIncrement = {
                            onDraftChange(draft.copy(research = draft.research.copy(citations = draft.research.citations + 5)))
                        },
                        onDecrement = {
                            val newC = (draft.research.citations - 5).coerceAtLeast(0)
                            onDraftChange(draft.copy(research = draft.research.copy(citations = newC)))
                        },
                        stepText = "+5",
                        modifier = Modifier.weight(1f)
                    )
                }

                ProfileInputField(
                    value = draft.research.researchAreas,
                    onValueChange = { onDraftChange(draft.copy(research = draft.research.copy(researchAreas = it))) },
                    label = "Primary Research Domains",
                    placeholder = "e.g. Natural Language Processing, Code LLMs"
                )

                // Preprint toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GradPathRadii.rM))
                        .clickable {
                            onDraftChange(draft.copy(research = draft.research.copy(hasPreprint = !draft.research.hasPreprint)))
                        }
                        .background(palette.surfaceLow)
                        .padding(horizontal = GradPathSpace.md, vertical = GradPathSpace.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ArXiv / TechRxiv Preprints Active",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "+5 bonus research points for preprint manuscripts",
                            fontSize = 11.sp,
                            color = palette.textMuted
                        )
                    }
                    Switch(
                        checked = draft.research.hasPreprint,
                        onCheckedChange = {
                            onDraftChange(draft.copy(research = draft.research.copy(hasPreprint = it)))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = palette.academicBlue,
                            checkedTrackColor = palette.academicBlueSoft
                        )
                    )
                }
            }
        }

        // 5. TARGET INTAKE & FULL FUNDING
        GCard(
            modifier = Modifier.fillMaxWidth(),
            radius = GradPathRadii.rL,
            padding = PaddingValues(GradPathSpace.md)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(GradPathSpace.sm)) {
                GKicker(text = "TARGET CYCLE")
                Text(
                    text = "Edit Admission Preferences",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )

                if (isMobile) {
                    Column(verticalArrangement = Arrangement.spacedBy(GradPathSpace.sm)) {
                        ProfileInputField(
                            value = draft.target.targetTerm,
                            onValueChange = { onDraftChange(draft.copy(target = draft.target.copy(targetTerm = it))) },
                            label = "Target Intake Term",
                            placeholder = "Fall 2026 / Fall 2027"
                        )
                        ProfileInputField(
                            value = draft.target.targetCountries.joinToString(", "),
                            onValueChange = {
                                val list = it.split(",").map { c -> c.trim() }.filter { c -> c.isNotEmpty() }
                                onDraftChange(draft.copy(target = draft.target.copy(targetCountries = list)))
                            },
                            label = "Target Countries (comma separated)",
                            placeholder = "USA, Germany, UK, Canada"
                        )
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(GradPathSpace.md)) {
                        ProfileInputField(
                            value = draft.target.targetTerm,
                            onValueChange = { onDraftChange(draft.copy(target = draft.target.copy(targetTerm = it))) },
                            label = "Target Intake Term",
                            placeholder = "Fall 2026 / Fall 2027",
                            modifier = Modifier.weight(1f)
                        )
                        ProfileInputField(
                            value = draft.target.targetCountries.joinToString(", "),
                            onValueChange = {
                                val list = it.split(",").map { c -> c.trim() }.filter { c -> c.isNotEmpty() }
                                onDraftChange(draft.copy(target = draft.target.copy(targetCountries = list)))
                            },
                            label = "Target Countries (comma separated)",
                            placeholder = "USA, Germany, UK, Canada",
                            modifier = Modifier.weight(1.8f)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GradPathRadii.rM))
                        .clickable {
                            onDraftChange(draft.copy(target = draft.target.copy(fullFundingMandatory = !draft.target.fullFundingMandatory)))
                        }
                        .background(palette.surfaceLow)
                        .padding(horizontal = GradPathSpace.md, vertical = GradPathSpace.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mandatory 100% Full Funding Only",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "Filter programs strictly with 100% tuition waiver + GRA/GTA stipend",
                            fontSize = 11.sp,
                            color = palette.textMuted
                        )
                    }
                    Switch(
                        checked = draft.target.fullFundingMandatory,
                        onCheckedChange = {
                            onDraftChange(draft.copy(target = draft.target.copy(fullFundingMandatory = it)))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = palette.verifiedEmerald,
                            checkedTrackColor = palette.verifiedSoft
                        )
                    )
                }
            }
        }

        // Bottom Save & Cancel Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GradPathSpace.md)
        ) {
            GButton(
                label = "Cancel Changes",
                onClick = onCancel,
                variant = GBtnVariant.Ghost,
                size = GBtnSize.MD,
                modifier = Modifier.weight(1f)
            )
            GButton(
                label = "Save Profile",
                onClick = onSave,
                variant = GBtnVariant.Primary,
                size = GBtnSize.MD,
                modifier = Modifier.weight(1f),
                leadingIcon = {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            )
        }
    }
}

// =============================================================================
// REUSABLE SUB-COMPONENTS
// =============================================================================

@Composable
private fun SummaryDataTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val palette = GradPathThemeTokens.palette
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(GradPathRadii.rM))
            .background(palette.surfaceLow)
            .border(1.dp, palette.borderSoft, RoundedCornerShape(GradPathRadii.rM))
            .padding(horizontal = GradPathSpace.sm, vertical = GradPathSpace.sm)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = palette.textMuted,
                maxLines = 1
            )
            Text(
                text = value,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MaterialStatusBadge(
    label: String,
    isReady: Boolean,
    detail: String? = null,
    modifier: Modifier = Modifier
) {
    val palette = GradPathThemeTokens.palette
    val statusColor = if (isReady) palette.verifiedEmerald else palette.textMuted
    val statusBg = if (isReady) palette.verifiedSoft else palette.surfaceLow

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(GradPathRadii.rM))
            .background(statusBg)
            .border(1.dp, if (isReady) palette.verifiedBorder else palette.borderSoft, RoundedCornerShape(GradPathRadii.rM))
            .padding(horizontal = GradPathSpace.sm, vertical = GradPathSpace.sm)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                color = palette.textSecondary,
                maxLines = 1
            )
            Text(
                text = detail ?: (if (isReady) "✓ Ready" else "Pending"),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RadialReadinessDial(score: Int, size: Dp) {
    val palette = GradPathThemeTokens.palette
    val animatedProgress by animateFloatAsState(
        targetValue = (score / 100f).coerceIn(0f, 1f),
        animationSpec = tween(500)
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(size)
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidth = (size.toPx() * 0.09f)
            drawArc(
                color = palette.borderSoft,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                color = if (score >= 75) palette.verifiedEmerald else palette.academicBlue,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score",
                fontSize = (size.value * 0.28f).sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = palette.textPrimary
            )
            Text(
                text = "/100",
                fontSize = (size.value * 0.11f).sp,
                fontFamily = FontFamily.Monospace,
                color = palette.textMuted
            )
        }
    }
}

@Composable
private fun ScoreMetricBar(
    label: String,
    score: Int,
    max: Int,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val palette = GradPathThemeTokens.palette
    val pct = if (max > 0) (score.toFloat() / max).coerceIn(0f, 1f) else 0f

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(GradPathRadii.rM))
            .background(palette.surfaceLow)
            .border(1.dp, palette.borderSoft, RoundedCornerShape(GradPathRadii.rM))
            .padding(GradPathSpace.md)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(GradPathSpace.xs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(GradPathSpace.xs)
                ) {
                    Icon(icon, contentDescription = null, tint = palette.academicBlue, modifier = Modifier.size(14.dp))
                    Text(
                        text = label,
                        fontSize = 11.5.sp,
                        color = palette.textSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "$score / $max",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
            }

            LinearProgressIndicator(
                progress = { pct },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = palette.academicBlue,
                trackColor = palette.borderSoft
            )
        }
    }
}

@Composable
private fun ScoreBadgePill(score: Int, isMobile: Boolean) {
    val palette = GradPathThemeTokens.palette
    val isCompetitive = score >= 75
    val badgeColor = if (isCompetitive) palette.verifiedEmerald else palette.academicBlue
    val badgeBg = if (isCompetitive) palette.verifiedSoft else palette.academicBlueSoft
    val tierLabel = if (score >= 80) "Tier-1 Competitive" else if (score >= 65) "Competitive Candidate" else "Developing Candidate"

    Row(
        modifier = Modifier
            .then(if (isMobile) Modifier.fillMaxWidth() else Modifier)
            .clip(RoundedCornerShape(GradPathRadii.rPill))
            .background(badgeBg)
            .border(1.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(GradPathRadii.rPill))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isMobile) Arrangement.Center else Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(badgeColor)
        )
        HGap(6.dp)
        Text(
            text = "$score/100",
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = badgeColor
        )
        Text(
            text = "· $tierLabel",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = palette.textSecondary,
            maxLines = 1
        )
    }
}

@Composable
private fun TrackOptionCard(
    title: String,
    weights: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = GradPathThemeTokens.palette
    val shape = RoundedCornerShape(GradPathRadii.rM)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isSelected) palette.card else palette.surfaceLow)
            .border(
                BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) palette.academicBlue else palette.borderSoft
                ),
                shape
            )
            .clickable(onClick = onClick)
            .padding(GradPathSpace.md)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) palette.academicBlue else palette.textPrimary
                )
                if (isSelected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = palette.academicBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = weights,
                fontSize = 10.5.sp,
                color = palette.textMuted,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun PublicationCounterCard(
    title: String,
    count: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    stepText: String = "+1",
    modifier: Modifier = Modifier
) {
    val palette = GradPathThemeTokens.palette
    val shape = RoundedCornerShape(GradPathRadii.rM)

    Box(
        modifier = modifier
            .clip(shape)
            .background(palette.surfaceLow)
            .border(1.dp, palette.borderSoft, shape)
            .padding(horizontal = 4.dp, vertical = GradPathSpace.sm)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$count",
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(palette.card)
                        .border(1.dp, palette.borderSoft, CircleShape)
                        .clickable(onClick = onDecrement),
                    contentAlignment = Alignment.Center
                ) {
                    Text("-", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                }
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(palette.academicBlue)
                        .clickable(onClick = onIncrement),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stepText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = palette.onAcademicBlue)
                }
            }
        }
    }
}

@Composable
private fun ProfileInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    val palette = GradPathThemeTokens.palette
    val shape = RoundedCornerShape(GradPathRadii.rM)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            color = palette.textSecondary
        )
        VGap(GradPathSpace.xs)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, fontSize = 13.sp, color = palette.textMuted) },
            singleLine = true,
            shape = shape,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = TextStyle(
                color = palette.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = palette.card,
                unfocusedContainerColor = palette.card,
                focusedBorderColor = palette.academicBlue,
                unfocusedBorderColor = palette.borderSoft,
                focusedTextColor = palette.textPrimary,
                unfocusedTextColor = palette.textPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
