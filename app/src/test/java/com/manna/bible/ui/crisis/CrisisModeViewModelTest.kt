package com.manna.bible.ui.crisis

import app.cash.turbine.test
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.crisis.CrisisAiEngine
import com.manna.bible.domain.crisis.CrisisAiResult
import com.manna.bible.domain.crisis.CrisisCompanion
import com.manna.bible.domain.crisis.PersecutionCategory
import com.manna.bible.domain.crisis.PersecutionCompanion
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
            persecutionCompanion = FakePersecutionCompanion(),
            preferencesStore = FakePreferencesStore(activeId = "web"),
            translationRepository = FakeTranslationRepository(listOf(translation("web"))),
            bibleContentRepository = FakeContentRepository(),
            crisisAiEngine = FakeCrisisAiEngine()
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

    @Test
    @DisplayName("selectPersecutionCategory resolves verse texts from the active translation")
    fun selectPersecutionResolvesText() = runTest {
        val vm = CrisisModeViewModel(
            crisisCompanion = FakeCompanion(
                comfort = listOf(ReadingRef("PSA", 23, 4)),
                listen = ReadingRef("PSA", 23, 1)
            ),
            persecutionCompanion = FakePersecutionCompanion(
                verses = mapOf(
                    PersecutionCategory.FAMILY_REJECTION to listOf(ReadingRef("MAT", 10, 34))
                )
            ),
            preferencesStore = FakePreferencesStore(activeId = "web"),
            translationRepository = FakeTranslationRepository(listOf(translation("web"))),
            bibleContentRepository = FakeContentRepository(),
            crisisAiEngine = FakeCrisisAiEngine()
        )
        advanceUntilIdle()

        vm.selectPersecutionCategory(PersecutionCategory.FAMILY_REJECTION)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(PersecutionCategory.FAMILY_REJECTION, state.selectedPersecutionCategory)
        assertFalse(state.isPersecutionLoading)
        assertEquals(1, state.persecutionVerses.size)
        val verse = state.persecutionVerses.first()
        assertEquals("Matthew 10:34", verse.reference)
        assertEquals("MAT 10:34", verse.text)
        assertEquals("MAT.10.34", verse.osisRef)
        // The curated comfort list is untouched by the category selection.
        assertEquals(1, state.comfortVerses.size)
    }

    @Test
    @DisplayName("selecting the same category again clears the selection")
    fun selectSameCategoryClears() = runTest {
        val vm = CrisisModeViewModel(
            crisisCompanion = FakeCompanion(
                comfort = listOf(ReadingRef("PSA", 23, 4)),
                listen = ReadingRef("PSA", 23, 1)
            ),
            persecutionCompanion = FakePersecutionCompanion(
                verses = mapOf(
                    PersecutionCategory.FAMILY_REJECTION to listOf(ReadingRef("MAT", 10, 34))
                )
            ),
            preferencesStore = FakePreferencesStore(activeId = "web"),
            translationRepository = FakeTranslationRepository(listOf(translation("web"))),
            bibleContentRepository = FakeContentRepository(),
            crisisAiEngine = FakeCrisisAiEngine()
        )
        advanceUntilIdle()

        vm.selectPersecutionCategory(PersecutionCategory.FAMILY_REJECTION)
        advanceUntilIdle()
        assertEquals(PersecutionCategory.FAMILY_REJECTION, vm.uiState.value.selectedPersecutionCategory)

        vm.selectPersecutionCategory(PersecutionCategory.FAMILY_REJECTION)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(null, state.selectedPersecutionCategory)
        assertTrue(state.persecutionVerses.isEmpty())
    }

    // --- F-03 Crisis AI Companion -------------------------------------------

    private fun aiViewModel(engine: FakeCrisisAiEngine): CrisisModeViewModel =
        CrisisModeViewModel(
            crisisCompanion = FakeCompanion(
                comfort = listOf(ReadingRef("PSA", 23, 4)),
                listen = ReadingRef("PSA", 23, 1)
            ),
            persecutionCompanion = FakePersecutionCompanion(),
            preferencesStore = FakePreferencesStore(activeId = "web"),
            translationRepository = FakeTranslationRepository(listOf(translation("web"))),
            bibleContentRepository = FakeContentRepository(),
            crisisAiEngine = engine
        )

    @Test
    @DisplayName("submitSituation emits loading then a Success response")
    fun submitEmitsLoadingThenSuccess() = runTest {
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        val engine = FakeCrisisAiEngine(
            configured = true,
            result = CrisisAiResult.Success("Psalm 34:18", "PSA.34.18", "He is near."),
            gate = gate
        )
        val vm = aiViewModel(engine)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.aiConfigured)

        vm.updateSituation("I am grieving")
        vm.submitSituation()
        advanceUntilIdle()

        // The request is gated, so the ViewModel is observably in the loading state.
        assertTrue(vm.uiState.value.isAiLoading)

        gate.complete(Unit)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isAiLoading)
        assertEquals(
            CrisisAiResult.Success("Psalm 34:18", "PSA.34.18", "He is near."),
            state.aiResponse
        )
    }

    @Test
    @DisplayName("submitSituation is a no-op when the engine is not configured")
    fun submitNoOpWhenNotConfigured() = runTest {
        val engine = FakeCrisisAiEngine(configured = false)
        val vm = aiViewModel(engine)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.aiConfigured)

        vm.updateSituation("I am grieving")
        vm.submitSituation()
        advanceUntilIdle()

        assertEquals(0, engine.callCount)
        assertEquals(null, vm.uiState.value.aiResponse)
    }

    @Test
    @DisplayName("clearAiResponse resets the situation text and the AI response")
    fun clearResetsState() = runTest {
        val engine = FakeCrisisAiEngine(
            configured = true,
            result = CrisisAiResult.Success("Psalm 34:18", "PSA.34.18", "He is near.")
        )
        val vm = aiViewModel(engine)
        advanceUntilIdle()

        vm.updateSituation("I am grieving")
        vm.submitSituation()
        advanceUntilIdle()

        vm.clearAiResponse()
        assertEquals("", vm.uiState.value.situationText)
        assertEquals(null, vm.uiState.value.aiResponse)
    }

    @Test
    @DisplayName("an offline result is surfaced as the Offline state")
    fun offlineResultSurfaced() = runTest {
        val engine = FakeCrisisAiEngine(configured = true, result = CrisisAiResult.Offline)
        val vm = aiViewModel(engine)
        advanceUntilIdle()

        vm.updateSituation("I am grieving")
        vm.submitSituation()
        advanceUntilIdle()

        assertEquals(CrisisAiResult.Offline, vm.uiState.value.aiResponse)
    }

    @Test
    @DisplayName("the situation text is not retained in state after a successful submit (privacy)")
    fun situationTextNotRetainedAfterSubmit() = runTest {
        val engine = FakeCrisisAiEngine(
            configured = true,
            result = CrisisAiResult.Success("Psalm 34:18", "PSA.34.18", "He is near.")
        )
        val vm = aiViewModel(engine)
        advanceUntilIdle()

        vm.updateSituation("a private and painful disclosure")
        vm.submitSituation()
        advanceUntilIdle()

        assertEquals("", vm.uiState.value.situationText)
    }

    private fun translation(id: String) = Translation(
        id = id, name = "World English Bible", languageCode = "en",
        canonType = CanonType.PROTESTANT_66, hasDeuterocanon = false,
        isDownloaded = true, isBundled = true
    )

    private class FakeCrisisAiEngine(
        private val configured: Boolean = true,
        private val result: CrisisAiResult = CrisisAiResult.Offline,
        private val gate: kotlinx.coroutines.CompletableDeferred<Unit>? = null
    ) : CrisisAiEngine {
        var callCount = 0
            private set

        override val isConfigured: Boolean get() = configured

        override suspend fun respond(
            situation: String,
            languageCode: String,
            isNight: Boolean,
            denomination: com.manna.bible.domain.model.Denomination?
        ): CrisisAiResult {
            callCount++
            gate?.await()
            return result
        }
    }

    private class FakeCompanion(
        private val comfort: List<ReadingRef>,
        private val listen: ReadingRef
    ) : CrisisCompanion {
        override fun comfortVerses(): List<ReadingRef> = comfort
        override fun listenPassage(): ReadingRef = listen
    }

    private class FakePersecutionCompanion(
        private val verses: Map<PersecutionCategory, List<ReadingRef>> = emptyMap()
    ) : PersecutionCompanion {
        override fun categoriesForDenomination(
            denomination: com.manna.bible.domain.model.Denomination?
        ): List<PersecutionCategory> = PersecutionCategory.entries.toList()

        override fun versesFor(category: PersecutionCategory): List<ReadingRef> =
            verses[category] ?: emptyList()
    }

    private class FakeContentRepository : BibleContentRepository {
        private val names = mapOf("PSA" to "Psalms", "ISA" to "Isaiah", "MAT" to "Matthew")
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
