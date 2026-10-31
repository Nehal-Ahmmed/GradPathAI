package org.gradpath.ai

import kotlinx.serialization.Serializable

enum class TargetDegree(val label: String) {
    PHD("Direct Ph.D. Track"),
    MASTERS("Funded Master's (M.S.) Track")
}

@Serializable
data class AcademicProfile(
    val gpa: Double = 0.0,
    val major: String = "",
    val institution: String = "",
    val graduationYear: Int = 2026
)

@Serializable
data class TestScores(
    val ieltsOverall: Double = 0.0,
    val greQuant: Int = 0,
    val greVerbal: Int = 0,
    val greAwa: Double = 0.0
)

@Serializable
data class ResearchExperience(
    val conferencePapers: Int = 0,
    val journalPapers: Int = 0,
    val citations: Int = 0,
    val researchAreas: String = "",
    val hasPreprint: Boolean = false
)

@Serializable
data class ReadinessPrep(
    val sopReady: Boolean = false,
    val lorCount: Int = 0,
    val cvReady: Boolean = false
)

@Serializable
data class TargetPreferences(
    val targetDegree: TargetDegree = TargetDegree.PHD,
    val targetTerm: String = "Fall 2026",
    val targetCountries: List<String> = listOf("USA", "Germany", "Canada"),
    val fullFundingMandatory: Boolean = true
)

@Serializable
data class ScoreBreakdown(
    val totalScore: Int,
    val academicScore: Int,
    val academicMax: Int,
    val testScore: Int,
    val testMax: Int,
    val researchScore: Int,
    val researchMax: Int,
    val prepScore: Int,
    val prepMax: Int,
    val degreeTrack: TargetDegree
)

@Serializable
data class UserProfile(
    val fullName: String = "",
    val academic: AcademicProfile = AcademicProfile(),
    val tests: TestScores = TestScores(),
    val research: ResearchExperience = ResearchExperience(),
    val prep: ReadinessPrep = ReadinessPrep(),
    val target: TargetPreferences = TargetPreferences()
)
