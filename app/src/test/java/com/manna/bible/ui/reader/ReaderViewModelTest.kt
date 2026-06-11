package com.manna.bible.ui.reader

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
import com.manna.bible.domain.audio.TtsReader
import com.manna.bible.domain.audio.TtsState
import com.manna.bible.domain.audio.TtsStatus
import com.manna.bible.domain.audio.TtsVerse
import com.manna.bible.domain.download.DownloadManager
import com.manna.bible.domain.download.DownloadOutcome
import com.manna.bible.domain.download.DownloadProgress
import com.manna.bible.domain.model.Testament
import com.manna.bible.domain.repository.AnnotationRepository
import com.manna.bible.domain.repository.BibleContentRepository
import com.manna.bible.domain.repository.BookSummary
import com.manna.bible.domain.repository.ChapterContent
import com.manna.bible.domain.repository.VerseLine
import com.manna.bible.domain.repository.VerseMatch
import com.manna.bible.domain.repository.DownloadResult
import com.manna.bible.domain.repository.TranslationRepository
import com.manna.bible.domain.translation.Translation
import com.manna.bible.domain.usecase.GetChapterUseCase
import com.manna.bible.domain.usecase.NavigateChapterUseCase
import com.manna.bible.domain.usecase.RestoreReadingPositionUseCase
import com.manna.bible.domain.usecase.SaveReadingPositionUseCase
import com.manna.bible.domain.usecase.SetActiveTranslationUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit tests for [ReaderViewModel] covering chapter open/navigation, reading-position
 * persistence, Septuagint Psalm display numbering, and verse annotation flags
 * (Requirements 2, 3, 7, 8).
 *
 * Runs on the JVM (JUnit 5) with hand-rolled fakes — no emulator or DI container.
 */
class ReaderViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        denomination: Denomination = Denomination.PROTESTANT_OTHER,
        content: FakeBibleContentRepository = protestantContent(),
        prefs: FakePreferencesStore = FakePreferencesStore(),
        annotations: FakeAnnotationRepository = FakeAnnotationRepository(),
        ttsReader: FakeTtsReader = FakeTtsReader()
    ): ReaderViewModel {
        val getChapter = GetChapterUseCase(content)
        return ReaderViewModel(
            getChapterUseCase = getChapter,
            navigateChapterUseCase = NavigateChapterUseCase(),
            setActiveTranslationUseCase = SetActiveTranslationUseCase(prefs),
            restoreReadingPositionUseCase = RestoreReadingPositionUseCase(prefs),
            saveReadingPositionUseCase = SaveReadingPositionUseCase(prefs),
            canonEngine = FakeCanonEngine(denomination),
            preferencesStore = prefs,
            annotationRepository = annotations,
            bibleContentRepository = content,
            translationRepository = FakeTranslationRepository(),
            downloadManager = FakeDownloadManager(),
            ttsReader = ttsReader
        )
    }

    @Test
    @DisplayName("init opens the restored position and persists it (Req 2, 7.1, 7.3)")
    fun initOpensRestoredPosition() = runTest {
        val prefs = FakePreferencesStore()
        val vm = viewModel(prefs = prefs)

        vm.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals("GEN", state.osisId)
            assertEquals(1, state.chapter)
            assertEquals(1, state.displayedChapterNumber)
            assertEquals("Genesis", state.bookName)
            assertEquals(listOf("In the beginning", "And the earth"), state.verses.map { it.text })
            assertFalse(state.isLoading)
            assertFalse(state.canPrev) // first book, first chapter (Req 3.3)
            assertTrue(state.canNext)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals("GEN.1.1", prefs.lastSaved)
    }

    @Test
    @DisplayName("nextChapter advances within the book (Req 3.1)")
    fun nextChapterAdvances() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            advanceUntilIdle()
            vm.nextChapter()
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals("GEN", state.osisId)
            assertEquals(2, state.chapter)
            // GEN's last chapter, but PSA follows in canon order, so next is still offered.
            assertTrue(state.canNext)
            assertTrue(state.canPrev)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("Septuagint profile displays Psalm 23 as 22 (Req 2.3)")
    fun septuagintPsalmDisplay() = runTest {
        val vm = viewModel(
            denomination = Denomination.CATHOLIC,
            content = psalmContent(),
            prefs = FakePreferencesStore(lastRead = "PSA.23.1", denomination = Denomination.CATHOLIC)
        )
        vm.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals("PSA", state.osisId)
            assertEquals(23, state.chapter) // stored numbering stays canonical
            assertEquals(22, state.displayedChapterNumber) // Septuagint display
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("toggleHighlight flags the verse and toggling again clears it (Req 8.2, 8.3, 8.4)")
    fun toggleHighlightUpdatesFlag() = runTest {
        val annotations = FakeAnnotationRepository()
        val vm = viewModel(annotations = annotations)

        vm.uiState.test {
            advanceUntilIdle()
            assertFalse(expectMostRecentItem().verses.first { it.verse == 1 }.hasHighlight)

            vm.toggleHighlight(1)
            advanceUntilIdle()
            assertTrue(expectMostRecentItem().verses.first { it.verse == 1 }.hasHighlight)

            vm.toggleHighlight(1)
            advanceUntilIdle()
            assertFalse(expectMostRecentItem().verses.first { it.verse == 1 }.hasHighlight)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("onAudioPlayPause starts read-aloud for the open chapter and reflects engine state (Req 9.1, 9.2)")
    fun audioPlayStartsAndReflectsState() = runTest {
        val tts = FakeTtsReader()
        val vm = viewModel(ttsReader = tts)

        vm.uiState.test {
            advanceUntilIdle()
            assertFalse(expectMostRecentItem().isAudioPlaying)

            vm.onAudioPlayPause()
            advanceUntilIdle()

            // The whole chapter is queued, starting at the first verse.
            assertEquals(listOf("In the beginning", "And the earth"), tts.lastVerses.map { it.text })
            val state = expectMostRecentItem()
            assertTrue(state.isAudioPlaying)
            assertEquals(1, state.audioVerse)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("continuous play advances to the next chapter at chapter end (Req 9.7)")
    fun continuousPlayAdvancesChapter() = runTest {
        val tts = FakeTtsReader()
        val prefs = FakePreferencesStore(continuousPlay = true)
        val vm = viewModel(prefs = prefs, ttsReader = tts)

        vm.uiState.test {
            advanceUntilIdle()
            vm.onAudioPlayPause()
            advanceUntilIdle()
            assertEquals(1, expectMostRecentItem().chapter)

            tts.completeChapter()
            advanceUntilIdle()

            // Reader moved to GEN 2 and restarted audio there.
            val state = expectMostRecentItem()
            assertEquals(2, state.chapter)
            assertEquals(listOf("Thus the heavens"), tts.lastVerses.map { it.text })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("an RTL Bible language renders the reader right-to-left (Req 14.4)")
    fun rtlLanguageSetsRtlState() = runTest {
        val vm = viewModel(prefs = FakePreferencesStore(bibleLanguage = "ur"))
        vm.uiState.test {
            advanceUntilIdle()
            assertTrue(expectMostRecentItem().isRtl)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("an LTR Bible language keeps the reader left-to-right (Req 14.4)")
    fun ltrLanguageKeepsLtrState() = runTest {
        val vm = viewModel(prefs = FakePreferencesStore(bibleLanguage = "en"))
        vm.uiState.test {
            advanceUntilIdle()
            assertFalse(expectMostRecentItem().isRtl)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- fakes ---------------------------------------------------------------

    private fun protestantContent() = FakeBibleContentRepository(
        books = listOf(
            BookSummary("GEN", "Genesis", Testament.OLD, orderIndex = 0, chapterCount = 2)
        ),
        chapters = mapOf(
            "GEN" to mapOf(
                1 to listOf(VerseLine(1, "In the beginning"), VerseLine(2, "And the earth")),
                2 to listOf(VerseLine(1, "Thus the heavens"))
            )
        )
    )

    private fun psalmContent() = FakeBibleContentRepository(
        books = listOf(
            BookSummary("PSA", "Psalms", Testament.OLD, orderIndex = 0, chapterCount = 150)
        ),
        chapters = mapOf(
            "PSA" to mapOf(23 to listOf(VerseLine(1, "The Lord is my shepherd")))
        )
    )

    private class FakeBibleContentRepository(
        private val books: List<BookSummary>,
        private val chapters: Map<String, Map<Int, List<VerseLine>>>
    ) : BibleContentRepository {
        override fun books(translationId: String): Flow<List<BookSummary>> = MutableStateFlow(books)

        override suspend fun chapter(translationId: String, osisId: String, chapter: Int): ChapterContent? {
            val verses = chapters[osisId]?.get(chapter) ?: return null
            return ChapterContent(translationId, osisId, chapter, verses)
        }

        override suspend fun hasContent(translationId: String): Boolean = books.isNotEmpty()

        override suspend fun search(translationId: String, query: String): List<VerseMatch> = emptyList()
    }

    private class FakeCanonEngine(private val denomination: Denomination) : CanonEngine {
        override fun canonTypeFor(denomination: Denomination): CanonType = when (denomination) {
            Denomination.CATHOLIC -> CanonType.CATHOLIC_73
            else -> CanonType.PROTESTANT_66
        }

        override suspend fun profileFor(denomination: Denomination, bibleLanguage: String): CanonProfile {
            val canonType = canonTypeFor(denomination)
            val numbering = if (canonType == CanonType.CATHOLIC_73) {
                NumberingScheme.SEPTUAGINT
            } else {
                NumberingScheme.MASORETIC
            }
            val books = listOf(
                CanonBook("GEN", Testament.OLD, orderIndex = 0, isDeuterocanonical = false),
                CanonBook("PSA", Testament.OLD, orderIndex = 1, isDeuterocanonical = false)
            )
            return CanonProfile(
                denomination = denomination,
                canonType = canonType,
                books = books,
                numberingScheme = numbering,
                namingConventionId = null,
                suggestedTranslationId = null,
                lectionaryId = null
            )
        }
    }

    private class FakePreferencesStore(
        lastRead: String? = null,
        denomination: Denomination = Denomination.PROTESTANT_OTHER,
        continuousPlay: Boolean = false,
        bibleLanguage: String = "en"
    ) : PreferencesStore {
        private val continuousPlayFlow = MutableStateFlow(continuousPlay)
        private val state = MutableStateFlow(
            SetupState(
                denomination = denomination,
                canonType = CanonType.PROTESTANT_66,
                uiLanguage = "en",
                bibleLanguage = bibleLanguage,
                numberingScheme = NumberingScheme.MASORETIC,
                namingConventionId = null,
                bibleTranslationId = "web",
                lectionaryId = null,
                showDeuterocanonical = false,
                setupCompleted = true
            )
        )
        private val lastReadFlow = MutableStateFlow(lastRead)
        var lastSaved: String? = lastRead
            private set

        override val setupState: Flow<SetupState> = state
        override val lastReadPosition: Flow<String?> = lastReadFlow
        override val continuousPlay: Flow<Boolean> = continuousPlayFlow

        override suspend fun saveSetup(state: SetupState) { this.state.value = state }
        override suspend fun setSetupCompleted(value: Boolean) {}
        override suspend fun updateDenomination(profile: CanonProfile) {}
        override suspend fun setShowDeuterocanonical(value: Boolean) {}
        override suspend fun setActiveTranslation(translationId: String) {
            state.value = state.value.copy(bibleTranslationId = translationId)
        }
        override suspend fun setLastReadPosition(ref: String) {
            lastSaved = ref
            lastReadFlow.value = ref
        }
        override suspend fun setContinuousPlay(value: Boolean) {
            continuousPlayFlow.value = value
        }
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
            highlights.value = highlights.value + Highlight(id, verseRef, colorArgb, createdAt = 0)
            return id
        }

        override suspend fun addBookmark(verseRef: String, label: String?): Long {
            val id = nextId++
            bookmarks.value = bookmarks.value + Bookmark(id, verseRef, label, createdAt = 0)
            return id
        }

        override suspend fun addNote(verseRef: String, content: String): Long {
            val id = nextId++
            notes.value = notes.value + Note(id, verseRef, null, content, createdAt = 0, updatedAt = 0)
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

    private class FakeTranslationRepository : TranslationRepository {
        override fun catalog(): Flow<List<Translation>> = flowOf(emptyList())
        override suspend fun refreshCatalog() {}
        override suspend fun download(id: String): DownloadResult = DownloadResult.Success
        override suspend fun markPendingDownload(id: String) {}
        override suspend fun retryPendingDownloads() {}
    }

    private class FakeDownloadManager : DownloadManager {
        override fun progress(): Flow<DownloadProgress?> = flowOf(null)
        override suspend fun download(translationId: String): DownloadOutcome = DownloadOutcome.Success
        override suspend fun cancel(translationId: String) {}
        override suspend fun delete(translationId: String) {}
        override suspend fun retryPending() {}
    }

    /** In-memory [TtsReader] that records the queued verses and drives state directly. */
    private class FakeTtsReader : TtsReader {
        private val _state = MutableStateFlow(TtsState())
        override val state: StateFlow<TtsState> get() = _state
        private val _completions = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        override val completionEvents: Flow<Unit> = _completions.asSharedFlow()

        var lastVerses: List<TtsVerse> = emptyList()
            private set

        override fun play(verses: List<TtsVerse>, languageTag: String?) {
            lastVerses = verses
            _state.value = TtsState(
                status = TtsStatus.PLAYING,
                currentVerse = verses.firstOrNull()?.verse
            )
        }

        override fun pause() {
            _state.value = _state.value.copy(status = TtsStatus.PAUSED)
        }

        override fun resume() {
            _state.value = _state.value.copy(status = TtsStatus.PLAYING)
        }

        override fun stop() {
            _state.value = TtsState(status = TtsStatus.IDLE, currentVerse = null)
        }

        override fun setSpeed(speed: Float) {
            _state.value = _state.value.copy(speed = speed)
        }

        override fun shutdown() {}

        /** Simulates reaching the natural end of the queued chapter. */
        fun completeChapter() {
            _state.value = TtsState(status = TtsStatus.IDLE, currentVerse = null)
            _completions.tryEmit(Unit)
        }
    }
}
