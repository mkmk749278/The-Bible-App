package com.manna.bible.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.audio.TtsReader
import com.manna.bible.domain.audio.TtsStatus
import com.manna.bible.domain.audio.TtsVerse
import com.manna.bible.domain.canon.CanonEngine
import com.manna.bible.domain.download.DownloadManager
import com.manna.bible.domain.download.DownloadOutcome
import com.manna.bible.domain.model.CanonProfile
import com.manna.bible.domain.model.Denomination
import com.manna.bible.domain.model.Bookmark
import com.manna.bible.domain.model.Highlight
import com.manna.bible.domain.model.Note
import com.manna.bible.domain.reader.PsalmDisplay
import com.manna.bible.domain.reader.ScriptDirection
import com.manna.bible.domain.repository.AnnotationRepository
import com.manna.bible.domain.repository.BibleContentRepository
import com.manna.bible.domain.repository.BookSummary
import com.manna.bible.domain.repository.TranslationRepository
import com.manna.bible.domain.usecase.GetChapterUseCase
import com.manna.bible.domain.usecase.NavigateChapterUseCase
import com.manna.bible.domain.usecase.ReadingRef
import com.manna.bible.domain.usecase.RestoreReadingPositionUseCase
import com.manna.bible.domain.usecase.SaveReadingPositionUseCase
import com.manna.bible.domain.usecase.SetActiveTranslationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A single rendered verse row in the reader (Requirement 2, 8).
 *
 * @property verse Canonical (Masoretic) verse number — the stable identity used
 *   for annotations and the reading position.
 * @property displayNumber Number to show to the reader; null falls back to [verse].
 * @property text Plain-text verse content.
 * @property hasHighlight True when a highlight exists on this verse.
 * @property hasBookmark True when a bookmark exists on this verse.
 * @property hasNote True when a note exists on this verse.
 */
data class ReaderVerse(
    val verse: Int,
    val displayNumber: Int?,
    val text: String,
    val hasHighlight: Boolean = false,
    val hasBookmark: Boolean = false,
    val hasNote: Boolean = false
)

/**
 * Immutable UI state for the `Reader_Screen` (Requirements 2, 3, 7, 8).
 *
 * Verse numbers are canonical; [displayedChapterNumber] applies the active
 * profile's numbering scheme (e.g. Septuagint Psalms) via `PsalmDisplay`.
 */
data class ReaderUiState(
    val activeTranslationId: String? = null,
    val profile: CanonProfile? = null,
    val books: List<BookSummary> = emptyList(),
    val bookName: String = "",
    val osisId: String? = null,
    val chapter: Int = 0,
    val displayedChapterNumber: Int = 0,
    val verses: List<ReaderVerse> = emptyList(),
    val canPrev: Boolean = false,
    val canNext: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isEmptyContent: Boolean = false,
    /** True when the active translation has no stored content for the requested chapter (Req 2.6). */
    val chapterUnavailable: Boolean = false,
    /** True while the active translation's content is being downloaded (Req 5.2). */
    val isDownloading: Boolean = false,
    /** Determinate download progress in 0f..1f, or null when no download is running. */
    val downloadProgress: Float? = null,
    /** Verse the reader currently has open in the annotation sheet, if any (Req 8.1). */
    val selectedVerse: Int? = null,
    /** A one-shot scroll target consumed by the screen, then cleared (Req 3.6, 10.4). */
    val scrollToVerse: Int? = null,
    // --- audio read-aloud (Req 9) -------------------------------------------
    /** True while a chapter is being read aloud (Req 9.1, 9.3). */
    val isAudioPlaying: Boolean = false,
    /** True while read-aloud is paused on the current verse (Req 9.3). */
    val isAudioPaused: Boolean = false,
    /** Canonical verse number currently being read aloud, or null (Req 9.2). */
    val audioVerse: Int? = null,
    /** Active read-aloud speed in 0.5x..2.0x (Req 9.4). */
    val ttsSpeed: Float = TtsReader.DEFAULT_SPEED,
    /** When true, read-aloud continues into the next chapter at chapter end (Req 9.7). */
    val continuousPlay: Boolean = false,
    /** True when no on-device voice matched the Bible language; default voice is used (Req 9.6). */
    val audioVoiceUnavailable: Boolean = false,
    // --- accessibility (Req 14) ---------------------------------------------
    /** True when the active Bible language uses a right-to-left script (Req 14.4). */
    val isRtl: Boolean = false,
    /** True when Simplified Mode (audio-first, enlarged controls) is enabled (Req 14.5). */
    val simplifiedMode: Boolean = false
) {
    /** True when audio is playing or paused — the audio bar shows stop/resume controls. */
    val isAudioActive: Boolean get() = isAudioPlaying || isAudioPaused
}

