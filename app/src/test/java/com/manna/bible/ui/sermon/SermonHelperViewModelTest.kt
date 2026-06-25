package com.manna.bible.ui.sermon

import app.cash.turbine.test
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.model.CanonProfile
import com.manna.bible.domain.model.Denomination
import com.manna.bible.domain.model.SetupState
import com.manna.bible.domain.repository.SermonRepository
import com.manna.bible.domain.sermon.CongregationType
import com.manna.bible.domain.sermon.SermonAiEngine
import com.manna.bible.domain.sermon.SermonNote
import com.manna.bible.domain.sermon.SermonOutlineRequest
import com.manna.bible.domain.sermon.SermonOutlineResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** Unit tests for [SermonHelperViewModel] — list + editor over a fake repository. */
class SermonHelperViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    @DisplayName("new → edit fields → save persists and closes the editor")
    fun newSaveFlow() = runTest {
        val repo = FakeSermonRepository()
        val vm = SermonHelperViewModel(repo)

        vm.uiState.test {
            assertNull(awaitItem().draft) // list view initially

            vm.newSermon()
            assertNotNull(awaitItem().draft)

            vm.updateTitle("Light of the world")
            assertEquals("Light of the world", awaitItem().draft?.title)

            vm.updateReference("John 8:12")
            awaitItem()
            vm.updateContent("Walk in the light")
            awaitItem()

            vm.save()
            advanceUntilIdle()
            // Editor closes and the saved sermon shows up in the list.
            val saved = awaitItem()
            assertNull(saved.draft)
            assertEquals(1, saved.sermons.size)
            assertEquals("Light of the world", saved.sermons.first().title)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("saving an empty draft persists nothing")
    fun emptyDraftNotSaved() = runTest {
        val repo = FakeSermonRepository()
        val vm = SermonHelperViewModel(repo)

        vm.newSermon()
        vm.save()
        advanceUntilIdle()

        assertTrue(repo.current().isEmpty())
    }

    @Test
    @DisplayName("deleteCurrent removes an existing sermon")
    fun deleteExisting() = runTest {
        val repo = FakeSermonRepository()
        repo.seed(SermonNote(1, "Old", "Ps 23", "Shepherd", 1L, 1L))
        val vm = SermonHelperViewModel(repo)
        advanceUntilIdle()

        vm.edit(repo.current().first())
        vm.deleteCurrent()
        advanceUntilIdle()

        assertTrue(repo.current().isEmpty())
    }

    // --- F-04 AI Sermon Builder (C-06) --------------------------------------

    @Test
    @DisplayName("generateOutline inserts the AI outline into the draft content")
    fun generateOutlineInsertsContent() = runTest {
        val repo = FakeSermonRepository()
        val outline = "**Introduction** — A question from the village well..."
        val engine = FakeSermonAiEngine(configured = true, result = SermonOutlineResult.Success(outline))
        val prefs = FakePreferencesStore(denomination = Denomination.CSI, bibleLanguage = "ta")
        val vm = SermonHelperViewModel(repo, engine, prefs)

        vm.uiState.test {
            skipItems(1) // initial list state
            vm.newSermon()
            vm.updateReference("John 4:1-26")
            vm.selectCongregationType(CongregationType.YOUTH)
            vm.generateOutline()
            advanceUntilIdle()

            val state = expectMostRecentItem()
            assertEquals(outline, state.draft?.content)
            assertFalse(state.isGeneratingOutline)
            assertNull(state.outlineError)
            cancelAndIgnoreRemainingEvents()
        }

        // The request carried the setup denomination/language and the chosen congregation.
        assertEquals("John 4:1-26", engine.lastRequest?.reference)
        assertEquals(Denomination.CSI, engine.lastRequest?.denomination)
        assertEquals("ta", engine.lastRequest?.languageCode)
        assertEquals(CongregationType.YOUTH, engine.lastRequest?.congregationType)
    }

    @Test
    @DisplayName("generateOutline sets isGeneratingOutline true during the call")
    fun generateOutlineLoadingDuringCall() = runTest {
        val repo = FakeSermonRepository()
        val gate = CompletableDeferred<Unit>()
        val engine = FakeSermonAiEngine(
            configured = true,
            result = SermonOutlineResult.Success("outline"),
            suspendUntil = gate
        )
        val vm = SermonHelperViewModel(repo, engine, FakePreferencesStore())

        vm.uiState.test {
            skipItems(1)
            vm.newSermon()
            vm.updateReference("Psalm 23")
            vm.generateOutline()
            advanceUntilIdle() // runs up to the engine's suspension point

            assertTrue(expectMostRecentItem().isGeneratingOutline)

            gate.complete(Unit)
            advanceUntilIdle()

            assertFalse(expectMostRecentItem().isGeneratingOutline)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("generateOutline sets outlineError when the engine is offline")
    fun generateOutlineOfflineSetsError() = runTest {
        val repo = FakeSermonRepository()
        val engine = FakeSermonAiEngine(configured = true, result = SermonOutlineResult.Offline)
        val vm = SermonHelperViewModel(repo, engine, FakePreferencesStore())

        vm.uiState.test {
            skipItems(1)
            vm.newSermon()
            vm.updateReference("Romans 8")
            vm.generateOutline()
            advanceUntilIdle()

            val state = expectMostRecentItem()
            assertNotNull(state.outlineError)
            assertFalse(state.isGeneratingOutline)
            // The manual editor content is untouched on failure.
            assertEquals("", state.draft?.content)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("generateOutline is a no-op when the AI gate is off (engine unconfigured)")
    fun generateOutlineNoOpWhenFlagOff() = runTest {
        val repo = FakeSermonRepository()
        // An unconfigured engine is the runtime equivalent of the feature being off; the
        // compile-time SERMON_AI_BUILDER flag is gated at the UI layer (see C-07).
        val engine = FakeSermonAiEngine(configured = false, result = SermonOutlineResult.Success("outline"))
        val vm = SermonHelperViewModel(repo, engine, FakePreferencesStore())

        vm.uiState.test {
            skipItems(1)
            vm.newSermon()
            vm.updateReference("John 1:1")
            vm.generateOutline()
            advanceUntilIdle()

            val state = expectMostRecentItem()
            assertEquals("", state.draft?.content)
            assertFalse(state.isGeneratingOutline)
            cancelAndIgnoreRemainingEvents()
        }

        assertFalse(engine.called, "engine must not be called when the gate is closed")
    }

    @Test
    @DisplayName("canGenerateOutline is false when the reference is blank")
    fun canGenerateOutlineFalseWhenReferenceBlank() = runTest {
        val repo = FakeSermonRepository()
        val engine = FakeSermonAiEngine(configured = true, result = SermonOutlineResult.Success("outline"))
        val vm = SermonHelperViewModel(repo, engine, FakePreferencesStore())

        vm.uiState.test {
            skipItems(1)

            vm.newSermon()
            advanceUntilIdle()
            assertFalse(expectMostRecentItem().canGenerateOutline, "blank reference must not allow generation")

            vm.updateReference("John 3:16")
            advanceUntilIdle()
            assertTrue(expectMostRecentItem().canGenerateOutline, "non-blank reference with a configured engine allows it")

            vm.updateReference("   ")
            advanceUntilIdle()
            assertFalse(expectMostRecentItem().canGenerateOutline, "whitespace-only reference is treated as blank")

            cancelAndIgnoreRemainingEvents()
        }
    }

    /** Canned [SermonAiEngine] for the AI Sermon Builder tests — no Mockito. */
    private class FakeSermonAiEngine(
        private val configured: Boolean,
        private val result: SermonOutlineResult,
        private val suspendUntil: CompletableDeferred<Unit>? = null
    ) : SermonAiEngine {
        var called = false
            private set
        var lastRequest: SermonOutlineRequest? = null
            private set

        override val isConfigured: Boolean get() = configured

        override suspend fun generateOutline(request: SermonOutlineRequest): SermonOutlineResult {
            called = true
            lastRequest = request
            suspendUntil?.await()
            return result
        }
    }

    /** Minimal [PreferencesStore] emitting a fixed [SetupState]; mutators are no-ops. */
    private class FakePreferencesStore(
        denomination: Denomination? = null,
        bibleLanguage: String? = null
    ) : PreferencesStore {
        private val state = SetupState(
            denomination = denomination,
            canonType = null,
            uiLanguage = null,
            bibleLanguage = bibleLanguage,
            numberingScheme = null,
            namingConventionId = null,
            bibleTranslationId = null,
            lectionaryId = null
        )

        override val setupState: Flow<SetupState> = flowOf(state)
        override val lastReadPosition: Flow<String?> = flowOf(null)
        override suspend fun saveSetup(state: SetupState) {}
        override suspend fun setSetupCompleted(value: Boolean) {}
        override suspend fun updateDenomination(profile: CanonProfile) {}
        override suspend fun setShowDeuterocanonical(value: Boolean) {}
        override suspend fun setActiveTranslation(translationId: String) {}
        override suspend fun setLastReadPosition(ref: String) {}
    }

    private class FakeSermonRepository : SermonRepository {
        private val rows = MutableStateFlow<List<SermonNote>>(emptyList())
        private var nextId = 1L

        fun current() = rows.value
        fun seed(note: SermonNote) {
            rows.value = rows.value + note
            nextId = maxOf(nextId, note.id + 1)
        }

        override fun observeSermons(): Flow<List<SermonNote>> =
            rows.map { list -> list.sortedByDescending { it.updatedAt } }

        override suspend fun get(id: Long): SermonNote? = rows.value.firstOrNull { it.id == id }

        override suspend fun save(id: Long, title: String, reference: String, content: String): Long {
            val resolvedTitle = title.trim().ifEmpty { reference.trim().ifEmpty { "Untitled sermon" } }
            if (title.isBlank() && reference.isBlank() && content.isBlank()) return -1
            return if (id <= 0L) {
                val newId = nextId++
                rows.value = rows.value + SermonNote(newId, resolvedTitle, reference.trim(), content.trim(), 1L, 1L)
                newId
            } else {
                rows.value = rows.value.map {
                    if (it.id == id) it.copy(title = resolvedTitle, reference = reference.trim(), content = content.trim()) else it
                }
                id
            }
        }

        override suspend fun delete(id: Long) {
            rows.value = rows.value.filterNot { it.id == id }
        }
    }
}
