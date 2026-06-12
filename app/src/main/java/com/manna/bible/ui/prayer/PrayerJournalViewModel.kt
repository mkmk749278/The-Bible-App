package com.manna.bible.ui.prayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manna.bible.domain.model.Prayer
import com.manna.bible.domain.model.PrayerStatus
import com.manna.bible.domain.repository.PrayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the Prayer Journal.
 *
 * @property draft Text being composed for a new prayer.
 * @property active Prayers still being prayed, newest first.
 * @property answered Answered prayers — the "Faith Timeline" — newest first.
 */
data class PrayerJournalUiState(
    val draft: String = "",
    val active: List<Prayer> = emptyList(),
    val answered: List<Prayer> = emptyList()
)

/**
 * Drives the Prayer Journal + Faith Timeline: records prayers, marks them answered
 * (and back), and deletes them. Local-first and private.
 *
 * Uses only `androidx.lifecycle` + coroutines/flow — no Android framework types.
 */
@HiltViewModel
class PrayerJournalViewModel @Inject constructor(
    private val repository: PrayerRepository
) : ViewModel() {

    private val draft = MutableStateFlow("")

    val uiState: StateFlow<PrayerJournalUiState> =
        combine(draft, repository.observePrayers()) { draftText, prayers ->
            PrayerJournalUiState(
                draft = draftText,
                active = prayers.filter { it.status == PrayerStatus.ACTIVE },
                answered = prayers.filter { it.status == PrayerStatus.ANSWERED }
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PrayerJournalUiState()
        )

    /** Records the draft text without saving. */
    fun onDraftChange(text: String) {
        draft.value = text
    }

    /** Saves the current draft as a new active prayer and clears the field. */
    fun add() {
        val text = draft.value
        viewModelScope.launch {
            if (repository.add(text) >= 0) draft.value = ""
        }
    }

    /** Marks the prayer [id] answered. */
    fun markAnswered(id: Long) {
        viewModelScope.launch { repository.markAnswered(id) }
    }

    /** Returns an answered prayer [id] to the active list. */
    fun reopen(id: Long) {
        viewModelScope.launch { repository.reopen(id) }
    }

    /** Deletes the prayer [id]. */
    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }
}
