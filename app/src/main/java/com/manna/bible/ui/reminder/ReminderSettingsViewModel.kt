package com.manna.bible.ui.reminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.reminder.ReminderTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the reminder settings.
 *
 * @property enabled Whether reminders are on (the master switch).
 * @property times The reminder times (prayer-bell schedule), sorted by time of day —
 *   always at least one for display.
 */
data class ReminderSettingsUiState(
    val enabled: Boolean = false,
    val times: List<ReminderTime> = listOf(ReminderTime.DEFAULT)
)

/**
 * Reads and writes the reminder preferences. Scheduling is driven by
 * `ReminderCoordinator`, which observes the same preferences — so this ViewModel only
 * persists the user's choices: the master switch and the list of times.
 *
 * Uses only `androidx.lifecycle` + coroutines/flow — no Android framework types.
 */
@HiltViewModel
class ReminderSettingsViewModel @Inject constructor(
    private val preferencesStore: PreferencesStore
) : ViewModel() {

    val uiState: StateFlow<ReminderSettingsUiState> = combine(
        preferencesStore.dailyReminderEnabled,
        preferencesStore.dailyReminderTimes
    ) { enabled, csv ->
        val times = ReminderTime.parseList(csv).ifEmpty { listOf(ReminderTime.DEFAULT) }
        ReminderSettingsUiState(enabled = enabled, times = times)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReminderSettingsUiState()
    )

    /** Turns reminders on or off. */
    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesStore.setDailyReminderEnabled(enabled) }
    }

    /** Adds a reminder time (ignored if that time is already in the list). */
    fun addTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            val current = currentTimes()
            val added = ReminderTime(hour, minute)
            if (added !in current) persist(current + added)
        }
    }

    /** Removes a reminder time. The last remaining time is kept (use the switch to stop). */
    fun removeTime(time: ReminderTime) {
        viewModelScope.launch {
            val current = currentTimes()
            if (current.size > 1) persist(current - time)
        }
    }

    private suspend fun currentTimes(): List<ReminderTime> =
        ReminderTime.parseList(preferencesStore.dailyReminderTimes.first())
            .ifEmpty { listOf(ReminderTime.DEFAULT) }

    private suspend fun persist(times: List<ReminderTime>) {
        preferencesStore.setDailyReminderTimes(ReminderTime.formatList(times))
    }
}
