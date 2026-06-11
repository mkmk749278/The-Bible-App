package com.manna.bible.ui.reminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.reminder.ReminderTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the daily verse reminder settings.
 *
 * @property enabled Whether the reminder is on.
 * @property time The reminder time of day.
 */
data class ReminderSettingsUiState(
    val enabled: Boolean = false,
    val time: ReminderTime = ReminderTime.DEFAULT
)

/**
 * Reads and writes the daily-reminder preferences. The actual scheduling is driven
 * by `ReminderCoordinator`, which observes the same preferences — so this ViewModel
 * only has to persist the user's choices.
 *
 * Uses only `androidx.lifecycle` + coroutines/flow — no Android framework types.
 */
@HiltViewModel
class ReminderSettingsViewModel @Inject constructor(
    private val preferencesStore: PreferencesStore
) : ViewModel() {

    val uiState: StateFlow<ReminderSettingsUiState> = combine(
        preferencesStore.dailyReminderEnabled,
        preferencesStore.dailyReminderTime
    ) { enabled, time ->
        ReminderSettingsUiState(enabled = enabled, time = ReminderTime.parse(time))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReminderSettingsUiState()
    )

    /** Turns the reminder on or off. */
    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesStore.setDailyReminderEnabled(enabled) }
    }

    /** Sets the reminder time of day. */
    fun setTime(hour: Int, minute: Int) {
        viewModelScope.launch { preferencesStore.setDailyReminderTime(ReminderTime(hour, minute).format()) }
    }
}
