package org.gradpath.ai

import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String,
    val mode: String = "cloud_gateway",
    val firestore_connected: Boolean = false,
    val antigravityAvailable: Boolean = false
)

@Serializable
data class SimpleStatus(
    val status: String
)

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // Ensure cloud/local workspace directory exists and auto-migrate legacy data if present
    WorkspaceManager.performAutoMigration()

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }

    install(ContentNegotiation) {
        json()
    }

    val cloudSyncService = CloudSyncService()
    val profileService = ProfileService(cloudSyncService)
    val watchdogService = WatchdogSweeperService(cloudSyncService)
    val diaryService = DiaryService(profileService, cloudSyncService)

    routing {
        get("/") {
            call.respondText(sayHello("Ktor Cloud Gateway"))
        }

        get("/health") {
            call.respond(
                HealthResponse(
                    status = "OK",
                    mode = "cloud_gateway",
                    firestore_connected = cloudSyncService.isConfigured(),
                    antigravityAvailable = false
                )
            )
        }

        get("/api/sync/status") {
            val status = cloudSyncService.getSyncStatus()
            call.respond(status)
        }

        post("/api/sync/trigger") {
            cloudSyncService.processQueue()
            val status = cloudSyncService.getSyncStatus()
            call.respond(status)
        }

        // --- Academic Profile Endpoints ---
        get("/api/profile") {
            val profile = profileService.loadProfile()
            call.respond(profile)
        }

        post("/api/profile") {
            val profile = call.receive<UserProfile>()
            profileService.saveProfile(profile)
            call.respond(SimpleStatus("saved"))
        }

        // --- Watchdog Portals & Sweeper Endpoints ---
        get("/api/watchdogs") {
            val sources = watchdogService.loadSources()
            call.respond(sources)
        }

        post("/api/watchdogs") {
            val source = call.receive<MonitoredSource>()
            val updated = watchdogService.updateSource(source)
            call.respond(updated)
        }

        post("/api/watchdogs/sweep") {
            val result = watchdogService.runSweep()
            call.respond(result)
        }

        get("/api/feed") {
            val feed = watchdogService.loadFeed()
            call.respond(feed)
        }

        // --- Research Diary, Watchlist & Cold Email Endpoints ---
        get("/api/diary") {
            val diary = diaryService.loadDiary()
            call.respond(diary)
        }

        post("/api/diary/university") {
            val uni = call.receive<WatchlistUniversity>()
            val updated = diaryService.addOrUpdateUniversity(uni)
            call.respond(updated)
        }

        delete("/api/diary/university/{id}") {
            val id = call.parameters["id"] ?: ""
            val updated = diaryService.removeUniversity(id)
            call.respond(updated)
        }

        post("/api/diary/outreach") {
            val outreach = call.receive<ProfessorOutreach>()
            val updated = diaryService.addOrUpdateOutreach(outreach)
            call.respond(updated)
        }

        delete("/api/diary/outreach/{id}") {
            val id = call.parameters["id"] ?: ""
            val updated = diaryService.removeOutreach(id)
            call.respond(updated)
        }

        post("/api/diary/cold-email") {
            val req = call.receive<ColdEmailRequest>()
            val res = diaryService.generateColdEmail(req)
            call.respond(res)
        }

        get("/api/diary/export/csv") {
            val diary = diaryService.loadDiary()
            val csv = diaryService.exportToCsv(diary)
            call.respondText(csv, io.ktor.http.ContentType.Text.CSV)
        }

        get("/api/diary/export/markdown") {
            val diary = diaryService.loadDiary()
            val md = diaryService.exportToMarkdown(diary)
            call.respondText(md, io.ktor.http.ContentType.Text.Plain)
        }
    }
}
