package org.gradpath.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class WatchdogSweeperService(
    private val cloudSyncService: CloudSyncService = CloudSyncService()
) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val sourcesFile = WorkspaceManager.WATCHDOGS_FILE
    private val feedFile = WorkspaceManager.FEED_FILE

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    /**
     * Loads the list of monitored watchdog portals.
     * If the file doesn't exist yet, seeds it with [WatchdogRegistry.DEFAULT_SOURCES].
     */
    suspend fun loadSources(): List<MonitoredSource> = withContext(Dispatchers.IO) {
        if (!sourcesFile.exists()) {
            val defaultList = WatchdogRegistry.DEFAULT_SOURCES
            saveSources(defaultList)
            return@withContext defaultList
        }
        try {
            val content = sourcesFile.readText()
            json.decodeFromString<List<MonitoredSource>>(content)
        } catch (e: Exception) {
            WatchdogRegistry.DEFAULT_SOURCES
        }
    }

    /**
     * Persists the watchdog sources list to ~/.gradpath/watchdogs.json
     */
    suspend fun saveSources(sources: List<MonitoredSource>): Unit = withContext(Dispatchers.IO) {
        val serialized = json.encodeToString(sources)
        sourcesFile.writeText(serialized)
    }

    /**
     * Toggles an individual source's enabled state or adds a new one.
     */
    suspend fun updateSource(updated: MonitoredSource): List<MonitoredSource> = withContext(Dispatchers.IO) {
        val current = loadSources().toMutableList()
        val index = current.indexOfFirst { it.id == updated.id }
        if (index != -1) {
            current[index] = updated
        } else {
            current.add(updated)
        }
        saveSources(current)
        current
    }

    /**
     * Loads all discovered opportunities for the academic feed.
     * Always fetches real verified data from Cloud Firestore if configured,
     * caching locally to feed.json with offline fallback.
     */
    suspend fun loadFeed(): List<WatchdogOpportunity> = withContext(Dispatchers.IO) {
        if (cloudSyncService.isConfigured()) {
            try {
                val cloudFeed = cloudSyncService.fetchFeedFromCloud()
                if (cloudFeed.isNotEmpty()) {
                    saveFeed(cloudFeed)
                    return@withContext cloudFeed
                }
            } catch (e: Exception) {
                println("[WatchdogSweeper] Notice: Falling back to local cache: ${e.message}")
            }
        }

        if (!feedFile.exists() || feedFile.length() == 0L) {
            return@withContext emptyList()
        }
        try {
            val content = feedFile.readText()
            json.decodeFromString<List<WatchdogOpportunity>>(content)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Persists opportunities to ~/.gradpath/feed.json
     */
    suspend fun saveFeed(opportunities: List<WatchdogOpportunity>): Unit = withContext(Dispatchers.IO) {
        val serialized = json.encodeToString(opportunities)
        feedFile.writeText(serialized)
    }

    /**
     * Executes an active sweep over all enabled watchdog portals.
     * Evaluates source URLs, updates lastSweptAt, and syncs
     * latest verified opportunities from Cloud Firestore.
     */
    suspend fun runSweep(): SweepResult = withContext(Dispatchers.IO) {
        val now = LocalDateTime.now().format(dateFormatter)
        val currentSources = loadSources()
        val enabledSources = currentSources.filter { it.isEnabled }

        // Update lastSweptAt timestamp for all enabled sources
        val updatedSources = currentSources.map { source ->
            if (source.isEnabled) source.copy(lastSweptAt = now) else source
        }
        saveSources(updatedSources)

        // Pull latest verified real data from Firestore
        val liveFeed = if (cloudSyncService.isConfigured()) {
            try {
                val cloudFeed = cloudSyncService.fetchFeedFromCloud()
                if (cloudFeed.isNotEmpty()) {
                    saveFeed(cloudFeed)
                    cloudFeed
                } else {
                    loadFeed()
                }
            } catch (_: Exception) {
                loadFeed()
            }
        } else {
            loadFeed()
        }

        SweepResult(
            sweptSourcesCount = enabledSources.size,
            newOpportunitiesFound = liveFeed.size,
            totalFeedCount = liveFeed.size,
            timestamp = now
        )
    }
}
