package org.gradpath.ai

import androidx.compose.ui.graphics.Color
import org.gradpath.ai.viewmodel.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SharedCommonTest {

    @Test
    fun testGradPathThemeColorTokens() {
        // Verify Dark mode token contracts
        val dark = GradPathDarkPalette
        assertTrue(dark.isDark)
        assertEquals(Color(0xFF121417), dark.canvas)

        // Verify Light mode token contracts
        val light = GradPathLightPalette
        assertFalse(light.isDark)
        assertEquals(Color(0xFFFFFFFF), light.canvas)
    }

    @Test
    fun testFeedViewModelFilteringAndBookmarks() {
        val vm = FeedViewModel()

        // Test search query
        assertEquals("", vm.searchQuery.value)
        vm.setSearchQuery("Stanford")
        assertEquals("Stanford", vm.searchQuery.value)

        // Test tags
        assertEquals("ALL", vm.selectedTagFilter.value)
        vm.setSelectedTagFilter("PHD")
        assertEquals("PHD", vm.selectedTagFilter.value)

        // Test degree filter
        assertEquals("ALL", vm.selectedDegreeFilter.value)
        vm.setSelectedDegreeFilter("MASTERS")
        assertEquals("MASTERS", vm.selectedDegreeFilter.value)

        // Test bookmarking
        val oppId = "mit-csail-1"
        assertFalse(vm.bookmarkedIds.value.contains(oppId))
        vm.toggleBookmark(oppId)
        assertTrue(vm.bookmarkedIds.value.contains(oppId))
        assertEquals("Bookmark saved to Dossier", vm.toastMessage.value)

        vm.dismissToast()
        assertEquals(null, vm.toastMessage.value)

        vm.toggleBookmark(oppId)
        assertFalse(vm.bookmarkedIds.value.contains(oppId))
        assertEquals("Bookmark removed", vm.toastMessage.value)
    }

    @Test
    fun testResearchStudioViewModel() {
        val vm = ResearchStudioViewModel()

        // Initial assistant greeting
        assertTrue(vm.messages.value.isNotEmpty())
        assertFalse(vm.messages.value.first().isUser)

        // Input text
        vm.updateInputText("Evaluate my PhD profile")
        assertEquals("Evaluate my PhD profile", vm.inputText.value)

        // Thinking toggle
        val initialId = vm.messages.value.first().id
        assertFalse(vm.messages.value.first().isThinkingExpanded)
        vm.toggleThinking(initialId)
        assertTrue(vm.messages.value.first().isThinkingExpanded)
        vm.toggleThinking(initialId)
        assertFalse(vm.messages.value.first().isThinkingExpanded)
    }

    @Test
    fun testProfileViewModelAndDualTrackScoring() {
        val vm = ProfileViewModel()
        val defaultProfile = vm.profile.value

        // Ph.D. Profile Score with publication
        val phdProfile = defaultProfile.copy(
            academic = AcademicProfile(gpa = 3.9, major = "Computer Science"),
            research = ResearchExperience(conferencePapers = 2, journalPapers = 1, citations = 15),
            tests = TestScores(ieltsOverall = 8.0, greQuant = 168),
            prep = ReadinessPrep(sopReady = true, cvReady = true, lorCount = 3),
            target = TargetPreferences(targetDegree = TargetDegree.PHD)
        )
        val phdScore = ProfileScoringEngine.calculateScore(phdProfile)
        assertTrue(phdScore.totalScore >= 70, "PhD candidate with 3 papers should score high, got ${phdScore.totalScore}")
        assertEquals(TargetDegree.PHD, phdScore.degreeTrack)

        // Master's Track Score with high CGPA
        val msProfile = defaultProfile.copy(
            academic = AcademicProfile(gpa = 3.95, major = "Software Engineering"),
            tests = TestScores(ieltsOverall = 8.5, greQuant = 170),
            prep = ReadinessPrep(sopReady = true, cvReady = true, lorCount = 3),
            target = TargetPreferences(targetDegree = TargetDegree.MASTERS)
        )
        val msScore = ProfileScoringEngine.calculateScore(msProfile)
        assertTrue(msScore.totalScore >= 70, "MS candidate with 3.95 CGPA and 170 Quant should score high, got ${msScore.totalScore}")
        assertEquals(TargetDegree.MASTERS, msScore.degreeTrack)
    }

    @Test
    fun testWatchdogsViewModel() {
        val vm = WatchdogsViewModel()
        assertEquals(emptyList(), vm.sources.value)

        // Check defaults registry
        val defaults = WatchdogRegistry.DEFAULT_SOURCES
        assertEquals(6, defaults.size)

        val daad = defaults.first { it.id == "daad-germany" }
        assertEquals("Germany", daad.country)
        assertEquals(SourceConfidence.VERIFIED_OFFICIAL, WatchdogRegistry.evaluateConfidence(daad.portalUrl))

        // Query updates
        vm.setSearchQuery("EECS")
        assertEquals("EECS", vm.searchQuery.value)

        vm.setSelectedCategoryFilter("SCHOLARSHIP")
        assertEquals("SCHOLARSHIP", vm.selectedCategoryFilter.value)
    }

    @Test
    fun testDiaryViewModel() {
        val vm = DiaryViewModel()

        // Tab switching
        assertEquals(DiaryTab.UNIVERSITIES, vm.activeTab.value)
        vm.setActiveTab(DiaryTab.PROFESSOR_OUTREACH)
        assertEquals(DiaryTab.PROFESSOR_OUTREACH, vm.activeTab.value)

        // Saving university
        val testUni = WatchlistUniversity(
            id = "mit-eecs",
            name = "Massachusetts Institute of Technology",
            country = "USA",
            tier = UniversityTier.DREAM,
            deadline = "December 15, 2026"
        )
        vm.saveUniversity(testUni, isServerConnected = false)
        assertTrue(vm.diary.value.universities.any { it.id == "mit-eecs" })
        assertEquals("University saved to Watchlist", vm.toastMessage.value)

        // Deleting university
        vm.deleteUniversity("mit-eecs", isServerConnected = false)
        assertFalse(vm.diary.value.universities.any { it.id == "mit-eecs" })

        // Saving professor outreach
        val testProf = ProfessorOutreach(
            id = "prof-test",
            professorName = "Dr. Leslie Kaelbling",
            university = "MIT CSAIL",
            email = "lpk@csail.mit.edu",
            researchArea = "Robotics & Reinforcement Learning"
        )
        vm.saveOutreach(testProf, isServerConnected = false)
        assertTrue(vm.diary.value.outreach.any { it.id == "prof-test" })

        vm.deleteOutreach("prof-test", isServerConnected = false)
        assertFalse(vm.diary.value.outreach.any { it.id == "prof-test" })
    }

    @Test
    fun testConfidenceEvaluation() {
        assertEquals(SourceConfidence.VERIFIED_OFFICIAL, WatchdogRegistry.evaluateConfidence("https://www.csail.mit.edu/admissions"))
        assertEquals(SourceConfidence.VERIFIED_OFFICIAL, WatchdogRegistry.evaluateConfidence("https://www.nsf.gov/funding/pgm_summ.jsp"))
        assertEquals(SourceConfidence.VERIFIED_OFFICIAL, WatchdogRegistry.evaluateConfidence("https://www2.daad.de/deutschland/stipendien/datenbank/en/"))
        assertEquals(SourceConfidence.VERIFIED_OFFICIAL, WatchdogRegistry.evaluateConfidence("https://www.cam.ac.uk/postgraduate/courses"))
        assertEquals(SourceConfidence.NEEDS_REVIEW, WatchdogRegistry.evaluateConfidence("https://random-scholarship-blog.com/post/123"))
    }

    @Test
    fun testProfileScoringEngineBoundaries() {
        val emptyProfile = UserProfile(
            academic = AcademicProfile(gpa = 0.0, major = "", institution = ""),
            research = ResearchExperience(),
            tests = TestScores(),
            prep = ReadinessPrep(),
            target = TargetPreferences(targetDegree = TargetDegree.PHD)
        )
        val minScore = ProfileScoringEngine.calculateScore(emptyProfile)
        assertTrue(minScore.totalScore in 0..15, "Empty profile should have minimal score, got ${minScore.totalScore}")
        assertEquals(TargetDegree.PHD, minScore.degreeTrack)

        // Switching track to MASTERS adapts weights
        val msEmptyScore = ProfileScoringEngine.calculateScore(emptyProfile.copy(target = TargetPreferences(targetDegree = TargetDegree.MASTERS)))
        assertEquals(TargetDegree.MASTERS, msEmptyScore.degreeTrack)
        assertEquals(35, msEmptyScore.academicMax)
        assertEquals(25, msEmptyScore.testMax)
        assertEquals(20, msEmptyScore.researchMax)
    }
}