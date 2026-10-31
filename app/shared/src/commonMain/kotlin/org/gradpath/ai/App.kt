package org.gradpath.ai

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.gradpath.ai.ui.components.*
import org.gradpath.ai.viewmodel.FeedViewModel
import org.gradpath.ai.viewmodel.ProfileViewModel

enum class Screen(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    FEED("Opportunities", Icons.Default.DynamicFeed),
    RESEARCH_STUDIO("AI Studio", Icons.Default.SmartToy),
    AUTOMATIONS("Automations", Icons.Default.PrecisionManufacturing),
    PROFILE("Profile", Icons.Default.AccountCircle),
    WATCHDOGS("University Portals", Icons.Default.TravelExplore),
    DIARY("Diary & Watchlist", Icons.AutoMirrored.Filled.MenuBook),
    DOCUMENT_AUDIT("Document Auditor", Icons.Default.RateReview),
    NOTIFICATIONS("Notifications", Icons.Default.Notifications),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun App(
    onTriggerManualSweep: (() -> Unit)? = null,
    directChatRunner: ((String) -> Flow<String>)? = null,
    onLoadAutomations: (() -> List<ScheduledAutomationDisplay>)? = null,
    onSaveAutomation: ((ScheduledAutomationDisplay) -> Unit)? = null,
    onDeleteAutomation: ((String) -> Unit)? = null,
    onToggleAutomation: ((String, Boolean) -> Unit)? = null
) {
    val currentOwner = LocalViewModelStoreOwner.current
    val fallbackOwner = remember {
        object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = ViewModelStore()
        }
    }

    DisposableEffect(fallbackOwner) {
        onDispose {
            fallbackOwner.viewModelStore.clear()
        }
    }

    CompositionLocalProvider(
        LocalViewModelStoreOwner provides (currentOwner ?: fallbackOwner)
    ) {
        val feedViewModel: FeedViewModel = viewModel { FeedViewModel() }
        val profileViewModel: ProfileViewModel = viewModel { ProfileViewModel() }
        val userProfile by profileViewModel.profile.collectAsState()
        var isDarkMode by remember { mutableStateOf(true) }
        var currentScreen by remember { mutableStateOf(Screen.HOME) }
        var isNavRailExpanded by remember { mutableStateOf(false) }
        var isServerConnected by remember { mutableStateOf(false) }
        var showSplashScreen by remember { mutableStateOf(true) }
        var unreadNotificationCount by remember { mutableStateOf(14) }
        var showExportDialog by remember { mutableStateOf(false) }
        var showShortcutsDialog by remember { mutableStateOf(false) }
        var isSweeping by remember { mutableStateOf(false) }
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val homeListState = rememberLazyListState()
        val isHomeScrolled by remember {
            derivedStateOf {
                homeListState.firstVisibleItemIndex > 0 || homeListState.firstVisibleItemScrollOffset > 24
            }
        }

        val scope = rememberCoroutineScope()
        val client = remember { HttpClient(CIO) }

        // Periodic Health Check to Ktor Backend
        LaunchedEffect(Unit) {
            scope.launch {
                while (true) {
                    try {
                        val response = client.get(GradPathConfig.endpoint("/health"))
                        isServerConnected = response.status.value == 200
                    } catch (e: Exception) {
                        isServerConnected = false
                    }
                    delay(5000)
                }
            }
        }

        // Initialize and fetch user profile from Cloud Firestore / Gateway on launch
        LaunchedEffect(Unit) {
            profileViewModel.loadProfile()
        }
        LaunchedEffect(isServerConnected) {
            if (isServerConnected) {
                profileViewModel.loadProfile()
            }
        }

        GradPathTheme(darkTheme = isDarkMode) {
            val colors = GradPathTheme.colors

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.canvas)
            ) {
                val isMobile = maxWidth < 768.dp
                val isTabletLandscape = maxWidth in 768.dp..1024.dp

                Crossfade(
                    targetState = showSplashScreen,
                    animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
                    label = "SplashScreenCrossfade"
                ) { isSplash ->
                    if (isSplash) {
                        UniversalSplashScreen(
                            isServerConnected = isServerConnected,
                            onDismiss = { showSplashScreen = false }
                        )
                    } else {
                        ModalNavigationDrawer(
                        drawerState = drawerState,
                        gesturesEnabled = true,
                        drawerContent = {
                            ModalDrawerSheet(
                                drawerContainerColor = colors.surfaceLow,
                                drawerContentColor = colors.onSurface,
                                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                                modifier = Modifier.widthIn(max = 320.dp)
                            ) {
                                GradPathDrawerContent(
                                    currentScreen = currentScreen,
                                    onScreenSelected = { screen ->
                                        currentScreen = screen
                                        scope.launch { drawerState.close() }
                                    },
                                    unreadNotificationCount = unreadNotificationCount,
                                    isDarkMode = isDarkMode,
                                    onToggleTheme = { isDarkMode = !isDarkMode },
                                    isServerConnected = isServerConnected,
                                    onClose = { scope.launch { drawerState.close() } },
                                    onTriggerRadarSweep = {
                                        scope.launch {
                                            drawerState.close()
                                            isSweeping = true
                                            onTriggerManualSweep?.invoke()
                                            feedViewModel.fetchFeed(isServerConnected)
                                            delay(1000)
                                            isSweeping = false
                                        }
                                    },
                                    onOpenExportDialog = {
                                        scope.launch { drawerState.close() }
                                        showExportDialog = true
                                    },
                                    onOpenShortcutsDialog = {
                                        scope.launch { drawerState.close() }
                                        showShortcutsDialog = true
                                    },
                                    userName = userProfile.fullName.ifBlank { "Candidate" },
                                    degreeTrack = if (userProfile.target.targetDegree == TargetDegree.PHD) "Direct Ph.D. Track" else "Funded Master's Track"
                                )
                            }
                        }
                    ) {
                        Scaffold(
                            topBar = {
                                if (!isMobile) {
                                    DesktopTopNavBar(
                                        currentScreen = currentScreen,
                                        onScreenSelected = { currentScreen = it },
                                        isNavRailExpanded = isNavRailExpanded,
                                        onToggleNavRail = { isNavRailExpanded = !isNavRailExpanded },
                                        isServerConnected = isServerConnected,
                                        isDarkMode = isDarkMode,
                                        onToggleTheme = { isDarkMode = !isDarkMode },
                                        onOpenNotifications = { currentScreen = Screen.NOTIFICATIONS },
                                        unreadNotificationCount = unreadNotificationCount,
                                        userName = userProfile.fullName.ifBlank { "Candidate" },
                                        degreeTrack = if (userProfile.target.targetDegree == TargetDegree.PHD) "Direct Ph.D. Track" else "Funded Master's Track"
                                    )
                                } else if (currentScreen != Screen.HOME) {
                                    MobileTopAppBar(
                                        currentScreen = currentScreen,
                                        isDarkMode = isDarkMode,
                                        onToggleTheme = { isDarkMode = !isDarkMode },
                                        isServerConnected = isServerConnected,
                                        onOpenStudio = { currentScreen = Screen.RESEARCH_STUDIO },
                                        onOpenDrawer = { scope.launch { drawerState.open() } },
                                        onOpenNotifications = { currentScreen = Screen.NOTIFICATIONS },
                                        onScreenSelected = { currentScreen = it },
                                        unreadNotificationCount = unreadNotificationCount
                                    )
                                }
                            },
                            bottomBar = {
                                if (isMobile) {
                                    MobileBottomDockBar(
                                        currentScreen = currentScreen,
                                        onScreenSelected = { currentScreen = it }
                                    )
                                }
                            },
                            contentWindowInsets = WindowInsets(0, 0, 0, 0),
                            containerColor = colors.canvas
                        ) { innerPadding ->
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                if (!isMobile) {
                                    DesktopSideNavRail(
                                        currentScreen = currentScreen,
                                        onScreenSelected = { currentScreen = it },
                                        isExpanded = isNavRailExpanded,
                                        onToggleExpand = { isNavRailExpanded = !isNavRailExpanded },
                                        onTriggerRadarSweep = {
                                            scope.launch {
                                                isSweeping = true
                                                onTriggerManualSweep?.invoke()
                                                feedViewModel.fetchFeed(isServerConnected)
                                                delay(1000)
                                                isSweeping = false
                                            }
                                        },
                                        onOpenExportDialog = { showExportDialog = true },
                                        onOpenShortcutsDialog = { showShortcutsDialog = true }
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                ) {
                                    AnimatedContent(
                                        targetState = currentScreen,
                                        transitionSpec = {
                                            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                                        }
                                    ) { screen ->
                                        when (screen) {
                                            Screen.HOME -> HomeScreen(
                                                isServerConnected = isServerConnected,
                                                feedViewModel = feedViewModel,
                                                listState = homeListState,
                                                onNavigateToFeed = { selectedId ->
                                                    if (selectedId != null) {
                                                        val match = feedViewModel.feedItems.value.firstOrNull { it.id == selectedId }
                                                        if (match != null) {
                                                            feedViewModel.selectItem(match)
                                                        }
                                                    }
                                                    currentScreen = Screen.FEED
                                                },
                                                onNavigateToStudio = { prompt ->
                                                    currentScreen = Screen.RESEARCH_STUDIO
                                                },
                                                onNavigateToWatchdogs = { currentScreen = Screen.WATCHDOGS },
                                                onNavigateToProfile = { currentScreen = Screen.PROFILE },
                                                onNavigateToDiary = { currentScreen = Screen.DIARY },
                                                onNavigateToNotifications = { currentScreen = Screen.NOTIFICATIONS }
                                            )
                                            Screen.FEED -> FeedScreen(
                                                isServerConnected = isServerConnected,
                                                viewModel = feedViewModel,
                                                onNavigateToStudio = { currentScreen = Screen.RESEARCH_STUDIO },
                                                onNavigateToWatchdogs = { currentScreen = Screen.WATCHDOGS },
                                                onNavigateToProfile = { currentScreen = Screen.PROFILE },
                                                onNavigateToDiary = { currentScreen = Screen.DIARY },
                                                onNavigateToNotifications = { currentScreen = Screen.NOTIFICATIONS }
                                            )
                                            Screen.RESEARCH_STUDIO -> ResearchStudioScreen(
                                                isServerConnected = isServerConnected,
                                                directChatRunner = directChatRunner
                                            )
                                            Screen.AUTOMATIONS -> AutomationsScreen(
                                                isServerConnected = isServerConnected,
                                                onTriggerManualSweep = {
                                                    onTriggerManualSweep?.invoke()
                                                    feedViewModel.fetchFeed(isServerConnected)
                                                },
                                                onLoadAutomations = onLoadAutomations,
                                                onSaveAutomation = onSaveAutomation,
                                                onDeleteAutomation = onDeleteAutomation,
                                                onToggleAutomation = onToggleAutomation
                                            )
                                            Screen.PROFILE -> ProfileScreen(
                                                isServerConnected = isServerConnected,
                                                viewModel = profileViewModel
                                            )
                                            Screen.WATCHDOGS -> WatchdogsScreen(
                                                isServerConnected = isServerConnected
                                            )
                                            Screen.DIARY -> DiaryScreen(
                                                isServerConnected = isServerConnected
                                            )
                                            Screen.DOCUMENT_AUDIT -> DocumentAuditScreen(
                                                isServerConnected = isServerConnected,
                                                onBack = { currentScreen = Screen.HOME }
                                            )
                                            Screen.NOTIFICATIONS -> NotificationScreen(
                                                isServerConnected = isServerConnected,
                                                onNavigateToFeed = { currentScreen = Screen.FEED },
                                                onNavigateToStudio = { currentScreen = Screen.RESEARCH_STUDIO },
                                                onNavigateToWatchdogs = { currentScreen = Screen.WATCHDOGS },
                                                onNavigateToProfile = { currentScreen = Screen.PROFILE },
                                                onNavigateToDiary = { currentScreen = Screen.DIARY },
                                                onNavigateToSettings = { currentScreen = Screen.SETTINGS },
                                                onUnreadCountChanged = { unreadNotificationCount = it }
                                            )
                                            Screen.SETTINGS -> SettingsScreen(
                                                isServerConnected = isServerConnected,
                                                isDarkMode = isDarkMode,
                                                onToggleTheme = { isDarkMode = !isDarkMode },
                                                onNavigateToNotifications = { currentScreen = Screen.NOTIFICATIONS },
                                                onNavigateToFeed = { currentScreen = Screen.FEED }
                                            )
                                        }
                                    }

                                    if (isMobile && currentScreen == Screen.HOME) {
                                        androidx.compose.animation.AnimatedVisibility(
                                            visible = isHomeScrolled,
                                            enter = fadeIn(animationSpec = tween(220)) + slideInVertically(animationSpec = tween(220)) { -it },
                                            exit = fadeOut(animationSpec = tween(180)) + slideOutVertically(animationSpec = tween(180)) { -it },
                                            modifier = Modifier.align(Alignment.TopCenter)
                                        ) {
                                            MobileTopAppBar(
                                                currentScreen = currentScreen,
                                                isDarkMode = isDarkMode,
                                                onToggleTheme = { isDarkMode = !isDarkMode },
                                                isServerConnected = isServerConnected,
                                                onOpenStudio = { currentScreen = Screen.RESEARCH_STUDIO },
                                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                                onOpenNotifications = { currentScreen = Screen.NOTIFICATIONS },
                                                onScreenSelected = { currentScreen = it },
                                                unreadNotificationCount = unreadNotificationCount
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (showExportDialog) {
                            ExportDossierDialog(
                                onDismiss = { showExportDialog = false }
                            )
                        }

                        if (showShortcutsDialog) {
                            ShortcutsHelpDialog(
                                onDismiss = { showShortcutsDialog = false }
                            )
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
 * Desktop Top Navigation Bar (Clean Minimal Header)
 * Workspace breadcrumbs and Settings have been removed from the top bar.
 * -----------------------------------------------------------------------------
 */
@Composable
fun DesktopTopNavBar(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit,
    isNavRailExpanded: Boolean,
    onToggleNavRail: () -> Unit,
    isServerConnected: Boolean,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    onOpenNotifications: () -> Unit,
    unreadNotificationCount: Int = 14,
    userName: String = "Candidate",
    degreeTrack: String = "Doctoral Track"
) {
    val colors = GradPathTheme.colors

    Box(modifier = Modifier.fillMaxWidth().background(colors.surfaceLow)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Navigation Rail Toggle & Brand Logo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DesktopTopBarActionButton(
                        onClick = onToggleNavRail,
                        isSelected = isNavRailExpanded,
                        contentDescription = if (isNavRailExpanded) "Collapse Navigation Rail" else "Expand Navigation Rail"
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = null,
                            tint = if (isNavRailExpanded) colors.academicBlue else colors.onSurface,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onScreenSelected(Screen.HOME) }
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .rotate(45f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.surfaceHigh)
                                .border(1.dp, colors.academicBlue.copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "◆",
                                color = colors.academicBlue,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.rotate(-45f)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "GradPath ",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.onSurface
                                )
                                Text(
                                    text = "AI",
                                    fontSize = 16.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = colors.academicBlue,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "ACADEMIC RADAR",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.1.sp,
                                color = colors.faintText
                            )
                        }
                    }
                }

                // Right: Professional Executive Action Cluster (Notifications, Theme, Profile)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Desktop Notifications Button with Knockout Badge
                    DesktopTopBarActionButton(
                        onClick = onOpenNotifications,
                        isSelected = currentScreen == Screen.NOTIFICATIONS,
                        contentDescription = "Notifications"
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = if (currentScreen == Screen.NOTIFICATIONS) colors.academicBlue else colors.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            if (unreadNotificationCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 1.dp, y = (-1).dp)
                                        .clip(CircleShape)
                                        .background(colors.academicBlue)
                                        .border(1.5.dp, colors.surfaceLow, CircleShape)
                                        .padding(horizontal = 4.dp, vertical = 0.5.dp)
                                ) {
                                    Text(
                                        text = if (unreadNotificationCount > 99) "99+" else "$unreadNotificationCount",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    // Desktop Theme Toggle Button (Clean Vector Icons)
                    DesktopTopBarActionButton(
                        onClick = onToggleTheme,
                        contentDescription = if (isDarkMode) "Switch to Light Theme" else "Switch to Dark Theme"
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = if (isDarkMode) colors.alertAmber else colors.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Elegant Hairline Vertical Divider
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(20.dp)
                            .width(1.dp)
                            .background(colors.outlineVariant.copy(alpha = 0.45f))
                    )

                    // Executive Profile Pill Trigger
                    Surface(
                        onClick = { onScreenSelected(Screen.PROFILE) },
                        shape = RoundedCornerShape(20.dp),
                        color = if (currentScreen == Screen.PROFILE) colors.academicBlue.copy(alpha = 0.12f) else colors.surface,
                        border = BorderStroke(
                            1.dp,
                            if (currentScreen == Screen.PROFILE) colors.academicBlue else colors.outlineVariant.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 3.dp, end = 10.dp, top = 2.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val userInitials = remember(userName) {
                                val parts = userName.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
                                when {
                                    parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
                                    parts.size == 1 && parts[0].isNotEmpty() -> parts[0].take(2).uppercase()
                                    else -> "AK"
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(colors.academicBlue, colors.academicBlueGlow)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userInitials,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White
                                )
                            }

                            Column(verticalArrangement = Arrangement.Center) {
                                Text(
                                    text = userName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.onSurface
                                )
                                Text(
                                    text = degreeTrack,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = colors.faintText
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Profile Details",
                                tint = colors.faintText,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Clean Hairline Bottom Border
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(GradPathThemeTokens.palette.borderSoft)
            )
        }
    }
}

/**
 * -----------------------------------------------------------------------------
 * Desktop Side Navigation Rail
 * Expanded to 248dp to eliminate label truncation.
 * Features 3 organized categories: ACADEMIC WORKSPACE, HANDY TOOLS, and SYSTEM & PREFERENCES.
 * -----------------------------------------------------------------------------
 */
@Composable
fun DesktopSideNavRail(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onTriggerRadarSweep: () -> Unit,
    onOpenExportDialog: () -> Unit,
    onOpenShortcutsDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = GradPathTheme.colors
    val railWidth by animateDpAsState(
        targetValue = if (isExpanded) 248.dp else 68.dp,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "RailWidthAnimation"
    )

    val primaryScreens = listOf(
        Screen.HOME,
        Screen.FEED,
        Screen.RESEARCH_STUDIO,
        Screen.AUTOMATIONS,
        Screen.PROFILE,
        Screen.WATCHDOGS,
        Screen.DIARY
    )

    Surface(
        modifier = modifier
            .width(railWidth)
            .fillMaxHeight(),
        color = colors.surfaceLow,
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp, horizontal = if (isExpanded) 10.dp else 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Scrollable Content: Academic Workspace & Handy Tools
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: ACADEMIC WORKSPACE
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    if (isExpanded) {
                        Text(
                            text = "ACADEMIC WORKSPACE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.1.sp,
                            color = colors.faintText,
                            modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 4.dp)
                        )
                    }

                    primaryScreens.forEach { screen ->
                        DesktopNavRailItem(
                            screen = screen,
                            isSelected = currentScreen == screen,
                            isExpanded = isExpanded,
                            onClick = { onScreenSelected(screen) }
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    color = colors.outlineVariant.copy(alpha = 0.3f),
                    thickness = 1.dp
                )

                // Section 2: HANDY TOOLS
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    if (isExpanded) {
                        Text(
                            text = "HANDY TOOLS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.1.sp,
                            color = colors.faintText,
                            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                        )
                    }

                    // 1. Document Auditor (SOP, Research Proposal, CV Audit)
                    DesktopNavRailItem(
                        screen = Screen.DOCUMENT_AUDIT,
                        isSelected = currentScreen == Screen.DOCUMENT_AUDIT,
                        isExpanded = isExpanded,
                        onClick = { onScreenSelected(Screen.DOCUMENT_AUDIT) }
                    )

                    // 2. Live Radar Sweep Trigger
                    DesktopNavRailActionItem(
                        title = "Live Radar Sweep",
                        icon = Icons.Default.Radar,
                        badgeText = "Sweep",
                        isExpanded = isExpanded,
                        onClick = onTriggerRadarSweep
                    )

                    // 3. Export Academic Dossier
                    DesktopNavRailActionItem(
                        title = "Export Dossier",
                        icon = Icons.Default.FileDownload,
                        badgeText = "JSON/ZIP",
                        isExpanded = isExpanded,
                        onClick = onOpenExportDialog
                    )

                    // 4. Shortcuts & Help Guide
                    DesktopNavRailActionItem(
                        title = "Shortcuts & Guide",
                        icon = Icons.Default.Keyboard,
                        badgeText = "Help",
                        isExpanded = isExpanded,
                        onClick = onOpenShortcutsDialog
                    )
                }
            }

            // Bottom Section: Category 3 - SYSTEM & PREFERENCES
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 4.dp),
                    color = colors.outlineVariant.copy(alpha = 0.3f),
                    thickness = 1.dp
                )

                if (isExpanded) {
                    Text(
                        text = "SYSTEM & PREFERENCES",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.1.sp,
                        color = colors.faintText,
                        modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                    )
                }

                // Settings Navigation Item (Moved from top bar)
                DesktopNavRailItem(
                    screen = Screen.SETTINGS,
                    isSelected = currentScreen == Screen.SETTINGS,
                    isExpanded = isExpanded,
                    onClick = { onScreenSelected(Screen.SETTINGS) }
                )

                // Cloud Database Sync Status Widget
                if (isExpanded) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colors.surface,
                        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(colors.verifiedEmerald.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = "Cloud Synced",
                                    tint = colors.verifiedEmerald,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Cloud Database",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.onSurface
                                )
                                Text(
                                    text = "Synced with Cloud",
                                    fontSize = 10.sp,
                                    color = colors.verifiedEmerald,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surface)
                                .border(1.dp, colors.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = "Cloud Synced",
                                tint = colors.verifiedEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Expand / Shrink Toggle Action
                Surface(
                    onClick = onToggleExpand,
                    shape = RoundedCornerShape(8.dp),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = if (isExpanded) Arrangement.SpaceBetween else Arrangement.Center
                    ) {
                        if (isExpanded) {
                            Text(
                                text = "Collapse Rail",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.onSurfaceVariant
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Collapse Sidebar",
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Expand Sidebar",
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual navigation rail item handling both compact (shrunk) and expanded display modes.
 */
@Composable
fun DesktopNavRailItem(
    screen: Screen,
    isSelected: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val colors = GradPathTheme.colors

    val title = when (screen) {
        Screen.HOME -> "Home"
        Screen.FEED -> "Opportunities"
        Screen.RESEARCH_STUDIO -> "AI Studio"
        Screen.AUTOMATIONS -> "Automations"
        Screen.PROFILE -> "Profile"
        Screen.WATCHDOGS -> "University Portals"
        Screen.DIARY -> "Diary & Watchlist"
        Screen.DOCUMENT_AUDIT -> "Document Auditor"
        Screen.SETTINGS -> "Settings"
        else -> screen.title
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) colors.academicBlue.copy(alpha = 0.14f) else Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, colors.academicBlue.copy(alpha = 0.45f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        if (isExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = title,
                        tint = if (isSelected) colors.academicBlue else colors.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) colors.academicBlue else colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Badges in expanded state
                when (screen) {
                    Screen.FEED -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.academicBlue)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "42",
                                fontSize = 9.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Screen.RESEARCH_STUDIO -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surfaceHigh)
                                .border(1.dp, colors.academicBlue.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Active",
                                fontSize = 9.sp,
                                color = colors.academicBlue,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Screen.AUTOMATIONS -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surfaceHigh)
                                .border(1.dp, colors.alertAmber.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Daily 2x",
                                fontSize = 9.sp,
                                color = colors.alertAmber,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Screen.PROFILE -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surfaceHigh)
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "88/100",
                                fontSize = 9.sp,
                                color = colors.onSurfaceVariant,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Screen.WATCHDOGS -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surfaceHigh)
                                .border(1.dp, colors.verifiedEmerald.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "6 Online",
                                fontSize = 9.sp,
                                color = colors.verifiedEmerald,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Screen.DOCUMENT_AUDIT -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surfaceHigh)
                                .border(1.dp, colors.academicBlue.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "AI Review",
                                fontSize = 9.sp,
                                color = colors.academicBlue,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    else -> {}
                }
            }
        } else {
            // Collapsed rail mode: Centered icon with compact micro status dot
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = screen.icon,
                    contentDescription = title,
                    tint = if (isSelected) colors.academicBlue else colors.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )

                // Micro indicator badge in top right corner of the collapsed item box
                when (screen) {
                    Screen.FEED -> {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 6.dp, end = 6.dp)
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(colors.academicBlue)
                        )
                    }
                    Screen.RESEARCH_STUDIO -> {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 6.dp, end = 6.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(colors.academicBlue)
                        )
                    }
                    Screen.AUTOMATIONS -> {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 6.dp, end = 6.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(colors.alertAmber)
                        )
                    }
                    Screen.WATCHDOGS -> {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 6.dp, end = 6.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(colors.verifiedEmerald)
                        )
                    }
                    Screen.DOCUMENT_AUDIT -> {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 6.dp, end = 6.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(colors.academicBlue)
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}

/**
 * Action item in navigation rail (e.g., Live Radar Sweep, Export Dossier, Shortcuts).
 */
@Composable
fun DesktopNavRailActionItem(
    title: String,
    icon: ImageVector,
    badgeText: String? = null,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val colors = GradPathTheme.colors

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        border = null,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        if (isExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (badgeText != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surfaceHigh)
                            .border(1.dp, colors.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 9.sp,
                            color = colors.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Dedicated Desktop Top Bar Action Button with strictly bounded layout.
 * Eliminates touch-target expanding collisions and guarantees zero component overlap.
 */
@Composable
fun DesktopTopBarActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    contentDescription: String? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = GradPathTheme.colors

    Box(
        modifier = modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) colors.academicBlue.copy(alpha = 0.14f)
                else colors.surface.copy(alpha = 0.65f)
            )
            .border(
                width = 1.dp,
                color = if (isSelected) colors.academicBlue.copy(alpha = 0.5f) else colors.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content
    )
}

/**
 * -----------------------------------------------------------------------------
 * Mobile Top App Bar
 * -----------------------------------------------------------------------------
 */
@Composable
fun MobileTopAppBar(
    currentScreen: Screen,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    isServerConnected: Boolean,
    onOpenStudio: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenNotifications: () -> Unit,
    onScreenSelected: (Screen) -> Unit,
    unreadNotificationCount: Int = 14
) {
    val colors = GradPathTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceLow)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open Drawer Menu",
                        tint = colors.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(45f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.surfaceHigh)
                        .border(1.dp, colors.academicBlue.copy(alpha = 0.5f), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("◆", color = colors.academicBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.rotate(-45f))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onScreenSelected(Screen.HOME) }
                ) {
                    Text("GradPath ", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
                    Text("AI", fontSize = 16.sp, fontStyle = FontStyle.Italic, color = colors.academicBlue, fontWeight = FontWeight.SemiBold)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Notifications bell with badge
                IconButton(
                    onClick = onOpenNotifications,
                    modifier = Modifier.size(32.dp)
                ) {
                    BadgedBox(
                        badge = {
                            if (unreadNotificationCount > 0) {
                                Badge(
                                    containerColor = colors.academicBlue,
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = if (unreadNotificationCount > 99) "99+" else "$unreadNotificationCount",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = if (currentScreen == Screen.NOTIFICATIONS) colors.academicBlue else colors.onSurfaceVariant,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                // Quick AI Studio button on mobile
                Button(
                    onClick = onOpenStudio,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceHigh),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.Default.SmartToy, contentDescription = "Studio", tint = colors.academicBlue, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Studio", fontSize = 10.sp, color = colors.onSurface)
                }

                // Theme Toggle
                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier.size(28.dp)
                ) {
                    Text(if (isDarkMode) "🌙" else "☀", fontSize = 12.sp)
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(GradPathThemeTokens.palette.borderSoft)
        )
    }
}

/**
 * -----------------------------------------------------------------------------
 * Side Menu Drawer Content (Academic Workspace & System Preferences)
 * -----------------------------------------------------------------------------
 */
@Composable
fun GradPathDrawerContent(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit,
    unreadNotificationCount: Int,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    isServerConnected: Boolean = false,
    onClose: () -> Unit,
    onTriggerRadarSweep: () -> Unit = {},
    onOpenExportDialog: () -> Unit = {},
    onOpenShortcutsDialog: () -> Unit = {},
    userName: String = "Candidate",
    degreeTrack: String = "Doctoral Track"
) {
    val colors = GradPathTheme.colors

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(colors.surfaceLow)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header with Brand and Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .rotate(45f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.surfaceHigh)
                            .border(1.dp, colors.academicBlue.copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "◆",
                            color = colors.academicBlue,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.rotate(-45f)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("GradPath ", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
                        Text("AI", fontSize = 17.sp, fontStyle = FontStyle.Italic, color = colors.academicBlue, fontWeight = FontWeight.SemiBold)
                    }
                }

                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close Drawer", tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }

            // Candidate Profile Card (Clickable to navigate to Profile)
            Surface(
                onClick = { onScreenSelected(Screen.PROFILE) },
                shape = RoundedCornerShape(10.dp),
                color = colors.surface,
                border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val drawerInitials = remember(userName) {
                        val parts = userName.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
                        when {
                            parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
                            parts.size == 1 && parts[0].isNotEmpty() -> parts[0].take(2).uppercase()
                            else -> "AK"
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.academicBlue.copy(alpha = 0.15f))
                            .border(1.dp, colors.academicBlue.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(drawerInitials, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.academicBlue)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(userName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                        Text("$degreeTrack · Fall 2026", fontSize = 11.sp, color = colors.onSurfaceVariant)
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Profile",
                        tint = colors.faintText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = colors.outlineVariant.copy(alpha = 0.35f),
                thickness = 1.dp
            )

            // Category 1: Academic Workspace Items
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "ACADEMIC WORKSPACE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.1.sp,
                    color = colors.faintText,
                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                )

                DrawerNavItem(
                    title = "Home",
                    icon = Icons.Default.Home,
                    isSelected = currentScreen == Screen.HOME,
                    onClick = { onScreenSelected(Screen.HOME) }
                )

                DrawerNavItem(
                    title = "Opportunities",
                    icon = Icons.Default.DynamicFeed,
                    badgeText = "42",
                    badgeHighlight = true,
                    isSelected = currentScreen == Screen.FEED,
                    onClick = { onScreenSelected(Screen.FEED) }
                )

                DrawerNavItem(
                    title = "AI Studio",
                    icon = Icons.Default.SmartToy,
                    badgeText = "Active",
                    isSelected = currentScreen == Screen.RESEARCH_STUDIO,
                    onClick = { onScreenSelected(Screen.RESEARCH_STUDIO) }
                )

                DrawerNavItem(
                    title = "Automations",
                    icon = Icons.Default.PrecisionManufacturing,
                    badgeText = "Daily 2x",
                    isSelected = currentScreen == Screen.AUTOMATIONS,
                    onClick = { onScreenSelected(Screen.AUTOMATIONS) }
                )

                DrawerNavItem(
                    title = "University Portals",
                    icon = Icons.Default.TravelExplore,
                    badgeText = "6 Online",
                    isSelected = currentScreen == Screen.WATCHDOGS,
                    onClick = { onScreenSelected(Screen.WATCHDOGS) }
                )

                DrawerNavItem(
                    title = "Diary & Watchlist",
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    isSelected = currentScreen == Screen.DIARY,
                    onClick = { onScreenSelected(Screen.DIARY) }
                )
            }

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = colors.outlineVariant.copy(alpha = 0.35f),
                thickness = 1.dp
            )

            // Category 2: Handy Tools Section
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "HANDY TOOLS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.1.sp,
                    color = colors.faintText,
                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                )

                DrawerNavItem(
                    title = "Document Auditor",
                    icon = Icons.Default.RateReview,
                    badgeText = "AI Review",
                    badgeHighlight = true,
                    isSelected = currentScreen == Screen.DOCUMENT_AUDIT,
                    onClick = { onScreenSelected(Screen.DOCUMENT_AUDIT) }
                )

                DrawerNavItem(
                    title = "Live Radar Sweep",
                    icon = Icons.Default.Radar,
                    badgeText = "Sweep",
                    isSelected = false,
                    onClick = onTriggerRadarSweep
                )

                DrawerNavItem(
                    title = "Export Dossier",
                    icon = Icons.Default.FileDownload,
                    badgeText = "JSON/ZIP",
                    isSelected = false,
                    onClick = onOpenExportDialog
                )

                DrawerNavItem(
                    title = "Shortcuts & Guide",
                    icon = Icons.Default.Keyboard,
                    badgeText = "Help",
                    isSelected = false,
                    onClick = onOpenShortcutsDialog
                )
            }

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = colors.outlineVariant.copy(alpha = 0.35f),
                thickness = 1.dp
            )

            // Category 3: System & Preferences Section
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "SYSTEM & PREFERENCES",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.1.sp,
                    color = colors.faintText,
                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                )

                DrawerNavItem(
                    title = "Profile",
                    icon = Icons.Default.AccountCircle,
                    badgeText = "88/100",
                    isSelected = currentScreen == Screen.PROFILE,
                    onClick = { onScreenSelected(Screen.PROFILE) }
                )

                DrawerNavItem(
                    title = "Notifications",
                    icon = Icons.Default.Notifications,
                    badgeText = if (unreadNotificationCount > 0) "$unreadNotificationCount" else null,
                    badgeHighlight = true,
                    isSelected = currentScreen == Screen.NOTIFICATIONS,
                    onClick = { onScreenSelected(Screen.NOTIFICATIONS) }
                )

                DrawerNavItem(
                    title = "Settings",
                    icon = Icons.Default.Settings,
                    isSelected = currentScreen == Screen.SETTINGS,
                    onClick = { onScreenSelected(Screen.SETTINGS) }
                )
            }
        }

        // Drawer Bottom Footer
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                color = colors.outlineVariant.copy(alpha = 0.35f),
                thickness = 1.dp
            )

            // Cloud Database Sync Status Card
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colors.surface,
                border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(colors.verifiedEmerald.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Cloud Synced",
                            tint = colors.verifiedEmerald,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Cloud Database",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface
                        )
                        Text(
                            text = "Synced with Cloud",
                            fontSize = 10.sp,
                            color = colors.verifiedEmerald,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Clean Theme Toggle Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggleTheme() }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(if (isDarkMode) "🌙" else "☀", fontSize = 14.sp)
                    Text(
                        text = if (isDarkMode) "Dark Theme" else "Light Theme",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.onSurface
                    )
                }

                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { onToggleTheme() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.canvas,
                        checkedTrackColor = colors.academicBlue,
                        uncheckedThumbColor = colors.onSurfaceVariant,
                        uncheckedTrackColor = colors.surfaceHigh
                    )
                )
            }
        }
    }
}

