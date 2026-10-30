package org.gradpath.ai

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Serializable
data class FirebaseConfig(
    val projectId: String = "",
    val apiKey: String = "",
    val fcmServerKey: String? = null
)

@Serializable
data class SyncStatus(
    val isCloudConnected: Boolean,
    val pendingQueueCount: Int,
    val lastSyncTimestamp: String?,
    val message: String
)

@Serializable
data class QueuedSyncItem(
    val collection: String,
    val documentId: String,
    val payloadJson: String,
    val queuedAt: String
)

/**
 * CloudSyncService: Full Production-Ready Cloud Firestore REST Synchronizer.
 * Supports environment variables (FIREBASE_PROJECT_ID, FIREBASE_API_KEY)
 * and local workspace configuration.
 */
class CloudSyncService {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val configFile = File(WorkspaceManager.DB_DIR, "firebase_config.json")
    private val queueFile = File(WorkspaceManager.DB_DIR, "sync_queue.json")

    private var lastSyncTime: String? = null

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }

    /**
     * Reads Firebase credentials:
     * 1. Environment variables first (Cloud-native deployment)
     * 2. Local JSON config file second (Desktop local development)
     */
    fun loadConfig(): FirebaseConfig? {
        val envProjectId = System.getenv("FIREBASE_PROJECT_ID")
        val envApiKey = System.getenv("FIREBASE_API_KEY")
        if (!envProjectId.isNullOrBlank() && !envApiKey.isNullOrBlank()) {
            return FirebaseConfig(
                projectId = envProjectId.trim(),
                apiKey = envApiKey.trim(),
                fcmServerKey = System.getenv("FIREBASE_FCM_SERVER_KEY")
            )
        }

        if (!configFile.exists() || configFile.length() == 0L) return null
        return try {
            val cfg = json.decodeFromString<FirebaseConfig>(configFile.readText())
            if (cfg.projectId.isNotBlank() && cfg.apiKey.isNotBlank() && !cfg.projectId.contains("your-firebase")) {
                cfg
            } else null
        } catch (_: Exception) {
            null
        }
    }

    fun isConfigured(): Boolean = loadConfig() != null

    suspend fun getSyncStatus(): SyncStatus = withContext(Dispatchers.IO) {
        val queue = loadQueue()
        val config = loadConfig()
        SyncStatus(
            isCloudConnected = config != null,
            pendingQueueCount = queue.size,
            lastSyncTimestamp = lastSyncTime,
            message = if (config != null) {
                "Active Firestore Sync to project: ${config.projectId}"
            } else {
                "Local-First Mode (Set FIREBASE_PROJECT_ID & FIREBASE_API_KEY env vars or configure firebase_config.json)"
            }
        )
    }

    suspend fun syncFeedOpportunity(opp: WatchdogOpportunity) = withContext(Dispatchers.IO) {
        enqueueItem("feed", opp.id, json.encodeToString(opp))
        processQueue()
    }

    suspend fun syncFeedBatch(opportunities: List<WatchdogOpportunity>) = withContext(Dispatchers.IO) {
        opportunities.forEach { opp ->
            enqueueItem("feed", opp.id, json.encodeToString(opp))
        }
        processQueue()
    }

    suspend fun syncProfile(profile: UserProfile) = withContext(Dispatchers.IO) {
        enqueueItem("profile", "current_profile", json.encodeToString(profile))
        processQueue()
    }

    suspend fun syncDiary(diary: ResearchDiary) = withContext(Dispatchers.IO) {
        enqueueItem("diary", "current_diary", json.encodeToString(diary))
        processQueue()
    }

    suspend fun fetchFeedFromCloud(): List<WatchdogOpportunity> = withContext(Dispatchers.IO) {
        val config = loadConfig() ?: return@withContext emptyList()
        val url = "https://firestore.googleapis.com/v1/projects/${config.projectId}/databases/(default)/documents/feed?key=${config.apiKey}"
        try {
            val response = httpClient.get(url)
            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                val root = json.parseToJsonElement(body).jsonObject
                val documents = root["documents"]?.jsonArray ?: return@withContext emptyList()
                documents.mapNotNull { doc ->
                    val payload = doc.jsonObject["fields"]
                        ?.jsonObject?.get("payloadJson")
                        ?.jsonObject?.get("stringValue")
                        ?.jsonPrimitive?.contentOrNull
                    if (!payload.isNullOrBlank()) {
                        try {
                            json.decodeFromString<WatchdogOpportunity>(payload)
                        } catch (_: Exception) { null }
                    } else null
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            println("[CloudSync] Cloud feed fetch notice: ${e.message}")
            emptyList()
        }
    }

    private suspend fun enqueueItem(collection: String, documentId: String, payloadJson: String) = withContext(Dispatchers.IO) {
        val queue = loadQueue().toMutableList()
        queue.removeIf { it.collection == collection && it.documentId == documentId }
        queue.add(
            QueuedSyncItem(
                collection = collection,
                documentId = documentId,
                payloadJson = payloadJson,
                queuedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
            )
        )
        saveQueue(queue)
    }

    /**
     * Drains the queue and commits documents to Cloud Firestore REST API.
     */
    suspend fun processQueue(): Int = withContext(Dispatchers.IO) {
        val config = loadConfig() ?: return@withContext 0
        val queue = loadQueue()
        if (queue.isEmpty()) return@withContext 0

        val failed = mutableListOf<QueuedSyncItem>()
        var successCount = 0

        for (item in queue) {
            val success = commitDocumentToFirestore(config, item)
            if (success) {
                successCount++
            } else {
                failed.add(item)
            }
        }

        saveQueue(failed)
        if (successCount > 0) {
            lastSyncTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            println("[CloudSync] Successfully synchronized $successCount documents to Firestore (Remaining in queue: ${failed.size}).")
        }
        successCount
    }

    /**
     * Issues HTTP PATCH to Cloud Firestore REST API:
     * https://firestore.googleapis.com/v1/projects/{projectId}/databases/(default)/documents/{collection}/{documentId}?key={apiKey}
     */
    private suspend fun commitDocumentToFirestore(config: FirebaseConfig, item: QueuedSyncItem): Boolean {
        val url = "https://firestore.googleapis.com/v1/projects/${config.projectId}/databases/(default)/documents/${item.collection}/${item.documentId}?key=${config.apiKey}"

        val firestoreBody = buildJsonObject {
            putJsonObject("fields") {
                putJsonObject("payloadJson") { put("stringValue", item.payloadJson) }
                putJsonObject("collection") { put("stringValue", item.collection) }
                putJsonObject("documentId") { put("stringValue", item.documentId) }
                putJsonObject("syncedAt") { put("stringValue", LocalDateTime.now().toString()) }
            }
        }.toString()

        return try {
            val response = httpClient.patch(url) {
                contentType(ContentType.Application.Json)
                setBody(firestoreBody)
            }

            if (response.status.isSuccess()) {
                true
            } else {
                println("[CloudSync] Firestore HTTP error ${response.status.value}: ${response.bodyAsText()}")
                false
            }
        } catch (e: Exception) {
            println("[CloudSync] Network error syncing ${item.collection}/${item.documentId}: ${e.message}")
            false
        }
    }

    private fun loadQueue(): List<QueuedSyncItem> {
        if (!queueFile.exists()) return emptyList()
        return try {
            json.decodeFromString(queueFile.readText())
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveQueue(items: List<QueuedSyncItem>) {
        try {
            queueFile.writeText(json.encodeToString(items))
        } catch (e: Exception) {
            println("Warning saving sync queue: ${e.message}")
        }
    }
}
