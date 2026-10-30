package org.gradpath.ai

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class AutomationSidecar(
    val builtin: String = "schedule",
    val args: List<String>,
    val restart_policy: String = "always",
    val display_name: String,
    val description: String,
    val agent_permissions: AgentPermissions
)

@Serializable
data class AgentPermissions(
    val workspace_uris: List<String>,
    val access_grants: List<String>
)

object AutomationManager {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val sidecarsBaseDir = File(System.getProperty("user.home"), ".gemini/config/sidecars").apply {
        if (!exists()) mkdirs()
    }
    val tasksDir = File("D:\\University Research\\tasks").apply {
        if (!exists()) mkdirs()
    }
    val dbDir = File("D:\\University Research\\db").apply {
        if (!exists()) mkdirs()
    }

    /**
     * Seeds the 2 Daily Focused Automations:
     * 1. Morning Sweeper (8:00 AM) — Admissions, Deadlines, Fellowships, Top 200 Universities
     * 2. Evening Advisor (7:00 PM) — Country Tech Intelligence, Visa, Career Milestones
     */
    fun initializeDefaultAutomations(profile: UserProfile, diary: ResearchDiary) {
        syncAllContextFiles(profile, diary)

        val targetCountriesStr = if (profile.target.targetCountries.isNotEmpty()) {
            profile.target.targetCountries.joinToString(", ")
        } else {
            "USA, Germany, Canada"
        }
        val majorStr = if (profile.academic.major.isNotBlank()) profile.academic.major else "Computer Science & Engineering"

        // 1. Morning Sweeper (8:00 AM Asia/Dhaka)
        registerAutomation(
            id = "gradpath-admissions-funding-sweeper",
            displayName = "GradPath Admissions & Funding Sweeper",
            description = "Daily 8:00 AM sweep of Top 200 Universities & official portals for Masters/PhD circulars, funding breakdowns, and PIs.",
            cron = "CRON_TZ=Asia/Dhaka 0 8 * * *",
            prompt = """
                You are GradPath AI's Elite Academic Opportunity Sweeper.
                1. Read the student's profile and Top 200 scope from 'D:\University Research\tasks\admissions_context.md'.
                2. Research active admissions circulars, fully-funded PhD openings, and graduate assistantships (RA/TA) in the student's target countries ($targetCountriesStr) for their major ($majorStr).
                3. For each opportunity discovered, extract:
                   - Accurate funding breakdown (Tuition waiver amount + Monthly stipend).
                   - Hiring Principal Investigator (PI), Lab name, and recent 2025/2026 paper citation.
                   - Admission criteria (Min CGPA cutoff, GRE waiver policy, IELTS requirement).
                   - Official circular URL and high-res campus/crest OpenGraph image URL.
                   - Calculate candidate alignment match score (%) based on the student's profile.
                4. Prepend newly discovered opportunities to 'D:\University Research\db\feed.json' as WatchdogOpportunity JSON.
                5. Execute: powershell.exe -ExecutionPolicy Bypass -File "D:\University Research\tasks\sync_to_firestore.ps1" -Collection "feed" -FilePath "D:\University Research\db\feed.json".
            """.trimIndent()
        )

        // 2. Evening Advisor (7:00 PM Asia/Dhaka)
        registerAutomation(
            id = "gradpath-career-policy-advisor",
            displayName = "GradPath Country Career & Policy Advisor",
            description = "Daily 7:00 PM intelligence sweep for tech job market trends, post-study work visa updates, and next-step milestones.",
            cron = "CRON_TZ=Asia/Dhaka 0 19 * * *",
            prompt = """
                You are GradPath AI's Global Career & Policy Advisor.
                1. Read the student's background and country goals from 'D:\University Research\tasks\career_policy_context.md'.
                2. Conduct live web research on immigration & career news affecting graduate students in target countries ($targetCountriesStr):
                   - Post-Study Work Visas (e.g., US STEM OPT, Germany Chancenkarte / EU Blue Card, Canada PGWP updates).
                   - Tech job market hiring trends and average starting graduate salaries in the student's field ($majorStr).
                   - Strategic 3-Month Profile Boost Milestones (next steps: preprints, GRE quant goals, professors to target).
                3. Structure findings as WatchdogOpportunity objects tagged with #CountryCareerIntel and #CareerActionPlan.
                4. Append to 'D:\University Research\db\feed.json' and commit:
                   powershell.exe -ExecutionPolicy Bypass -File "D:\University Research\tasks\sync_to_firestore.ps1" -Collection "feed" -FilePath "D:\University Research\db\feed.json".
            """.trimIndent()
        )
    }