@Composable
fun DrawerNavItem(
    title: String,
    icon: ImageVector,
    badgeText: String? = null,
    badgeHighlight: Boolean = false,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = GradPathTheme.colors

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) colors.academicBlue.copy(alpha = 0.12f) else Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, colors.academicBlue.copy(alpha = 0.35f)) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (isSelected) colors.academicBlue else colors.onSurfaceVariant
                )
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) colors.academicBlue else colors.onSurface
                )
            }

            if (badgeText != null) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (badgeHighlight) colors.academicBlue
                            else colors.surfaceHigh
                        )
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (badgeHighlight) Color.White else colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * -----------------------------------------------------------------------------
 * Mobile Docked 4-Tab Bottom Navigation Bar (matching mobile-home-discovery.html)
 * -----------------------------------------------------------------------------
 */
@Composable
fun MobileBottomDockBar(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit
) {
    val palette = GradPathThemeTokens.palette
    val tabs = listOf(
        Triple(Screen.HOME, "Home", Icons.Default.Home),
        Triple(Screen.FEED, "Feed", Icons.Default.DynamicFeed),
        Triple(Screen.PROFILE, "Profile", Icons.Default.AccountCircle),
        Triple(Screen.WATCHDOGS, "Portals", Icons.Default.TravelExplore),
        Triple(Screen.DIARY, "Diary", Icons.AutoMirrored.Filled.MenuBook)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = GradPathSpace.lg, vertical = GradPathSpace.sm),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(GradPathRadii.rPill),
            color = palette.card,
            tonalElevation = 4.dp,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, palette.borderSoft),
            modifier = Modifier.fillMaxWidth().height(60.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = GradPathSpace.xs),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { (screen, label, icon) ->
                    val isSelected = currentScreen == screen
                    val activeColor = palette.academicBlue
                    val inactiveColor = palette.textMuted

                    val badgeText = when (screen) {
                        Screen.WATCHDOGS -> "6"
                        Screen.DIARY -> "2"
                        else -> null
                    }

                    GPressScale(
                        onClick = { onScreenSelected(screen) }
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(GradPathRadii.rPill))
                                .background(if (isSelected) palette.academicBlueSoft else Color.Transparent)
                                .padding(horizontal = GradPathSpace.md, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isSelected) activeColor else inactiveColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    if (badgeText != null) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = 8.dp, y = (-4).dp)
                                        ) {
                                            GCountBadge(
                                                text = badgeText,
                                                color = if (isSelected) activeColor else palette.alertAmber
                                            )
                                        }
                                    }
                                }

                                VGap(GradPathSpace.xxs)
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) activeColor else palette.textSecondary
                                )
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
 * Universal Editorial Minimal Splash Screen & Bootloader (DESIGN.md aligned)
 * -----------------------------------------------------------------------------
 */
