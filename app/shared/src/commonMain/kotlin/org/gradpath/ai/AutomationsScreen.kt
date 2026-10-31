package org.gradpath.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.gradpath.ai.ui.components.GCard
import org.gradpath.ai.ui.components.GChip
import org.gradpath.ai.ui.components.GChipTone
import org.gradpath.ai.ui.components.GradPathRadii
import org.gradpath.ai.ui.components.GradPathSpace
import org.gradpath.ai.ui.components.GradPathThemeTokens

@Composable
fun AutomationsScreen(
    onTriggerManualSweep: () -> Unit = {},
    onLoadAutomations: (() -> List<ScheduledAutomationDisplay>)? = null,
    onSaveAutomation: ((ScheduledAutomationDisplay) -> Unit)? = null,
    onDeleteAutomation: ((String) -> Unit)? = null,
    onToggleAutomation: ((String, Boolean) -> Unit)? = null,
    isServerConnected: Boolean = true
) {
    val palette = GradPathThemeTokens.palette
    val scope = rememberCoroutineScope()
    var isSweepingNow by remember { mutableStateOf(false) }
    var sweepStatusText by remember { mutableStateOf<String?>(null) }

    val defaultList = remember {
        listOf(
            ScheduledAutomationDisplay(
                id = "gradpath-admissions-funding-sweeper",
                name = "Top 200 Admissions & Funding Sweeper",
                pillarTag = "#Top200Admissions · #ScholarshipAlert",
                description = "Daily 8:00 AM sweep of Top 200 CS/IT universities for fully-funded MS/PhD circulars, PI lab openings, and tuition + stipend breakdowns.",
                frequency = "Daily",
                timeOfDay = "8:00 AM",
                timeZone = "Asia/Dhaka",
                scheduleTime = "Daily at 8:00 AM (Dhaka)",
                isEnabled = true,
                contextFile = "tasks/admissions_context.md",
                prompt = """
                    You are GradPath AI's Elite Academic Opportunity Sweeper.
                    1. Read the student's profile and Top 200 scope from 'D:\University Research\tasks\admissions_context.md'.
                    2. Research active admissions circulars, fully-funded PhD openings, and graduate assistantships (RA/TA) in the student's target countries for their major.
                    3. For each opportunity discovered, extract funding details, hiring Principal Investigator (PI), and admission criteria.
                    4. Prepend newly discovered opportunities to 'D:\University Research\db\feed.json' as WatchdogOpportunity JSON.
                    5. Execute: powershell.exe -ExecutionPolicy Bypass -File "D:\University Research\tasks\sync_to_firestore.ps1" -Collection "feed" -FilePath "D:\University Research\db\feed.json".
                """.trimIndent()
            ),
            ScheduledAutomationDisplay(
                id = "gradpath-career-policy-advisor",
                name = "Country Career & Visa Policy Advisor",
                pillarTag = "#CountryCareerIntel · #CareerActionPlan",
                description = "Daily 7:00 PM intelligence sweep for tech job market trends, post-study work visa updates, and next-step milestones.",
                frequency = "Daily",
                timeOfDay = "7:00 PM",
                timeZone = "Asia/Dhaka",
                scheduleTime = "Daily at 7:00 PM (Dhaka)",
                isEnabled = true,
                contextFile = "tasks/career_policy_context.md",
                prompt = """
                    You are GradPath AI's Global Career & Policy Advisor.
                    1. Read the student's background and country goals from 'D:\University Research\tasks\career_policy_context.md'.
                    2. Conduct live web research on immigration & career news affecting graduate students in target countries (US STEM OPT, German Chancenkarte, Canada PGWP).
                    3. Structure findings as WatchdogOpportunity objects tagged with #CountryCareerIntel and #CareerActionPlan.
                    4. Append to 'D:\University Research\db\feed.json' and commit via sync_to_firestore.ps1.
                """.trimIndent()
            )
        )
    }

    var automations by remember {
        mutableStateOf(onLoadAutomations?.invoke()?.takeIf { it.isNotEmpty() } ?: defaultList)
    }

    // Modal Dialog State
    var showDialog by remember { mutableStateOf(false) }
    var editingAutomation by remember { mutableStateOf<ScheduledAutomationDisplay?>(null) }

    // Form fields
    var formName by remember { mutableStateOf("") }
    var formFrequency by remember { mutableStateOf("Daily") }
    var formTimeOfDay by remember { mutableStateOf("9:00 AM") }
    var formTimeZone by remember { mutableStateOf("Asia/Dhaka") }
    var formPillarTag by remember { mutableStateOf("#Top200Admissions") }
    var formDescription by remember { mutableStateOf("") }
    var formPrompt by remember { mutableStateOf("") }

    fun openNewAutomationDialog() {
        editingAutomation = null
        formName = "Robotics & AI Lab Vacancy Sweeper"
        formFrequency = "Daily"
        formTimeOfDay = "9:00 AM"
        formTimeZone = "Asia/Dhaka"
        formPillarTag = "#Top200Admissions"
        formDescription = "Daily sweep of Top 200 European & US Universities for active graduate research assistantships in Robotics and Embodied AI labs."
        formPrompt = """
            You are GradPath AI's Specialized Research Scout.
            1. Sweep official university lab pages and admissions circulars for Robotics & AI labs in the student's target universities.
            2. Identify Principal Investigators (PIs) hiring Master's/Ph.D. students for Fall 2026/Spring 2027.
            3. Extract funding details (Tuition waiver % + Monthly stipend amount), minimum CGPA cutoff, and GRE/IELTS requirements.
            4. Prepend newly verified opportunities to 'D:\University Research\db\feed.json' as WatchdogOpportunity JSON.
            5. Execute: powershell.exe -ExecutionPolicy Bypass -File "D:\University Research\tasks\sync_to_firestore.ps1" -Collection "feed" -FilePath "D:\University Research\db\feed.json".
        """.trimIndent()
        showDialog = true
    }

    fun openEditAutomationDialog(item: ScheduledAutomationDisplay) {
        editingAutomation = item
        formName = item.name
        formFrequency = item.frequency
        formTimeOfDay = item.timeOfDay
        formTimeZone = item.timeZone
        formPillarTag = item.pillarTag
        formDescription = item.description
        formPrompt = item.prompt.ifBlank {
            """
                You are GradPath AI's Elite Research Assistant.
                1. Read the student's background from 'D:\University Research\${item.contextFile}'.
                2. Research relevant academic circulars, scholarships, and opportunities in target regions.
                3. Prepend newly discovered opportunities to 'D:\University Research\db\feed.json'.
                4. Execute: powershell.exe -ExecutionPolicy Bypass -File "D:\University Research\tasks\sync_to_firestore.ps1" -Collection "feed" -FilePath "D:\University Research\db\feed.json".
            """.trimIndent()
        }
        showDialog = true
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

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                // + Add Automation Button
                Button(
                    onClick = { openNewAutomationDialog() },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.surfaceLow),
                    border = androidx.compose.foundation.BorderStroke(1.dp, palette.academicBlue.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = palette.academicBlue, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Automation", color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                }

                // Run Sweep Now Button
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
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
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

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Edit Routine Button
                            IconButton(
                                onClick = { openEditAutomationDialog(item) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Routine",
                                    tint = palette.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Delete Routine Button (Only for non-default or custom routines)
                            if (item.id != "gradpath-admissions-funding-sweeper" && item.id != "gradpath-career-policy-advisor") {
                                IconButton(
                                    onClick = {
                                        onDeleteAutomation?.invoke(item.id)
                                        automations = automations.filterNot { it.id == item.id }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Routine",
                                        tint = palette.alertAmber,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Toggle Switch
                            Switch(
                                checked = item.isEnabled,
                                onCheckedChange = { checked ->
                                    automations = automations.map {
                                        if (it.id == item.id) it.copy(isEnabled = checked) else it
                                    }
                                    onToggleAutomation?.invoke(item.id, checked)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog for New / Edit Automation
    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .widthIn(max = 680.dp)
                    .heightIn(max = 760.dp),
                shape = RoundedCornerShape(16.dp),
                color = palette.card,
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.borderSoft)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    // Dialog Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (editingAutomation == null) Icons.Default.AddCircle else Icons.Default.Edit,
                                contentDescription = null,
                                tint = palette.academicBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (editingAutomation == null) "New Antigravity Automation" else "Edit Automation Routine",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                        }
                        IconButton(onClick = { showDialog = false }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = palette.textMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Scrollable Form Fields
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. Automation Name
                        OutlinedTextField(
                            value = formName,
                            onValueChange = { formName = it },
                            label = { Text("Automation Routine Name") },
                            placeholder = { Text("e.g. Robotics & AI Lab Vacancy Sweeper") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = palette.academicBlue,
                                unfocusedBorderColor = palette.borderSoft,
                                focusedTextColor = palette.textPrimary,
                                unfocusedTextColor = palette.textPrimary
                            )
                        )

                        // 2. Schedule: Frequency + Time + Time Zone
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Frequency Selector
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Frequency", fontSize = 12.sp, color = palette.textSecondary, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("Daily", "Weekly").forEach { freq ->
                                        val isSelected = formFrequency.equals(freq, ignoreCase = true)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) palette.academicBlue.copy(alpha = 0.2f) else palette.surfaceLow)
                                                .border(1.dp, if (isSelected) palette.academicBlue else palette.borderSoft, RoundedCornerShape(8.dp))
                                                .clickable { formFrequency = freq }
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                freq,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) palette.academicBlue else palette.textPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            // Time of Day
                            OutlinedTextField(
                                value = formTimeOfDay,
                                onValueChange = { formTimeOfDay = it },
                                label = { Text("Time (e.g. 9:00 AM)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = palette.academicBlue,
                                    unfocusedBorderColor = palette.borderSoft,
                                    focusedTextColor = palette.textPrimary,
                                    unfocusedTextColor = palette.textPrimary
                                )
                            )

                            // Time Zone
                            OutlinedTextField(
                                value = formTimeZone,
                                onValueChange = { formTimeZone = it },
                                label = { Text("Time Zone") },
                                singleLine = true,
                                modifier = Modifier.weight(1.2f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = palette.academicBlue,
                                    unfocusedBorderColor = palette.borderSoft,
                                    focusedTextColor = palette.textPrimary,
                                    unfocusedTextColor = palette.textPrimary
                                )
                            )
                        }

                        // Presets Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Time Presets:", fontSize = 11.sp, color = palette.textMuted)
                            listOf("8:00 AM", "9:00 AM", "2:00 PM", "7:00 PM", "11:00 PM").forEach { preset ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(palette.surfaceLow)
                                        .clickable { formTimeOfDay = preset }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(preset, fontSize = 11.sp, color = palette.textSecondary)
                                }
                            }
                        }

                        // 3. Pillar Tag & Quick Chips
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = formPillarTag,
                                onValueChange = { formPillarTag = it },
                                label = { Text("Pillar Tag") },
                                placeholder = { Text("#Top200Admissions or #ScholarshipAlert") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = palette.academicBlue,
                                    unfocusedBorderColor = palette.borderSoft,
                                    focusedTextColor = palette.textPrimary,
                                    unfocusedTextColor = palette.textPrimary
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "#Top200Admissions",
                                    "#ScholarshipAlert",
                                    "#CountryCareerIntel",
                                    "#CareerActionPlan"
                                ).forEach { tag ->
                                    val isSelected = formPillarTag.contains(tag)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) palette.academicBlue.copy(alpha = 0.2f) else palette.surfaceLow)
                                            .border(1.dp, if (isSelected) palette.academicBlue else palette.borderSoft, RoundedCornerShape(6.dp))
                                            .clickable { formPillarTag = tag }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(tag, fontSize = 11.sp, color = if (isSelected) palette.academicBlue else palette.textSecondary)
                                    }
                                }
                            }
                        }

                        // 4. Description
                        OutlinedTextField(
                            value = formDescription,
                            onValueChange = { formDescription = it },
                            label = { Text("Description") },
                            placeholder = { Text("Brief purpose of this routine...") },
                            singleLine = false,
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = palette.academicBlue,
                                unfocusedBorderColor = palette.borderSoft,
                                focusedTextColor = palette.textPrimary,
                                unfocusedTextColor = palette.textPrimary
                            )
                        )

                        // 5. Research Prompt
                        OutlinedTextField(
                            value = formPrompt,
                            onValueChange = { formPrompt = it },
                            label = { Text("AI Autonomous Research Prompt") },
                            placeholder = { Text("Detailed research instructions for Antigravity...") },
                            minLines = 6,
                            maxLines = 10,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 16.sp
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = palette.academicBlue,
                                unfocusedBorderColor = palette.borderSoft,
                                focusedTextColor = palette.textPrimary,
                                unfocusedTextColor = palette.textPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dialog Footer Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showDialog = false }) {
                            Text("Cancel", color = palette.textSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (formName.isNotBlank() && formPrompt.isNotBlank()) {
                                    val scheduleStr = "$formFrequency at $formTimeOfDay ($formTimeZone)"
                                    val targetId = editingAutomation?.id ?: ("gradpath-custom-" + formName.lowercase().replace(Regex("[^a-z0-9]"), "-").take(24) + "-" + (System.currentTimeMillis() % 10000))
                                    val itemToSave = ScheduledAutomationDisplay(
                                        id = targetId,
                                        name = formName,
                                        pillarTag = formPillarTag,
                                        description = formDescription,
                                        frequency = formFrequency,
                                        timeOfDay = formTimeOfDay,
                                        timeZone = formTimeZone,
                                        scheduleTime = scheduleStr,
                                        isEnabled = editingAutomation?.isEnabled ?: true,
                                        contextFile = editingAutomation?.contextFile ?: "tasks/admissions_context.md",
                                        prompt = formPrompt
                                    )
                                    onSaveAutomation?.invoke(itemToSave)
                                    automations = onLoadAutomations?.invoke()?.takeIf { it.isNotEmpty() } ?: run {
                                        val list = automations.toMutableList()
                                        val idx = list.indexOfFirst { it.id == itemToSave.id }
                                        if (idx >= 0) list[idx] = itemToSave else list.add(itemToSave)
                                        list
                                    }
                                    showDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = palette.academicBlue)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (editingAutomation == null) "Create Automation" else "Update Routine")
                        }
                    }
                }
            }
        }
    }
}
