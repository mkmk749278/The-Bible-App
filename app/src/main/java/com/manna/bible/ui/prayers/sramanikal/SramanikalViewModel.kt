package com.manna.bible.ui.prayers.sramanikal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.devotion.SramanikalJourney
import com.manna.bible.domain.usecase.ResolveVerseTextUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * UI state for the 40-day Sramanikal (memorial) journey.
 *
 * @property isLoading True until the observance state resolves.
 * @property started True once an observance has been begun.
 * @property name The name of the one being remembered.
 * @property currentDay The day unlocked today (1..total), 0 before starting.
 * @property viewedDay The day currently shown (1..currentDay).
 * @property totalDays Total days in the observance (forty).
 * @property reference Display reference for the viewed day's passage.
 * @property verseText Passage text in the active translation, or null when unavailable.
 * @property osisRef Canonical reference for "read in context".
 */
data class SramanikalUiState(
    val isLoading: Boolean = true,
    val started: Boolean = false,
    val name: String = "",
    val currentDay: Int = 0,
    val viewedDay: Int = 0,
    val totalDays: Int = 40,
    val reference: String = "",
    val verseText: String? = null,
    val osisRef: String? = null
) {
    val canGoPrevious: Boolean get() = viewedDay > 1
    val canGoNext: Boolean get() = viewedDay < currentDay
}

/**
 * Drives the Sramanikal: a date-paced 40-day memorial observance for a departed loved
 * one. The unlocked day is derived from the persisted start date and today, so the
 * journey gently reveals one day at a time; the family can revisit any earlier day.
 * Verse text comes from the active translation (offline) via [ResolveVerseTextUseCase];
 * the daily reflection and the prayer of commendation are supplied by the screen as
 * string resources.
 *
 * Uses only `androidx.lifecycle` + coroutines — no Android framework types.
 */
@HiltViewModel
class SramanikalViewModel @Inject constructor(
    private val journey: SramanikalJourney,
    private val preferencesStore: PreferencesStore,
    private val resolveVerseText: ResolveVerseTextUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SramanikalUiState(totalDays = journey.dayCount))
    val uiState: StateFlow<SramanikalUiState> = _uiState.asStateFlow()

    private var currentDay = 0
    private var viewedDay = 0
    private var name = ""

    init {
        refresh()
    }

    /** Re-reads the observance state and loads the active day. */
    fun refresh() {
        viewModelScope.launch {
            val start = preferencesStore.sramanikalStartEpochDay.first()
            name = preferencesStore.sramanikalName.first()
            if (start < 0) {
                _uiState.value = SramanikalUiState(
                    isLoading = false,
                    started = false,
                    totalDays = journey.dayCount
                )
                return@launch
            }
            currentDay = ((LocalDate.now().toEpochDay() - start) + 1)
                .coerceIn(1L, journey.dayCount.toLong())
                .toInt()
            viewedDay = currentDay
            loadDay()
        }
    }

    /** Begins an observance today, remembering [forName]. */
    fun begin(forName: String) {
        viewModelScope.launch {
            preferencesStore.setSramanikal(LocalDate.now().toEpochDay(), forName.trim())
            refresh()
        }
    }

    /** Ends the active observance. */
    fun end() {
        viewModelScope.launch {
            preferencesStore.clearSramanikal()
            refresh()
        }
    }

    /** Moves to the next unlocked day, if any. */
    fun next() {
        if (viewedDay < currentDay) {
            viewedDay++
            viewModelScope.launch { loadDay() }
        }
    }

    /** Moves to the previous day, if any. */
    fun previous() {
        if (viewedDay > 1) {
            viewedDay--
            viewModelScope.launch { loadDay() }
        }
    }

    private suspend fun loadDay() {
        val ref = journey.verseFor(viewedDay)
        val resolved = ref?.let { resolveVerseText(it) }
        _uiState.value = SramanikalUiState(
            isLoading = false,
            started = true,
            name = name,
            currentDay = currentDay,
            viewedDay = viewedDay,
            totalDays = journey.dayCount,
            reference = resolved?.reference ?: "",
            verseText = resolved?.text,
            osisRef = resolved?.osisRef ?: ref?.format()
        )
    }
}