@Composable
fun UniversalSplashScreen(
    isServerConnected: Boolean,
    onDismiss: () -> Unit
) {
    val colors = GradPathTheme.colors
    var phaseIndex by remember { mutableStateOf(0) }

    val statusPhases = remember {
        listOf(
            "Initializing Antigravity Engine...",
            "Synchronizing Global Feeds...",
            "Research Workstation Ready"
        )
    }

    LaunchedEffect(Unit) {
        delay(650)
        phaseIndex = 1
        delay(750)
        phaseIndex = 2
        delay(650)
        onDismiss()
    }

    // Breathing pulse for emblem ambient glow & soft elevation float
    val infiniteTransition = rememberInfiniteTransition(label = "EmblemBreathing")
    val auraScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AuraScale"
    )
    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.50f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AuraAlpha"
    )
    val emblemFloat by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "EmblemFloat"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        // Ambient soft atmospheric sapphire glow
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerOffset = Offset(size.width / 2f, size.height * 0.42f)
            val glowRadius = size.minDimension * 0.45f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colors.academicBlue.copy(alpha = 0.14f * auraAlpha),
                        Color.Transparent
                    ),
                    center = centerOffset,
                    radius = glowRadius * auraScale
                ),
                center = centerOffset,
                radius = glowRadius * auraScale
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 440.dp)
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            // Top Micro-Telemetry Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
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
                        text = "SECURE TELEMETRY",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = colors.faintText
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceLow)
                        .border(1.dp, colors.outlineVariant, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "v4.2.1",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = colors.faintText
                    )
                }
            }

            // Center Stage: Luminous Breathing Emblem & Editorial Title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                // Precision Rotated Diamond Emblem
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Outer Dashed Geometric Halo
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .rotate(45f)
                            .dashedBorder(
                                width = 1.dp,
                                color = colors.lineDashed,
                                cornerRadius = 16.dp
                            )
                    )

                    // Inner Elevated Core with breathing float
                    Box(
                        modifier = Modifier
                            .offset(y = emblemFloat.dp)
                            .size(62.dp)
                            .rotate(45f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.surfaceHigh)
                            .border(
                                width = 1.dp,
                                color = colors.academicBlue.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "◆",
                            fontSize = 30.sp,
                            color = colors.academicBlue,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.rotate(-45f)
                        )
                    }

                    // Verification accent bead
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 2.dp, y = 2.dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(colors.canvas)
                            .border(1.dp, colors.outlineVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(colors.verifiedEmerald)
                        )
                    }
                }

                // Typography & Brand Wordmark
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Monospace Kernel Version Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.surfaceLow)
                            .border(1.dp, colors.academicBlue.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(colors.academicBlue)
                            )
                            Text(
                                text = "ANTIGRAVITY KERNEL 127.0.0.1:8080",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = colors.academicBlue,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Editorial Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GradPath ",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Normal,
                            fontFamily = FontFamily.Serif,
                            color = colors.onSurface,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "AI",
                            fontSize = 36.sp,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Light,
                            fontFamily = FontFamily.Serif,
                            color = colors.academicBlue
                        )
                    }

                    // Subtitle
                    Text(
                        text = "Precision Intelligence for Global Research & Fellowships",
                        fontSize = 12.5.sp,
                        color = colors.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }

            // Bottom Stage: Morphing Status Pill & Harmonic AI Waveform
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Morphing Status Chip
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.surfaceLow)
                        .border(1.dp, colors.outlineVariant, RoundedCornerShape(24.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Crossfade(
                        targetState = statusPhases.getOrElse(phaseIndex) { "Ready" },
                        animationSpec = tween(300),
                        label = "StatusCrossfade"
                    ) { currentStatus ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (phaseIndex >= 2) "✦" else "●",
                                fontSize = 11.sp,
                                color = if (phaseIndex >= 2) colors.verifiedEmerald else colors.academicBlue
                            )
                            Text(
                                text = currentStatus,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = colors.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Rhythmic AI Telemetry Waveform (Replacing linear progress indicator)
                HarmonicWaveform(
                    modifier = Modifier.padding(vertical = 4.dp),
                    barCount = 7,
                    baseHeight = 7.dp,
                    maxHeight = 26.dp,
                    barWidth = 4.dp,
                    spacing = 6.dp
                )

                // Skip to Workstation Text Button
                TextButton(
                    onClick = onDismiss,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Skip to Workstation",
                            fontSize = 11.sp,
                            color = colors.faintText
                        )
                        Text(
                            text = "→",
                            fontSize = 11.sp,
                            color = colors.faintText
                        )
                    }
                }
            }
        }
    }
}

