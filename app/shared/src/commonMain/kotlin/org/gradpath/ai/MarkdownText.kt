package org.gradpath.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * High-performance, zero-dependency Markdown Renderer Composable for Compose Multiplatform.
 * Elegantly formats headers (#, ##, ###), bold (**text**), italics (*text*),
 * inline code (`code`), bullet lists (*, -), numbered lists (1., 2.), dividers (---),
 * and multi-line code blocks.
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    textColor: Color = GradPathTextPrimary,
    fontSize: TextUnit = 14.sp,
    lineHeight: TextUnit = 22.sp
) {
    val lines = remember(markdown) { markdown.lines() }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        var inCodeBlock = false
        val codeBlockLines = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()

            // Handle multi-line code blocks (```)
            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
                    // Flush accumulated code block
                    val codeContent = codeBlockLines.joinToString("\n")
                    CodeBlock(codeContent)
                    codeBlockLines.clear()
                    inCodeBlock = false
                } else {
                    inCodeBlock = true
                }
                continue
            }

            if (inCodeBlock) {
                codeBlockLines.add(line)
                continue
            }

            // Empty lines
            if (trimmed.isEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                continue
            }

            // Horizontal Divider (--- or ***)
            if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    color = GradPathCardBorder,
                    thickness = 1.dp
                )
                continue
            }

            // Headers (#, ##, ###)
            when {
                trimmed.startsWith("### ") -> {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = parseInlineMarkdown(trimmed.removePrefix("### ")),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = GradPathCyanAccent,
                        lineHeight = 22.sp
                    )
                }
                trimmed.startsWith("## ") -> {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = parseInlineMarkdown(trimmed.removePrefix("## ")),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = GradPathTextPrimary,
                        lineHeight = 24.sp
                    )
                }
                trimmed.startsWith("# ") -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = parseInlineMarkdown(trimmed.removePrefix("# ")),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = GradPathCyanAccent,
                        lineHeight = 26.sp
                    )
                }

                // Bullet Lists (* or -)
                trimmed.startsWith("* ") || trimmed.startsWith("- ") -> {
                    val content = if (trimmed.startsWith("* ")) trimmed.removePrefix("* ") else trimmed.removePrefix("- ")
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 6.dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            color = GradPathCyanAccent,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = parseInlineMarkdown(content),
                            color = textColor,
                            fontSize = fontSize,
                            lineHeight = lineHeight
                        )
                    }
                }

                // Numbered Lists (e.g. "1. ", "2. ", "10. ")
                trimmed.matches(Regex("""^\d+\.\s.*""")) -> {
                    val dotIndex = trimmed.indexOf(". ")
                    val number = trimmed.substring(0, dotIndex + 1)
                    val content = trimmed.substring(dotIndex + 2)

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = number,
                            color = GradPathCyanAccent,
                            fontSize = fontSize,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = parseInlineMarkdown(content),
                            color = textColor,
                            fontSize = fontSize,
                            lineHeight = lineHeight
                        )
                    }
                }

                // Regular Paragraph
                else -> {
                    Text(
                        text = parseInlineMarkdown(trimmed),
                        color = textColor,
                        fontSize = fontSize,
                        lineHeight = lineHeight
                    )
                }
            }
        }

        // Handle any unclosed code block
        if (inCodeBlock && codeBlockLines.isNotEmpty()) {
            CodeBlock(codeBlockLines.joinToString("\n"))
        }
    }
}

@Composable
private fun CodeBlock(code: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .border(1.dp, GradPathCardBorder, RoundedCornerShape(6.dp))
            .padding(10.dp)
    ) {
        Text(
            text = code,
            color = GradPathCyanAccent,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    }
}

/**
 * Parses inline formatting:
 * **bold text** -> Bold Span
 * *italic text* -> Italic Span
 * `inline code` -> Monospace Code Span
 */
fun parseInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        val len = text.length

        while (i < len) {
            when {
                // Bold: **text**
                i + 1 < len && text[i] == '*' && text[i + 1] == '*' -> {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        val boldContent = text.substring(i + 2, end)
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) {
                            append(boldContent)
                        }
                        i = end + 2
                    } else {
                        append("**")
                        i += 2
                    }
                }

                // Inline code: `code`
                text[i] == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end != -1) {
                        val codeContent = text.substring(i + 1, end)
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = Color(0x3338BDF8),
                                color = Color(0xFF38BDF8),
                                fontSize = 12.sp
                            )
                        ) {
                            append(" $codeContent ")
                        }
                        i = end + 1
                    } else {
                        append('`')
                        i++
                    }
                }

                // Italic: *text* (single asterisk, not followed by asterisk)
                text[i] == '*' -> {
                    val end = text.indexOf('*', i + 1)
                    if (end != -1 && end > i + 1) {
                        val italicContent = text.substring(i + 1, end)
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(italicContent)
                        }
                        i = end + 1
                    } else {
                        append('*')
                        i++
                    }
                }

                // Normal Character
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}
