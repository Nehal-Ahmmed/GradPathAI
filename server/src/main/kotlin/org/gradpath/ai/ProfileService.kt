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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.File

/**
 * ProfileService: Direct Backend Bridge between Client Application and Cloud Firestore.
 * 
 * Architecture:
 * Client Application <---> Backend (/api/profile) <---> Cloud Firestore DB
 * 
 * No complex queues or dummy data. Real Firestore data is always fetched and pushed.
 */
class ProfileService(
    @Suppress("UNUSED_PARAMETER") cloudSyncService: CloudSyncService? = null
) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val profileFile = WorkspaceManager.PROFILE_FILE

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) { json(json) }
    }

    private val projectId = System.getenv("FIREBASE_PROJECT_ID")?.takeIf { it.isNotBlank() } ?: "gradpathai-3915f"
    private val apiKey = System.getenv("FIREBASE_API_KEY")?.takeIf { it.isNotBlank() } ?: "AIzaSyAGOSdty4jWUwW4lvQPFAuqiA3oUnon1SU"
    private val firestoreUrl = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/profile/current_profile?key=$apiKey"

    /**
     * Loads the live academic profile directly from Cloud Firestore.
     * Caches to local disk for offline resilience.
     */
    suspend fun loadProfile(): UserProfile = withContext(Dispatchers.IO) {
        try {
            val response = httpClient.get(firestoreUrl)
            if (response.status == HttpStatusCode.OK) {
                val root = json.parseToJsonElement(response.bodyAsText()).jsonObject
                val fields = root["fields"]?.jsonObject
                val payloadString = fields?.get("payloadJson")?.jsonObject?.get("stringValue")?.jsonPrimitive?.contentOrNull

                if (!payloadString.isNullOrBlank() && payloadString != "{}") {
                    val profile = json.decodeFromString<UserProfile>(payloadString)
                    runCatching { profileFile.writeText(json.encodeToString(profile)) }
                    return@withContext profile
                }

                // If payloadJson was empty, construct UserProfile from individual Firestore fields
                if (fields != null) {
                    val name = fields["fullName"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.contentOrNull ?: ""
                    val major = fields["major"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.contentOrNull ?: ""
                    val institution = fields["institution"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.contentOrNull ?: ""
                    val gpa = fields["gpa"]?.jsonObject?.get("doubleValue")?.jsonPrimitive?.doubleOrNull
                        ?: fields["gpa"]?.jsonObject?.get("integerValue")?.jsonPrimitive?.longOrNull?.toDouble()
                        ?: 0.0
                    val degreeTrackStr = fields["degreeTrack"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.contentOrNull
                    val degreeTrack = try {
                        TargetDegree.valueOf(degreeTrackStr ?: "MASTERS")
                    } catch (_: Exception) {
                        TargetDegree.MASTERS
                    }
                    val targetTerm = fields["targetTerm"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.contentOrNull ?: "Fall 2026"

                    val reconstructed = UserProfile(
                        fullName = name,
                        academic = AcademicProfile(
                            gpa = gpa,
                            major = major,
                            institution = institution
                        ),
                        target = TargetPreferences(
                            targetDegree = degreeTrack,
                            targetTerm = targetTerm
                        )
                    )
                    runCatching { profileFile.writeText(json.encodeToString(reconstructed)) }
                    return@withContext reconstructed
                }
            }
        } catch (e: Exception) {
            println("[ProfileService] Notice: Could not read Firestore profile: ${e.message}")
        }

        // Local cache fallback if cloud is unreachable
        if (profileFile.exists() && profileFile.length() > 0) {
            try {
                return@withContext json.decodeFromString<UserProfile>(profileFile.readText())
            } catch (_: Exception) {}
        }

        UserProfile()
    }

    /**
     * Saves the academic profile directly to Cloud Firestore with structured fields.
     */
    suspend fun saveProfile(profile: UserProfile): Unit = withContext(Dispatchers.IO) {
        // 1. Update local cache
        runCatching { profileFile.writeText(json.encodeToString(profile)) }

        // 2. Direct push to Cloud Firestore
        try {
            val payloadStr = json.encodeToString(profile)
            val body = buildJsonObject {
                putJsonObject("fields") {
                    putJsonObject("collection") { put("stringValue", "profile") }
                    putJsonObject("documentId") { put("stringValue", "current_profile") }
                    putJsonObject("fullName") { put("stringValue", profile.fullName) }
                    putJsonObject("major") { put("stringValue", profile.academic.major) }
                    putJsonObject("institution") { put("stringValue", profile.academic.institution) }
                    putJsonObject("gpa") { put("doubleValue", profile.academic.gpa) }
                    putJsonObject("degreeTrack") { put("stringValue", profile.target.targetDegree.name) }
                    putJsonObject("targetTerm") { put("stringValue", profile.target.targetTerm) }
                    putJsonObject("payloadJson") { put("stringValue", payloadStr) }
                    putJsonObject("syncedAt") { put("stringValue", System.currentTimeMillis().toString()) }
                }
            }.toString()

            val response = httpClient.patch(firestoreUrl) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }

            if (!response.status.isSuccess()) {
                println("[ProfileService] Firestore PATCH error: ${response.status.value}")
            }
        } catch (e: Exception) {
            println("[ProfileService] Error pushing profile to Firestore: ${e.message}")
        }
    }
}
