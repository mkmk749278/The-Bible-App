package com.manna.bible.ui.card

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.share.VerseRecommendation
import com.manna.bible.domain.share.VerseRecommendationEngine
import com.manna.bible.domain.share.VerseRecommendationRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the Context-Aware Verse Cards screen (F-05).
 *
 * @property situationText The occasion/situation being typed. Transient UI state only —
 *   it is not persisted to Room, DataStore, or logs, mirroring the Crisis AI privacy
 *   discipline; editing it clears any previously shown result.
 * @property result The latest recommendation, or null before one is requested.
 * @property isLoading True while a recommendation request is in flight.
 * @property engineConfigured True when the engine has its credentials; gates whether the
 *   input is interactive vs. shows the offline hint (the compile-time flag gates the
 *   surface itself at the UI layer).
 */
data class VerseRecommendationUiState(
    val situationText: String = "",
    val result: VerseRecommendation? = null,
    val isLoading: Boolean = false,
    val engineConfigured: Boolean = false
)

/**
 * Drives the Context-Aware Verse Cards screen (F-05): the user describes an occasion, the
 * engine recommends a passage (with its text resolved from the active local translation)
 * and a short personal message, and the result is rendered with the existing card pipeline.
 *
 * Reads the denomination and Bible language from [PreferencesStore.setupState] at call
 * time. The situation text lives only in transient state and is never persisted.
 *
 * Uses only `androidx.lifecycle` + coroutines/flow — no Android framework types.
 */
@HiltViewModel
class VerseRecommendationViewModel @Inject constructor(
    private val engine: VerseRecommendationEngine,
    private val preferencesStore: PreferencesStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(VerseRecommendationUiState())
    val uiState: StateFlow<VerseRecommendationUiState> = _uiState.asStateFlow()

    init {
        // The compile-time flag gates the on-screen surface; this engine-config signal
        // gates whether the input is interactive vs. shows the offline hint (F-05).
        _uiState.value = _uiState.value.copy(engineConfigured = engine.isConfigured)
    }

    /** Records the latest situation text; editing clears any previously shown result. */
    fun onSituationChange(text: String) {
        _uiState.value = _uiState.value.copy(situationText = text, result = null)
    }

    /**
     * Requests a recommendation for the current situation. No-op when the text is blank
     * or the engine is unconfigured. The situation is read into a local `val` for the
     * duration of the call and never copied onto the ViewModel beyond the transient input.
     */
    fun recommend() {
        val situation = _uiState.value.situationText.trim()
        if (situation.isBlank() || !_uiState.value.engineConfigured) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, result = null)
            val setup = preferencesStore.setupState.first()
            val languageCode = setup.bibleLanguage ?: DEFAULT_LANGUAGE
            val result = engine.recommend(
                VerseRecommendationRequest(
                    situationText = situation,
                    languageCode = languageCode,
                    denomination = setup.denomination
                )
            )
            _uiState.value = _uiState.value.copy(isLoading = false, result = result)
        }
    }

    /** Clears the situation text and the result (e.g. when starting over). */
    fun clear() {
        _uiState.value = _uiState.value.copy(situationText = "", result = null)
    }

    private companion object {
        /** Fallback Bible-language ISO code when setup has not recorded one. */
        const val DEFAULT_LANGUAGE = "en"
    }
}
