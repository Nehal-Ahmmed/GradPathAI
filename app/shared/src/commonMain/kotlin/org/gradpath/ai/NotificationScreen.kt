package org.gradpath.ai

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class NotificationCategory(val label: String) {
    ALL("All"),
    CIRCULAR("Circulars"),
    WATCHDOG("Watchdogs"),
    DEADLINE("Deadlines"),
    AUDIT("Audits & Studio"),
    SYSTEM("System")
}

data class NotificationItem(
    val id: String,
    val category: NotificationCategory,
    val title: String,
    val sourceOrUniversity: String,
    val summary: String,
    val timestamp: String,
    val matchScore: Int? = null,
    val fundingStipend: String? = null,
    val isPriority: Boolean = false,
    val isUnread: Boolean = true,
    val targetScreen: Screen? = null
)

@Composable
fun NotificationScreen(
    isServerConnected: Boolean,
    onNavigateToFeed: () -> Unit,
    onNavigateToStudio: () -> Unit,
    onNavigateToWatchdogs: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToDiary: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onUnreadCountChanged: (Int) -> Unit = {}
) {
    val colors = GradPathTheme.colors

    var selectedCategory by remember { mutableStateOf(NotificationCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    var notifications by remember {
        mutableStateOf(
            listOf(
                NotificationItem(
                    id = "circ-1",
                    category = NotificationCategory.CIRCULAR,
                    title = "Generative Diffusion Models for Multi-Agent Systems",
                    sourceOrUniversity = "MIT CSAIL · Dr. Phillip Isola & Prof. Regina Barzilay",
                    summary = "Fully funded doctoral & postdoc research circular matching your profile. Research in neural discrete representations and multimodal agent architectures.",
                    timestamp = "8:00 AM Today",
                    matchScore = 98,
                    fundingStipend = "Full Tuition + \$54,000/yr",
                    isPriority = true,
                    isUnread = true,
                    targetScreen = Screen.FEED
                ),
                NotificationItem(
                    id = "circ-2",
                    category = NotificationCategory.CIRCULAR,
                    title = "Doctoral Fellowship in Geometric Deep Learning & Robotics",
                    sourceOrUniversity = "Max Planck Institute for Intelligent Systems · Tübingen",
                    summary = "Empirical Inference Dept. Opening for graduate fellows with strong background in SE(3) equivariance and reinforcement learning.",
                    timestamp = "7:55 AM Today",
                    matchScore = 95,
                    fundingStipend = "TV-L E13 (100%) + Research Allowance",
                    isPriority = false,
                    isUnread = true,
                    targetScreen = Screen.FEED
                ),
                NotificationItem(
                    id = "circ-3",
                    category = NotificationCategory.CIRCULAR,
                    title = "Cavendish Laboratory PhD Studentship in Quantum Machine Learning",
                    sourceOrUniversity = "University of Cambridge · Cavendish Laboratory",
                    summary = "EPSRC DTP Full Scholarship for 4-year doctoral research program in quantum algorithms and variational tensor networks.",
                    timestamp = "7:48 AM Today",
                    matchScore = 93,
                    fundingStipend = "£21,500/yr tax-free + College Fees",
                    isPriority = false,
                    isUnread = true,
                    targetScreen = Screen.FEED
                ),
                NotificationItem(
                    id = "circ-4",
                    category = NotificationCategory.CIRCULAR,
                    title = "HAI Graduate Fellowship & Visiting Scholar Call for Fall 2027",
                    sourceOrUniversity = "Stanford University · Human-Centered AI Institute",
                    summary = "Interdisciplinary doctoral cohort call in mechanistic interpretability, reasoning models, and safety alignment verification.",
                    timestamp = "7:40 AM Today",
                    matchScore = 91,
                    fundingStipend = "Complete Fellowship + Research Budget",
                    isPriority = false,
                    isUnread = true,
                    targetScreen = Screen.FEED
                ),
                NotificationItem(
                    id = "watch-1",
                    category = NotificationCategory.WATCHDOG,
                    title = "Watchdog #1 Triggered: 3 New Preprints on arXiv:quant-ph",
                    sourceOrUniversity = "Watchdog Portal · Quantum ML Cluster",
                    summary = "Autonomous crawler matched your monitored keywords with 3 freshly posted manuscripts citing Prof. John Preskill and Prof. Seth Lloyd.",
                    timestamp = "2 hours ago",
                    matchScore = 89,
                    isPriority = false,
                    isUnread = true,
                    targetScreen = Screen.WATCHDOGS
                ),
                NotificationItem(
                    id = "watch-2",
                    category = NotificationCategory.WATCHDOG,
                    title = "Stanford Faculty Portal Updated: Prof. Jure Leskovec",
                    sourceOrUniversity = "Watchdog Portal · Graph Neural Networks",
                    summary = "Faculty lab portal modified with candidate call: 'Actively considering PhD applicants for Fall 2027 with preprints in geometric ML'.",
                    timestamp = "5 hours ago",
                    matchScore = 94,
                    isPriority = true,
                    isUnread = true,
                    targetScreen = Screen.WATCHDOGS
                ),
                NotificationItem(
                    id = "dead-1",
                    category = NotificationCategory.DEADLINE,
                    title = "Oxford Clarendon Fund Application Closes in 8 Days",
                    sourceOrUniversity = "University of Oxford · Academic Funding Office",
                    summary = "Ensure your Research Proposal, Transcripts, and 3 Confidential Referee letters are submitted prior to November 5, 23:59 GMT.",
                    timestamp = "Yesterday 9:30 PM",
                    isPriority = true,
                    isUnread = true,
                    targetScreen = Screen.PROFILE
                ),
                NotificationItem(
                    id = "audit-1",
                    category = NotificationCategory.AUDIT,
                    title = "Statement of Purpose Readiness Score Re-calculated (88/100)",
                    sourceOrUniversity = "Research Studio · LLM Document Calibrator",
                    summary = "Incorporated latest research diary entries into SOP methodology section. Academic alignment increased by +4 points.",
                    timestamp = "Yesterday 4:15 PM",
                    matchScore = 88,
                    isPriority = false,
                    isUnread = false,
                    targetScreen = Screen.RESEARCH_STUDIO
                ),
                NotificationItem(
                    id = "circ-5",
                    category = NotificationCategory.CIRCULAR,
                    title = "ETH Zurich AI Center Doctoral Fellowship Call 2027",
                    sourceOrUniversity = "ETH Zürich · Department of Computer Science",
                    summary = "Open call across learning algorithms, foundations of AI, and robotics. Fully funded 4-year doctoral appointment with competitive Swiss salary.",
                    timestamp = "Yesterday 11:00 AM",
                    matchScore = 90,
                    fundingStipend = "CHF 75,000 - 84,000/yr",
                    isPriority = false,
                    isUnread = false,
                    targetScreen = Screen.FEED
                ),
                NotificationItem(
                    id = "watch-3",
                    category = NotificationCategory.WATCHDOG,
                    title = "Max Planck Tübingen Intake Cycle Verified",
                    sourceOrUniversity = "Watchdog Portal · European Labs",
                    summary = "Autonomous crawler verified that the portal opened the official application submission window for 2027 admissions.",
                    timestamp = "2 days ago",
                    matchScore = 87,
                    isPriority = false,
                    isUnread = false,
                    targetScreen = Screen.WATCHDOGS
                ),
                NotificationItem(
                    id = "dead-2",
                    category = NotificationCategory.DEADLINE,
                    title = "Cambridge Gates Trust Scholarship Pre-Registration",
                    sourceOrUniversity = "University of Cambridge · Trust Board",
                    summary = "Pre-registration checkpoint for non-UK doctoral applicants. Complete the eligibility verification form in your candidate portal.",
                    timestamp = "2 days ago",
                    isPriority = false,
                    isUnread = false,
                    targetScreen = Screen.PROFILE
                ),
                NotificationItem(
                    id = "audit-2",
                    category = NotificationCategory.AUDIT,
                    title = "Referee Recommendation Dossier Received",
                    sourceOrUniversity = "Application Dossier Manager",
                    summary = "Prof. S. Banerjee submitted confidential reference letter for MIT EECS Track. Dossier now has 2 of 3 required endorsements.",
                    timestamp = "3 days ago",
                    isPriority = false,
                    isUnread = false,
                    targetScreen = Screen.PROFILE
                ),
                NotificationItem(
                    id = "sys-1",
                    category = NotificationCategory.SYSTEM,
                    title = "Local Daemon Telemetry Sync Completed",
                    sourceOrUniversity = "Ktor Engine Daemon · 127.0.0.1:8080",
                    summary = "Cached 42 university circulars and 6 watchdog channels to local persistent database storage.",
                    timestamp = "3 days ago",
                    isPriority = false,
                    isUnread = false,
                    targetScreen = Screen.SETTINGS
                ),
                NotificationItem(
                    id = "dead-3",
                    category = NotificationCategory.DEADLINE,
                    title = "MIT EECS Graduate Admission Early Notification Track",
                    sourceOrUniversity = "MIT EECS Graduate Office",
                    summary = "Early priority consideration deadline approaching. Finalize your research statement and publication citations.",
                    timestamp = "4 days ago",
                    isPriority = true,
                    isUnread = false,
                    targetScreen = Screen.PROFILE
                )
            )
        )
    }

    LaunchedEffect(notifications) {
        val count = notifications.count { it.isUnread }
        onUnreadCountChanged(count)
    }

    val unreadCount = remember(notifications) { notifications.count { it.isUnread } }

    val filteredNotifications = remember(notifications, selectedCategory, searchQuery) {
        notifications.filter { item ->
            val matchesCategory = when (selectedCategory) {
                NotificationCategory.ALL -> true
                NotificationCategory.CIRCULAR -> item.category == NotificationCategory.CIRCULAR
                NotificationCategory.WATCHDOG -> item.category == NotificationCategory.WATCHDOG
                NotificationCategory.DEADLINE -> item.category == NotificationCategory.DEADLINE
                NotificationCategory.AUDIT -> item.category == NotificationCategory.AUDIT
                NotificationCategory.SYSTEM -> item.category == NotificationCategory.SYSTEM
            }
            val matchesQuery = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.sourceOrUniversity.contains(searchQuery, ignoreCase = true) ||
                item.summary.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(colors.canvas)) {
        val isMobile = maxWidth < 768.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = if (isMobile) 16.dp else 32.dp,
                vertical = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // -----------------------------------------------------------------
            // 1. TOP HEADER & METRICS
            // -----------------------------------------------------------------
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Notifications",
                                fontSize = if (isMobile) 22.sp else 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface
                            )
                            if (unreadCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.academicBlue)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$unreadCount New",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Action Buttons: Mark all read / Clear
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (unreadCount > 0) {
                                OutlinedButton(
                                    onClick = {
                                        notifications = notifications.map { it.copy(isUnread = false) }
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, colors.outline),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.DoneAll,
                                        contentDescription = "Mark all read",
                                        modifier = Modifier.size(14.dp),
                                        tint = colors.academicBlue
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Mark Read", fontSize = 11.sp, color = colors.onSurface)
                                }
                            }
                        }
                    }
                }
            }

            // -----------------------------------------------------------------
            // 2. FEATURED ACTIVE ALERT BANNER (Exact screenshot update)
            // -----------------------------------------------------------------
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.surfaceLow,
                    border = BorderStroke(1.dp, colors.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(if (isMobile) 14.dp else 18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // The exact pill from the user's screenshot
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(colors.surface)
                                .border(1.dp, colors.outline, RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(colors.verifiedEmerald)
                            )
                            Text(
                                text = "Daily Sweep Complete: 14 New Circulars Today · 8:00 AM · MIT, Max Planck, Cambridge, Stanford",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.onSurface
                            )
                        }

                        // Context breakdown
                        Text(
                            text = "The autonomous crawler swept 4 target research institutions at 08:00 AM EDT. 14 newly released doctoral calls, postdoc fellowships, and lab vacancies matched your research portfolio.",
                            fontSize = 12.sp,
                            color = colors.onSurfaceVariant,
                            lineHeight = 18.sp
                        )

                        // Action links
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = onNavigateToFeed,
                                colors = ButtonDefaults.buttonColors(containerColor = colors.academicBlue),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.DynamicFeed,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Inspect 14 Circulars in Feed",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }

                            OutlinedButton(
                                onClick = onNavigateToWatchdogs,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, colors.outline),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.TravelExplore,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = colors.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Watchdog Portals",
                                    fontSize = 11.sp,
                                    color = colors.onSurface
                                )
                            }

                            OutlinedButton(
                                onClick = onNavigateToSettings,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, colors.outline),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = colors.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Sweep Schedule",
                                    fontSize = 11.sp,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // -----------------------------------------------------------------
            // 3. SEARCH & CATEGORY FILTER TABS
            // -----------------------------------------------------------------
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Category Chips Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(NotificationCategory.values()) { cat ->
                            val isSelected = selectedCategory == cat
                            val count = when (cat) {
                                NotificationCategory.ALL -> notifications.size
                                else -> notifications.count { it.category == cat }
                            }

                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = cat },
                                label = {
                                    Text(
                                        text = "${cat.label} ($count)",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.academicBlue.copy(alpha = 0.15f),
                                    selectedLabelColor = colors.academicBlue,
                                    containerColor = colors.surfaceLow,
                                    labelColor = colors.onSurfaceVariant
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = if (isSelected) colors.academicBlue.copy(alpha = 0.5f) else colors.outline,
                                    selectedBorderColor = colors.academicBlue,
                                    borderWidth = 1.dp,
                                    enabled = true,
                                    selected = isSelected
                                )
                            )
                        }
                    }
                }
            }

            // -----------------------------------------------------------------
            // 4. NOTIFICATION LIST
            // -----------------------------------------------------------------
            if (filteredNotifications.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.NotificationsNone,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = colors.faintText
                            )
                            Text(
                                text = "No notifications found in this category",
                                fontSize = 14.sp,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredNotifications, key = { it.id }) { item ->
                    NotificationCard(
                        item = item,
                        onMarkAsRead = {
                            notifications = notifications.map {
                                if (it.id == item.id) it.copy(isUnread = false) else it
                            }
                        },
                        onDismiss = {
                            notifications = notifications.filter { it.id != item.id }
                        },
                        onActionClicked = {
                            when (item.targetScreen) {
                                Screen.FEED -> onNavigateToFeed()
                                Screen.RESEARCH_STUDIO -> onNavigateToStudio()
                                Screen.WATCHDOGS -> onNavigateToWatchdogs()
                                Screen.PROFILE -> onNavigateToProfile()
                                Screen.DIARY -> onNavigateToDiary()
                                Screen.SETTINGS -> onNavigateToSettings()
                                else -> {}
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
fun NotificationCard(
    item: NotificationItem,
    onMarkAsRead: () -> Unit,
    onDismiss: () -> Unit,
    onActionClicked: () -> Unit
) {
    val colors = GradPathTheme.colors

    val (badgeBg, badgeText, typeIcon) = when (item.category) {
        NotificationCategory.CIRCULAR -> Triple(colors.academicBlue.copy(alpha = 0.15f), colors.academicBlue, Icons.Default.School)
        NotificationCategory.WATCHDOG -> Triple(colors.verifiedEmerald.copy(alpha = 0.15f), colors.verifiedEmerald, Icons.Default.TravelExplore)
        NotificationCategory.DEADLINE -> Triple(colors.alertAmber.copy(alpha = 0.15f), colors.alertAmber, Icons.Default.Alarm)
        NotificationCategory.AUDIT -> Triple(Color(0xFF8B5CF6).copy(alpha = 0.15f), Color(0xFF8B5CF6), Icons.Default.SmartToy)
        NotificationCategory.SYSTEM -> Triple(colors.surfaceHigh, colors.onSurfaceVariant, Icons.Default.Dns)
        NotificationCategory.ALL -> Triple(colors.surfaceHigh, colors.onSurfaceVariant, Icons.Default.Notifications)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (item.isUnread) colors.surfaceLow else colors.surface.copy(alpha = 0.5f),
        border = BorderStroke(
            1.dp,
            if (item.isPriority) colors.alertAmber.copy(alpha = 0.6f)
            else if (item.isUnread) colors.outline.copy(alpha = 0.8f)
            else colors.outline.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: Type badge + Source + Timestamp + Read Dot
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Category Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeBg)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(typeIcon, contentDescription = null, modifier = Modifier.size(11.dp), tint = badgeText)
                            Text(
                                text = item.category.label.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = badgeText
                            )
                        }
                    }

                    if (item.isPriority) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.alertAmber.copy(alpha = 0.15f))
                                .border(1.dp, colors.alertAmber.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "PRIORITY",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = colors.alertAmber
                            )
                        }
                    }

                    // Source
                    Text(
                        text = item.sourceOrUniversity,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.timestamp,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = colors.faintText
                    )

                    if (item.isUnread) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(colors.academicBlue)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = colors.faintText,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            // Title
            Text(
                text = item.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface
            )

            // Summary description
            Text(
                text = item.summary,
                fontSize = 12.sp,
                color = colors.onSurfaceVariant,
                lineHeight = 17.sp
            )

            // Bottom Badges & Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (item.matchScore != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.surfaceHigh)
                                .border(1.dp, colors.outline, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Match: ${item.matchScore}%",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = colors.academicBlue
                            )
                        }
                    }

                    if (item.fundingStipend != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.verifiedEmerald.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = item.fundingStipend,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = colors.verifiedEmerald
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (item.isUnread) {
                        TextButton(
                            onClick = onMarkAsRead,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text("Mark Read", fontSize = 10.sp, color = colors.faintText)
                        }
                    }

                    if (item.targetScreen != null) {
                        Button(
                            onClick = {
                                onMarkAsRead()
                                onActionClicked()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceHigh),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                text = when (item.targetScreen) {
                                    Screen.FEED -> "View Circular →"
                                    Screen.WATCHDOGS -> "Inspect Portal →"
                                    Screen.PROFILE -> "Go to Profile →"
                                    Screen.RESEARCH_STUDIO -> "Open Studio →"
                                    else -> "View Details →"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
