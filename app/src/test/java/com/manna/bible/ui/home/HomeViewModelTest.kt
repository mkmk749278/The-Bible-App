package com.manna.bible.ui.home

import app.cash.turbine.test
import com.manna.bible.data.preferences.PreferencesStore
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit tests for [HomeViewModel] — resolves the persisted reading position into a
 * "Continue reading" label for the Home and Listen surfaces.
 */
class HomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    @DisplayName("resolves the last position into a book-name + chapter label")
    fun resolvesContinueLabel() = runTest {
        val vm = HomeViewModel(
            preferencesStore = FakePreferencesStore(activeId = "web", position = "JHN.3.16"),
            translationRepository = FakeTranslationRepository(listOf(translation("web"))),
            bibleContentRepository = FakeBibleContentRepository(bookName = "John")
        )
        vm.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertEquals("John 3", state.continueLabel)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("with no stored position, the label is null (Start reading)")
    fun noPositionMeansNoLabel() = runTest {
        val vm = HomeViewModel(
            preferencesStore = FakePreferencesStore(activeId = "web", position = null),
            translationRepository = FakeTranslationRepository(listOf(translation("web"))),
            bibleContentRepository = FakeBibleContentRepository(bookName = "John")
        )
        vm.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertNull(state.continueLabel)
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

    private class FakeBibleContentRepository(
        private val bookName: String
    ) : BibleContentRepository {
        override fun books(translationId: String): Flow<List<BookSummary>> =
            MutableStateFlow(listOf(BookSummary("JHN", bookName, Testament.NEW, 0, 21)))

        override suspend fun chapter(translationId: String, osisId: String, chapter: Int): ChapterContent? = null

        override suspend fun hasContent(translationId: String): Boolean = true

        override suspend fun search(translationId: String, query: String): List<VerseMatch> = emptyList()
    }

    private class FakePreferencesStore(
        private val activeId: String?,
        private val position: String?
    ) : PreferencesStore {
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
        override val lastReadPosition: Flow<String?> = MutableStateFlow(position)
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
        override suspend fun retryPendingDownloads() {}
        override suspend fun markPendingDownload(id: String) {}
        override suspend fun download(id: String): DownloadResult = DownloadResult.Success
    }
}
