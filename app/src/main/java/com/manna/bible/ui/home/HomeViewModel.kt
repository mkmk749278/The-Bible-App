package com.manna.bible.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.repository.BibleContentRepository
import com.manna.bible.domain.repository.TranslationRepository
import com.manna.bible.domain.usecase.ReadingRef
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the Home experience (UX directive: Continue Reading · Today's
 * Verse · Continue Listening — nothing else).
 *
 * @property isLoading True until the reading position resolves.
 * @property continueLabel Human-readable last position (e.g. "John 3"), or null
 *   when the user has not read anything yet.
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val continueLabel: String? = null
)

/**
 * Resolves the persisted reading position into a display label for the Home and
 * Listen surfaces. The label combines the localized book name from the active
 * translation with the canonical chapter number. Fully offline.
 *
 * Uses only `androidx.lifecycle` + coroutines/flow — no Android framework types.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val preferencesStore: PreferencesStore,
    private val translationRepository: TranslationRepository,
    private val bibleContentRepository: BibleContentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                preferencesStore.lastReadPosition,
                preferencesStore.setupState
            ) { position, setup -> position to setup.bibleTranslationId }
                .collect { (position, persistedId) ->
                    _uiState.value = HomeUiState(
                        isLoading = false,
                        continueLabel = labelFor(ReadingRef.parse(position), persistedId)
                    )
                }
        }
    }

    private suspend fun labelFor(ref: ReadingRef?, persistedId: String?): String? {
        if (ref == null) return null
        val translationId = resolveActiveTranslation(persistedId) ?: return null
        val bookName = runCatching {
            bibleContentRepository.books(translationId).first()
                .firstOrNull { it.osisId == ref.osisId }?.name
        }.getOrNull() ?: ref.osisId
        return "$bookName ${ref.chapter}"
    }

    /** Picks the persisted translation, else the first downloaded/bundled one available. */
    private suspend fun resolveActiveTranslation(persistedId: String?): String? {
        if (!persistedId.isNullOrBlank()) return persistedId
        val catalog = runCatching { translationRepository.catalog().first() }.getOrDefault(emptyList())
        return catalog.firstOrNull { it.isDownloaded }?.id ?: catalog.firstOrNull()?.id
    }
}
