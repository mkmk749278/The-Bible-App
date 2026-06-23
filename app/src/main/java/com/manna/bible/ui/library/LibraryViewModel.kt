package com.manna.bible.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.canon.CanonEngine
import com.manna.bible.domain.model.Denomination
import com.manna.bible.domain.repository.AnnotationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val DEFAULT_BIBLE_LANGUAGE = "en"

/** The kind of saved item, also used as the filter selection. */
enum class LibraryFilter { ALL, HIGHLIGHTS, BOOKMARKS, NOTES }

/** A single saved item, normalized across highlights, bookmarks, and notes. */
data class LibraryItem(
    val id: Long,
    val type: LibraryFilter,
    /** Canonical OSIS reference used to open the verse, e.g. "GEN.1.1" (null for note-only refs that lack one). */
    val openRef: String?,
    /** Human-readable reference, e.g. "GEN 1:1". */
    val displayRef: String,
    /** Bookmark label or note content; null for highlights. */
    val detail: String?,
    /** Highlight color (ARGB); null for other types. */
    val colorArgb: Int?,
    val createdAt: Long,
)

data class LibraryUiState(
    val items: List<LibraryItem> = emptyList(),
    val filter: LibraryFilter = LibraryFilter.ALL,
    val isLoading: Boolean = true,
) {
    val visibleItems: List<LibraryItem>
        get() = if (filter == LibraryFilter.ALL) items else items.filter { it.type == filter }
}

/**
 * Backs the Library — the user's collected highlights, bookmarks, and notes in one
 * browsable place, newest first, each tappable to jump back to the verse.
 *
 * Items are scoped to the active canon (the same visibility rule the reader uses):
 * annotations on books outside the current canon stay stored but hidden, never deleted.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val preferencesStore: PreferencesStore,
    private val canonEngine: CanonEngine,
    private val annotationRepository: AnnotationRepository,
) : ViewModel() {

    private val filter = MutableStateFlow(LibraryFilter.ALL)

    private val items: StateFlow<List<LibraryItem>?> =
        preferencesStore.setupState
            .map { setup ->
                (setup.denomination ?: Denomination.PROTESTANT_OTHER) to
                    (setup.bibleLanguage ?: DEFAULT_BIBLE_LANGUAGE)
            }
            .distinctUntilChanged()
            .flatMapLatest { (denomination, language) ->
                val profile = canonEngine.profileFor(denomination, language)
                val visible = profile.books.mapTo(mutableSetOf()) { it.osisId }
                combine(
                    annotationRepository.visibleHighlights(visible),
                    annotationRepository.visibleBookmarks(visible),
                    annotationRepository.visibleNotes(visible),
                ) { highlights, bookmarks, notes ->
                    buildList {
                        highlights.forEach {
                            add(
                                LibraryItem(
                                    id = it.id,
                                    type = LibraryFilter.HIGHLIGHTS,
                                    openRef = it.verseRef,
                                    displayRef = formatRef(it.verseRef),
                                    detail = null,
                                    colorArgb = it.colorArgb,
                                    createdAt = it.createdAt,
                                ),
                            )
                        }
                        bookmarks.forEach {
                            add(
                                LibraryItem(
                                    id = it.id,
                                    type = LibraryFilter.BOOKMARKS,
                                    openRef = it.verseRef,
                                    displayRef = formatRef(it.verseRef),
                                    detail = it.label?.takeIf(String::isNotBlank),
                                    colorArgb = null,
                                    createdAt = it.createdAt,
                                ),
                            )
                        }
                        notes.forEach {
                            val ref = it.verseRef ?: it.chapterRef
                            add(
                                LibraryItem(
                                    id = it.id,
                                    type = LibraryFilter.NOTES,
                                    openRef = it.verseRef,
                                    displayRef = ref?.let(::formatRef) ?: "",
                                    detail = it.content,
                                    colorArgb = null,
                                    createdAt = it.createdAt,
                                ),
                            )
                        }
                    }.sortedByDescending { it.createdAt }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val uiState: StateFlow<LibraryUiState> =
        combine(items, filter) { items, filter ->
            LibraryUiState(
                items = items.orEmpty(),
                filter = filter,
                isLoading = items == null,
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            LibraryUiState(),
        )

    fun setFilter(value: LibraryFilter) {
        filter.value = value
    }

    fun delete(item: LibraryItem) {
        viewModelScope.launch {
            when (item.type) {
                LibraryFilter.HIGHLIGHTS -> annotationRepository.deleteHighlight(item.id)
                LibraryFilter.BOOKMARKS -> annotationRepository.deleteBookmark(item.id)
                LibraryFilter.NOTES -> annotationRepository.deleteNote(item.id)
                LibraryFilter.ALL -> Unit
            }
        }
    }

    companion object {
        /**
         * Formats a canonical OSIS reference for display: "GEN.1.1" → "GEN 1:1",
         * "PSA.23" → "PSA 23". Unknown shapes are returned unchanged.
         */
        fun formatRef(ref: String): String {
            val parts = ref.split(".")
            return when (parts.size) {
                3 -> "${parts[0]} ${parts[1]}:${parts[2]}"
                2 -> "${parts[0]} ${parts[1]}"
                else -> ref
            }
        }
    }
}
