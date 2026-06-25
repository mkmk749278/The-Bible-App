package com.manna.bible.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.model.CanonType
import com.manna.bible.domain.model.Denomination
import com.manna.bible.domain.model.SetupState
import com.manna.bible.domain.usecase.ApplyDenominationChangeUseCase
import com.manna.bible.domain.usecase.CanonSwitchImpact
import com.manna.bible.domain.usecase.ResolveCanonSwitchImpactUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Immutable UI state for the "Denomination & Languages" settings section
 * (Requirements 11, 12, 13, 15, 16).
 *
 * The persisted fields are sourced from [PreferencesStore.setupState]; the `pending*`
 * fields and [errorMessage] are transient interaction state used to drive the
 * canon-switch warning dialog (Req 12) and surface failures.
 *
 * @property denomination The currently persisted tradition.
 * @property canonType The currently persisted canon.
 * @property bibleLanguage The persisted Bible text language code (Req 13).
 * @property showDeuterocanonical The Protestant deuterocanonical visibility toggle (Req 15).
 * @property pendingDenomination A denomination change awaiting user confirmation (Req 12).
 * @property pendingImpact The impact of the pending change, populated when a warning is needed.
 * @property errorMessage Non-null when the last change failed.
 */
data class DenominationSettingsUiState(
    val denomination: Denomination? = null,
    val canonType: CanonType? = null,
    val bibleLanguage: String? = null,
    val showDeuterocanonical: Boolean = false,
    val pendingDenomination: Denomination? = null,
    val pendingImpact: CanonSwitchImpact? = null,
    val errorMessage: String? = null
) {
    /** Deuterocanonical toggle is only offered for the Protestant 66-book canon (Req 15.1). */
    val showDeuterocanonicalToggle: Boolean
        get() = canonType == CanonType.PROTESTANT_66

    /** Whether the canon-switch warning dialog should be shown (Req 12.1). */
    val showCanonSwitchDialog: Boolean
        get() = pendingImpact != null && pendingDenomination != null
}

/**
 * Backs the Settings "Denomination & Languages" section, letting the user change their
 * denomination, UI/Bible language, and the Protestant deuterocanonical toggle after the
 * initial setup (Requirements 11, 12, 13, 15, 16).
 *
 * Denomination changes are routed through [ResolveCanonSwitchImpactUseCase] first: if the
 * candidate canon would hide annotated books, the change is held as
 * [DenominationSettingsUiState.pendingDenomination] until the user confirms via the warning
 * dialog (Req 12). Confirmed changes are applied with [ApplyDenominationChangeUseCase], which
 * preserves all stored annotations (Req 11). Language changes apply immediately.
 */
@HiltViewModel
class DenominationSettingsViewModel @Inject constructor(
    private val preferencesStore: PreferencesStore,
    private val applyDenominationChangeUseCase: ApplyDenominationChangeUseCase,
    private val resolveCanonSwitchImpactUseCase: ResolveCanonSwitchImpactUseCase
) : ViewModel() {

    /** Transient, non-persisted interaction state layered on top of the persisted setup state. */
    private data class InteractionState(
        val pendingDenomination: Denomination? = null,
        val pendingImpact: CanonSwitchImpact? = null,
        val errorMessage: String? = null
    )

    private val interaction = MutableStateFlow(InteractionState())

    /**
     * Latest persisted [SetupState], kept so language updates can save a copy with the other
     * fields preserved (Req 11.4). Updated by the [init] collector below.
     */
    @Volatile
    private var latestSetupState: SetupState? = null

    init {
        viewModelScope.launch {
            preferencesStore.setupState.collect { latestSetupState = it }
        }
    }

    val uiState: StateFlow<DenominationSettingsUiState> =
        combine(preferencesStore.setupState, interaction) { setup, inter ->
            latestSetupState = setup
            DenominationSettingsUiState(
                denomination = setup.denomination,
                canonType = setup.canonType,
                bibleLanguage = setup.bibleLanguage,
                showDeuterocanonical = setup.showDeuterocanonical,
                pendingDenomination = inter.pendingDenomination,
                pendingImpact = inter.pendingImpact,
                errorMessage = inter.errorMessage
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DenominationSettingsUiState()
        )

    /**
     * Evaluates switching to [target]. If the candidate canon would hide annotated books,
     * holds the change as pending and surfaces the warning dialog (Req 12.1); otherwise the
     * change is applied immediately.
     */
    fun requestDenominationChange(target: Denomination) {
        viewModelScope.launch {
            val bibleLanguage = latestSetupState?.bibleLanguage.orEmpty()
            val impact = resolveCanonSwitchImpactUseCase(target, bibleLanguage)
            if (impact.hasImpact) {
                interaction.update {
                    it.copy(
                        pendingDenomination = target,
                        pendingImpact = impact,
                        errorMessage = null
                    )
                }
            } else {
                applyChange(target)
            }
        }
    }

    /** Applies the pending denomination change after the user confirms the warning (Req 11, 12). */
    fun confirmDenominationChange() {
        val target = interaction.value.pendingDenomination ?: return
        viewModelScope.launch { applyChange(target) }
    }

    /** Dismisses the pending denomination change, leaving the current canon unchanged. */
    fun cancelDenominationChange() {
        interaction.update { it.copy(pendingDenomination = null, pendingImpact = null) }
    }

    /** Applies [target] via the use case, clearing pending state and recording any failure. */
    private suspend fun applyChange(target: Denomination) {
        val bibleLanguage = latestSetupState?.bibleLanguage.orEmpty()
        val result = applyDenominationChangeUseCase(target, bibleLanguage)
        interaction.update {
            result.fold(
                onSuccess = { _ ->
                    it.copy(pendingDenomination = null, pendingImpact = null, errorMessage = null)
                },
                onFailure = { error ->
                    it.copy(
                        pendingDenomination = null,
                        pendingImpact = null,
                        errorMessage = error.message ?: "Failed to change denomination"
                    )
                }
            )
        }
    }

    /** Persists a new Bible language immediately, preserving all other setup fields (Req 13). */
    fun changeBibleLanguage(code: String) {
        val current = latestSetupState ?: return
        viewModelScope.launch {
            runCatching { preferencesStore.saveSetup(current.copy(bibleLanguage = code)) }
                .onFailure { error -> interaction.update { it.copy(errorMessage = error.message) } }
        }
    }

    /** Toggles deuterocanonical visibility for the Protestant canon (Req 15). */
    fun setShowDeuterocanonical(value: Boolean) {
        viewModelScope.launch {
            runCatching { preferencesStore.setShowDeuterocanonical(value) }
                .onFailure { error -> interaction.update { it.copy(errorMessage = error.message) } }
        }
    }
}
