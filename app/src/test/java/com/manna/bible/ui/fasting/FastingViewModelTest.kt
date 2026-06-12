package com.manna.bible.ui.fasting

import app.cash.turbine.test
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.fasting.DefaultFastingPlans
import com.manna.bible.domain.model.CanonProfile
import com.manna.bible.domain.model.CanonType
import com.manna.bible.domain.model.Denomination
import com.manna.bible.domain.model.NumberingScheme
import com.manna.bible.domain.model.SetupState
import com.manna.bible.domain.model.Testament
import com.manna.bible.domain.repository.BibleContentRepository
import com.manna.bible.domain.repository.BookSummary
import com.manna.bible.domain.repository.ChapterContent
import com.manna.bible.domain.repository.DownloadResult
import com.manna.bible.domain.repository.TranslationRepository
import com.manna.bible.domain.repository.VerseLine
import com.manna.bible.domain.repository.VerseMatch
import com.manna.bible.domain.translation.Translation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** Unit tests for [FastingViewModel] — choosing, starting, and ending a fast. */
class FastingViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeEach fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterEach fun tearDown() { Dispatchers.resetMain() }

    private fun vm(store: PreferencesStore) = FastingViewModel(
        fastingPlans = DefaultFastingPlans(),
        preferencesStore = store,
        translationRepository = FakeTranslationRepository(listOf(translation())),
        bibleContentRepository = FakeContentRepository()
    )

    @Test
    @DisplayName("offers plans when no fast is active; start then end toggles the active state")
    fun startAndEnd() = runTest {
        val model = vm(FakePreferencesStore())
        model.uiState.test {
            advanceUntilIdle()
            var state = expectMostRecentItem()
            assertFalse(state.active)
            assertTrue(state.plans.isNotEmpty())

            model.start("sunrise_sunset")
            advanceUntilIdle()
            state = expectMostRecentItem()
            assertTrue(state.active)
            assertEquals("sunrise_sunset", state.activePlanId)
            assertNotNull(state.focusText)
            assertTrue(state.remainingMillis > 0)

            model.end()
            advanceUntilIdle()
            assertFalse(expectMostRecentItem().active)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun translation() = Translation(
        id = "web", name = "WEB", languageCode = "en", canonType = CanonType.PROTESTANT_66,
        hasDeuterocanon = false, isDownloaded = true, isBundled = true
    )

    private class FakeContentRepository : BibleContentRepository {
        override fun books(translationId: String): Flow<List<BookSummary>> =
            MutableStateFlow(listOf(BookSummary("MAT", "Matthew", Testament.NEW, 0, 28)))
        override suspend fun chapter(translationId: String, osisId: String, chapter: Int): ChapterContent =
            ChapterContent(translationId, osisId, chapter, (1..30).map { VerseLine(it, "$osisId $chapter:$it") })
        override suspend fun hasContent(translationId: String): Boolean = true
        override suspend fun search(translationId: String, query: String): List<VerseMatch> = emptyList()
    }

    private class FakePreferencesStore : PreferencesStore {
        private val start = MutableStateFlow(-1L)
        private val planId = MutableStateFlow("")
        override val fastStartMillis: Flow<Long> = start
        override val fastPlanId: Flow<String> = planId
        override suspend fun setActiveFast(startMillis: Long, planId: String) {
            start.value = startMillis
            this.planId.value = planId
        }

        override val setupState: Flow<SetupState> = MutableStateFlow(
            SetupState(
                denomination = Denomination.PROTESTANT_OTHER, canonType = CanonType.PROTESTANT_66,
                uiLanguage = "en", bibleLanguage = "en", numberingScheme = NumberingScheme.MASORETIC,
                namingConventionId = null, bibleTranslationId = "web", lectionaryId = null,
                showDeuterocanonical = false, setupCompleted = true
            )
        )
        override val lastReadPosition: Flow<String?> = MutableStateFlow(null)
        override suspend fun saveSetup(state: SetupState) {}
        override suspend fun setSetupCompleted(value: Boolean) {}
        override suspend fun updateDenomination(profile: CanonProfile) {}
        override suspend fun setShowDeuterocanonical(value: Boolean) {}
        override suspend fun setActiveTranslation(translationId: String) {}
        override suspend fun setLastReadPosition(ref: String) {}
    }

    private class FakeTranslationRepository(private val catalog: List<Translation>) : TranslationRepository {
        override fun catalog(): Flow<List<Translation>> = flowOf(catalog)
        override suspend fun refreshCatalog() {}
        override suspend fun retryPendingDownloads() {}
        override suspend fun markPendingDownload(id: String) {}
        override suspend fun download(id: String): DownloadResult = DownloadResult.Success
    }
}
