package org.gradpath.ai

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.gradpath.ai.viewmodel.ChatMessage
import org.gradpath.ai.viewmodel.ResearchStudioViewModel

val SUGGESTION_CHIPS = listOf(
    "How to approach MIT CSAIL professor for Fall 2026 Ph.D.?",
    "Evaluate GRE waiver policy for German universities (TUM/LMU)",
    "Review my SOP abstract and recommend improvements",
    "List top 5 fully-funded AI labs in Canada (Mila, Vector, Amii)"
)

@Composable
fun ResearchStudioScreen(
    isServerConnected: Boolean,
    viewModel: ResearchStudioViewModel = viewModel { ResearchStudioViewModel() },
    directChatRunner: ((String) -> Flow<String>)? = null
) {
    val colors = GradPathTheme.colors
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(directChatRunner) {
        if (directChatRunner != null) {
            viewModel.setDirectChatRunner(directChatRunner)
        }
    }

    val isChatActive = directChatRunner != null || isServerConnected

    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
    ) {
        val isMobile = maxWidth < 768.dp

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // -------------------------------------------------------------
            // 1. STUDIO SUB-HEADER & ENGINE SESSION TELEMETRY
            // -------------------------------------------------------------
            Column(modifier = Modifier.fillMaxWidth().background(colors.surfaceLow)) {
                if (isMobile) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Research Studio",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.onSurface
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.academicBlue.copy(alpha = 0.12f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Gemini 2.5",
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = colors.academicBlue,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Compact status indicator
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colors.surface)
                                    .border(1.dp, colors.outline, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isGenerating) colors.academicBlue else (if (isChatActive) colors.verifiedEmerald else colors.alertAmber))
                                )
                                Text(
                                    text = if (isGenerating) "Reasoning..." else (if (directChatRunner != null) "Direct (0ms)" else (if (isServerConnected) "Online" else "Offline")),
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Research Studio",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.onSurface
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colors.academicBlue.copy(alpha = 0.12f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Gemini 2.5 Flash Thinking",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = colors.academicBlue,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Autonomous AI dialogue partner with live thinking deliberation trace & academic citations",
                                fontSize = 11.sp,
                                color = colors.onSurfaceVariant
                            )
                        }

                        // Status indicator
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.surface)
                                .border(1.dp, colors.outline, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isGenerating) colors.academicBlue else (if (isChatActive) colors.verifiedEmerald else colors.alertAmber))
                            )
                            Text(
                                text = if (isGenerating) "AI Reasoning Active" else (if (directChatRunner != null) "Direct Antigravity Engine: Online (0ms)" else (if (isServerConnected) "Local Engine: Online" else "Engine Offline")),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().dashedHairlineBottom(color = colors.lineDashed))
            }

            // -------------------------------------------------------------
            // 2. CONVERSATION MESSAGE STREAM
            // -------------------------------------------------------------
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = if (isMobile) 14.dp else 24.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatMessageItem(
                        message = msg,
                        onToggleThinking = { viewModel.toggleThinking(msg.id) },
                        onCopyText = { clipboardManager.setText(AnnotatedString(msg.text)) }
                    )
                }
            }

            // -------------------------------------------------------------
            // 3. BOTTOM SUGGESTIONS & PROMPT INPUT DOCK
            // -------------------------------------------------------------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceLow)
            ) {
                Box(modifier = Modifier.fillMaxWidth().dashedHairlineTop(color = colors.lineDashed))

                // Suggestion Chips Row
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (isMobile) 14.dp else 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(SUGGESTION_CHIPS) { chipText ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.surface)
                                .border(1.dp, colors.outline, RoundedCornerShape(16.dp))
                                .clickable {
                                    viewModel.sendMessage(chipText, isChatActive)
                                }
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = chipText,
                                fontSize = 11.sp,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }
                }

                // Input TextField & Send Action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = if (isMobile) 12.dp else 20.dp, end = if (isMobile) 12.dp else 20.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { viewModel.updateInputText(it) },
                        placeholder = {
                            Text(
                                if (isMobile) "Type research question..." else "Type your research query, university question, or SOP inquiry...",
                                fontSize = 13.sp,
                                color = colors.faintText
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.academicBlue,
                            unfocusedBorderColor = colors.outline,
                            focusedContainerColor = colors.surface,
                            unfocusedContainerColor = colors.surface,
                            focusedTextColor = colors.onSurface,
                            unfocusedTextColor = colors.onSurface
                        ),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendMessage(inputText, isChatActive)
                            }
                        },
                        enabled = isChatActive && !isGenerating && inputText.isNotBlank(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.onSurface,
                            disabledContainerColor = colors.surfaceHigh
                        ),
                        modifier = Modifier.height(48.dp)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = colors.canvas, strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = colors.canvas,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    onToggleThinking: () -> Unit,
    onCopyText: () -> Unit
) {
    val colors = GradPathTheme.colors

    if (message.isUser) {
        // User Message Bubble (Right-aligned, editorial clean)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 620.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceHigh)
                    .border(1.dp, colors.outline, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                SelectionContainer {
                    Text(
                        text = message.text,
                        fontSize = 13.sp,
                        color = colors.onSurface,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    } else {
        // AI Message Container (Left-aligned with diamond emblem & Thinking Accordion)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Identity row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .rotate(45f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.surfaceHigh)
                                .border(1.dp, colors.academicBlue.copy(alpha = 0.5f), RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("◆", color = colors.academicBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.rotate(-45f))
                        }
                        Text(
                            text = "GradPath AI Research Partner",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface
                        )
                    }

                    IconButton(onClick = onCopyText, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = colors.faintText, modifier = Modifier.size(14.dp))
                    }
                }

                // ---------------------------------------------------------
                // THINKING ACCORDION PANEL (Collapsible deliberation trace)
                // ---------------------------------------------------------
                if (message.thinking.isNotBlank()) {
                    val rotationState by animateFloatAsState(targetValue = if (message.isThinkingExpanded) 45f else 0f)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.surfaceLow)
                            .dashedBorder(width = 1.dp, color = colors.lineDashed, cornerRadius = 6.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleThinking() }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Psychology,
                                        contentDescription = "Thinking",
                                        tint = colors.academicBlue,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Deliberation & Internal Reasoning Trace",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.academicBlue
                                    )
                                }

                                Text(
                                    text = if (message.isThinkingExpanded) "− Collapse" else "+ Expand",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = colors.onSurfaceVariant
                                )
                            }

                            AnimatedVisibility(visible = message.isThinkingExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(colors.surface)
                                        .padding(12.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxWidth().dashedHairlineBottom(color = colors.lineDashed))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    SelectionContainer {
                                        Text(
                                            text = message.thinking.trim(),
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = colors.onSurfaceVariant,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Main Markdown Response Bubble
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surfaceLow)
                        .border(1.dp, colors.outline, RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    SelectionContainer {
                        MarkdownText(
                            markdown = message.text,
                            textColor = colors.onSurface,
                            fontSize = 13.sp,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }
    }
}