/**
 * Drives the offline `Reader_Screen` (Requirements 2, 3, 7, 8, 13.3).
 *
 * On init it resolves the active `CanonProfile` from the persisted setup, picks the
 * active translation (persisted selection, else the first bundled/downloaded one),
 * loads that translation's books, and opens at the restored `Reading_Position`.
 * Navigation is canon-aware via [NavigateChapterUseCase]; Psalm chapter numbers are
 * displayed through [PsalmDisplay]. Each viewed chapter is persisted via
 * [SaveReadingPositionUseCase]. Verse annotation indicators are kept current by
 * observing the canon-visible annotation streams.
 *
 * Uses only `androidx.lifecycle` + coroutines/flow — no Android framework types.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val getChapterUseCase: GetChapterUseCase,
    private val navigateChapterUseCase: NavigateChapterUseCase,
    private val setActiveTranslationUseCase: SetActiveTranslationUseCase,
    private val restoreReadingPositionUseCase: RestoreReadingPositionUseCase,
    private val saveReadingPositionUseCase: SaveReadingPositionUseCase,
    private val canonEngine: CanonEngine,
    private val preferencesStore: PreferencesStore,
    private val annotationRepository: AnnotationRepository,
    private val bibleContentRepository: BibleContentRepository,
    private val translationRepository: TranslationRepository,
    private val downloadManager: DownloadManager,
    private val ttsReader: TtsReader
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    // Latest canon-visible annotation snapshots, used to flag verses in the reader.
    private var highlights: List<Highlight> = emptyList()
    private var bookmarks: List<Bookmark> = emptyList()
    private var notes: List<Note> = emptyList()

    // BCP-47 language tag of the active Bible text, used to pick a TTS voice (Req 9.5).
    private var bibleLanguageTag: String? = null

    init {
        observeSetup()
        observeAnnotations()
        observeDownloadProgress()
        observeAudio()
    }

    /** Mirrors the active download's progress into [ReaderUiState.downloadProgress]. */
    private fun observeDownloadProgress() {
        viewModelScope.launch {
            downloadManager.progress().collect { progress ->
                val fraction = progress
                    ?.takeIf { it.totalChapters > 0 }
                    ?.let { it.completedChapters.toFloat() / it.totalChapters }
                _uiState.value = _uiState.value.copy(downloadProgress = fraction)
            }
        }
    }

    /**
     * Mirrors the read-aloud engine into the UI state and drives continuous play.
     *
     * Three streams feed the audio bar: the engine's [TtsReader.state] (playing
     * verse, speed, voice availability), the persisted continuous-play preference,
     * and [TtsReader.completionEvents] which, at a chapter's natural end, advances to
     * the next chapter and keeps reading when continuous play is enabled (Req 9.7).
     */
    private fun observeAudio() {
        viewModelScope.launch {
            ttsReader.state.collect { tts ->
                _uiState.value = _uiState.value.copy(
                    isAudioPlaying = tts.status == TtsStatus.PLAYING,
                    isAudioPaused = tts.status == TtsStatus.PAUSED,
                    audioVerse = tts.currentVerse,
                    ttsSpeed = tts.speed,
                    audioVoiceUnavailable = tts.voiceUnavailable
                )
            }
        }
        viewModelScope.launch {
            preferencesStore.continuousPlay.collect { enabled ->
                _uiState.value = _uiState.value.copy(continuousPlay = enabled)
            }
        }
        viewModelScope.launch {
            preferencesStore.simplifiedMode.collect { enabled ->
                _uiState.value = _uiState.value.copy(simplifiedMode = enabled)
            }
        }
        viewModelScope.launch {
            ttsReader.completionEvents.collect {
                if (_uiState.value.continuousPlay && loadNextChapter()) {
                    startAudio()
                }
            }
        }
    }

    /**
     * Downloads the active translation's content on demand (Req 5.1, 5.2). Invoked
     * from the empty-content state so a freshly chosen translation can be fetched
     * and opened without leaving the reader. On success the current position is
     * (re)loaded; offline/failure surface an error and leave the empty state.
     */
    fun downloadActiveTranslation() {
        val translationId = _uiState.value.activeTranslationId ?: return
        if (_uiState.value.isDownloading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDownloading = true, errorMessage = null)
            val outcome = runCatching { downloadManager.download(translationId) }
                .getOrElse { DownloadOutcome.Failure(it.message ?: "download failed") }
            _uiState.value = _uiState.value.copy(isDownloading = false, downloadProgress = null)
            when (outcome) {
                DownloadOutcome.Success -> refresh()
                DownloadOutcome.Offline ->
                    _uiState.value = _uiState.value.copy(errorMessage = "offline")
                is DownloadOutcome.Failure ->
                    _uiState.value = _uiState.value.copy(errorMessage = outcome.reason)
            }
        }
    }

    /**
     * Recomputes the active profile + translation whenever setup changes and (re)opens
     * the restored reading position. Switching the active translation re-renders the
     * current position in the newly active edition (Req 6.3).
     */
    private fun observeSetup() {
        viewModelScope.launch {
            preferencesStore.setupState
                .map { setup ->
                    val denomination = setup.denomination ?: Denomination.PROTESTANT_OTHER
                    val language = setup.bibleLanguage ?: DEFAULT_BIBLE_LANGUAGE
                    Triple(denomination, language, setup.bibleTranslationId)
                }
                .distinctUntilChanged()
                .collect { (denomination, language, translationId) ->
                    bibleLanguageTag = language
                    val profile = canonEngine.profileFor(denomination, language)
                    val resolvedTranslation = resolveActiveTranslation(translationId)
                    _uiState.value = _uiState.value.copy(
                        profile = profile,
                        activeTranslationId = resolvedTranslation,
                        isRtl = ScriptDirection.isRightToLeft(language)
                    )
                    loadInitial(profile, resolvedTranslation)
                }
        }
    }

    /** Picks the persisted translation, else the first downloaded/bundled one available. */
    private suspend fun resolveActiveTranslation(persistedId: String?): String? {
        if (!persistedId.isNullOrBlank()) return persistedId
        val catalog = runCatching { translationRepository.catalog().first() }.getOrDefault(emptyList())
        return catalog.firstOrNull { it.isDownloaded }?.id ?: catalog.firstOrNull()?.id
    }

    /** Loads books and opens the restored position, or surfaces the empty/seeding state. */
    private suspend fun loadInitial(profile: CanonProfile, translationId: String?) {
        if (translationId == null) {
            _uiState.value = _uiState.value.copy(isLoading = false, isEmptyContent = true)
            return
        }
        if (!bibleContentRepository.hasContent(translationId)) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isEmptyContent = true,
                books = emptyList()
            )
            return
        }
        val books = getChapterUseCase.books(translationId).first()
        val start = restoreReadingPositionUseCase(profile, books)
        _uiState.value = _uiState.value.copy(books = books, isEmptyContent = false)
        loadChapter(start.osisId, start.chapter, targetVerse = start.verse)
    }

    /** Keeps verse annotation indicators current for the active canon (Req 8.3). */
    private fun observeAnnotations() {
        viewModelScope.launch {
            _uiState
                .map { it.profile }
                .distinctUntilChanged()
                .flatMapLatest { profile ->
                    if (profile == null) {
                        flowOf(Triple(emptyList<Highlight>(), emptyList<Bookmark>(), emptyList<Note>()))
                    } else {
                        val visible = profile.books.mapTo(mutableSetOf()) { it.osisId }
                        combine(
                            annotationRepository.visibleHighlights(visible),
                            annotationRepository.visibleBookmarks(visible),
                            annotationRepository.visibleNotes(visible)
                        ) { h, b, n -> Triple(h, b, n) }
                    }
                }
                .collect { (h, b, n) ->
                    highlights = h
                    bookmarks = b
                    notes = n
                    refreshVerseFlags()
                }
        }
    }

    /** Opens an explicit book+chapter (e.g. from the picker, Req 3.6). */
    fun openChapter(osisId: String, chapter: Int) {
        stopAudio()
        viewModelScope.launch { loadChapter(osisId, chapter, targetVerse = 1) }
    }

    /** Opens [osisId]/[chapter] and scrolls to [verse] (e.g. from search, Req 10.4). */
    fun openAt(osisId: String, chapter: Int, verse: Int) {
        stopAudio()
        viewModelScope.launch { loadChapter(osisId, chapter, targetVerse = verse) }
    }

    /** Advances to the next chapter in canon order, if one exists (Req 3.1). */
    fun nextChapter() {
        stopAudio()
        viewModelScope.launch { loadNextChapter() }
    }

    /**
     * Loads the next canon-ordered chapter in place, returning false at the canon's
     * end. Used both by manual navigation and continuous-play advance (Req 3.1, 9.7).
     */
    private suspend fun loadNextChapter(): Boolean {
        val state = _uiState.value
        val profile = state.profile ?: return false
        val osisId = state.osisId ?: return false
        val next = navigateChapterUseCase.next(
            profile,
            ReadingRef(osisId, state.chapter),
            state.books
        ) ?: return false
        loadChapter(next.osisId, next.chapter, targetVerse = 1)
        return true
    }

    /** Moves to the previous chapter in canon order, if one exists (Req 3.2). */
    fun previousChapter() {
        stopAudio()
        val state = _uiState.value
        val profile = state.profile ?: return
        val osisId = state.osisId ?: return
        val previous = navigateChapterUseCase.previous(
            profile,
            ReadingRef(osisId, state.chapter),
            state.books
        ) ?: return
        viewModelScope.launch { loadChapter(previous.osisId, previous.chapter, targetVerse = 1) }
    }

    /** Requests a scroll to [verse] within the current chapter (Req 10.4). */
    fun goToVerse(verse: Int) {
        _uiState.value = _uiState.value.copy(scrollToVerse = verse)
    }

    /** Clears the one-shot scroll target after the screen has consumed it. */
    fun onScrollHandled() {
        if (_uiState.value.scrollToVerse != null) {
            _uiState.value = _uiState.value.copy(scrollToVerse = null)
        }
    }

    /** Re-loads the current chapter (e.g. after content becomes available). */
    fun refresh() {
        val state = _uiState.value
        val profile = state.profile ?: return
        val translationId = state.activeTranslationId
        viewModelScope.launch {
            if (state.osisId != null) {
                loadChapter(state.osisId, state.chapter, targetVerse = 1)
            } else {
                loadInitial(profile, translationId)
            }
        }
    }

    /** Sets and persists the active translation; the setup observer re-renders (Req 6.1, 6.2). */
    fun setActiveTranslation(translationId: String) {
        stopAudio()
        viewModelScope.launch { setActiveTranslationUseCase(translationId) }
    }

    // --- audio read-aloud (Req 9) -------------------------------------------

    /**
     * Play/pause/resume toggle for the audio bar (Req 9.1, 9.3): starts read-aloud
     * from the top of the current chapter when idle, pauses when playing, and
     * resumes the current verse when paused.
     */
    fun onAudioPlayPause() {
        val state = _uiState.value
        when {
            state.isAudioPlaying -> ttsReader.pause()
            state.isAudioPaused -> ttsReader.resume()
            else -> startAudio()
        }
    }

    /** Stops read-aloud and clears the spoken-verse indicator (Req 9.3). */
    fun stopAudio() {
        ttsReader.stop()
    }

    /** Sets the read-aloud speed; the engine clamps to 0.5x..2.0x (Req 9.4). */
    fun setAudioSpeed(speed: Float) {
        ttsReader.setSpeed(speed)
    }

    /** Persists the continuous-play preference used at chapter end (Req 9.7). */
    fun setContinuousPlay(enabled: Boolean) {
        viewModelScope.launch { preferencesStore.setContinuousPlay(enabled) }
    }

    /** Begins reading the current chapter's verses aloud in the Bible language (Req 9.1, 9.5). */
    private fun startAudio() {
        val verses = _uiState.value.verses
        if (verses.isEmpty()) return
        ttsReader.play(
            verses.map { TtsVerse(it.verse, it.text) },
            bibleLanguageTag
        )
    }

    // --- annotation interactions (Req 8.1, 8.2, 8.4) -------------------------

    /** Opens the annotation sheet for [verse], or closes it when null. */
    fun selectVerse(verse: Int?) {
        _uiState.value = _uiState.value.copy(selectedVerse = verse)
    }

    /** Toggles a highlight on [verse] using the default reader highlight color. */
    fun toggleHighlight(verse: Int) {
        val ref = verseRefOf(verse) ?: return
        viewModelScope.launch {
            val existing = highlights.firstOrNull { it.verseRef == ref }
            if (existing != null) {
                annotationRepository.deleteHighlight(existing.id)
            } else {
                annotationRepository.addHighlight(ref, DEFAULT_HIGHLIGHT_COLOR_ARGB)
            }
        }
    }

    /** Toggles a bookmark on [verse]. */
    fun toggleBookmark(verse: Int) {
        val ref = verseRefOf(verse) ?: return
        viewModelScope.launch {
            val existing = bookmarks.firstOrNull { it.verseRef == ref }
            if (existing != null) {
                annotationRepository.deleteBookmark(existing.id)
            } else {
                annotationRepository.addBookmark(ref, label = null)
            }
        }
    }

    /** Adds a note with [content] to [verse]; blank content is ignored. */
    fun addNote(verse: Int, content: String) {
        val ref = verseRefOf(verse) ?: return
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { annotationRepository.addNote(ref, trimmed) }
    }

    /** Removes every note attached to [verse] (Req 8.4). */
    fun removeNotes(verse: Int) {
        val ref = verseRefOf(verse) ?: return
        viewModelScope.launch {
            notes.filter { it.verseRef == ref }.forEach { annotationRepository.deleteNote(it.id) }
        }
    }

    private fun verseRefOf(verse: Int): String? {
        val osisId = _uiState.value.osisId ?: return null
        return ReadingRef(osisId, _uiState.value.chapter, verse).format()
    }

    /**
     * Fetches and renders [osisId]/[chapter] for the active translation, persisting
     * the position and recomputing navigation availability. Surfaces the
     * download/switch state when the chapter is not stored (Req 2.6).
     */
    private suspend fun loadChapter(osisId: String, chapter: Int, targetVerse: Int) {
        val state = _uiState.value
        val translationId = state.activeTranslationId ?: return
        val profile = state.profile ?: return

        _uiState.value = state.copy(
            isLoading = true,
            errorMessage = null,
            chapterUnavailable = false
        )

        val content = runCatching {
            getChapterUseCase(translationId, osisId, chapter)
        }.getOrElse { error ->
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = error.message
            )
            return
        }

        if (content == null) {
            _uiState.value = _uiState.value.copy(
                osisId = osisId,
                chapter = chapter,
                isLoading = false,
                chapterUnavailable = true,
                verses = emptyList()
            )
            return
        }

        val books = state.books.ifEmpty { getChapterUseCase.books(translationId).first() }
        val bookName = books.firstOrNull { it.osisId == osisId }?.name ?: osisId
        val displayedChapter =
            if (osisId == PSALMS_OSIS_ID) PsalmDisplay.displayPsalmNumber(profile, chapter) else chapter
        val canPrev = navigateChapterUseCase.previous(profile, ReadingRef(osisId, chapter), books) != null
        val canNext = navigateChapterUseCase.next(profile, ReadingRef(osisId, chapter), books) != null

        _uiState.value = _uiState.value.copy(
            books = books,
            bookName = bookName,
            osisId = osisId,
            chapter = chapter,
            displayedChapterNumber = displayedChapter,
            verses = content.verses.map { line ->
                buildVerse(osisId, chapter, line.verse, line.text)
            },
            canPrev = canPrev,
            canNext = canNext,
            isLoading = false,
            chapterUnavailable = false,
            isEmptyContent = false,
            scrollToVerse = targetVerse.takeIf { it > 1 }
        )

        saveReadingPositionUseCase(ReadingRef(osisId, chapter, targetVerse))
    }

    /** Recomputes annotation flags on the currently rendered verses. */
    private fun refreshVerseFlags() {
        val state = _uiState.value
        val osisId = state.osisId ?: return
        if (state.verses.isEmpty()) return
        _uiState.value = state.copy(
            verses = state.verses.map { verse ->
                buildVerse(osisId, state.chapter, verse.verse, verse.text, verse.displayNumber)
            }
        )
    }

    private fun buildVerse(
        osisId: String,
        chapter: Int,
        verse: Int,
        text: String,
        displayNumber: Int? = verse
    ): ReaderVerse {
        val ref = ReadingRef(osisId, chapter, verse).format()
        return ReaderVerse(
            verse = verse,
            displayNumber = displayNumber ?: verse,
            text = text,
            hasHighlight = highlights.any { it.verseRef == ref },
            hasBookmark = bookmarks.any { it.verseRef == ref },
            hasNote = notes.any { it.verseRef == ref }
        )
    }

    private companion object {
        const val PSALMS_OSIS_ID = "PSA"
        const val DEFAULT_BIBLE_LANGUAGE = "en"
        // Manna gold (0xFFC9952A) — kept in sync with the design system highlight color.
        const val DEFAULT_HIGHLIGHT_COLOR_ARGB = 0xFFC9952A.toInt()
    }
}
