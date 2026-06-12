package com.manna.bible.ui.crisis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.crisis.CrisisCompanion
import com.manna.bible.domain.repository.BibleContentRepository
import com.manna.bible.domain.repository.TranslationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A comforting verse ready for display.
 *
 * @property reference Display reference, e.g. "Psalm 23:4".
 * @property text Plain-text verse content.
 * @property osisRef Canonical `OSIS.CHAPTER.VERSE` for "read in context".
 */
data class ComfortVerse(
    val reference: String,
    val text: String,
    val osisRef: String
)

/**
 * UI state for 3AM / Crisis Mode.
 *
 * @property isLoading True until the comfort verses resolve.
 * @property comfortVerses Curated comforting verses with text from the active translation.
 * @property listenRef Canonical reference for the "Just listen" path, or null.
 */
data class CrisisUiState(
    val isLoading: Boolean = true,
    val comfortVerses: List<ComfortVerse> = emptyList(),
    val listenRef: String? = null
)

/**
 * Loads the curated comfort verses ([CrisisCompanion]) and resolves their text from
 * the active translation (persisted selection, else first downloaded/bundled). Fully
 * offline; verses not present locally are skipped.
 *
 * Uses only `androidx.lifecycle` + coroutines/flow — no Android framework types.
 */
@HiltViewModel
class CrisisModeViewModel @Inject constructor(
    private val crisisCompanion: CrisisCompanion,
    private val preferencesStore: PreferencesStore,
    private val translationRepository: TranslationRepository,
    private val bibleContentRepository: BibleContentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CrisisUiState())
    val uiState: StateFlow<CrisisUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /** Resolves comfort verses' text for display. */
    fun load() {
        viewModelScope.launch {
            val listenRef = crisisCompanion.listenPassage().format()
            val translationId = resolveActiveTranslation()
            if (translationId == null) {
                _uiState.value = CrisisUiState(isLoading = false, listenRef = listenRef)
                return@launch
            }
            val bookNames = runCatching {
                bibleContentRepository.books(translationId).first().associate { it.osisId to it.name }
            }.getOrDefault(emptyMap())

            val verses = crisisCompanion.comfortVerses().mapNotNull { ref ->
                val content = runCatching {
                    bibleContentRepository.chapter(translationId, ref.osisId, ref.chapter)
                }.getOrNull()
                val text = content?.verses?.firstOrNull { it.verse == ref.verse }?.text
                    ?: return@mapNotNull null
                val name = bookNames[ref.osisId] ?: ref.osisId
                ComfortVerse(
                    reference = "$name ${ref.chapter}:${ref.verse}",
                    text = text,
                    osisRef = ref.format()
                )
            }
            _uiState.value = CrisisUiState(
                isLoading = false,
                comfortVerses = verses,
                listenRef = listenRef
            )
        }
    }

    /** Picks the persisted translation, else the first downloaded/bundled one available. */
    private suspend fun resolveActiveTranslation(): String? {
        val persistedId = preferencesStore.setupState.first().bibleTranslationId
        if (!persistedId.isNullOrBlank()) return persistedId
        val catalog = runCatching { translationRepository.catalog().first() }.getOrDefault(emptyList())
        return catalog.firstOrNull { it.isDownloaded }?.id ?: catalog.firstOrNull()?.id
    }
}
