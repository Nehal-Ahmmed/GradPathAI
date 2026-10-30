package org.gradpath.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class ProfileService(
    private val cloudSyncService: CloudSyncService = CloudSyncService()
) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val profileFile = WorkspaceManager.PROFILE_FILE

    suspend fun loadProfile(): UserProfile = withContext(Dispatchers.IO) {
        if (!profileFile.exists()) {
            val defaultProfile = UserProfile()
            saveProfile(defaultProfile)
            return@withContext defaultProfile
        }
        try {
            val content = profileFile.readText()
            json.decodeFromString<UserProfile>(content)
        } catch (e: Exception) {
            UserProfile()
        }
    }

    suspend fun saveProfile(profile: UserProfile): Unit = withContext(Dispatchers.IO) {
        val serialized = json.encodeToString(profile)
        profileFile.writeText(serialized)
        cloudSyncService.syncProfile(profile)
    }
}