/**
 * -----------------------------------------------------------------------------
 * Harmonic AI Telemetry Waveform
 * 7 harmonic sinusoidal bars shimmering in Academic Blue -> Verified Emerald gradient.
 * -----------------------------------------------------------------------------
 */
@Composable
fun HarmonicWaveform(
    modifier: Modifier = Modifier,
    barCount: Int = 7,
    baseHeight: androidx.compose.ui.unit.Dp = 7.dp,
    maxHeight: androidx.compose.ui.unit.Dp = 26.dp,
    barWidth: androidx.compose.ui.unit.Dp = 4.dp,
    spacing: androidx.compose.ui.unit.Dp = 6.dp
) {
    val colors = GradPathTheme.colors
    val infiniteTransition = rememberInfiniteTransition(label = "WaveformAnimation")

    val barGradients = remember {
        listOf(
            Color(0xFF38BDF8), // Academic Blue
            Color(0xFF38BDF8),
            Color(0xFF0EA5E9), // Sky Blue
            Color(0xFF06B6D4), // Cyan
            Color(0xFF14B8A6), // Teal
            Color(0xFF10B981), // Verified Emerald
            Color(0xFF10B981)
        )
    }

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * kotlin.math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val centerDist = kotlin.math.abs(i - (barCount - 1) / 2f)
            val amplitudeFactor = 1f - (centerDist / (barCount / 2f)) * 0.35f
            val barPhase = phase + i * 0.85f
            val wave = (kotlin.math.sin(barPhase) + 1f) / 2f
            val currentHeight = baseHeight + (maxHeight - baseHeight) * (wave * amplitudeFactor)
            val barColor = barGradients.getOrElse(i) { colors.academicBlue }

            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(currentHeight)
                    .clip(CircleShape)
                    .background(barColor)
            )
        }
    }
}

