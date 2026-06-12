package com.manna.bible.ui.crisis

import app.cash.turbine.test
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.crisis.CrisisCompanion
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

/**
 * Unit tests for [CrisisModeViewModel] — resolves comfort verse text and the listen ref.
 */
class CrisisModeViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeEach fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterEach fun tearDown() { Dispatchers.resetMain() }

    @Test
    @DisplayName("loads comfort verse text and the listen passage reference")
    fun loadsComfort() = runTest {
        val vm = CrisisModeViewModel(
            crisisCompanion = FakeCompanion(
                comfort = listOf(ReadingRef("PSA", 23, 4), ReadingRef("ISA", 43, 2)),
                listen = ReadingRef("PSA", 23, 1)
            ),
            preferencesStore = FakePreferencesStore(activeId = "web"),
            translationRepository = FakeTranslationRepository(listOf(translation("web"))),
            bibleContentRepository = FakeContentRepository()
        )
        vm.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertEquals("PSA.23.1", state.listenRef)
            assertEquals(2, state.comfortVerses.size)
            val first = state.comfortVerses.first()
            assertEquals("Psalms 23:4", first.reference)
            assertEquals("PSA 23:4", first.text)
            assertEquals("PSA.23.4", first.osisRef)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun translation(id: String) = Translation(
        id = id, name = "World English Bible", languageCode = "en",
        canonType = CanonType.PROTESTANT_66, hasDeuterocanon = false,
        isDownloaded = true, isBundled = true
    )

    private class FakeCompanion(
        private val comfort: List<ReadingRef>,
        private val listen: ReadingRef
    ) : CrisisCompanion {
        override fun comfortVerses(): List<ReadingRef> = comfort
        override fun listenPassage(): ReadingRef = listen
    }

    private class FakeContentRepository : BibleContentRepository {
        private val names = mapOf("PSA" to "Psalms", "ISA" to "Isaiah")
        override fun books(translationId: String): Flow<List<BookSummary>> =
            MutableStateFlow(names.entries.mapIndexed { i, e ->
                BookSummary(e.key, e.value, Testament.OLD, i, 150)
            })

        override suspend fun chapter(translationId: String, osisId: String, chapter: Int): ChapterContent =
            ChapterContent(
                translationId, osisId, chapter,
                (1..200).map { VerseLine(it, "$osisId $chapter:$it") }
            )

        override suspend fun hasContent(translationId: String): Boolean = true
        override suspend fun search(translationId: String, query: String): List<VerseMatch> = emptyList()
    }

    private class FakePreferencesStore(private val activeId: String?) : PreferencesStore {
        private val state = MutableStateFlow(
            SetupState(
                denomination = Denomination.PROTESTANT_OTHER,
                canonType = CanonType.PROTESTANT_66,
                uiLanguage = "en", bibleLanguage = "en",
                numberingScheme = NumberingScheme.MASORETIC,
                namingConventionId = null, bibleTranslationId = activeId,
                lectionaryId = null, showDeuterocanonical = false, setupCompleted = true
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

    private class FakeTranslationRepository(private val catalog: List<Translation>) : TranslationRepository {
        override fun catalog(): Flow<List<Translation>> = flowOf(catalog)
        override suspend fun refreshCatalog() {}
        override suspend fun retryPendingDownloads() {}
        override suspend fun markPendingDownload(id: String) {}
        override suspend fun download(id: String): DownloadResult = DownloadResult.Success
    }
}
