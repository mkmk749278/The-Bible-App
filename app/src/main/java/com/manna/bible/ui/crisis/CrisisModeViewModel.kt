package com.manna.bible.ui.crisis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.crisis.CrisisCompanion
import com.manna.bible.domain.crisis.PersecutionCategory
import com.manna.bible.domain.crisis.PersecutionCompanion
import com.manna.bible.domain.repository.BibleContentRepository
import com.manna.bible.domain.repository.TranslationRepository
import com.manna.bible.domain.usecase.ReadingRef
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
 * @property selectedPersecutionCategory The persecution category currently selected, or
 *   null when none is selected (F-06).
 * @property persecutionVerses The resolved verses for [selectedPersecutionCategory].
 * @property isPersecutionLoading True while a persecution category's verses are resolving.
 */
data class CrisisUiState(
    val isLoading: Boolean = true,
    val comfortVerses: List<ComfortVerse> = emptyList(),
    val listenRef: String? = null,
    val selectedPersecutionCategory: PersecutionCategory? = null,
    val persecutionVerses: List<ComfortVerse> = emptyList(),
    val isPersecutionLoading: Boolean = false
)

/**
 * Loads the curated comfort verses ([CrisisCompanion]) and resolves their text from
 * the active translation (persisted selection, else first downloaded/bundled). Fully
 * offline; verses not present locally are skipped.
 *
 * Also serves the Persecution-Aware Comfort tier (F-06): when a [PersecutionCategory] is
 * selected, its curated passages are resolved against the same active translation using
 * the same offline pattern as the comfort list.
 *
 * Uses only `androidx.lifecycle` + coroutines/flow — no Android framework types.
 */
@HiltViewModel
class CrisisModeViewModel @Inject constructor(
    private val crisisCompanion: CrisisCompanion,
    private val persecutionCompanion: PersecutionCompanion,
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
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    comfortVerses = emptyList(),
                    listenRef = listenRef
                )
                return@launch
            }
            val bookNames = bookNamesFor(translationId)
            val verses = resolveVerses(translationId, crisisCompanion.comfortVerses(), bookNames)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                comfortVerses = verses,
                listenRef = listenRef
            )
        }
    }

    /**
     * Selects a persecution category and resolves its curated passages against the active
     * translation (F-06). Selecting the already-selected category deselects it and clears
     * the resolved list. The curated comfort list is left untouched throughout.
     */
    fun selectPersecutionCategory(category: PersecutionCategory) {
        if (_uiState.value.selectedPersecutionCategory == category) {
            _uiState.value = _uiState.value.copy(
                selectedPersecutionCategory = null,
                persecutionVerses = emptyList(),
                isPersecutionLoading = false
            )
            return
        }
        _uiState.value = _uiState.value.copy(
            selectedPersecutionCategory = category,
            persecutionVerses = emptyList(),
            isPersecutionLoading = true
        )
        viewModelScope.launch {
            val translationId = resolveActiveTranslation()
            if (translationId == null) {
                _uiState.value = _uiState.value.copy(isPersecutionLoading = false)
                return@launch
            }
            val bookNames = bookNamesFor(translationId)
            val verses = resolveVerses(
                translationId,
                persecutionCompanion.versesFor(category),
                bookNames
            )
            // Ignore a stale result if the user changed selection while we resolved.
            if (_uiState.value.selectedPersecutionCategory == category) {
                _uiState.value = _uiState.value.copy(
                    persecutionVerses = verses,
                    isPersecutionLoading = false
                )
            }
        }
    }

    /** Resolves [refs] to displayable [ComfortVerse]s from [translationId]; missing verses skipped. */
    private suspend fun resolveVerses(
        translationId: String,
        refs: List<ReadingRef>,
        bookNames: Map<String, String>
    ): List<ComfortVerse> = refs.mapNotNull { ref ->
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

    private suspend fun bookNamesFor(translationId: String): Map<String, String> =
        runCatching {
            bibleContentRepository.books(translationId).first().associate { it.osisId to it.name }
        }.getOrDefault(emptyMap())

    /** Picks the persisted translation, else the first downloaded/bundled one available. */
    private suspend fun resolveActiveTranslation(): String? {
        val persistedId = preferencesStore.setupState.first().bibleTranslationId
        if (!persistedId.isNullOrBlank()) return persistedId
        val catalog = runCatching { translationRepository.catalog().first() }.getOrDefault(emptyList())
        return catalog.firstOrNull { it.isDownloaded }?.id ?: catalog.firstOrNull()?.id
    }
}
