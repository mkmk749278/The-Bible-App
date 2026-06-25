package com.manna.bible.ui.crisis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.crisis.CrisisAiEngine
import com.manna.bible.domain.crisis.CrisisAiResult
import com.manna.bible.domain.crisis.CrisisCompanion
import com.manna.bible.domain.crisis.NightWindow
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
import java.util.Calendar
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
 * @property situationText The free-text crisis description currently being typed (F-03).
 *   It is transient UI state only: it is cleared the moment a response returns and is
 *   never persisted to Room, DataStore, or logs.
 * @property aiResponse The AI companion's response to the last submitted situation, or null.
 * @property isAiLoading True while an AI companion request is in flight.
 * @property aiConfigured True when the AI companion engine has its credentials; gates the
 *   text input (the curated list is always available regardless).
 */
data class CrisisUiState(
    val isLoading: Boolean = true,
    val comfortVerses: List<ComfortVerse> = emptyList(),
    val listenRef: String? = null,
    val selectedPersecutionCategory: PersecutionCategory? = null,
    val persecutionVerses: List<ComfortVerse> = emptyList(),
    val isPersecutionLoading: Boolean = false,
    val situationText: String = "",
    val aiResponse: CrisisAiResult? = null,
    val isAiLoading: Boolean = false,
    val aiConfigured: Boolean = false
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
    private val bibleContentRepository: BibleContentRepository,
    private val crisisAiEngine: CrisisAiEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(CrisisUiState())
    val uiState: StateFlow<CrisisUiState> = _uiState.asStateFlow()

    init {
        load()
        // The compile-time flag gates the on-screen surface; this engine-config signal
        // gates whether the input is interactive vs. shows the offline hint (F-03).
        _uiState.value = _uiState.value.copy(aiConfigured = crisisAiEngine.isConfigured)
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

    /**
     * Records the latest free-text situation as the user types (F-03). Editing the text
     * clears any previously shown AI response so a stale answer is never paired with new
     * input. The text lives only in transient UI state.
     */
    fun updateSituation(text: String) {
        _uiState.value = _uiState.value.copy(situationText = text, aiResponse = null)
    }

    /**
     * Submits the current situation to the AI companion (F-03). No-op when the text is
     * blank or the engine is unconfigured.
     *
     * Privacy: the situation is read into a local `val` for the duration of the call and
     * is never copied to the ViewModel or [CrisisUiState]. The moment the response
     * returns, [CrisisUiState.situationText] is cleared, so no crisis disclosure lingers
     * in state, and nothing is persisted or logged (FR-03.3, FR-03.4).
     */
    fun submitSituation() {
        val situation = _uiState.value.situationText.trim()
        if (situation.isBlank() || !_uiState.value.aiConfigured) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAiLoading = true, aiResponse = null)
            val setup = preferencesStore.setupState.first()
            val languageCode = setup.bibleLanguage ?: DEFAULT_LANGUAGE
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val result = crisisAiEngine.respond(
                situation = situation,
                languageCode = languageCode,
                isNight = NightWindow.isNight(hour),
                denomination = setup.denomination
            )
            // Discard the situation immediately — never retained on state after the call.
            _uiState.value = _uiState.value.copy(
                isAiLoading = false,
                aiResponse = result,
                situationText = ""
            )
        }
    }

    /** Clears the AI response and the situation text (e.g. on navigate away — FR-03 AC5). */
    fun clearAiResponse() {
        _uiState.value = _uiState.value.copy(aiResponse = null, situationText = "")
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

    private companion object {
        /** Fallback Bible-language ISO code when setup has not recorded one. */
        const val DEFAULT_LANGUAGE = "en"
    }
}
