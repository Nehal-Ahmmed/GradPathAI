package org.gradpath.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import org.gradpath.ai.*

/**
 * -------------------------------------------------------------------------
 * 1. FeedViewModel - Manages Opportunities feed, search, tags, filtering & details
 * -------------------------------------------------------------------------
 */
class FeedViewModel(
    private val client: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
    }
) : ViewModel() {

    private val _feedItems = MutableStateFlow<List<WatchdogOpportunity>>(emptyList())
    val feedItems: StateFlow<List<WatchdogOpportunity>> = _feedItems.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedItem = MutableStateFlow<WatchdogOpportunity?>(null)
    val selectedItem: StateFlow<WatchdogOpportunity?> = _selectedItem.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTagFilter = MutableStateFlow("ALL")
    val selectedTagFilter: StateFlow<String> = _selectedTagFilter.asStateFlow()

    private val _selectedDegreeFilter = MutableStateFlow("ALL")
    val selectedDegreeFilter: StateFlow<String> = _selectedDegreeFilter.asStateFlow()

    private val _bookmarkedIds = MutableStateFlow<Set<String>>(emptySet())
    val bookmarkedIds: StateFlow<Set<String>> = _bookmarkedIds.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedTagFilter(tag: String) {
        _selectedTagFilter.value = tag
    }

    fun setSelectedDegreeFilter(degree: String) {
        _selectedDegreeFilter.value = degree
    }

    fun selectItem(item: WatchdogOpportunity?) {
        _selectedItem.value = item
    }

    fun toggleBookmark(id: String) {
        val current = _bookmarkedIds.value
        _bookmarkedIds.value = if (current.contains(id)) current - id else current + id
        _toastMessage.value = if (_bookmarkedIds.value.contains(id)) "Bookmark saved to Dossier" else "Bookmark removed"
    }

    fun dismissToast() {
        _toastMessage.value = null
    }

    suspend fun fetchFromFirestore() {
        try {
            val firestoreUrl = "https://firestore.googleapis.com/v1/projects/gradpathai-3915f/databases/(default)/documents/feed?key=AIzaSyAGOSdty4jWUwW4lvQPFAuqiA3oUnon1SU"
            val res = client.get(firestoreUrl)
            if (res.status == HttpStatusCode.OK) {
                val jsonText = res.bodyAsText()
                val jsonParser = Json { ignoreUnknownKeys = true }
                val root = jsonParser.parseToJsonElement(jsonText).jsonObject
                val docs = root["documents"]?.jsonArray ?: emptyList()
                val items = mutableListOf<WatchdogOpportunity>()

                for (doc in docs) {
                    try {
                        val fields = doc.jsonObject["fields"]?.jsonObject
                        val payloadString = fields?.get("payloadJson")?.jsonObject?.get("stringValue")?.jsonPrimitive?.content
                        if (!payloadString.isNullOrBlank()) {
                            val parsed = jsonParser.decodeFromString<WatchdogOpportunity>(payloadString)
                            items.add(parsed)
                        }
                    } catch (_: Exception) {}
                }

                if (items.isNotEmpty()) {
                    _feedItems.value = items.toList()
                    if (_selectedItem.value == null) _selectedItem.value = items.first()
                }
            }
        } catch (e: Exception) {
            println("Firestore direct fallback error: ${e.message}")
        }
    }

    fun fetchFeed(isServerConnected: Boolean) {
        if (_feedItems.value.isNotEmpty() && _isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (isServerConnected) {
                    val response = client.get(GradPathConfig.endpoint("/api/feed"))
                    if (response.status == HttpStatusCode.OK) {
                        val items = response.body<List<WatchdogOpportunity>>()
                        _feedItems.value = items
                        if (_selectedItem.value == null && items.isNotEmpty()) {
                            _selectedItem.value = items.first()
                        }
                    } else {
                        fetchFromFirestore()
                    }
                } else {
                    fetchFromFirestore()
                }
            } catch (e: Exception) {
                fetchFromFirestore()
            } finally {
                _isLoading.value = false
            }
        }
    }
}

