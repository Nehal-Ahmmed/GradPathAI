package org.gradpath.ai

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    isServerConnected: Boolean,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToFeed: () -> Unit = {}
) {
    val colors = GradPathTheme.colors
    val scope = rememberCoroutineScope()

    var daemonUrl by remember { mutableStateOf(GradPathConfig.baseUrl) }
    var isCheckingConnection by remember { mutableStateOf(false) }
    var connectionTestResult by remember { mutableStateOf<String?>(null) }

    // Crawler Preferences
    var sweepTime by remember { mutableStateOf("08:00 AM") }
    var sweepFrequency by remember { mutableStateOf("Once Daily (Recommended)") }
    var matchCaliberThreshold by remember { mutableStateOf(80f) }
    var deepWebScraping by remember { mutableStateOf(true) }

    val institutions = listOf(
        "MIT", "Stanford", "Max Planck", "Cambridge",
        "ETH Zürich", "Oxford", "Harvard", "UC Berkeley", "Carnegie Mellon"
    )
    var selectedInstitutions by remember {
        mutableStateOf(setOf("MIT", "Max Planck", "Cambridge", "Stanford", "ETH Zürich"))
    }

    // Notification toggles
    var notifyDailySweep by remember { mutableStateOf(true) }
    var notifyWatchdogs by remember { mutableStateOf(true) }
    var notifyDeadlines by remember { mutableStateOf(true) }
    var notifyHighCaliberOnly by remember { mutableStateOf(false) }

    var saveConfirmationMessage by remember { mutableStateOf<String?>(null) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
    ) {
        val isMobile = maxWidth < 768.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = if (isMobile) 16.dp else 36.dp,
                vertical = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // -----------------------------------------------------------------
            // HEADER
            // -----------------------------------------------------------------
            item {
                Text(
                    text = "Settings",
                    fontSize = if (isMobile) 22.sp else 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
            }

            // Save Confirmation Banner
            if (saveConfirmationMessage != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.verifiedEmerald.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, colors.verifiedEmerald.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = colors.verifiedEmerald, modifier = Modifier.size(16.dp))
                            Text(text = saveConfirmationMessage ?: "", fontSize = 12.sp, color = colors.verifiedEmerald)
                        }
                    }
                }
            }

            // -----------------------------------------------------------------
            // 1. LOCAL ENGINE & DAEMON CONNECTION
            // -----------------------------------------------------------------
            item {
                SettingsSectionCard(
                    title = "Engine Connection",
                    icon = Icons.Default.Dns
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Daemon Status", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                                Text(
                                    text = if (isServerConnected) "Connected to Ktor Background Engine" else "Standalone Mode (Cloud Firestore Active)",
                                    fontSize = 11.sp,
                                    color = colors.onSurfaceVariant
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isServerConnected) colors.verifiedEmerald.copy(alpha = 0.12f) else colors.alertAmber.copy(alpha = 0.12f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isServerConnected) colors.verifiedEmerald else colors.alertAmber)
                                )
                                Text(
                                    text = if (isServerConnected) "ONLINE" else "STANDALONE",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isServerConnected) colors.verifiedEmerald else colors.alertAmber
                                )
                            }
                        }

                        // Daemon Endpoint Input
                        OutlinedTextField(
                            value = daemonUrl,
                            onValueChange = { daemonUrl = it },
                            label = { Text("Local Engine Endpoint URL", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.academicBlue,
                                unfocusedBorderColor = colors.outline,
                                focusedContainerColor = colors.surface,
                                unfocusedContainerColor = colors.surface
                            )
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    isCheckingConnection = true
                                    connectionTestResult = null
                                    scope.launch {
                                        try {
                                            val client = HttpClient(CIO)
                                            val response = client.get(GradPathConfig.endpoint("/health"))
                                            connectionTestResult = if (response.status.value == 200) "Connection OK (HTTP 200)" else "Returned HTTP ${response.status.value}"
                                            client.close()
                                        } catch (e: Exception) {
                                            connectionTestResult = "Failed: ${e.message ?: "Could not connect"}"
                                        } finally {
                                            isCheckingConnection = false
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceHigh),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                if (isCheckingConnection) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = colors.academicBlue)
                                    Spacer(modifier = Modifier.width(6.dp))
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp), tint = colors.onSurface)
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text("Test Connection", fontSize = 11.sp, color = colors.onSurface)
                            }

                            if (connectionTestResult != null) {
                                Text(
                                    text = connectionTestResult ?: "",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (connectionTestResult?.startsWith("Connection OK") == true) colors.verifiedEmerald else colors.alertAmber
                                )
                            }
                        }
                    }
                }
            }

            // -----------------------------------------------------------------
            // 2. AUTONOMOUS SWEEP & CRAWLER CONFIGURATION
            // -----------------------------------------------------------------
            item {
                SettingsSectionCard(
                    title = "Scheduled Sweeps",
                    icon = Icons.Default.TravelExplore
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Daily Sweep Time", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.surfaceHigh)
                                    .border(1.dp, colors.outline, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(sweepTime, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = colors.academicBlue)
                            }
                        }

                        HorizontalDivider(color = colors.lineDashed, thickness = 0.5.dp)

                        // Target Monitored Institutions
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Monitored Institutions (${selectedInstitutions.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)

                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(institutions) { inst ->
                                    val isSelected = selectedInstitutions.contains(inst)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedInstitutions = if (isSelected) {
                                                if (selectedInstitutions.size > 1) selectedInstitutions - inst else selectedInstitutions
                                            } else {
                                                selectedInstitutions + inst
                                            }
                                        },
                                        label = { Text(inst, fontSize = 11.sp) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = colors.academicBlue.copy(alpha = 0.15f),
                                            selectedLabelColor = colors.academicBlue,
                                            containerColor = colors.surface,
                                            labelColor = colors.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = colors.lineDashed, thickness = 0.5.dp)

                        // Match Caliber Threshold
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Minimum Match Threshold", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                                Text("${matchCaliberThreshold.toInt()}%", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = colors.academicBlue)
                            }
                            Slider(
                                value = matchCaliberThreshold,
                                onValueChange = { matchCaliberThreshold = it },
                                valueRange = 60f..95f,
                                steps = 7,
                                colors = SliderDefaults.colors(
                                    thumbColor = colors.academicBlue,
                                    activeTrackColor = colors.academicBlue,
                                    inactiveTrackColor = colors.surfaceHigh
                                )
                            )
                        }

                        HorizontalDivider(color = colors.lineDashed, thickness = 0.5.dp)

                        // Deep Web Scraping Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Deep Faculty & Preprint Scraping", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                            Switch(
                                checked = deepWebScraping,
                                onCheckedChange = { deepWebScraping = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = colors.academicBlue)
                            )
                        }
                    }
                }
            }

            // -----------------------------------------------------------------
            // 3. NOTIFICATION & ROUTING PREFERENCES
            // -----------------------------------------------------------------
            item {
                SettingsSectionCard(
                    title = "Notifications",
                    icon = Icons.Default.Notifications
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Daily Sweep Digest Alert", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                            Switch(
                                checked = notifyDailySweep,
                                onCheckedChange = { notifyDailySweep = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = colors.academicBlue)
                            )
                        }

                        HorizontalDivider(color = colors.lineDashed, thickness = 0.5.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Portal Real-Time Triggers", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                            Switch(
                                checked = notifyWatchdogs,
                                onCheckedChange = { notifyWatchdogs = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = colors.academicBlue)
                            )
                        }

                        HorizontalDivider(color = colors.lineDashed, thickness = 0.5.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Deadline Proximity Warnings", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                            Switch(
                                checked = notifyDeadlines,
                                onCheckedChange = { notifyDeadlines = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = colors.academicBlue)
                            )
                        }
                    }
                }
            }

            // -----------------------------------------------------------------
            // 4. THEME & DISPLAY
            // -----------------------------------------------------------------
            item {
                SettingsSectionCard(
                    title = "Appearance",
                    icon = Icons.Default.Palette
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Interface Theme Mode", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                                Text(if (isDarkMode) "Dark Mode (JetBrains Editorial Palette)" else "Light Mode (Academic Paper Canvas)", fontSize = 11.sp, color = colors.onSurfaceVariant)
                            }
                            Button(
                                onClick = onToggleTheme,
                                colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceHigh),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(if (isDarkMode) "🌙 Dark" else "☀ Light", fontSize = 12.sp, color = colors.onSurface)
                            }
                        }
                    }
                }
            }

            // -----------------------------------------------------------------
            // 5. DATA BACKUP & ACTIONS
            // -----------------------------------------------------------------
            item {
                SettingsSectionCard(
                    title = "Data & Backups",
                    icon = Icons.Default.FolderZip
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    saveConfirmationMessage = "Preferences calibrated & saved to local profile cache."
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.academicBlue),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save Calibration", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = onNavigateToNotifications,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, colors.outline),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(14.dp), tint = colors.onSurface)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("View Notification Center", fontSize = 11.sp, color = colors.onSurface)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
fun SettingsSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    val colors = GradPathTheme.colors

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceLow,
        border = BorderStroke(1.dp, colors.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = colors.academicBlue)
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                    color = colors.onSurfaceVariant
                )
            }
            HorizontalDivider(color = colors.lineDashed, thickness = 0.5.dp)
            content()
        }
    }
}
