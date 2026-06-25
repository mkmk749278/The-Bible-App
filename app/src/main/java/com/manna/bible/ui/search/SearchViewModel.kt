package com.manna.bible.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.canon.CanonEngine
import com.manna.bible.domain.model.Denomination
import com.manna.bible.domain.FeatureFlags
import com.manna.bible.domain.repository.BibleContentRepository
import com.manna.bible.domain.repository.TranslationRepository
import com.manna.bible.domain.topical.Topic
import com.manna.bible.domain.topical.TopicalIndex
import com.manna.bible.domain.usecase.ReadingRef
import com.manna.bible.domain.usecase.SearchResult
import com.manna.bible.domain.usecase.SearchScriptureUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Immutable UI state for the `Search_Screen` (Requirement 10).
 *
 * @property query Current query text.
 * @property results Canon-visible matches for the last submitted query.
 * @property isSearching True while a search is running.
 * @property hasSearched True once a query has been submitted (gates the no-results state).
 * @property contentAvailable False when no translation with stored content is active.
 */
data class SearchUiState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val contentAvailable: Boolean = true,
    /** Browsable topics for topical search; empty when the feature is disabled. */
    val topics: List<Topic> = emptyList(),
    /** Label of the topic whose verses are currently shown, or null for text search. */
    val activeTopicLabel: String? = null
) {
    /** True when a search ran for a non-blank query and produced nothing (Req 10.6). */
    val isNoResults: Boolean
        get() = hasSearched && !isSearching && results.isEmpty() && query.isNotBlank() && contentAvailable
}

/**
 * Drives full-text search over the active translation (spec task 10.1).
 *
 * Resolves the active translation + canon profile from the persisted setup (the
 * same way the reader does), then runs [SearchScriptureUseCase] against locally
 * stored content — no network required (Req 10.1, 10.5). Results carry a display
 * reference and snippet (Req 10.3); selection is handled by the screen via a
 * canonical reference (Req 10.4).
 *
 * Uses only `androidx.lifecycle` + coroutines/flow — no Android framework types.
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchScriptureUseCase: SearchScriptureUseCase,
    private val bibleContentRepository: BibleContentRepository,
    private val translationRepository: TranslationRepository,
    private val canonEngine: CanonEngine,
    private val preferencesStore: PreferencesStore,
    private val topicalIndex: TopicalIndex
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SearchUiState(
            topics = if (FeatureFlags.TOPICAL_SEARCH) topicalIndex.topics() else emptyList()
        )
    )
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    /** Records the latest query text without running a search. */
    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query, activeTopicLabel = null) }
    }

    /** Clears the query and results, keeping the browsable topic list. */
    fun clear() {
        _uiState.value = SearchUiState(topics = _uiState.value.topics)
    }

    /**
     * Loads the curated verses for [topic] (topical search) and shows them as
     * results — fully offline, resolving each reference's text from the active
     * translation. References not present in the active content are skipped.
     */
    fun searchTopic(topic: Topic) {
        _uiState.update {
            it.copy(isSearching = true, query = "", activeTopicLabel = topic.label)
        }
        viewModelScope.launch {
            val setup = preferencesStore.setupState.first()
            val translationId = setup.bibleTranslationId?.ifBlank { null }
                ?: translationRepository.catalog().first().firstOrNull { it.isDownloaded }?.id
            if (translationId == null) {
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        hasSearched = true,
                        results = emptyList(),
                        contentAvailable = false
                    )
                }
                return@launch
            }
            val bookNames = bibleContentRepository.books(translationId).first()
                .associate { it.osisId to it.name }
            val results = resolveTopicResults(translationId, topicalIndex.versesFor(topic.id), bookNames)
            _uiState.update {
                it.copy(
                    isSearching = false,
                    hasSearched = true,
                    results = results,
                    contentAvailable = true
                )
            }
        }
    }

    /** Resolves curated topical [refs] into displayable results with verse text. */
    private suspend fun resolveTopicResults(
        translationId: String,
        refs: List<ReadingRef>,
        bookNames: Map<String, String>
    ): List<SearchResult> = refs.mapNotNull { ref ->
        val content = runCatching {
            bibleContentRepository.chapter(translationId, ref.osisId, ref.chapter)
        }.getOrNull() ?: return@mapNotNull null
        val text = content.verses.firstOrNull { it.verse == ref.verse }?.text ?: return@mapNotNull null
        val name = bookNames[ref.osisId] ?: ref.osisId
        SearchResult(
            osisId = ref.osisId,
            chapter = ref.chapter,
            verse = ref.verse,
            reference = "$name ${ref.chapter}:${ref.verse}",
            snippet = text
        )
    }

    /** Runs the search for the current query against the active translation. */
    fun search() {
        val query = _uiState.value.query.trim()
        if (query.isEmpty()) return
        _uiState.update { it.copy(isSearching = true, activeTopicLabel = null) }
        viewModelScope.launch {
            val setup = preferencesStore.setupState.first()
            val translationId = setup.bibleTranslationId?.ifBlank { null }
                ?: translationRepository.catalog().first().firstOrNull { it.isDownloaded }?.id
            if (translationId == null) {
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        hasSearched = true,
                        results = emptyList(),
                        contentAvailable = false
                    )
                }
                return@launch
            }
            val language = setup.bibleLanguage ?: ""
            val denomination = setup.denomination ?: Denomination.SHOW_EVERYTHING
            val profile = canonEngine.profileFor(denomination, language)
            val bookNames = bibleContentRepository.books(translationId).first()
                .associate { it.osisId to it.name }
            val results = searchScriptureUseCase(translationId, query, profile, bookNames)
            _uiState.update {
                it.copy(
                    isSearching = false,
                    hasSearched = true,
                    results = results,
                    contentAvailable = true
                )
            }
        }
    }
}
