package org.gradpath.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DiaryService(
    private val profileService: ProfileService,
    private val cloudSyncService: CloudSyncService = CloudSyncService()
) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val diaryFile = WorkspaceManager.DIARY_FILE

    /**
     * Loads the research diary from local JSON storage.
     * Seeds with default initial data if none exists.
     */
    suspend fun loadDiary(): ResearchDiary = withContext(Dispatchers.IO) {
        if (!diaryFile.exists()) {
            val defaultDiary = ResearchDiary(
                universities = emptyList(),
                outreach = emptyList()
            )
            saveDiary(defaultDiary)
            return@withContext defaultDiary
        }
        try {
            val content = diaryFile.readText()
            json.decodeFromString<ResearchDiary>(content)
        } catch (e: Exception) {
            ResearchDiary()
        }
    }

    /**
     * Saves the research diary to ~/.gradpath/diary.json
     */
    suspend fun saveDiary(diary: ResearchDiary): Unit = withContext(Dispatchers.IO) {
        val serialized = json.encodeToString(diary)
        diaryFile.writeText(serialized)
        cloudSyncService.syncDiary(diary)
    }

    /**
     * Adds or updates a university in the user's watchlist.
     */
    suspend fun addOrUpdateUniversity(uni: WatchlistUniversity): ResearchDiary = withContext(Dispatchers.IO) {
        val diary = loadDiary()
        val list = diary.universities.toMutableList()
        val index = list.indexOfFirst { it.id == uni.id }
        if (index != -1) {
            list[index] = uni
        } else {
            list.add(0, uni)
        }
        val updated = diary.copy(universities = list)
        saveDiary(updated)
        updated
    }

    /**
     * Removes a university by ID.
     */
    suspend fun removeUniversity(id: String): ResearchDiary = withContext(Dispatchers.IO) {
        val diary = loadDiary()
        val updated = diary.copy(universities = diary.universities.filter { it.id != id })
        saveDiary(updated)
        updated
    }

    /**
     * Adds or updates a professor outreach record.
     * Automatically computes 7-day follow-up reminder if sentDate is provided.
     */
    suspend fun addOrUpdateOutreach(outreach: ProfessorOutreach): ResearchDiary = withContext(Dispatchers.IO) {
        val diary = loadDiary()
        val list = diary.outreach.toMutableList()

        // Auto calculate follow-up due date (7 days later) if sentDate is present
        var itemToSave = outreach
        if (outreach.status == OutreachStatus.EMAIL_SENT && outreach.sentDate != null && outreach.followUpDueDate == null) {
            try {
                val sent = LocalDate.parse(outreach.sentDate)
                val followUp = sent.plusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE)
                itemToSave = outreach.copy(followUpDueDate = followUp)
            } catch (e: Exception) {
                // Ignore parse errors, keep original
            }
        }

        val index = list.indexOfFirst { it.id == itemToSave.id }
        if (index != -1) {
            list[index] = itemToSave
        } else {
            list.add(0, itemToSave)
        }
        val updated = diary.copy(outreach = list)
        saveDiary(updated)
        updated
    }

    /**
     * Removes an outreach entry by ID.
     */
    suspend fun removeOutreach(id: String): ResearchDiary = withContext(Dispatchers.IO) {
        val diary = loadDiary()
        val updated = diary.copy(outreach = diary.outreach.filter { it.id != id })
        saveDiary(updated)
        updated
    }

    /**
     * Generates a tailored academic cold email fusing student's profile & professor's work.
     */
    suspend fun generateColdEmail(req: ColdEmailRequest): ColdEmailResponse = withContext(Dispatchers.IO) {
        val profile = profileService.loadProfile()

        val subject = "Prospective ${profile.target.targetDegree.label.take(5)} Inquiry – ${profile.academic.major} Research in ${req.researchArea}"

        val studentBackground = """
            I recently completed my undergraduate degree in ${profile.academic.major} with a CGPA of ${profile.academic.gpa}/4.0. My research background focuses on ${profile.research.researchAreas}, and I have co-authored ${profile.research.conferencePapers} conference paper(s) and ${profile.research.journalPapers} journal article(s).
        """.trimIndent()

        val paperMention = if (req.recentPaperTitle.isNotBlank()) {
            "I recently studied your publication titled \"${req.recentPaperTitle}\" and was deeply fascinated by your methodologies and insights."
        } else {
            "I have been closely following your lab's recent contributions to ${req.researchArea}."
        }

        val specificAlignment = if (req.studentSpecificInterest.isNotBlank()) {
            "Specifically, I am interested in exploring ${req.studentSpecificInterest}, which strongly aligns with your ongoing projects."
        } else {
            "I am eager to contribute to your ongoing investigations in this domain during my graduate studies."
        }

        val body = """
Dear ${req.professorName},

I hope this email finds you well.

My name is [Your Name], and I am writing to express my strong interest in joining your research group as a ${profile.target.targetDegree.label} student for ${profile.target.targetTerm} at ${req.university}.

$paperMention $specificAlignment

$studentBackground

Given the synergy between my past research and your group's roadmap, I would love the opportunity to contribute to your lab. Are you currently accepting prospective graduate research students with funding (RA/TA) for ${profile.target.targetTerm}?

I have attached my Curriculum Vitae and academic transcripts for your review. If your schedule permits, I would be grateful for a brief 10-15 minute conversation to discuss potential research alignment.

Thank you very much for your time and consideration.

Sincerely,
[Your Name]
B.Sc. in ${profile.academic.major}
[Your Phone Number] | [Your LinkedIn/Google Scholar Profile]
        """.trimIndent()

        ColdEmailResponse(subject = subject, body = body)
    }

    /**
     * Generates a clean CSV export for Excel and Google Sheets.
     */
    fun exportToCsv(diary: ResearchDiary): String {
        val sb = StringBuilder()
        // UTF-8 BOM for Excel
        sb.append('\uFEFF')
        sb.append("Tier,University,Country,Degree Track,Program,Funding Status,Deadline,Notes\n")

        for (u in diary.universities) {
            val tier = u.tier.label.replace(",", " ")
            val name = u.name.replace(",", " ")
            val country = u.country.replace(",", " ")
            val track = u.degreeTrack.replace(",", " ")
            val prog = u.programName.replace(",", " ")
            val funding = u.fundingStatus.replace(",", " ")
            val deadline = u.deadline.replace(",", " ")
            val notes = u.notes.replace(",", " ").replace("\n", " ")

            sb.append("\"$tier\",\"$name\",\"$country\",\"$track\",\"$prog\",\"$funding\",\"$deadline\",\"$notes\"\n")
        }

        sb.append("\n\n")
        sb.append("Professor Name,University,Department,Email,Research Area,Status,Sent Date,Follow-up Due\n")
        for (o in diary.outreach) {
            val pName = o.professorName.replace(",", " ")
            val uName = o.university.replace(",", " ")
            val dept = o.department.replace(",", " ")
            val email = o.email.replace(",", " ")
            val area = o.researchArea.replace(",", " ")
            val status = o.status.label.replace(",", " ")
            val sent = o.sentDate ?: "N/A"
            val follow = o.followUpDueDate ?: "N/A"

            sb.append("\"$pName\",\"$uName\",\"$dept\",\"$email\",\"$area\",\"$status\",\"$sent\",\"$follow\"\n")
        }

        val result = sb.toString()
        try {
            val timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            File(WorkspaceManager.REPORTS_DIR, "diary_export_$timestamp.csv").writeText(result)
        } catch (e: Exception) {
            println("Report archive warning: ${e.message}")
        }
        return result
    }

    /**
     * Generates a structured Markdown report for sharing with academic mentors.
     */
    fun exportToMarkdown(diary: ResearchDiary): String {
        val sb = StringBuilder()
        sb.append("# 🎓 GradPath AI – Academic Research Diary & Watchlist Report\n\n")
        sb.append("> Generated on: ${LocalDate.now()}\n\n")

        sb.append("## 🏛️ University Shortlist (Classified by Tier)\n\n")
        sb.append("| Tier | University | Country | Track | Funding | Deadline |\n")
        sb.append("| :--- | :--- | :--- | :--- | :--- | :--- |\n")

        for (u in diary.universities) {
            val emoji = when (u.tier) {
                UniversityTier.DREAM -> "🌟 Dream"
                UniversityTier.TARGET -> "🎯 Target"
                UniversityTier.SAFE -> "🛡️ Safe"
            }
            sb.append("| $emoji | **${u.name}** | ${u.country} | ${u.degreeTrack} | ${u.fundingStatus} | ${u.deadline} |\n")
        }

        sb.append("\n\n## 📬 Professor Outreach & Cold Email Status\n\n")
        sb.append("| Professor | University | Research Focus | Status | 7-Day Follow-Up Due |\n")
        sb.append("| :--- | :--- | :--- | :--- | :--- |\n")

        for (o in diary.outreach) {
            val statusEmoji = when (o.status) {
                OutreachStatus.NOT_CONTACTED -> "⚪ Not Contacted"
                OutreachStatus.EMAIL_SENT -> "📨 Email Sent"
                OutreachStatus.FOLLOW_UP_SENT -> "⏳ Follow-Up Sent"
                OutreachStatus.INTERVIEW_SCHEDULED -> "🎉 Interview Scheduled"
                OutreachStatus.POSITIVE_REPLY -> "🟢 Positive Reply"
                OutreachStatus.REJECTED -> "🔴 Rejected"
            }
            sb.append("| **${o.professorName}** | ${o.university} | ${o.researchArea} | $statusEmoji | ${o.followUpDueDate ?: "—"} |\n")
        }

        sb.append("\n---\n*Report generated by GradPath AI Research Partner.*")
        val result = sb.toString()
        try {
            val timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            File(WorkspaceManager.REPORTS_DIR, "diary_export_$timestamp.md").writeText(result)
        } catch (e: Exception) {
            println("Report archive warning: ${e.message}")
        }
        return result
    }
}