/**
 * Backwards compatibility alias for MobileSplashScreen
 */
@Composable
fun MobileSplashScreen(
    isServerConnected: Boolean,
    onDismiss: () -> Unit
) {
    UniversalSplashScreen(isServerConnected = isServerConnected, onDismiss = onDismiss)
}

@Composable
fun TerminalLine(label: String, status: String, isOk: Boolean) {
    val colors = GradPathTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "● $label",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = colors.onSurfaceVariant
        )
        Text(
            text = status,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = if (isOk) colors.verifiedEmerald else colors.alertAmber
        )
    }
}

/**
 * Dialog for exporting student's academic dossiers, target labs, and tracked opportunities.
 */
@Composable
fun ExportDossierDialog(
    onDismiss: () -> Unit
) {
    val colors = GradPathTheme.colors
    var selectedFormat by remember { mutableStateOf("json") }
    var isExporting by remember { mutableStateOf(false) }
    var exportSuccess by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceLow,
        titleContentColor = colors.onSurface,
        textContentColor = colors.onSurfaceVariant,
        shape = RoundedCornerShape(16.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.academicBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = null,
                    tint = colors.academicBlue,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        title = {
            Text(
                text = "Export Academic Dossier",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Package your curated opportunities, research notes, target labs, and document reviews into a portable archive.",
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariant,
                    lineHeight = 17.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val formats = listOf(
                        Triple("json", "Structured JSON Archive", "Raw structured data with schema validation"),
                        Triple("md", "Markdown Research Dossier", "Formatted markdown summary report ready for Notion / Obsidian"),
                        Triple("zip", "Full Academic Bundle (ZIP)", "Complete archive containing raw data, markdown, and audit logs")
                    )

                    formats.forEach { (formatKey, title, desc) ->
                        Surface(
                            onClick = { selectedFormat = formatKey },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedFormat == formatKey) colors.academicBlue.copy(alpha = 0.12f) else colors.surface,
                            border = BorderStroke(
                                1.dp,
                                if (selectedFormat == formatKey) colors.academicBlue else colors.outlineVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                RadioButton(
                                    selected = selectedFormat == formatKey,
                                    onClick = { selectedFormat = formatKey },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = colors.academicBlue,
                                        unselectedColor = colors.onSurfaceVariant
                                    )
                                )
                                Column {
                                    Text(
                                        text = title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.onSurface
                                    )
                                    Text(
                                        text = desc,
                                        fontSize = 10.sp,
                                        color = colors.faintText
                                    )
                                }
                            }
                        }
                    }
                }

                if (exportSuccess) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = colors.verifiedEmerald.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, colors.verifiedEmerald.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = colors.verifiedEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Dossier exported successfully to Downloads folder!",
                                fontSize = 11.sp,
                                color = colors.verifiedEmerald,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        isExporting = true
                        delay(600)
                        isExporting = false
                        exportSuccess = true
                        delay(900)
                        onDismiss()
                    }
                },
                enabled = !isExporting,
                colors = ButtonDefaults.buttonColors(containerColor = colors.academicBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Exporting...", fontSize = 12.sp)
                } else {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export Archive", fontSize = 12.sp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = colors.onSurfaceVariant, fontSize = 12.sp)
            }
        }
    )
}

