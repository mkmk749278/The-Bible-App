package com.manna.bible.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manna.bible.domain.canon.CanonEngine
import com.manna.bible.domain.lectionary.LectionaryProvider
import com.manna.bible.domain.model.Denomination
import com.manna.bible.domain.repository.TranslationRepository
import com.manna.bible.domain.translation.Translation
import com.manna.bible.domain.translation.TranslationFilter
import com.manna.bible.domain.usecase.CompleteSetupUseCase
import com.manna.bible.domain.usecase.SetupSelections
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The ordered steps of the denomination-aware first-launch setup flow.
 *
 * The order of the entries defines the forward/back navigation order used by
 * [SetupViewModel.next] and [SetupViewModel.back].
 */
enum class SetupStep {
    WELCOME,
    DENOMINATION,
    UI_LANGUAGE,
    BIBLE_LANGUAGE,
    TRANSLATION,
    LECTIONARY,
    SUMMARY
}

/**
 * Immutable UI state driving the setup flow (Requirements 1, 2, 4, 5, 7, 9, 10, 15).
 *
 * @property step The current setup step.
 * @property denomination Chosen tradition, or null when not yet chosen / skipped (Req 2, 3).
 * @property uiLanguage Selected app UI language code (Req 4).
 * @property bibleLanguage Selected Bible text language code, independent of [uiLanguage] (Req 5).
 * @property bibleTranslationId Selected translation id (Req 5).
 * @property showDeuterocanonical Protestant deuterocanonical visibility toggle (Req 15).
 * @property availableTranslations Translations offered on the translation step (Req 5).
 * @property lectionaryId Lectionary derived from the chosen denomination (Req 9).
 * @property isSaving True while [SetupViewModel.complete] persists the selections.
 * @property completed True once setup has been persisted successfully (Req 1, 10).
 * @property errorMessage Non-null when completion failed; selections are retained (Req 15.4).
 */
data class SetupUiState(
    val step: SetupStep = SetupStep.WELCOME,
    val denomination: Denomination? = null,
    val uiLanguage: String? = null,
    val bibleLanguage: String? = null,
    val bibleTranslationId: String? = null,
    val showDeuterocanonical: Boolean = false,
    val availableTranslations: List<Translation> = emptyList(),
    val lectionaryId: String? = null,
    val isSaving: Boolean = false,
    val completed: Boolean = false,
    val errorMessage: String? = null
) {
    /** True when the current step is the final step of the flow. */
    val isLastStep: Boolean
        get() = step == SetupStep.SUMMARY

    /**
     * Whether the user can advance from the current step. Skipping bypasses this;
     * it only gates the primary "continue" action.
     */
    val canContinue: Boolean
        get() = when (step) {
            SetupStep.DENOMINATION -> denomination != null
            SetupStep.TRANSLATION -> bibleTranslationId != null || availableTranslations.isEmpty()
            else -> true
        }
}

/**
 * Drives the multi-step setup flow, exposing an observable [SetupUiState].
 *
 * Selections update the state immediately; entering the translation step computes the
 * compatible translation list and a suggested default from the catalog, canon profile,
 * and chosen languages. [complete] derives and persists the full configuration via
 * [CompleteSetupUseCase], retaining selections on failure (Requirement 15.4).
 */
@HiltViewModel
class SetupViewModel @Inject constructor(
    private val completeSetupUseCase: CompleteSetupUseCase,
    private val canonEngine: CanonEngine,
    private val lectionaryProvider: LectionaryProvider,
    private val translationFilter: TranslationFilter,
    private val translationRepository: TranslationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    /**
     * Records the chosen [denomination] and recomputes the associated lectionary
     * (Requirement 9).
     */
    fun selectDenomination(denomination: Denomination) {
        _uiState.update {
            it.copy(
                denomination = denomination,
                lectionaryId = lectionaryProvider.lectionaryIdFor(denomination)
            )
        }
    }

    /** Records the selected app UI language (Requirement 4). */
    fun selectUiLanguage(code: String) {
        _uiState.update { it.copy(uiLanguage = code) }
    }

    /** Records the selected Bible text language (Requirement 5). */
    fun selectBibleLanguage(code: String) {
        _uiState.update { it.copy(bibleLanguage = code) }
    }

    /** Records the selected translation (Requirement 5). */
    fun selectTranslation(id: String) {
        _uiState.update { it.copy(bibleTranslationId = id) }
    }

    /** Sets the Protestant deuterocanonical visibility toggle (Requirement 15). */
    fun setShowDeuterocanonical(value: Boolean) {
        _uiState.update { it.copy(showDeuterocanonical = value) }
    }

    /**
     * Advances to the next step in [SetupStep] order. Entering the translation step
     * refreshes the offered translations from the catalog (Requirement 5).
     */
    fun next() {
        val current = _uiState.value.step
        val nextStep = SetupStep.entries.getOrNull(current.ordinal + 1) ?: return
        _uiState.update { it.copy(step = nextStep) }
        if (nextStep == SetupStep.TRANSLATION) {
            refreshTranslations()
        }
    }

    /** Moves back to the previous step in [SetupStep] order. */
    fun back() {
        val current = _uiState.value.step
        val previousStep = SetupStep.entries.getOrNull(current.ordinal - 1) ?: return
        _uiState.update { it.copy(step = previousStep) }
    }

    /**
     * Skips the remaining setup, clearing the denomination so completion applies the
     * show-everything default (Requirements 2.2, 2.3). Jumps to the summary so the user
     * can confirm and complete with defaults.
     */
    fun skip() {
        _uiState.update {
            it.copy(
                denomination = null,
                lectionaryId = null,
                step = SetupStep.SUMMARY
            )
        }
    }

    /**
     * Derives and persists the configuration from the current selections (Requirements 2, 10).
     *
     * On success marks the flow [SetupUiState.completed]. On failure records an
     * [SetupUiState.errorMessage] and retains all selections so the user can retry
     * (Requirement 15.4).
     */
    fun complete() {
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            val state = _uiState.value
            val result = completeSetupUseCase(
                SetupSelections(
                    denomination = state.denomination,
                    uiLanguage = state.uiLanguage,
                    bibleLanguage = state.bibleLanguage,
                    bibleTranslationId = state.bibleTranslationId,
                    showDeuterocanonical = state.showDeuterocanonical
                )
            )
            _uiState.update {
                result.fold(
                    onSuccess = { _ -> it.copy(isSaving = false, completed = true) },
                    onFailure = { error ->
                        it.copy(
                            isSaving = false,
                            completed = false,
                            errorMessage = error.message ?: "Failed to save setup"
                        )
                    }
                )
            }
        }
    }

    /**
     * Recomputes [SetupUiState.availableTranslations] and a suggested default for the
     * current denomination + Bible language using the canon profile and catalog
     * (Requirement 5).
     */
    private fun refreshTranslations() {
        viewModelScope.launch {
            val snapshot = _uiState.value
            val effectiveDenomination = snapshot.denomination ?: Denomination.SHOW_EVERYTHING
            val effectiveBibleLanguage = snapshot.bibleLanguage ?: snapshot.uiLanguage ?: ""
            val catalog = translationRepository.catalog().first()
            val profile = canonEngine.profileFor(effectiveDenomination, effectiveBibleLanguage)
            val filtered = translationFilter.filter(catalog, profile, effectiveBibleLanguage)
            val suggested = snapshot.bibleTranslationId
                ?: translationFilter.suggestedDefault(catalog, profile, effectiveBibleLanguage)?.id
            _uiState.update {
                it.copy(
                    availableTranslations = filtered,
                    bibleTranslationId = suggested
                )
            }
        }
    }
}
