package com.manna.bible.ui.card

import app.cash.turbine.test
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.daily.DailyVerseProvider
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

/** Unit tests for [ScriptureCardViewModel] — resolving the day's verse text. */
class ScriptureCardViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeEach fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterEach fun tearDown() { Dispatchers.resetMain() }

    @Test
    @DisplayName("resolves the day's verse text and reference for the card")
    fun resolvesVerse() = runTest {
        val vm = ScriptureCardViewModel(
            dailyVerseProvider = object : DailyVerseProvider {
                override fun verseForDate(date: LocalDate) = ReadingRef("JHN", 3, 16)
            },
            preferencesStore = FakePreferencesStore(),
            translationRepository = FakeTranslationRepository(listOf(translation())),
            bibleContentRepository = FakeContentRepository()
        )
        vm.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertEquals("John 3:16", state.reference)
            assertEquals("JHN 3:16", state.verseText)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun translation() = Translation(
        id = "web", name = "WEB", languageCode = "en", canonType = CanonType.PROTESTANT_66,
        hasDeuterocanon = false, isDownloaded = true, isBundled = true
    )

    private class FakeContentRepository : BibleContentRepository {
        override fun books(translationId: String): Flow<List<BookSummary>> =
            MutableStateFlow(listOf(BookSummary("JHN", "John", Testament.NEW, 0, 21)))
        override suspend fun chapter(translationId: String, osisId: String, chapter: Int): ChapterContent =
            ChapterContent(translationId, osisId, chapter, (1..30).map { VerseLine(it, "$osisId $chapter:$it") })
        override suspend fun hasContent(translationId: String): Boolean = true
        override suspend fun search(translationId: String, query: String): List<VerseMatch> = emptyList()
    }

    private class FakePreferencesStore : PreferencesStore {
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
