package org.gradpath.ai

import kotlinx.serialization.Serializable

@Serializable
enum class UniversityTier(val label: String, val badgeColorHex: Long) {
    DREAM("Dream (High Reach)", 0xFF8B5CF6),    // Purple / Violet
    TARGET("Target (Competitive)", 0xFF38BDF8),  // Cyan / Sky
    SAFE("Safe (Safety Net)", 0xFF10B981)       // Emerald Green
}

@Serializable
enum class OutreachStatus(val label: String, val badgeColorHex: Long) {
    NOT_CONTACTED("Not Contacted", 0xFF6B7280),            // Gray
    EMAIL_SENT("Email Sent", 0xFF3B82F6),                  // Blue
    FOLLOW_UP_SENT("Follow-up Sent", 0xFFF59E0B),          // Amber
    INTERVIEW_SCHEDULED("Interview Scheduled", 0xFF10B981),// Emerald
    POSITIVE_REPLY("Positive Reply", 0xFF14B8A6),          // Teal
    REJECTED("No Position / Rejected", 0xFFEF4444)         // Red
}

@Serializable
data class WatchlistUniversity(
    val id: String,
    val name: String,
    val country: String,
    val tier: UniversityTier = UniversityTier.TARGET,
    val programName: String = "M.S. / Ph.D. in Computer Science",
    val degreeTrack: String = "Ph.D.",
    val fundingStatus: String = "Full Funding / TA-RA Required",
    val deadline: String = "December 15, 2026",
    val officialUrl: String = "",
    val notes: String = "",
    val addedAt: String = ""
)

@Serializable
data class ProfessorOutreach(
    val id: String,
    val professorName: String,
    val university: String,
    val department: String = "Computer Science & Engineering",
    val email: String,
    val researchArea: String,
    val status: OutreachStatus = OutreachStatus.NOT_CONTACTED,
    val sentDate: String? = null,
    val followUpDueDate: String? = null,
    val paperAlignment: String = "",
    val draftEmail: String = ""
)

@Serializable
data class ResearchDiary(
    val universities: List<WatchlistUniversity> = emptyList(),
    val outreach: List<ProfessorOutreach> = emptyList()
)

@Serializable
data class ColdEmailRequest(
    val professorName: String,
    val university: String,
    val researchArea: String,
    val recentPaperTitle: String = "",
    val studentSpecificInterest: String = ""
)

@Serializable
data class ColdEmailResponse(
    val subject: String,
    val body: String
)

object DiaryDefaults {
    val SEED_UNIVERSITIES = listOf(
        WatchlistUniversity(
            id = "cmu-scs",
            name = "Carnegie Mellon University (CMU)",
            country = "USA",
            tier = UniversityTier.DREAM,
            programName = "Ph.D. in Language Technologies (LTI)",
            degreeTrack = "Ph.D.",
            fundingStatus = "100% Guaranteed RA/TA + Full Tuition",
            deadline = "December 10, 2026",
            officialUrl = "https://www.cs.cmu.edu/academics/graduate",
            notes = "Top choice for NLP, Multimodal AI, and Speech research."
        ),
        WatchlistUniversity(
            id = "tum-germany",
            name = "Technical University of Munich (TUM)",
            country = "Germany",
            tier = UniversityTier.TARGET,
            programName = "M.Sc. in Informatics & AI",
            degreeTrack = "Master's",
            fundingStatus = "DAAD Scholarship & HiWi Research Student",
            deadline = "May 31, 2026",
            officialUrl = "https://www.tum.de/en/studies/degree-programs",
            notes = "Zero tuition fee, strong robotics and machine learning research labs."
        ),
        WatchlistUniversity(
            id = "arizona-state",
            name = "Arizona State University (ASU)",
            country = "USA",
            tier = UniversityTier.SAFE,
            programName = "Ph.D. in Computer Science",
            degreeTrack = "Ph.D.",
            fundingStatus = "Dean's Fellowship + Graduate Assistantship",
            deadline = "January 15, 2027",
            officialUrl = "https://scai.engineering.asu.edu/graduate-computer-science/",
            notes = "High acceptance likelihood, strong faculty in AI reasoning and cloud computing."
        )
    )

    val SEED_OUTREACH = listOf(
        ProfessorOutreach(
            id = "prof-graham-neubig",
            professorName = "Prof. Graham Neubig",
            university = "Carnegie Mellon University (CMU)",
            department = "Language Technologies Institute",
            email = "gneubig@cs.cmu.edu",
            researchArea = "Natural Language Processing, Code Generation, LLM Reasoning",
            status = OutreachStatus.EMAIL_SENT,
            sentDate = "2026-10-22",
            followUpDueDate = "2026-10-29",
            paperAlignment = "Recent paper on Code Agent Evaluation and Multilingual Transfer."
        ),
        ProfessorOutreach(
            id = "prof-daniel-cremers",
            professorName = "Prof. Daniel Cremers",
            university = "Technical University of Munich (TUM)",
            department = "Computer Vision & Artificial Intelligence Group",
            email = "cremers@tum.de",
            researchArea = "Computer Vision, Autonomous Systems, 3D Reconstruction",
            status = OutreachStatus.NOT_CONTACTED,
            paperAlignment = "Direct visual odometry and real-time deep representation learning."
        )
    )
}
