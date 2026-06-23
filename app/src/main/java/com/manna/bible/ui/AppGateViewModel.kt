package com.manna.bible.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manna.bible.data.preferences.PreferencesStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * First-launch gate state derived from the persisted [com.manna.bible.domain.model.SetupState]
 * (Requirement 1) plus Stealth (Persecution) Mode.
 *
 * - [Loading]: the persisted setup state has not been read yet.
 * - [NeedsSetup]: `setupCompleted` is absent or false; the setup flow must run (Req 1.1, 1.4).
 * - [Locked]: Stealth Mode is armed and the disguised lock has not been cleared this session.
 * - [Ready]: setup is complete (and unlocked); the main reading experience opens (Req 1.2).
 */
sealed interface GateState {
    data object Loading : GateState
    data object NeedsSetup : GateState
    data object Locked : GateState
    data object Ready : GateState
}

/**
 * Exposes the first-launch [GateState] by observing [PreferencesStore.setupState] and the
 * Stealth-Mode credential.
 *
 * Starts as [GateState.Loading] until DataStore emits. When setup is complete but Stealth Mode
 * is armed with a PIN, the gate is [GateState.Locked] until [unlock] is called for the session
 * (after the disguised lock screen accepts the PIN); otherwise it resolves to [GateState.Ready]
 * or [GateState.NeedsSetup].
 */
@HiltViewModel
class AppGateViewModel @Inject constructor(
    preferencesStore: PreferencesStore,
) : ViewModel() {

    /** Transient, session-only flag: true once the user clears the stealth lock. */
    private val unlocked = MutableStateFlow(false)

    val gateState: StateFlow<GateState> =
        combine(
            preferencesStore.setupState,
            preferencesStore.stealthEnabled,
            preferencesStore.stealthPinCredential,
            unlocked,
        ) { setup, stealthOn, credential, isUnlocked ->
            when {
                !setup.setupCompleted -> GateState.NeedsSetup
                stealthOn && credential.isNotBlank() && !isUnlocked -> GateState.Locked
                else -> GateState.Ready
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GateState.Loading,
        )

    /** Clears the stealth lock for this session after the correct PIN is entered. */
    fun unlock() {
        unlocked.value = true
    }
}