/**
 * Dialog displaying quick productivity keyboard shortcuts & reference guide.
 */
@Composable
fun ShortcutsHelpDialog(
    onDismiss: () -> Unit
) {
    val colors = GradPathTheme.colors

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceLow,
        titleContentColor = colors.onSurface,
        textContentColor = colors.onSurfaceVariant,
        shape = RoundedCornerShape(16.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.academicBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Keyboard,
                    contentDescription = null,
                    tint = colors.academicBlue,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        title = {
            Text(
                text = "Keyboard Shortcuts & Guide",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Power user shortcuts to navigate and discover research openings efficiently.",
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariant
                )

                val shortcuts = listOf(
                    "Ctrl + F / ⌘F" to "Global Search & Lab Discovery",
                    "Ctrl + R / ⌘R" to "Trigger Live Radar Sweep",
                    "Ctrl + B / ⌘B" to "Toggle Side Navigation Rail",
                    "Ctrl + S / ⌘S" to "Quick Save Opportunity to Watchlist",
                    "Ctrl + D / ⌘D" to "Toggle Theme Mode (Dark / Light)",
                    "Esc" to "Close Overlays, Modals, and Dialogs"
                )

                shortcuts.forEach { (combo, action) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.surface)
                            .border(1.dp, colors.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = colors.surfaceHigh,
                            border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = combo,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = colors.academicBlue,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = action,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = colors.academicBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Got It", fontSize = 12.sp)
            }
        }
    )
}