package org.gradpath.ai

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.serialization.json.Json
import java.io.File

fun main() {
    // Force CPU Software Rasterization (Bypasses DirectX 12 driver stall / headless state on Intel UHD Graphics)
    System.setProperty("skiko.renderApi", "SOFTWARE")

    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        System.err.println("CRITICAL ERROR on ${thread.name}: ${throwable.message}")
        throwable.printStackTrace()
        try {
            File("D:\\University Research\\desktop_crash.log").writeText(throwable.stackTraceToString())
        } catch (_: Exception) {}
    }

    application {
        // Initialize default Antigravity automations and sync context dossiers
        try {
            val profileFile = File("D:\\University Research\\db\\profile.json")
            val diaryFile = File("D:\\University Research\\db\\diary.json")
            val json = Json { ignoreUnknownKeys = true }
            val profile = if (profileFile.exists() && profileFile.length() > 0L) {
                try { json.decodeFromString<UserProfile>(profileFile.readText()) } catch (_: Exception) { UserProfile() }
            } else UserProfile()

            val diary = if (diaryFile.exists() && diaryFile.length() > 0L) {
                try { json.decodeFromString<ResearchDiary>(diaryFile.readText()) } catch (_: Exception) { ResearchDiary() }
            } else ResearchDiary()

            AutomationManager.initializeDefaultAutomations(profile, diary)
        } catch (e: Exception) {
            println("Automations init warning: ${e.message}")
        }

        val windowState = rememberWindowState(
            position = WindowPosition(Alignment.Center),
            width = 1280.dp,
            height = 820.dp
        )

        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            visible = true,
            alwaysOnTop = true,
            title = "GradPath AI - Graduate Research & Admissions Partner",
        ) {
            LaunchedEffect(Unit) {
                try {
                    window.minimumSize = java.awt.Dimension(1024, 680)
                    window.size = java.awt.Dimension(1280, 820)
                    window.setLocationRelativeTo(null)
                    window.isVisible = true
                    window.toFront()
                    window.requestFocus()
                    // Keep on top briefly to force Windows to show it over the IDE, then release
                    kotlinx.coroutines.delay(1200)
                    window.isAlwaysOnTop = false
                } catch (_: Exception) {}

                try {
                    File("D:\\University Research\\window_debug.txt").writeText("""
                        Window Debug Telemetry:
                        - Title: ${window.title}
                        - isVisible: ${window.isVisible}
                        - isShowing: ${window.isShowing}
                        - RenderApi: ${window.renderApi}
                        - WindowHandle: 0x${window.windowHandle.toString(16)}
                        - Bounds: ${window.bounds}
                        - ExtendedState: ${window.extendedState}
                    """.trimIndent())
                } catch (_: Exception) {}

                println("\n=======================================================")
                println("🚀 GradPath AI Desktop UI is LIVE and RUNNING!")
                println("📍 Window Size: 1280x820 (Centered)")
                println("💡 Renderer: ${window.renderApi}")
                println("💡 Tip: If the window is behind your IDE, press Alt + Tab")
                println("=======================================================\n")
            }

            val desktopAntigravityService = androidx.compose.runtime.remember { DesktopAntigravityService() }

            App(
                onTriggerManualSweep = {
                    AutomationManager.triggerManualSweep()
                },
                directChatRunner = { prompt ->
                    desktopAntigravityService.executeResearchStream(prompt)
                },
                onLoadAutomations = {
                    AutomationManager.loadAutomations()
                },
                onSaveAutomation = { item ->
                    AutomationManager.saveAutomation(item)
                },
                onDeleteAutomation = { id ->
                    AutomationManager.deleteAutomation(id)
                },
                onToggleAutomation = { id, enabled ->
                    AutomationManager.toggleAutomation(id, enabled)
                }
            )
        }
    }
}
