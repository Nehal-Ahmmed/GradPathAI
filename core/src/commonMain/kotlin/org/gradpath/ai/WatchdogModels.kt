package org.gradpath.ai

import kotlinx.serialization.Serializable

@Serializable
enum class ResearchPillar(val label: String, val tag: String) {
    TOP_200_ADMISSION("Top 200 Admissions", "#Top200Admissions"),
    SCHOLARSHIP_FUNDING("Full Funding & Fellowships", "#ScholarshipAlert"),
    COUNTRY_CAREER_INTELLIGENCE("Country & Visa Intelligence", "#CountryCareerIntel"),
    CAREER_ACTION_PLAN("Strategic Action Roadmap", "#CareerActionPlan")
}

/**
 * Source Confidence classification:
 * Verified Official: Trustworthy academic / government domain (.edu, .gov, .ac.uk, daad.de, etc.)
 * Needs User Review: Third-party portals, blogs, or unverified announcements.
 */
@Serializable
enum class SourceConfidence(val label: String, val badgeColorHex: Long) {
    VERIFIED_OFFICIAL("Verified Official", 0xFF10B981), // Emerald Green
    NEEDS_REVIEW("Needs User Review", 0xFFF59E0B)       // Amber / Orange
}

@Serializable
enum class OpportunityCategory(val label: String, val tag: String) {
    SCHOLARSHIP("Scholarship Alert", "#ScholarshipAlert"),
    PHD_OPENING("Ph.D. Opening", "#PhDOpening"),
    DEADLINE_ALERT("Deadline Countdown", "#DeadlineCountdown"),
    POLICY_UPDATE("Policy & Visa Update", "#PolicyUpdate")
}

@Serializable
enum class FundingType(val label: String) {
    FULL_FUNDING("Full Funding (Tuition + Living Stipend)"),
    TA_RA("Graduate Assistantship (TA / RA)"),
    TUITION_WAIVER("Tuition Waiver"),
    PARTIAL("Partial Scholarship / Grant"),
    INDUSTRY_FUNDED("Industry Sponsored Research")
}

@Serializable
data class PrincipalInvestigator(
    val name: String,
    val lab: String,
    val status: String, // e.g. "Accepting Ph.D.", "Active NSF Grant"
    val recentPaperTitle: String = "",
    val profileUrl: String = ""
)

@Serializable
data class AdmissionCriteria(
    val minCgpa: String = "Not Specified",
    val grePolicy: String = "Optional / Not Required",
    val ieltsMin: String = "6.5 - 7.5",
    val requiredDossier: String = "3 LoRs, Statement of Purpose, CV"
)

@Serializable
data class MonitoredSource(
    val id: String,
    val name: String,
    val country: String,
    val portalUrl: String,
    val isMandatory: Boolean = false,
    val isEnabled: Boolean = true,
    val category: OpportunityCategory = OpportunityCategory.SCHOLARSHIP,
    val lastSweptAt: String? = null,
    val notes: String = ""
)

@Serializable
data class WatchdogOpportunity(
    val id: String,
    val pillar: ResearchPillar = ResearchPillar.TOP_200_ADMISSION,
    val sourceId: String,
    val sourceName: String,
    val title: String,
    val university: String,
    val country: String,
    val degreeTrack: String, // "Ph.D.", "Master's", "Both"
    val fundingType: FundingType,
    val fundingBreakdown: String = "Full Tuition Remission + Monthly Living Allowance",
    val deadlineDate: String,
    val summary: String,
    val officialUrl: String,
    val imageUrl: String? = null, // OpenGraph Banner or Campus Crest URL
    val confidence: SourceConfidence = SourceConfidence.VERIFIED_OFFICIAL,
    val matchScore: Int = 90,
    val matchRationale: String = "",
    val principalInvestigators: List<PrincipalInvestigator> = emptyList(),
    val admissionCriteria: AdmissionCriteria = AdmissionCriteria(),
    val tags: List<String> = emptyList(),
    val discoveredAt: String
)

@Serializable
data class SweepResult(
    val sweptSourcesCount: Int,
    val newOpportunitiesFound: Int,
    val totalFeedCount: Int,
    val timestamp: String
)

object WatchdogRegistry {
    /**
     * Determines whether a given URL qualifies as a Verified Official source
     * based on academic/governmental TLDs and trusted global databases.
     */
    fun evaluateConfidence(url: String): SourceConfidence {
        val lower = url.lowercase()
        val isOfficial = lower.contains(".edu") ||
                lower.contains(".gov") ||
                lower.contains(".ac.uk") ||
                lower.contains(".edu.au") ||
                lower.contains(".edu.cn") ||
                lower.contains("daad.de") ||
                lower.contains("nsf.gov") ||
                lower.contains("europa.eu") ||
                lower.contains("fulbrightonline.org")

        return if (isOfficial) SourceConfidence.VERIFIED_OFFICIAL else SourceConfidence.NEEDS_REVIEW
    }

    /**
     * Pre-seeded default priority portals for graduate study abroad watchdogs.
     */
    val DEFAULT_SOURCES: List<MonitoredSource> = listOf(
        MonitoredSource(
            id = "daad-germany",
            name = "DAAD Scholarship Database (Germany)",
            country = "Germany",
            portalUrl = "https://www2.daad.de/deutschland/stipendien/datenbank/en/21148-scholarship-database/",
            isMandatory = true,
            category = OpportunityCategory.SCHOLARSHIP,
            notes = "Official German Academic Exchange Service database for fully funded master's and doctoral grants."
        ),
        MonitoredSource(
            id = "us-nsf-grfp",
            name = "US National Science Foundation (NSF) Research Grants",
            country = "USA",
            portalUrl = "https://www.nsf.gov/funding/",
            isMandatory = true,
            category = OpportunityCategory.PHD_OPENING,
            notes = "Federal graduate research fellowships and lab funding openings across US universities."
        ),
        MonitoredSource(
            id = "fulbright-intl",
            name = "Fulbright Foreign Student Program",
            country = "USA",
            portalUrl = "https://foreign.fulbrightonline.org/",
            isMandatory = true,
            category = OpportunityCategory.SCHOLARSHIP,
            notes = "Prestigious bi-national fully funded scholarship program for graduate students."
        ),
        MonitoredSource(
            id = "mit-eecs",
            name = "MIT EECS Graduate Admissions & RA Openings",
            country = "USA",
            portalUrl = "https://www.eecs.mit.edu/academics/graduate-programs/admission-process/",
            isMandatory = false,
            category = OpportunityCategory.PHD_OPENING,
            notes = "Direct research assistantships and doctoral fellowships at MIT."
        ),
        MonitoredSource(
            id = "cmu-scs",
            name = "Carnegie Mellon SCS Graduate Research Portals",
            country = "USA",
            portalUrl = "https://www.cs.cmu.edu/academics/graduate",
            isMandatory = false,
            category = OpportunityCategory.PHD_OPENING,
            notes = "Ph.D. and funded Master's openings in CS, Language Technologies, and Machine Learning."
        ),
        MonitoredSource(
            id = "stanford-cs",
            name = "Stanford Computer Science Graduate Fellowships",
            country = "USA",
            portalUrl = "https://cs.stanford.edu/admissions",
            isMandatory = false,
            category = OpportunityCategory.PHD_OPENING,
            notes = "Departmental fellowships, research appointments, and teaching assistantships."
        )
    )
}
