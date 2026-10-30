package org.gradpath.ai

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentAuditScreen(
    isServerConnected: Boolean,
    onBack: () -> Unit
) {
    val colors = GradPathTheme.colors
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var selectedDocType by remember { mutableStateOf(DocumentType.SOP) }
    var targetProgram by remember { mutableStateOf("Ph.D. in Computer Science") }
    var targetLab by remember { mutableStateOf("Carnegie Mellon LTI / MIT CSAIL") }
    var documentContent by remember { mutableStateOf("") }

    var isAuditing by remember { mutableStateOf(false) }
    var auditStreamOutput by remember { mutableStateOf("") }
    var thinkingOutput by remember { mutableStateOf("") }
    var isThinkingExpanded by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var mobileActiveTab by remember { mutableStateOf(0) } // 0: Editor, 1: Critique

    fun loadSample(type: DocumentType) {
        if (type == DocumentType.SOP) {
            documentContent = """
Statement of Purpose – Candidate for Ph.D. in Computer Science

Ever since my early undergraduate days, I have been captivated by the potential of artificial intelligence to automate complex human reasoning. During my bachelor's studies in Computer Science and Engineering, I developed a strong foundation in machine learning, linear algebra, and data structures.

My primary research interests lie at the intersection of Natural Language Processing and Autonomous Code Generation. In my senior thesis, I investigated parameter-efficient fine-tuning (PEFT) methods for code completion in resource-constrained environments. This work resulted in a first-authored paper presented at an IEEE regional conference, where our model achieved a 14% improvement in syntax validity over standard baselines.

I am particularly excited by the ongoing work at Carnegie Mellon University's Language Technologies Institute, specifically Dr. Graham Neubig's research on multilingual code intelligence and agentic code evaluation. My long-term goal is to build safe, verifiably correct foundation models that assist developers in writing secure systems. With my background in deep learning, active research publications, and academic perseverance, I am confident in my ability to thrive as a doctoral researcher in your department.
            """.trimIndent()
        } else {
            documentContent = """
Curriculum Vitae (CV) – Research Candidate

EDUCATION
B.Sc. in Computer Science and Engineering, 2024
CGPA: 3.88 / 4.00 (Rank: Top 3%)

PUBLICATIONS
1. "Adaptive Low-Rank Code Synthesis for Constrained Edge Compilers" – First Author, IEEE Access, 2024.
2. "Evaluating Prompt Robustness in Multi-Agent Code Generation" – Co-Author, NeurIPS Workshop on AI Systems, 2023.

RESEARCH EXPERIENCE
- Undergraduate Research Fellow, AI & Systems Laboratory
  * Designed novel attention caching algorithm reducing KV memory footprint by 28%.
  * Mentored 3 junior lab researchers in benchmark replication.

TECHNICAL PROFICIENCY
Languages: Python, Kotlin, C++, Rust, SQL.
Frameworks: PyTorch, HuggingFace Transformers, vLLM, DeepSpeed.
Standardized Tests: GRE Quant 168, GRE Verbal 158, IELTS Overall 8.0.
            """.trimIndent()
        }
    }

    fun startLiveAudit() {
        if (isAuditing || !isServerConnected || documentContent.isBlank()) return
        isAuditing = true
        mobileActiveTab = 1
        auditStreamOutput = ""
        thinkingOutput = ""

        coroutineScope.launch {
            val client = HttpClient(CIO) {
                install(WebSockets)
            }
            val jsonParser = Json { ignoreUnknownKeys = true }

            try {
                client.webSocket(GradPathConfig.wsEndpoint("/ws/profile/audit-doc")) {
                    val requestPayload = DocumentAuditRequest(
                        documentType = selectedDocType,
                        targetProgram = targetProgram,
                        targetProfessorOrLab = targetLab,
                        content = documentContent
                    )
                    send(Frame.Text(Json.encodeToString(requestPayload)))

                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val line = frame.readText().trim()
                            if (line.isEmpty()) continue

                            try {
                                val root = jsonParser.parseToJsonElement(line).jsonObject
                                val type = root["type"]?.jsonPrimitive?.content
                                val text = root["text"]?.jsonPrimitive?.content ?: ""

                                when (type) {
                                    "thinking" -> thinkingOutput += text + "\n"
                                    "delta" -> auditStreamOutput += text
                                    "final" -> auditStreamOutput = text
                                    else -> if (!line.startsWith("{")) auditStreamOutput += line + "\n"
                                }
                            } catch (_: Exception) {
                                if (!line.startsWith("{")) auditStreamOutput += line + "\n"
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                auditStreamOutput = "❌ Connection failed: ${e.message}\nPlease ensure Ktor backend server is running on port 8080."
            } finally {
                isAuditing = false
                client.close()
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
    ) {
        val isMobile = maxWidth < 768.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isMobile) 14.dp else 20.dp)
        ) {
            // --- Header Row ---
            if (isMobile) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.academicBlue)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SOP & CV Auditor",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface
                            )
                            Text(
                                text = "Antigravity Admissions Deliberation Engine",
                                fontSize = 10.sp,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }

                    // Mobile Quick Action Bar (Sample & Audit)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { loadSample(selectedDocType) },
                            border = BorderStroke(1.dp, colors.outline),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f).height(34.dp)
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = "Sample", tint = colors.academicBlue, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sample ${selectedDocType.name}", fontSize = 10.sp, color = colors.onSurface)
                        }

                        Button(
                            onClick = { startLiveAudit() },
                            enabled = isServerConnected && !isAuditing && documentContent.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.onSurface),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1.3f).height(34.dp)
                        ) {
                            if (isAuditing) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), color = colors.canvas, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Auditing...", fontSize = 10.sp, color = colors.canvas)
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Run", tint = colors.canvas, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Run Committee Audit", fontSize = 10.sp, color = colors.canvas, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Mobile Segmented Tab Switcher (Editor vs Live Critique)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.surfaceLow)
                            .border(1.dp, colors.outline, RoundedCornerShape(6.dp))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (mobileActiveTab == 0) colors.surfaceHigh else Color.Transparent)
                                .clickable { mobileActiveTab = 0 }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "1. Document Editor",
                                fontSize = 11.sp,
                                fontWeight = if (mobileActiveTab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (mobileActiveTab == 0) colors.academicBlue else colors.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (mobileActiveTab == 1) colors.surfaceHigh else Color.Transparent)
                                .clickable { mobileActiveTab = 1 }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (isAuditing) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                            .background(colors.verifiedEmerald)
                                    )
                                }
                                Text(
                                    text = if (isAuditing) "2. Live Review ●" else "2. Committee Review",
                                    fontSize = 11.sp,
                                    fontWeight = if (mobileActiveTab == 1) FontWeight.Bold else FontWeight.Medium,
                                    color = if (mobileActiveTab == 1) colors.academicBlue else colors.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                // Desktop Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.academicBlue)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Elite Admissions SOP & CV Auditor",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface
                            )
                            Text(
                                text = "Powered by Antigravity AI – Line-by-line faculty committee critique, flaws & polished rewrites",
                                fontSize = 11.sp,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }

                    // Quick Samples
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { loadSample(selectedDocType) },
                            border = BorderStroke(1.dp, colors.outline),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = "Sample", tint = colors.academicBlue, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Load Sample ${selectedDocType.name}", fontSize = 11.sp, color = colors.onSurface)
                        }

                        Button(
                            onClick = { startLiveAudit() },
                            enabled = isServerConnected && !isAuditing && documentContent.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.onSurface),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            if (isAuditing) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = colors.canvas, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Auditing...", fontSize = 11.sp, color = colors.canvas)
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Run", tint = colors.canvas, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Run Committee Audit", fontSize = 11.sp, color = colors.canvas, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Toast feedback
            AnimatedVisibility(visible = toastMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.academicBlue.copy(alpha = 0.15f))
                        .border(1.dp, colors.academicBlue.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = toastMessage ?: "", color = colors.academicBlue, fontSize = 11.sp)
                        IconButton(onClick = { toastMessage = null }, modifier = Modifier.size(16.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.academicBlue, modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }

            // Main Content Area
            if (isMobile) {
                // Mobile View: Render either Editor or Live Critique based on active tab
                if (mobileActiveTab == 0) {
                    DocumentEditorPanel(
                        selectedDocType = selectedDocType,
                        onSelectDocType = { selectedDocType = it },
                        targetProgram = targetProgram,
                        onTargetProgramChange = { targetProgram = it },
                        targetLab = targetLab,
                        onTargetLabChange = { targetLab = it },
                        documentContent = documentContent,
                        onDocumentContentChange = { documentContent = it },
                        isMobile = true,
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                } else {
                    DocumentCritiquePanel(
                        auditStreamOutput = auditStreamOutput,
                        thinkingOutput = thinkingOutput,
                        isThinkingExpanded = isThinkingExpanded,
                        onToggleThinking = { isThinkingExpanded = !isThinkingExpanded },
                        selectedDocType = selectedDocType,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(auditStreamOutput))
                            toastMessage = "📋 Copied audit report to clipboard!"
                        },
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                }
            } else {
                // Desktop View: Dual-pane horizontal split screen
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DocumentEditorPanel(
                        selectedDocType = selectedDocType,
                        onSelectDocType = { selectedDocType = it },
                        targetProgram = targetProgram,
                        onTargetProgramChange = { targetProgram = it },
                        targetLab = targetLab,
                        onTargetLabChange = { targetLab = it },
                        documentContent = documentContent,
                        onDocumentContentChange = { documentContent = it },
                        isMobile = false,
                        modifier = Modifier.weight(0.48f).fillMaxHeight()
                    )

                    DocumentCritiquePanel(
                        auditStreamOutput = auditStreamOutput,
                        thinkingOutput = thinkingOutput,
                        isThinkingExpanded = isThinkingExpanded,
                        onToggleThinking = { isThinkingExpanded = !isThinkingExpanded },
                        selectedDocType = selectedDocType,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(auditStreamOutput))
                            toastMessage = "📋 Copied audit report to clipboard!"
                        },
                        modifier = Modifier.weight(0.52f).fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
fun DocumentEditorPanel(
    selectedDocType: DocumentType,
    onSelectDocType: (DocumentType) -> Unit,
    targetProgram: String,
    onTargetProgramChange: (String) -> Unit,
    targetLab: String,
    onTargetLabChange: (String) -> Unit,
    documentContent: String,
    onDocumentContentChange: (String) -> Unit,
    isMobile: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = GradPathTheme.colors

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surfaceLow)
            .border(1.dp, colors.outline, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        // Doc Type Toggle Chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DocTypeChip(
                label = "📄 Statement of Purpose (SOP)",
                isSelected = selectedDocType == DocumentType.SOP
            ) {
                onSelectDocType(DocumentType.SOP)
            }
            DocTypeChip(
                label = "🎓 Academic CV",
                isSelected = selectedDocType == DocumentType.CV
            ) {
                onSelectDocType(DocumentType.CV)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isMobile) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = targetProgram,
                    onValueChange = onTargetProgramChange,
                    label = { Text("Target Program", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = targetLab,
                    onValueChange = onTargetLabChange,
                    label = { Text("Target Lab / Faculty", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = targetProgram,
                    onValueChange = onTargetProgramChange,
                    label = { Text("Target Program", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = targetLab,
                    onValueChange = onTargetLabChange,
                    label = { Text("Target Lab / Faculty", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text("Paste your document draft below:", fontSize = 11.sp, color = colors.onSurfaceVariant)
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = documentContent,
            onValueChange = onDocumentContentChange,
            placeholder = { Text("Paste your SOP essay or CV text here for deep admissions committee critique...", color = colors.faintText, fontSize = 12.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface,
                focusedTextColor = colors.onSurface,
                unfocusedTextColor = colors.onSurface
            ),
            shape = RoundedCornerShape(6.dp)
        )
    }
}

@Composable
fun DocumentCritiquePanel(
    auditStreamOutput: String,
    thinkingOutput: String,
    isThinkingExpanded: Boolean,
    onToggleThinking: () -> Unit,
    selectedDocType: DocumentType,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = GradPathTheme.colors

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surfaceLow)
            .border(1.dp, colors.outline, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Admissions Committee Review & Scores",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )

            if (auditStreamOutput.isNotBlank()) {
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = colors.academicBlue, modifier = Modifier.size(14.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Optional Thinking Accordion
        if (thinkingOutput.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.outline, RoundedCornerShape(6.dp))
                    .clickable { onToggleThinking() }
                    .padding(10.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Psychology, contentDescription = "Thinking", tint = colors.academicBlue, modifier = Modifier.size(15.dp))
                            Text("Antigravity Admissions Deliberation", fontSize = 11.sp, color = colors.academicBlue, fontWeight = FontWeight.SemiBold)
                        }
                        Icon(
                            imageVector = if (isThinkingExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle",
                            tint = colors.faintText,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    if (isThinkingExpanded) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = thinkingOutput,
                            fontSize = 10.sp,
                            color = colors.onSurfaceVariant,
                            maxLines = 8
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Main Critique View
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(colors.surface)
                .border(1.dp, colors.outline, RoundedCornerShape(6.dp))
                .padding(12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (auditStreamOutput.isNotBlank()) {
                MarkdownText(
                    markdown = auditStreamOutput,
                    textColor = colors.onSurface,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.RateReview, contentDescription = "Empty", tint = colors.faintText, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Click 'Run Committee Audit' to evaluate your ${selectedDocType.name}.",
                            color = colors.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Antigravity AI will score your document, point out red flags, and rewrite weak sections.",
                            color = colors.faintText,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DocTypeChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val colors = GradPathTheme.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) colors.surfaceHigh else colors.surface)
            .border(1.dp, if (isSelected) colors.academicBlue else colors.outline, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) colors.academicBlue else colors.onSurfaceVariant
        )
    }
}