/**
 * -------------------------------------------------------------------------
 * 2. ResearchStudioViewModel - Manages AI Dialogue, Thinking Accordion & WebSocket
 * -------------------------------------------------------------------------
 */
data class ChatMessage(
    val id: String = System.currentTimeMillis().toString(),
    val text: String,
    val isUser: Boolean,
    val thinking: String = "",
    val isThinkingExpanded: Boolean = false,
    val isStreaming: Boolean = false
)

class ResearchStudioViewModel(
    private val client: HttpClient = HttpClient(CIO) {
        install(WebSockets)
    },
    private var directChatRunner: ((String) -> Flow<String>)? = null
) : ViewModel() {

    fun setDirectChatRunner(runner: ((String) -> Flow<String>)?) {
        this.directChatRunner = runner
    }

    val hasDirectRunner: Boolean
        get() = directChatRunner != null

    private val json = Json { ignoreUnknownKeys = true }

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                text = "হ্যালো! আমি আপনার GradPath AI রিসার্চ পার্টনার। ফুল-ফান্ডেড পিএইচডি, মাস্টার্স, জিআরই ওয়েভার বা প্রফেসর ল্যাব নিয়ে যেকোনো প্রশ্ন করতে পারেন।",
                isUser = false
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun toggleThinking(messageId: String) {
        _messages.value = _messages.value.map { msg ->
            if (msg.id == messageId) msg.copy(isThinkingExpanded = !msg.isThinkingExpanded) else msg
        }
    }

    fun sendMessage(promptText: String, isServerConnected: Boolean) {
        if (promptText.isBlank() || _isGenerating.value) return
        if (directChatRunner == null && !isServerConnected) return

        val userMsg = ChatMessage(text = promptText, isUser = true)
        val aiMsgId = (System.currentTimeMillis() + 1).toString()
        val aiMsgPlaceholder = ChatMessage(
            id = aiMsgId,
            text = "গবেষণা ও তথ্য সংগ্রহ করা হচ্ছে...",
            isUser = false,
            isStreaming = true
        )

        _messages.value = _messages.value + userMsg + aiMsgPlaceholder
        _inputText.value = ""
        _isGenerating.value = true

        viewModelScope.launch {
            try {
                if (directChatRunner != null) {
                    var accumulatedText = ""
                    var accumulatedThinking = ""

                    directChatRunner!!.invoke(promptText).collect { line ->
                        val trimmed = line.trim()
                        if (trimmed.isNotEmpty()) {
                            try {
                                val root = json.parseToJsonElement(trimmed).jsonObject
                                val type = root["type"]?.jsonPrimitive?.contentOrNull
                                val text = root["text"]?.jsonPrimitive?.contentOrNull ?: ""

                                when (type) {
                                    "thinking" -> accumulatedThinking += text + "\n"
                                    "delta" -> accumulatedText += text
                                    "final" -> accumulatedText = text
                                    "error" -> accumulatedText = "❌ $text"
                                    else -> if (!trimmed.startsWith("{")) accumulatedText += trimmed + "\n"
                                }
                            } catch (_: Exception) {
                                if (!trimmed.startsWith("{")) accumulatedText += trimmed + "\n"
                            }

                            val displayText = if (accumulatedText.isNotBlank()) accumulatedText else "গবেষণা ও তথ্য সংগ্রহ করা হচ্ছে..."

                            _messages.value = _messages.value.map { msg ->
                                if (msg.id == aiMsgId) {
                                    msg.copy(
                                        text = displayText,
                                        thinking = accumulatedThinking,
                                        isStreaming = true
                                    )
                                } else msg
                            }
                        }
                    }
                } else {
                    client.webSocket(GradPathConfig.wsEndpoint("/ws/research")) {
                        send(Frame.Text(promptText))

                        var accumulatedText = ""
                        var accumulatedThinking = ""

                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val line = frame.readText().trim()
                                if (line.isEmpty()) continue

                                try {
                                    val root = json.parseToJsonElement(line).jsonObject
                                    val type = root["type"]?.jsonPrimitive?.contentOrNull
                                    val text = root["text"]?.jsonPrimitive?.contentOrNull ?: ""

                                    when (type) {
                                        "thinking" -> accumulatedThinking += text + "\n"
                                        "delta" -> accumulatedText += text
                                        "final" -> accumulatedText = text
                                        "error" -> accumulatedText = "❌ $text"
                                        else -> if (!line.startsWith("{")) accumulatedText += line + "\n"
                                    }
                                } catch (_: Exception) {
                                    if (!line.startsWith("{")) accumulatedText += line + "\n"
                                }

                                val displayText = if (accumulatedText.isNotBlank()) accumulatedText else "গবেষণা ও তথ্য সংগ্রহ করা হচ্ছে..."

                                _messages.value = _messages.value.map { msg ->
                                    if (msg.id == aiMsgId) {
                                        msg.copy(
                                            text = displayText,
                                            thinking = accumulatedThinking,
                                            isStreaming = true
                                        )
                                    } else msg
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _messages.value = _messages.value.map { msg ->
                    if (msg.id == aiMsgId) {
                        msg.copy(
                            text = if (msg.text.isNotBlank() && msg.text != "গবেষণা ও তথ্য সংগ্রহ করা হচ্ছে...") msg.text else "❌ কানেকশন এরর: ${e.message}\n(ডেস্কটপ Antigravity ইঞ্জিন বা ক্লাউড সংযোগ নিশ্চিত করুন)",
                            isStreaming = false
                        )
                    } else msg
                }
            } finally {
                _isGenerating.value = false
                _messages.value = _messages.value.map { if (it.id == aiMsgId) it.copy(isStreaming = false) else it }
            }
        }
    }
}

/**
 * -------------------------------------------------------------------------
 * 3. ProfileViewModel - Manages Dual-Track Profile, Score Engine & AI Gap Audit
 * -------------------------------------------------------------------------
 */
class ProfileViewModel(
    private val client: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) { json() }
        install(WebSockets)
    }
) : ViewModel() {

    private val jsonParser = Json { ignoreUnknownKeys = true }

    private val _profile = MutableStateFlow(UserProfile())
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    private val _auditResult = MutableStateFlow("")
    val auditResult: StateFlow<String> = _auditResult.asStateFlow()

    private val _isAuditing = MutableStateFlow(false)
    val isAuditing: StateFlow<Boolean> = _isAuditing.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    private val _showDocAuditor = MutableStateFlow(false)
    val showDocAuditor: StateFlow<Boolean> = _showDocAuditor.asStateFlow()

    fun setShowDocAuditor(show: Boolean) {
        _showDocAuditor.value = show
    }

    fun loadProfile(isServerConnected: Boolean) {
        if (!isServerConnected || _isLoaded.value) return
        viewModelScope.launch {
            try {
                val loaded: UserProfile = client.get(GradPathConfig.endpoint("/api/profile")).body()
                _profile.value = loaded
                _isLoaded.value = true
            } catch (_: Exception) {}
        }
    }

    fun updateProfile(newProfile: UserProfile, isServerConnected: Boolean) {
        _profile.value = newProfile
        viewModelScope.launch {
            if (isServerConnected) {
                try {
                    client.post(GradPathConfig.endpoint("/api/profile")) {
                        contentType(ContentType.Application.Json)
                        setBody(newProfile)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun runAudit(isServerConnected: Boolean) {
        if (_isAuditing.value || !isServerConnected) return
        _isAuditing.value = true
        _auditResult.value = "⚡ Antigravity এআই আপনার প্রোফাইল বিশ্লেষণ করছে..."

        viewModelScope.launch {
            try {
                client.webSocket(GradPathConfig.wsEndpoint("/ws/profile/audit")) {
                    var accumulated = ""
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val line = frame.readText().trim()
                            if (line.isEmpty()) continue
                            try {
                                val root = jsonParser.parseToJsonElement(line).jsonObject
                                val type = root["type"]?.jsonPrimitive?.contentOrNull
                                val text = root["text"]?.jsonPrimitive?.contentOrNull ?: ""
                                when (type) {
                                    "delta" -> accumulated += text
                                    "final" -> accumulated = text
                                    "thinking" -> {}
                                    else -> if (!line.startsWith("{")) accumulated += line + "\n"
                                }
                            } catch (_: Exception) {
                                if (!line.startsWith("{")) accumulated += line + "\n"
                            }
                            if (accumulated.isNotBlank()) _auditResult.value = accumulated
                        }
                    }
                }
            } catch (e: Exception) {
                _auditResult.value = "❌ অডিট সম্পন্ন করা সম্ভব হয়নি: ${e.message}"
            } finally {
                _isAuditing.value = false
            }
        }
    }
}

/**
 * -------------------------------------------------------------------------
 * 4. WatchdogsViewModel - Monitored Sources, Autonomous Sweeper & Telemetry
 * -------------------------------------------------------------------------
 */
class WatchdogsViewModel(
    private val client: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
    }
) : ViewModel() {

    private val _sources = MutableStateFlow<List<MonitoredSource>>(emptyList())
    val sources: StateFlow<List<MonitoredSource>> = _sources.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSweeping = MutableStateFlow(false)
    val isSweeping: StateFlow<Boolean> = _isSweeping.asStateFlow()

    private val _sweepStatusMessage = MutableStateFlow<String?>(null)
    val sweepStatusMessage: StateFlow<String?> = _sweepStatusMessage.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow("ALL")
    val selectedCategoryFilter: StateFlow<String> = _selectedCategoryFilter.asStateFlow()

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun setSelectedCategoryFilter(cat: String) { _selectedCategoryFilter.value = cat }
    fun setShowAddDialog(show: Boolean) { _showAddDialog.value = show }

    fun fetchSources(isServerConnected: Boolean) {
        if (!isServerConnected) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = client.get(GradPathConfig.endpoint("/api/watchdogs"))
                if (response.status == HttpStatusCode.OK) {
                    _sources.value = response.body<List<MonitoredSource>>()
                }
            } catch (e: Exception) {
                println("Failed to fetch sources: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleSource(source: MonitoredSource, isServerConnected: Boolean) {
        val updated = source.copy(isEnabled = !source.isEnabled)
        _sources.value = _sources.value.map { if (it.id == source.id) updated else it }
        if (isServerConnected) {
            viewModelScope.launch {
                try {
                    val response = client.post(GradPathConfig.endpoint("/api/watchdogs")) {
                        contentType(ContentType.Application.Json)
                        setBody(updated)
                    }
                    if (response.status == HttpStatusCode.OK) {
                        _sources.value = response.body<List<MonitoredSource>>()
                    }
                } catch (e: Exception) {
                    println("Failed to update source: ${e.message}")
                }
            }
        }
    }

    fun triggerSweep(isServerConnected: Boolean, onSweepSuccess: (() -> Unit)? = null) {
        if (!isServerConnected) return
        viewModelScope.launch {
            _isSweeping.value = true
            _sweepStatusMessage.value = "⚡ Scanning official portals with Antigravity & checking circulars..."
            try {
                val response = client.post(GradPathConfig.endpoint("/api/watchdogs/sweep"))
                if (response.status == HttpStatusCode.OK) {
                    val result = response.body<SweepResult>()
                    _sweepStatusMessage.value = "✅ Sweep Completed: ${result.sweptSourcesCount} portals scanned, ${result.newOpportunitiesFound} new circulars added to feed (Total feed: ${result.totalFeedCount})"
                    fetchSources(isServerConnected)
                    onSweepSuccess?.invoke()
                }
            } catch (e: Exception) {
                _sweepStatusMessage.value = "❌ Sweep failed: ${e.message}"
            } finally {
                _isSweeping.value = false
            }
        }
    }

    fun addSource(source: MonitoredSource, isServerConnected: Boolean) {
        viewModelScope.launch {
            try {
                val response = client.post(GradPathConfig.endpoint("/api/watchdogs")) {
                    contentType(ContentType.Application.Json)
                    setBody(source)
                }
                if (response.status == HttpStatusCode.OK) {
                    _sources.value = response.body<List<MonitoredSource>>()
                    _showAddDialog.value = false
                }
            } catch (e: Exception) {
                println("Failed to add source: ${e.message}")
            }
        }
    }
}

/**
 * -------------------------------------------------------------------------
 * 5. DiaryViewModel - University Watchlist, Outreach, Cold Email & Exports
 * -------------------------------------------------------------------------
 */
class DiaryViewModel(
    private val client: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
    }
) : ViewModel() {

    private val _diary = MutableStateFlow(ResearchDiary())
    val diary: StateFlow<ResearchDiary> = _diary.asStateFlow()

    private val _activeTab = MutableStateFlow(DiaryTab.UNIVERSITIES)
    val activeTab: StateFlow<DiaryTab> = _activeTab.asStateFlow()

    private val _selectedTierFilter = MutableStateFlow("ALL")
    val selectedTierFilter: StateFlow<String> = _selectedTierFilter.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _showAddUniDialog = MutableStateFlow(false)
    val showAddUniDialog: StateFlow<Boolean> = _showAddUniDialog.asStateFlow()

    private val _showAddOutreachDialog = MutableStateFlow(false)
    val showAddOutreachDialog: StateFlow<Boolean> = _showAddOutreachDialog.asStateFlow()

    private val _showExportModal = MutableStateFlow<String?>(null)
    val showExportModal: StateFlow<String?> = _showExportModal.asStateFlow()

    private val _exportContent = MutableStateFlow("")
    val exportContent: StateFlow<String> = _exportContent.asStateFlow()

    private val _coldEmailModalData = MutableStateFlow<ColdEmailResponse?>(null)
    val coldEmailModalData: StateFlow<ColdEmailResponse?> = _coldEmailModalData.asStateFlow()

    private val _isGeneratingEmail = MutableStateFlow(false)
    val isGeneratingEmail: StateFlow<Boolean> = _isGeneratingEmail.asStateFlow()

    fun setActiveTab(tab: DiaryTab) { _activeTab.value = tab }
    fun setSelectedTierFilter(tier: String) { _selectedTierFilter.value = tier }
    fun setShowAddUniDialog(show: Boolean) { _showAddUniDialog.value = show }
    fun setShowAddOutreachDialog(show: Boolean) { _showAddOutreachDialog.value = show }
    fun dismissToast() { _toastMessage.value = null }
    fun closeExportModal() { _showExportModal.value = null }
    fun closeColdEmailModal() { _coldEmailModalData.value = null }

    fun fetchDiary(isServerConnected: Boolean) {
        if (!isServerConnected) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val res = client.get(GradPathConfig.endpoint("/api/diary"))
                if (res.status == HttpStatusCode.OK) {
                    _diary.value = res.body<ResearchDiary>()
                }
            } catch (e: Exception) {
                println("Failed to fetch diary: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveUniversity(uni: WatchlistUniversity, isServerConnected: Boolean) {
        val updatedList = _diary.value.universities.filter { it.id != uni.id } + uni
        val updatedDiary = _diary.value.copy(universities = updatedList)
        _diary.value = updatedDiary
        _showAddUniDialog.value = false
        _toastMessage.value = "University saved to Watchlist"

        if (isServerConnected) {
            viewModelScope.launch {
                try {
                    client.post(GradPathConfig.endpoint("/api/diary/university")) {
                        contentType(ContentType.Application.Json)
                        setBody(uni)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun deleteUniversity(id: String, isServerConnected: Boolean) {
        val updated = _diary.value.copy(universities = _diary.value.universities.filter { it.id != id })
        _diary.value = updated
        _toastMessage.value = "Removed from Watchlist"

        if (isServerConnected) {
            viewModelScope.launch {
                try {
                    client.delete(GradPathConfig.endpoint("/api/diary/university/$id"))
                } catch (_: Exception) {}
            }
        }
    }

    fun saveOutreach(outreach: ProfessorOutreach, isServerConnected: Boolean) {
        val updatedList = _diary.value.outreach.filter { it.id != outreach.id } + outreach
        val updatedDiary = _diary.value.copy(outreach = updatedList)
        _diary.value = updatedDiary
        _showAddOutreachDialog.value = false
        _toastMessage.value = "Outreach log updated"

        if (isServerConnected) {
            viewModelScope.launch {
                try {
                    client.post(GradPathConfig.endpoint("/api/diary/outreach")) {
                        contentType(ContentType.Application.Json)
                        setBody(outreach)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun deleteOutreach(id: String, isServerConnected: Boolean) {
        val updated = _diary.value.copy(outreach = _diary.value.outreach.filter { it.id != id })
        _diary.value = updated
        _toastMessage.value = "Outreach entry removed"

        if (isServerConnected) {
            viewModelScope.launch {
                try {
                    client.delete(GradPathConfig.endpoint("/api/diary/outreach/$id"))
                } catch (_: Exception) {}
            }
        }
    }

    fun generateColdEmail(req: ColdEmailRequest, isServerConnected: Boolean) {
        if (!isServerConnected) {
            _toastMessage.value = "Cannot generate: Ktor server offline"
            return
        }
        viewModelScope.launch {
            _isGeneratingEmail.value = true
            try {
                val res = client.post(GradPathConfig.endpoint("/api/diary/cold-email")) {
                    contentType(ContentType.Application.Json)
                    setBody(req)
                }
                if (res.status == HttpStatusCode.OK) {
                    val body = res.body<ColdEmailResponse>()
                    _coldEmailModalData.value = body
                }
            } catch (e: Exception) {
                _toastMessage.value = "Cold email generation failed: ${e.message}"
            } finally {
                _isGeneratingEmail.value = false
            }
        }
    }

    fun exportCsv(isServerConnected: Boolean) {
        viewModelScope.launch {
            try {
                if (isServerConnected) {
                    val res = client.get(GradPathConfig.endpoint("/api/diary/export/csv"))
                    if (res.status == HttpStatusCode.OK) {
                        _exportContent.value = res.bodyAsText()
                        _showExportModal.value = "CSV"
                        return@launch
                    }
                }
                // Local fallback CSV export
                val sb = StringBuilder("University,Country,Tier,Program,Deadline,FundingStatus\n")
                _diary.value.universities.forEach {
                    sb.append("\"${it.name}\",\"${it.country}\",\"${it.tier.name}\",\"${it.programName}\",\"${it.deadline}\",\"${it.fundingStatus}\"\n")
                }
                _exportContent.value = sb.toString()
                _showExportModal.value = "CSV"
            } catch (e: Exception) {
                _toastMessage.value = "Export failed: ${e.message}"
            }
        }
    }

    fun exportMarkdown(isServerConnected: Boolean) {
        viewModelScope.launch {
            try {
                if (isServerConnected) {
                    val res = client.get(GradPathConfig.endpoint("/api/diary/export/markdown"))
                    if (res.status == HttpStatusCode.OK) {
                        _exportContent.value = res.bodyAsText()
                        _showExportModal.value = "MARKDOWN"
                        return@launch
                    }
                }
                // Local fallback Markdown export
                val sb = StringBuilder("# GradPath Research Diary & Watchlist Export\n\n## 1. University Watchlist\n\n")
                _diary.value.universities.forEach {
                    sb.append("- **${it.name}** (${it.country}) [${it.tier.label}] - ${it.programName}\n  * Deadline: ${it.deadline} | Funding: ${it.fundingStatus}\n")
                }
                _exportContent.value = sb.toString()
                _showExportModal.value = "MARKDOWN"
            } catch (e: Exception) {
                _toastMessage.value = "Export failed: ${e.message}"
            }
        }
    }
}
