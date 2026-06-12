package com.manna.bible.ui.card

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.daily.DailyVerseProvider
import com.manna.bible.domain.repository.BibleContentRepository
import com.manna.bible.domain.repository.TranslationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * UI state for the Scripture Card Generator.
 *
 * @property isLoading True until the verse resolves.
 * @property reference Display reference, e.g. "John 3:16".
 * @property verseText Verse text for the card, or null when unavailable.
 */
data class ScriptureCardUiState(
    val isLoading: Boolean = true,
    val reference: String = "",
    val verseText: String? = null
)

/**
 * Resolves today's verse of the day and its text from the active translation, ready
 * to render onto a shareable card. Fully offline.
 *
 * Uses only `androidx.lifecycle` + coroutines/flow — no Android framework types.
 */
@HiltViewModel
class ScriptureCardViewModel @Inject constructor(
    private val dailyVerseProvider: DailyVerseProvider,
    private val preferencesStore: PreferencesStore,
    private val translationRepository: TranslationRepository,
    private val bibleContentRepository: BibleContentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScriptureCardUiState())
    val uiState: StateFlow<ScriptureCardUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val ref = dailyVerseProvider.verseForDate(LocalDate.now())
            val translationId = resolveActiveTranslation()
            if (translationId == null) {
                _uiState.value = ScriptureCardUiState(isLoading = false)
                return@launch
            }
            val content = runCatching {
                bibleContentRepository.chapter(translationId, ref.osisId, ref.chapter)
            }.getOrNull()
            val text = content?.verses?.firstOrNull { it.verse == ref.verse }?.text
            val bookName = runCatching {
                bibleContentRepository.books(translationId).first()
                    .firstOrNull { it.osisId == ref.osisId }?.name
            }.getOrNull() ?: ref.osisId
            _uiState.value = ScriptureCardUiState(
                isLoading = false,
                reference = "$bookName ${ref.chapter}:${ref.verse}",
                verseText = text
            )
        }
    }

    private suspend fun resolveActiveTranslation(): String? {
        val persistedId = preferencesStore.setupState.first().bibleTranslationId
        if (!persistedId.isNullOrBlank()) return persistedId
        val catalog = runCatching { translationRepository.catalog().first() }.getOrDefault(emptyList())
        return catalog.firstOrNull { it.isDownloaded }?.id ?: catalog.firstOrNull()?.id
    }
}
