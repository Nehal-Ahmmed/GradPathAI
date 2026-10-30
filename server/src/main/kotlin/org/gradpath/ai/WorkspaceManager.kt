package org.gradpath.ai

import java.io.File

object WorkspaceManager {
    // Dedicated root workspace: environmental override or default Windows D: or fallback local workspace
    val ROOT_DIR: File = run {
        val envDir = System.getenv("WORKSPACE_DIR")
        if (!envDir.isNullOrBlank()) {
            File(envDir)
        } else if (File("/app/data").exists() || File("/app").exists()) {
            File("/app/data")
        } else if (System.getProperty("os.name").lowercase().contains("windows") && File("D:\\").exists()) {
            File("D:\\University Research")
        } else {
            File(System.getProperty("user.home"), ".gradpath_cloud_workspace")
        }
    }.apply {
        runCatching { if (!exists()) mkdirs() }
    }

    val DB_DIR: File = File(ROOT_DIR, "db").apply { runCatching { if (!exists()) mkdirs() } }
    val CHATS_DIR: File = File(ROOT_DIR, "chats").apply { runCatching { if (!exists()) mkdirs() } }
    val STORAGE_DIR: File = File(ROOT_DIR, "storage").apply { runCatching { if (!exists()) mkdirs() } }
    val REPORTS_DIR: File = File(ROOT_DIR, "reports").apply { runCatching { if (!exists()) mkdirs() } }
    val TASKS_DIR: File = File(ROOT_DIR, "tasks").apply { runCatching { if (!exists()) mkdirs() } }

    val PROFILE_FILE = File(DB_DIR, "profile.json")
    val WATCHDOGS_FILE = File(DB_DIR, "watchdogs.json")
    val FEED_FILE = File(DB_DIR, "feed.json")
    val DIARY_FILE = File(DB_DIR, "diary.json")
    val SETTINGS_FILE = File(DB_DIR, "settings.json")

    /**
     * Silently auto-migrates existing data from legacy ~/.gradpath/ folder
     * to the active workspace db directory.
     */
    fun performAutoMigration() {
        val legacyDir = File(System.getProperty("user.home"), ".gradpath")
        if (!legacyDir.exists()) return

        listOf("profile.json", "watchdogs.json", "feed.json", "diary.json").forEach { filename ->
            val legacyFile = File(legacyDir, filename)
            val targetFile = File(DB_DIR, filename)

            if (legacyFile.exists() && (!targetFile.exists() || targetFile.length() == 0L)) {
                try {
                    legacyFile.copyTo(targetFile, overwrite = true)
                    println("Successfully migrated $filename to ${DB_DIR.absolutePath}")
                } catch (e: Exception) {
                    println("Migration warning for $filename: ${e.message}")
                }
            }
        }
    }
}
