package com.manna.bible.ui.calendar

import androidx.lifecycle.ViewModel
import com.manna.bible.domain.calendar.JesusEventsProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import javax.inject.Inject

/**
 * A single Jesus-event row for display.
 *
 * @property id Stable event id; mapped to a localized name/description by the screen.
 * @property date The event's date this year.
 * @property osisRef First linked passage as `OSIS.CHAPTER.VERSE`, for "read" navigation.
 * @property isNext True for the next upcoming event (highlighted in the list).
 */
data class CalendarRow(
    val id: String,
    val date: LocalDate,
    val osisRef: String?,
    val isNext: Boolean
)

/**
 * UI state for the Jesus Events Calendar.
 *
 * @property year The calendar year being shown.
 * @property rows Events for [year], sorted ascending by date.
 * @property isLoading True until the events resolve.
 */
data class JesusCalendarUiState(
    val year: Int = 0,
    val rows: List<CalendarRow> = emptyList(),
    val isLoading: Boolean = true
)

/**
 * Resolves the current year's Jesus events from [JesusEventsProvider] and flags the
 * next upcoming one. Fully offline and synchronous — the provider is pure domain.
 *
 * Uses only `androidx.lifecycle` + coroutines/flow — no Android framework types.
 */
@HiltViewModel
class JesusCalendarViewModel @Inject constructor(
    private val provider: JesusEventsProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(JesusCalendarUiState())
    val uiState: StateFlow<JesusCalendarUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /** (Re)computes the calendar for the current year. */
    fun load() {
        val today = LocalDate.now()
        val next = provider.nextEntry(today)
        val rows = provider.entriesFor(today.year).map { entry ->
            CalendarRow(
                id = entry.id,
                date = entry.date,
                osisRef = entry.verseRefs.firstOrNull()?.format(),
                isNext = next != null && entry.id == next.id && entry.date == next.date
            )
        }
        _uiState.value = JesusCalendarUiState(year = today.year, rows = rows, isLoading = false)
    }
}
