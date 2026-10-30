package org.gradpath.ai

import kotlinx.serialization.Serializable

@Serializable
enum class DocumentType(val label: String) {
    SOP("Statement of Purpose (SOP)"),
    CV("Curriculum Vitae (CV) / Academic Resume")
}

@Serializable
data class DocumentAuditRequest(
    val documentType: DocumentType = DocumentType.SOP,
    val content: String,
    val targetProgram: String = "Ph.D. in Computer Science",
    val targetProfessorOrLab: String = ""
)

@Serializable
data class DocumentAuditScore(
    val clarityScore: Int = 85,
    val alignmentScore: Int = 80,
    val researchDepthScore: Int = 90,
    val overallReadiness: Int = 85
)

@Serializable
data class DocumentAuditResult(
    val documentType: DocumentType,
    val scores: DocumentAuditScore,
    val keyStrengths: List<String>,
    val criticalWeaknesses: List<String>,
    val lineByLineCritiqueMarkdown: String,
    val recommendedRevisionDraft: String
)
