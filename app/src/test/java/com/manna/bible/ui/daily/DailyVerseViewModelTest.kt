package com.manna.bible.ui.daily

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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Unit tests for [DailyVerseViewModel] — resolves today's verse text and the
 * "read in context" reference, and surfaces an unavailable state when no content
 * exists.
 */
class DailyVerseViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val ref = ReadingRef("JHN", 3, 16)

    @Test
    @DisplayName("loads today's verse text and reference from the active translation")
    fun loadsVerse() = runTest {
        val vm = DailyVerseViewModel(
            dailyVerseProvider = FakeDailyVerseProvider(ref),
            preferencesStore = FakePreferencesStore(activeId = "web"),
            translationRepository = FakeTranslationRepository(listOf(translation("web"))),
            bibleContentRepository = FakeBibleContentRepository(
                bookName = "John",
                verses = listOf(VerseLine(15, "..."), VerseLine(16, "For God so loved the world"))
            )
        )
        vm.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertFalse(state.unavailable)
            assertEquals("John 3:16", state.reference)
            assertEquals("For God so loved the world", state.verseText)
            assertEquals("JHN.3.16", state.osisRef)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("with no translation available, surfaces the unavailable state")
    fun unavailableWhenNoTranslation() = runTest {
        val vm = DailyVerseViewModel(
            dailyVerseProvider = FakeDailyVerseProvider(ref),
            preferencesStore = FakePreferencesStore(activeId = null),
            translationRepository = FakeTranslationRepository(emptyList()),
            bibleContentRepository = FakeBibleContentRepository(bookName = "John", verses = emptyList())
        )
        vm.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertTrue(state.unavailable)
            assertEquals(null, state.verseText)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun translation(id: String) = Translation(
        id = id,
        name = "World English Bible",
        languageCode = "en",
        canonType = CanonType.PROTESTANT_66,
        hasDeuterocanon = false,
        isDownloaded = true,
        isBundled = true
    )

    private class FakeDailyVerseProvider(private val ref: ReadingRef) : DailyVerseProvider {
        override fun verseForDate(date: LocalDate): ReadingRef = ref
    }

    private class FakeBibleContentRepository(
        private val bookName: String,
        private val verses: List<VerseLine>
    ) : BibleContentRepository {
        override fun books(translationId: String): Flow<List<BookSummary>> =
            MutableStateFlow(listOf(BookSummary("JHN", bookName, Testament.NEW, 0, 21)))

        override suspend fun chapter(translationId: String, osisId: String, chapter: Int): ChapterContent? =
            if (verses.isEmpty()) null else ChapterContent(translationId, osisId, chapter, verses)

        override suspend fun hasContent(translationId: String): Boolean = verses.isNotEmpty()

        override suspend fun search(translationId: String, query: String): List<VerseMatch> = emptyList()
    }

    private class FakePreferencesStore(private val activeId: String?) : PreferencesStore {
        private val state = MutableStateFlow(
            SetupState(
                denomination = Denomination.PROTESTANT_OTHER,
                canonType = CanonType.PROTESTANT_66,
                uiLanguage = "en",
                bibleLanguage = "en",
                numberingScheme = NumberingScheme.MASORETIC,
                namingConventionId = null,
                bibleTranslationId = activeId,
                lectionaryId = null,
                showDeuterocanonical = false,
                setupCompleted = true
            )
        )
        override val setupState: Flow<SetupState> = state
        override val lastReadPosition: Flow<String?> = MutableStateFlow(null)
        override suspend fun saveSetup(state: SetupState) {}
        override suspend fun setSetupCompleted(value: Boolean) {}
        override suspend fun updateDenomination(profile: CanonProfile) {}
        override suspend fun setShowDeuterocanonical(value: Boolean) {}
        override suspend fun setActiveTranslation(translationId: String) {}
        override suspend fun setLastReadPosition(ref: String) {}
    }

    private class FakeTranslationRepository(
        private val catalog: List<Translation>
    ) : TranslationRepository {
        override fun catalog(): Flow<List<Translation>> = flowOf(catalog)
        override suspend fun refreshCatalog() {}
        override suspend fun download(id: String): DownloadResult = DownloadResult.Success
        override suspend fun markPendingDownload(id: String) {}
        override suspend fun retryPendingDownloads() {}
    }
}