    fun syncAllContextFiles(profile: UserProfile, diary: ResearchDiary) {
        val major = if (profile.academic.major.isNotBlank()) profile.academic.major else "Computer Science & Engineering (CSE)"
        val institution = if (profile.academic.institution.isNotBlank()) profile.academic.institution else "Undergraduate Institution"
        val gpa = if (profile.academic.gpa > 0.0) "${profile.academic.gpa}" else "3.65"
        val targetDegree = profile.target.targetDegree.label
        val targetCountries = if (profile.target.targetCountries.isNotEmpty()) profile.target.targetCountries.joinToString(", ") else "USA, Germany, Canada"
        val researchAreas = if (profile.research.researchAreas.isNotBlank()) profile.research.researchAreas else "Machine Learning, Distributed Systems, AI"

        val watchlistScope = if (diary.universities.isNotEmpty()) {
            diary.universities.joinToString("\n") { "  * [${it.tier.name}] ${it.name} (${it.country}) - ${it.programName}" }
        } else {
            """
              * [Tier 1 - Dream] Carnegie Mellon University (USA) - School of Computer Science (Ph.D. / MSCS)
              * [Tier 1 - Dream] Technical University of Munich (TUM) (Germany) - Informatics & AI
              * [Tier 2 - Target] University of Waterloo (Canada) - David R. Cheriton School of Computer Science
              * [Tier 2 - Target] University of Massachusetts Amherst (USA) - Manning College of Information and Computer Sciences
              * [Tier 3 - Safety] Saarland University / Max Planck Institute (Germany) - Computer Science
            """.trimIndent()
        }

        // 1. Admissions context
        File(tasksDir, "admissions_context.md").writeText("""
            # 🎓 Student Admissions & Top 200 Research Context
            - **Current Status:** $major Undergraduate ($institution)
            - **Current CGPA:** $gpa / 4.00
            - **Target Degree:** $targetDegree (${profile.target.targetTerm})
            - **Target Countries:** $targetCountries
            - **Full Funding Mandatory:** ${profile.target.fullFundingMandatory}
            - **Standardized Tests:** IELTS ${if (profile.tests.ieltsOverall > 0) profile.tests.ieltsOverall else "7.5 (Target)"}, GRE Quant ${if (profile.tests.greQuant > 0) profile.tests.greQuant else "165+"}, Verbal ${if (profile.tests.greVerbal > 0) profile.tests.greVerbal else "155+"}
            - **Research & Publications:** ${profile.research.conferencePapers} Conference, ${profile.research.journalPapers} Journal. Areas: $researchAreas
            - **Target Scope:** Top 200 QS/THE Universities for $major + Watchlist Universities:
            $watchlistScope
        """.trimIndent())

        // 2. Career policy context
        File(tasksDir, "career_policy_context.md").writeText("""
            # 🌐 Country Career & Policy Intelligence Context
            - **Target Major:** $major
            - **Target Countries to Monitor:** $targetCountries
            - **Immigration & Policy Focus:**
              * USA: STEM OPT Extension (24-month extension, 36 months total), H-1B lottery cap exemptions.
              * Germany: Chancenkarte (Opportunity Card), EU Blue Card minimum salary thresholds for STEM shortage occupations.
              * Canada: Post-Graduation Work Permit (PGWP) eligibility rules, Express Entry STEM draws.
            - **Career Objective:** High-impact global career, Post-study work permit, High return-on-investment graduate placement.
            - **Current Profile Gaps:** SOP Ready: ${profile.prep.sopReady}, CV Ready: ${profile.prep.cvReady}, LORs: ${profile.prep.lorCount}/3.
        """.trimIndent())
    }

    fun registerAutomation(id: String, displayName: String, description: String, cron: String, prompt: String) {
        val sidecarDir = File(sidecarsBaseDir, id).apply { if (!exists()) mkdirs() }
        val sidecarFile = File(sidecarDir, "sidecar.json")

        val sidecar = AutomationSidecar(
            builtin = "schedule",
            args = listOf(
                cron,
                "agentapi",
                "new-conversation",
                "--",
                prompt
            ),
            restart_policy = "always",
            display_name = displayName,
            description = description,
            agent_permissions = AgentPermissions(
                workspace_uris = listOf("file:///D:/University%20Research"),
                access_grants = listOf(
                    "read_file(D:\\University Research)",
                    "write_file(D:\\University Research)",
                    "command(powershell.exe)"
                )
            )
        )
        sidecarFile.writeText(json.encodeToString(sidecar))
    }

    fun triggerManualSweep(): Boolean {
        return try {
            val scriptFile = File(tasksDir, "sync_to_firestore.ps1")
            if (!scriptFile.exists()) return false

            val processBuilder = ProcessBuilder(
                "powershell.exe",
                "-ExecutionPolicy", "Bypass",
                "-File", scriptFile.absolutePath,
                "-Collection", "feed",
                "-FilePath", File(dbDir, "feed.json").absolutePath
            )
            processBuilder.redirectErrorStream(true)
            val process = processBuilder.start()
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            println("Manual sweep trigger error: ${e.message}")
            false
        }
    }
}
