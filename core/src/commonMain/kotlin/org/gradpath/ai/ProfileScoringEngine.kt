package org.gradpath.ai

import kotlin.math.min
import kotlin.math.roundToInt

object ProfileScoringEngine {

    /**
     * Dual-gauge deterministic scoring formula.
     * Adapts weights based on Direct Ph.D. vs. Funded Master's track.
     */
    fun calculateScore(profile: UserProfile): ScoreBreakdown {
        val isPhd = profile.target.targetDegree == TargetDegree.PHD

        val academicMax = if (isPhd) 25 else 35
        val testMax = if (isPhd) 15 else 25
        val researchMax = if (isPhd) 40 else 20
        val prepMax = 20

        // 1. Academic Score calculation
        val gpaRatio = (profile.academic.gpa / 4.0).coerceIn(0.0, 1.0)
        val academicScore = (gpaRatio * academicMax).roundToInt()

        // 2. Standardized Tests Score calculation
        val ieltsScore = when {
            profile.tests.ieltsOverall >= 8.0 -> 1.0
            profile.tests.ieltsOverall >= 7.5 -> 0.9
            profile.tests.ieltsOverall >= 7.0 -> 0.75
            profile.tests.ieltsOverall >= 6.5 -> 0.5
            else -> 0.2
        }
        val greQuantRatio = ((profile.tests.greQuant - 140).coerceAtLeast(0) / 30.0).coerceIn(0.0, 1.0)
        val testRatio = (ieltsScore * 0.4) + (greQuantRatio * 0.6)
        val testScore = (testRatio * testMax).roundToInt()

        // 3. Research & Publications Score calculation
        var rawResearch = (profile.research.conferencePapers * 12) + 
                          (profile.research.journalPapers * 18) + 
                          (min(profile.research.citations, 20) * 1)
        if (profile.research.hasPreprint) rawResearch += 5
        val researchScore = min(rawResearch, researchMax)

        // 4. Statement & LOR Prep Score
        var rawPrep = 0
        if (profile.prep.sopReady) rawPrep += 8
        if (profile.prep.cvReady) rawPrep += 4
        rawPrep += min(profile.prep.lorCount * 3, 8)
        val prepScore = min(rawPrep, prepMax)

        val total = (academicScore + testScore + researchScore + prepScore).coerceIn(0, 100)

        return ScoreBreakdown(
            totalScore = total,
            academicScore = academicScore,
            academicMax = academicMax,
            testScore = testScore,
            testMax = testMax,
            researchScore = researchScore,
            researchMax = researchMax,
            prepScore = prepScore,
            prepMax = prepMax,
            degreeTrack = profile.target.targetDegree
        )
    }
}
