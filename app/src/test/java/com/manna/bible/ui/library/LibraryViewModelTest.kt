package com.manna.bible.ui.library

import app.cash.turbine.test
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.canon.CanonEngine
import com.manna.bible.domain.model.Bookmark
import com.manna.bible.domain.model.CanonBook
import com.manna.bible.domain.model.CanonProfile
import com.manna.bible.domain.model.CanonType
import com.manna.bible.domain.model.Denomination
import com.manna.bible.domain.model.Highlight
import com.manna.bible.domain.model.Note
import com.manna.bible.domain.model.NumberingScheme
import com.manna.bible.domain.model.SetupState
import com.manna.bible.domain.model.Testament
import com.manna.bible.domain.repository.AnnotationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** Unit tests for [LibraryViewModel] — the saved highlights / bookmarks / notes browser. */
class LibraryViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeEach fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterEach fun tearDown() { Dispatchers.resetMain() }

    @Test
    @DisplayName("collects highlights, bookmarks, and notes across the active canon")
    fun collectsAllTypes() = runTest {
        val annotations = FakeAnnotationRepository()
        annotations.addHighlight("GEN.1.1", 0xFFFFFF00.toInt())
        annotations.addBookmark("PSA.23.1", "Shepherd")
        annotations.addNote("GEN.1.2", "Creation")
        val vm = LibraryViewModel(FakePreferencesStore(), FakeCanonEngine(), annotations)

        vm.uiState.test {
            // Skip loading/intermediate emissions until all three are present.
            var state = awaitItem()
            while (state.items.size < 3) state = awaitItem()
            assertEquals(3, state.items.size)
            assertTrue(state.items.any { it.type == LibraryFilter.HIGHLIGHTS && it.displayRef == "GEN 1:1" })
            assertTrue(state.items.any { it.type == LibraryFilter.BOOKMARKS && it.detail == "Shepherd" })
            assertTrue(state.items.any { it.type == LibraryFilter.NOTES && it.detail == "Creation" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("filter narrows the visible items to a single type")
    fun filterNarrows() = runTest {
        val annotations = FakeAnnotationRepository()
        annotations.addHighlight("GEN.1.1", 0xFFFFFF00.toInt())
        annotations.addBookmark("PSA.23.1", null)
        val vm = LibraryViewModel(FakePreferencesStore(), FakeCanonEngine(), annotations)

        vm.uiState.test {
            var state = awaitItem()
            while (state.items.size < 2) state = awaitItem()
            vm.setFilter(LibraryFilter.BOOKMARKS)
            advanceUntilIdle()
            state = awaitItem()
            assertEquals(LibraryFilter.BOOKMARKS, state.filter)
            assertEquals(1, state.visibleItems.size)
            assertEquals(LibraryFilter.BOOKMARKS, state.visibleItems.first().type)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("annotations on books outside the active canon are hidden, not shown")
    fun hidesOutsideCanon() = runTest {
        val annotations = FakeAnnotationRepository()
        annotations.addHighlight("GEN.1.1", 0)
        annotations.addBookmark("TOB.3.1", null) // Tobit — not in the Protestant canon fake
        val vm = LibraryViewModel(FakePreferencesStore(), FakeCanonEngine(), annotations)

        vm.uiState.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()
            // Only the GEN highlight is visible; the Tobit bookmark stays hidden.
            assertEquals(1, state.items.size)
            assertEquals("GEN 1:1", state.items.first().displayRef)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("delete removes the item from the repository")
    fun deleteRemoves() = runTest {
        val annotations = FakeAnnotationRepository()
        val id = annotations.addHighlight("GEN.1.1", 0)
        val vm = LibraryViewModel(FakePreferencesStore(), FakeCanonEngine(), annotations)

        vm.uiState.test {
            var state = awaitItem()
            while (state.items.isEmpty()) state = awaitItem()
            vm.delete(state.items.first { it.id == id && it.type == LibraryFilter.HIGHLIGHTS })
            advanceUntilIdle()
            state = awaitItem()
            assertTrue(state.items.none { it.type == LibraryFilter.HIGHLIGHTS })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("formatRef renders OSIS references readably")
    fun formatsRef() {
        assertEquals("GEN 1:1", LibraryViewModel.formatRef("GEN.1.1"))
        assertEquals("PSA 23", LibraryViewModel.formatRef("PSA.23"))
        assertEquals("WEIRD", LibraryViewModel.formatRef("WEIRD"))
    }

    private class FakeCanonEngine : CanonEngine {
        override fun canonTypeFor(denomination: Denomination): CanonType = CanonType.PROTESTANT_66
        override suspend fun profileFor(denomination: Denomination, bibleLanguage: String): CanonProfile =
            CanonProfile(
                denomination = denomination,
                canonType = CanonType.PROTESTANT_66,
                books = listOf(
                    CanonBook("GEN", Testament.OLD, orderIndex = 0, isDeuterocanonical = false),
                    CanonBook("PSA", Testament.OLD, orderIndex = 1, isDeuterocanonical = false),
                ),
                numberingScheme = NumberingScheme.MASORETIC,
                namingConventionId = null,
                suggestedTranslationId = null,
                lectionaryId = null,
            )
    }

    private class FakePreferencesStore : PreferencesStore {
        override val setupState: Flow<SetupState> = MutableStateFlow(
            SetupState(
                denomination = Denomination.PROTESTANT_OTHER,
                canonType = CanonType.PROTESTANT_66,
                uiLanguage = "en",
                bibleLanguage = "en",
                numberingScheme = NumberingScheme.MASORETIC,
                namingConventionId = null,
                bibleTranslationId = "web",
                lectionaryId = null,
                showDeuterocanonical = false,
                setupCompleted = true,
            ),
        )
        override val lastReadPosition: Flow<String?> = MutableStateFlow(null)
        override suspend fun saveSetup(state: SetupState) {}
        override suspend fun setSetupCompleted(value: Boolean) {}
        override suspend fun updateDenomination(profile: CanonProfile) {}
        override suspend fun setShowDeuterocanonical(value: Boolean) {}
        override suspend fun setActiveTranslation(translationId: String) {}
        override suspend fun setLastReadPosition(ref: String) {}
    }

    private class FakeAnnotationRepository : AnnotationRepository {
        private val highlights = MutableStateFlow<List<Highlight>>(emptyList())
        private val bookmarks = MutableStateFlow<List<Bookmark>>(emptyList())
        private val notes = MutableStateFlow<List<Note>>(emptyList())
        private var nextId = 1L

        private fun bookOf(ref: String?): String? = ref?.substringBefore('.')?.ifBlank { null }

        override suspend fun allAnnotatedBookIds(): Set<String> =
            (highlights.value.map { it.verseRef } + bookmarks.value.map { it.verseRef })
                .mapNotNull { bookOf(it) }.toSet()

        override suspend fun annotatedBookIdsOutside(visibleBookIds: Set<String>): Set<String> =
            allAnnotatedBookIds() - visibleBookIds

        override fun visibleHighlights(visibleBookIds: Set<String>): Flow<List<Highlight>> =
            highlights.map { list -> list.filter { bookOf(it.verseRef) in visibleBookIds } }

        override fun visibleBookmarks(visibleBookIds: Set<String>): Flow<List<Bookmark>> =
            bookmarks.map { list -> list.filter { bookOf(it.verseRef) in visibleBookIds } }

        override fun visibleNotes(visibleBookIds: Set<String>): Flow<List<Note>> =
            notes.map { list -> list.filter { bookOf(it.verseRef) in visibleBookIds } }

        override suspend fun addHighlight(verseRef: String, colorArgb: Int): Long {
            val id = nextId++
            highlights.value = highlights.value + Highlight(id, verseRef, colorArgb, createdAt = id)
            return id
        }

        override suspend fun addBookmark(verseRef: String, label: String?): Long {
            val id = nextId++
            bookmarks.value = bookmarks.value + Bookmark(id, verseRef, label, createdAt = id)
            return id
        }

        override suspend fun addNote(verseRef: String, content: String): Long {
            val id = nextId++
            notes.value = notes.value + Note(id, verseRef, null, content, createdAt = id, updatedAt = id)
            return id
        }

        override suspend fun deleteHighlight(id: Long) {
            highlights.value = highlights.value.filterNot { it.id == id }
        }

        override suspend fun deleteBookmark(id: Long) {
            bookmarks.value = bookmarks.value.filterNot { it.id == id }
        }

        override suspend fun deleteNote(id: Long) {
            notes.value = notes.value.filterNot { it.id == id }
        }
    }
}
