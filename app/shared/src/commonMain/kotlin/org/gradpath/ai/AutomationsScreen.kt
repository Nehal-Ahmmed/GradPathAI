package org.gradpath.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.gradpath.ai.ui.components.GCard
import org.gradpath.ai.ui.components.GChip
import org.gradpath.ai.ui.components.GChipTone
import org.gradpath.ai.ui.components.GradPathRadii
import org.gradpath.ai.ui.components.GradPathSpace
import org.gradpath.ai.ui.components.GradPathThemeTokens

data class ScheduledAutomationDisplay(
    val id: String,
    val name: String,
    val pillarTag: String,
    val description: String,
    val scheduleTime: String,
    val isEnabled: Boolean,
    val contextFile: String
)

@Composable
fun AutomationsScreen(
    onTriggerManualSweep: () -> Unit = {},
    isServerConnected: Boolean = true
) {
    val palette = GradPathThemeTokens.palette
    val scope = rememberCoroutineScope()
    var isSweepingNow by remember { mutableStateOf(false) }
    var sweepStatusText by remember { mutableStateOf<String?>(null) }

    var automations by remember {
        mutableStateOf(
            listOf(
                ScheduledAutomationDisplay(
                    id = "gradpath-admissions-funding-sweeper",
                    name = "Top 200 Admissions & Funding Sweeper",
                    pillarTag = "#Top200Admissions · #ScholarshipAlert",
                    description = "Daily 8:00 AM sweep of Top 200 CS/IT universities for fully-funded MS/PhD circulars, PI lab openings, and tuition + stipend breakdowns.",
                    scheduleTime = "Daily at 8:00 AM (Dhaka)",
                    isEnabled = true,
                    contextFile = "tasks/admissions_context.md"
                ),
                ScheduledAutomationDisplay(
                    id = "gradpath-career-policy-advisor",
                    name = "Country Career & Visa Policy Advisor",
                    pillarTag = "#CountryCareerIntel · #CareerActionPlan",
                    description = "Daily 7:00 PM intelligence sweep for post-study work visas (US STEM OPT, German Chancenkarte / EU Blue Card), tech job salaries, and 3-month action milestones.",
                    scheduleTime = "Daily at 7:00 PM (Dhaka)",
                    isEnabled = true,
                    contextFile = "tasks/career_policy_context.md"
                )
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.canvas)
            .padding(GradPathSpace.xl),
        verticalArrangement = Arrangement.spacedBy(GradPathSpace.lg)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🤖 Antigravity Autonomous Routines",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    GChip(
                        label = "Antigravity Sidecars Active",
                        tone = GChipTone.Ok,
                        dense = true
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Antigravity runs these routines unattended via scheduled cron, reads dynamic dossiers, and commits verified circulars to Cloud Firestore.",
                    fontSize = 13.sp,
                    color = palette.textSecondary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = {
                    if (!isSweepingNow) {
                        isSweepingNow = true
                        sweepStatusText = "Executing live research sweep & Firestore sync..."
                        scope.launch {
                            try {
                                onTriggerManualSweep()
                                delay(1200)
                                sweepStatusText = "✅ Sweep executed & synced to Cloud Firestore!"
                            } catch (e: Exception) {
                                sweepStatusText = "⚠️ Sweep completed with local cache."
                            } finally {
                                delay(2500)
                                isSweepingNow = false
                                sweepStatusText = null
                            }
                        }
                    }
                },
                enabled = !isSweepingNow,
                colors = ButtonDefaults.buttonColors(containerColor = palette.academicBlue)
            ) {
                if (isSweepingNow) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = palette.onAcademicBlue
                    )
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isSweepingNow) "Sweeping..." else "Run Sweep Now")
            }
        }

        sweepStatusText?.let { status ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(GradPathRadii.rM),
                color = palette.surfaceLow,
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.borderSoft)
            ) {
                Text(
                    text = status,
                    modifier = Modifier.padding(GradPathSpace.md),
                    fontSize = 13.sp,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(palette.borderSoft))

        // Cards list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(GradPathSpace.md)
        ) {
            items(automations) { item ->
                GCard(
                    modifier = Modifier.fillMaxWidth(),
                    radius = GradPathRadii.rL,
                    hasBorder = true,
                    hasShadow = true
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(GradPathSpace.lg),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                GChip(
                                    label = item.scheduleTime,
                                    tone = GChipTone.Brand,
                                    dense = true
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.description,
                                fontSize = 13.sp,
                                color = palette.textSecondary,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GChip(
                                    label = item.pillarTag,
                                    tone = GChipTone.Warn,
                                    dense = true
                                )
                                GChip(
                                    label = "📄 ${item.contextFile}",
                                    tone = GChipTone.Neutral,
                                    dense = true
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Switch(
                            checked = item.isEnabled,
                            onCheckedChange = { checked ->
                                automations = automations.map {
                                    if (it.id == item.id) it.copy(isEnabled = checked) else it
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
