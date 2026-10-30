package org.gradpath.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.File

/**
 * DesktopAntigravityService: Client-side Antigravity Engine interface.
 * Directly runs agy.exe on the developer workstation without coupling
 * the remote Ktor backend.
 */
class DesktopAntigravityService {
    private val agyPath: String = run {
        val envPath = System.getenv("AGY_PATH")
        if (!envPath.isNullOrBlank()) envPath
        else System.getProperty("user.home") + "\\AppData\\Local\\agy\\bin\\agy.exe"
    }

    private val workspaceDir = run {
        val envDir = System.getenv("WORKSPACE_DIR")
        if (!envDir.isNullOrBlank()) File(envDir)
        else if (System.getProperty("os.name").lowercase().contains("windows") && File("D:\\").exists()) File("D:\\University Research")
        else File(System.getProperty("user.home"), "University Research")
    }.apply { runCatching { if (!exists()) mkdirs() } }

    private val chatsDir = File(workspaceDir, "chats").apply { runCatching { if (!exists()) mkdirs() } }
    private val json = Json { ignoreUnknownKeys = true }

    fun isInstalled(): Boolean = File(agyPath).exists()

    fun executeResearchStream(prompt: String): Flow<String> = flow {
        if (!isInstalled()) {
            emit("""{"type":"error","text":"Antigravity CLI not found at $agyPath"}""")
            return@flow
        }

        val enrichedPrompt = """
            You are GradPath AI, an expert Study Abroad & Graduate Research Advisor. 
            Prioritize fully-funded MS/PhD opportunities, TA/RA assistantships, lab research alignment, and verified deadlines.
            Provide a well-structured, clear markdown response with bullet points and links.
            
            Student Question: $prompt
        """.trimIndent()

        val processBuilder = ProcessBuilder(
            agyPath,
            "--print", enrichedPrompt,
            "--output-format", "stream-json",
            "--dangerously-skip-permissions"
        )
        processBuilder.directory(workspaceDir)
        processBuilder.redirectErrorStream(true)

        val process = processBuilder.start()
        val reader = process.inputStream.bufferedReader()
        val responseBuilder = StringBuilder()
        var finalExtractedResponse: String? = null

        try {
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isBlank()) continue

                try {
                    val root = json.parseToJsonElement(line).jsonObject
                    val eventType = root["event"]?.jsonPrimitive?.contentOrNull

                    when (eventType) {
                        "init" -> {
                            emit("""{"type":"thinking","text":"⚡ Initializing Antigravity Research Agent..."}""")
                        }
                        "step_update" -> {
                            val stepUpdate = root["step_update"]?.jsonObject
                            val stepType = stepUpdate?.get("step_type")?.jsonPrimitive?.contentOrNull
                            val textDelta = stepUpdate?.get("text_delta")?.jsonPrimitive?.contentOrNull

                            if (stepType == "thought" || stepType == "tool_call") {
                                val thought = stepUpdate["thought"]?.jsonPrimitive?.contentOrNull 
                                    ?: "🔍 Searching official academic portals..."
                                emit("""{"type":"thinking","text":${Json.encodeToString(thought)}}""")
                            } else if (!textDelta.isNullOrEmpty()) {
                                responseBuilder.append(textDelta)
                                emit("""{"type":"delta","text":${Json.encodeToString(textDelta)}}""")
                            }
                        }
                        "result" -> {
                            val resultObj = root["result"]?.jsonObject
                            val finalResponse = resultObj?.get("response")?.jsonPrimitive?.contentOrNull
                            if (!finalResponse.isNullOrEmpty()) {
                                finalExtractedResponse = finalResponse
                                emit("""{"type":"final","text":${Json.encodeToString(finalResponse)}}""")
                            }
                        }
                        else -> {}
                    }
                } catch (_: Exception) {
                    if (!line.startsWith("{")) {
                        responseBuilder.append(line).append("\n")
                        emit("""{"type":"delta","text":${Json.encodeToString(line + "\n")}}""")
                    }
                }
            }
        } catch (e: Exception) {
            emit("""{"type":"error","text":${Json.encodeToString(e.message ?: "Unknown error")}}""")
        } finally {
            reader.close()
            process.destroy()

            // Save persistent chat history
            val totalContent = finalExtractedResponse ?: responseBuilder.toString()
            if (totalContent.isNotBlank()) {
                val chatFile = File(chatsDir, "chat_${System.currentTimeMillis()}.md")
                chatFile.writeText("# Research Query: $prompt\n\n$totalContent\n")
            }
        }
    }.flowOn(Dispatchers.IO)
}
