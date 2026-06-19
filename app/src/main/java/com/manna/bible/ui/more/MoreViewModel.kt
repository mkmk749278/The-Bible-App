package com.manna.bible.ui.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manna.bible.data.preferences.PreferencesStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the More hub's settings.
 *
 * @property simplifiedMode Whether Simplified (Elder / Oral Bible) Mode is on — an
 *   audio-first, enlarged-control, slower-read-aloud presentation (Req 14.5).
 */
data class MoreUiState(
    val simplifiedMode: Boolean = false
)

/**
 * Backs the settings controls on the More hub. Today that is the Simplified Mode
 * toggle, which previously could only be chosen during onboarding — leaving no way
 * for an elderly user (or a caregiver setting up their phone) to turn it on later.
 *
 * Enabling Simplified Mode also turns on audio continuous-play, so read-aloud keeps
 * moving from one chapter to the next without a tap — the behaviour an audio-first,
 * oral-Bible user expects. Disabling it leaves continuous-play untouched (the user
 * may still want it on its own).
 *
 * Uses only `androidx.lifecycle` + coroutines/flow — no Android framework types.
 */
@HiltViewModel
class MoreViewModel @Inject constructor(
    private val preferencesStore: PreferencesStore
) : ViewModel() {

    val uiState: StateFlow<MoreUiState> = preferencesStore.simplifiedMode
        .map { MoreUiState(simplifiedMode = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MoreUiState()
        )

    /** Turns Simplified Mode on or off; enabling it also enables continuous play. */
    fun setSimplifiedMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesStore.setSimplifiedMode(enabled)
            if (enabled) preferencesStore.setContinuousPlay(true)
        }
    }
}
