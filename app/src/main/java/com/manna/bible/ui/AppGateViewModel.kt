package com.manna.bible.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manna.bible.data.preferences.PreferencesStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * First-launch gate state derived from the persisted [com.manna.bible.domain.model.SetupState]
 * (Requirement 1).
 *
 * - [Loading]: the persisted setup state has not been read yet.
 * - [NeedsSetup]: `setupCompleted` is absent or false; the setup flow must run (Req 1.1, 1.4).
 * - [Ready]: `setupCompleted` is true; the main reading experience opens (Req 1.2).
 */
sealed interface GateState {
    data object Loading : GateState
    data object NeedsSetup : GateState
    data object Ready : GateState
}

/**
 * Exposes the first-launch [GateState] by observing [PreferencesStore.setupState].
 *
 * The state starts as [GateState.Loading] until DataStore emits the first value, then resolves to
 * [GateState.Ready] when setup is complete or [GateState.NeedsSetup] otherwise.
 */
@HiltViewModel
class AppGateViewModel @Inject constructor(
    preferencesStore: PreferencesStore,
) : ViewModel() {

    val gateState: StateFlow<GateState> =
        preferencesStore.setupState
            .map { state ->
                if (state.setupCompleted) GateState.Ready else GateState.NeedsSetup
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = GateState.Loading,
            )
}
