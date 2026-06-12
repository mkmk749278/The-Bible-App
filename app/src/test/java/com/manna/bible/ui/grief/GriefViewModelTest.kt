package com.manna.bible.ui.grief

import app.cash.turbine.test
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.grief.GriefJourney
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
import com.manna.bible.domain.usecase.ReadingRef
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

/** Unit tests for [GriefViewModel] — start/begin, date-paced day, and navigation. */
class GriefViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeEach fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterEach fun tearDown() { Dispatchers.resetMain() }

    private fun vm(store: PreferencesStore) = GriefViewModel(
        griefJourney = FakeJourney,
        preferencesStore = store,
        translationRepository = FakeTranslationRepository(listOf(translation())),
        bibleContentRepository = FakeContentRepository()
    )

    @Test
    @DisplayName("shows the not-started state until begun")
    fun notStarted() = runTest {
        val model = vm(FakePreferencesStore(griefStart = -1L))
        model.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertFalse(state.started)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("unlocks day-by-day from the start date and supports navigation")
    fun datePacedNavigation() = runTest {
        // Started yesterday → today is day 2.
        val store = FakePreferencesStore(griefStart = LocalDate.now().toEpochDay() - 1)
        val model = vm(store)
        model.uiState.test {
            advanceUntilIdle()
            var state = expectMostRecentItem()
            assertTrue(state.started)
            assertEquals(2, state.currentDay)
            assertEquals(2, state.viewedDay)
            assertEquals("Psalms 2:1", state.reference)
            assertTrue(state.canGoPrevious)
            assertFalse(state.canGoNext, "can't go past today's unlocked day")

            model.previous()
            advanceUntilIdle()
            state = expectMostRecentItem()
            assertEquals(1, state.viewedDay)
            assertEquals("Psalms 1:1", state.reference)
            assertTrue(state.canGoNext)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("begin() starts the journey at day 1")
    fun beginStartsJourney() = runTest {
        val store = FakePreferencesStore(griefStart = -1L)
        val model = vm(store)
        model.uiState.test {
            advanceUntilIdle()
            assertFalse(expectMostRecentItem().started)

            model.begin()
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertTrue(state.started)
            assertEquals(1, state.currentDay)
            assertEquals(1, state.viewedDay)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private object FakeJourney : GriefJourney {
        private val verses = listOf(ReadingRef("PSA", 1, 1), ReadingRef("PSA", 2, 1), ReadingRef("PSA", 3, 1))
        override val dayCount: Int = verses.size
        override fun verseFor(day: Int): ReadingRef? = verses.getOrNull(day - 1)
    }

    private fun translation() = Translation(
        id = "web", name = "WEB", languageCode = "en", canonType = CanonType.PROTESTANT_66,
        hasDeuterocanon = false, isDownloaded = true, isBundled = true
    )

    private class FakeContentRepository : BibleContentRepository {
        override fun books(translationId: String): Flow<List<BookSummary>> =
            MutableStateFlow(listOf(BookSummary("PSA", "Psalms", Testament.OLD, 0, 150)))
        override suspend fun chapter(translationId: String, osisId: String, chapter: Int): ChapterContent =
            ChapterContent(translationId, osisId, chapter, (1..5).map { VerseLine(it, "$osisId $chapter:$it") })
        override suspend fun hasContent(translationId: String): Boolean = true
        override suspend fun search(translationId: String, query: String): List<VerseMatch> = emptyList()
    }

    private class FakePreferencesStore(griefStart: Long) : PreferencesStore {
        private val grief = MutableStateFlow(griefStart)
        override val griefStartEpochDay: Flow<Long> = grief
        override suspend fun setGriefStartEpochDay(value: Long) { grief.value = value }

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
