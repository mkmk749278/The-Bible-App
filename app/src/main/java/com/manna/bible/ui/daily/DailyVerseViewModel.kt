package com.manna.bible.ui.daily

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.daily.DailyVerseProvider
import com.manna.bible.domain.repository.BibleContentRepository
import com.manna.bible.domain.repository.TranslationRepository
import com.manna.bible.domain.translation.Translation
import com.manna.bible.domain.usecase.ReadingRef
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * UI state for the Verse of the Day surface.
 *
 * @property isLoading True until the daily verse text resolves.
 * @property reference Display reference, e.g. "John 3:16".
 * @property verseText Plain-text verse content, or null when unavailable.
 * @property osisRef Canonical `OSIS.CHAPTER.VERSE` for "read in context" navigation.
 * @property unavailable True when no translation content is available for the verse.
 */
data class DailyVerseUiState(
    val isLoading: Boolean = true,
    val reference: String = "",
    val verseText: String? = null,
    val osisRef: String? = null,
    val unavailable: Boolean = false
)

/**
 * Resolves today's deterministic [DailyVerseProvider] reference and loads its text
 * from the active translation (the persisted selection, falling back to the first
 * downloaded/bundled edition). Fully offline; when no content is available for the
 * verse the state surfaces an unavailable hint rather than failing.
 *
 * Uses only `androidx.lifecycle` + coroutines/flow — no Android framework types.
 */
@HiltViewModel
class DailyVerseViewModel @Inject constructor(
    private val dailyVerseProvider: DailyVerseProvider,
    private val preferencesStore: PreferencesStore,
    private val translationRepository: TranslationRepository,
    private val bibleContentRepository: BibleContentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DailyVerseUiState())
    val uiState: StateFlow<DailyVerseUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /** Recomputes today's verse and its text (e.g. on first open or after a retry). */
    fun load() {
        viewModelScope.launch {
            val ref = dailyVerseProvider.verseForDate(LocalDate.now())
            val translation = resolveActiveTranslation()
            if (translation == null) {
                _uiState.value = DailyVerseUiState(isLoading = false, unavailable = true)
                return@launch
            }
            val content = runCatching {
                bibleContentRepository.chapter(translation.id, ref.osisId, ref.chapter)
            }.getOrNull()
            val verseText = content?.verses?.firstOrNull { it.verse == ref.verse }?.text
            if (verseText == null) {
                _uiState.value = DailyVerseUiState(isLoading = false, unavailable = true)
                return@launch
            }
            val bookName = bibleContentRepository.books(translation.id).first()
                .firstOrNull { it.osisId == ref.osisId }?.name ?: ref.osisId
            _uiState.value = DailyVerseUiState(
                isLoading = false,
                reference = "$bookName ${ref.chapter}:${ref.verse}",
                verseText = verseText,
                osisRef = ref.format(),
                unavailable = false
            )
        }
    }

    /** Picks the persisted translation, else the first downloaded/bundled one available. */
    private suspend fun resolveActiveTranslation(): Translation? {
        val persistedId = preferencesStore.setupState.first().bibleTranslationId
        val catalog = runCatching { translationRepository.catalog().first() }.getOrDefault(emptyList())
        if (catalog.isEmpty()) return null
        if (!persistedId.isNullOrBlank()) {
            catalog.firstOrNull { it.id == persistedId }?.let { return it }
        }
        return catalog.firstOrNull { it.isDownloaded } ?: catalog.firstOrNull()
    }
}
