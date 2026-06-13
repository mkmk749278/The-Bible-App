package com.manna.bible.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.calendar.JesusEventsProvider
import com.manna.bible.domain.calendar.LectionaryReading
import com.manna.bible.domain.calendar.LectionaryReadingsProvider
import com.manna.bible.domain.calendar.LiturgicalCalendarProvider
import com.manna.bible.domain.calendar.LiturgicalColor
import com.manna.bible.domain.calendar.LiturgicalSeason
import com.manna.bible.domain.calendar.ReadingKind
import com.manna.bible.domain.canon.CanonEngine
import com.manna.bible.domain.model.CanonProfile
import com.manna.bible.domain.model.Denomination
import com.manna.bible.domain.share.ShareReferenceFormatter
import com.manna.bible.domain.usecase.ReadingRef
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/** One cell of the month grid. */
data class CalendarDayCell(
    val date: LocalDate,
    val color: LiturgicalColor,
    val isToday: Boolean,
    val isSunday: Boolean,
    val isFast: Boolean,
    val isFeast: Boolean
) {
    val dayOfMonth: Int get() = date.dayOfMonth
}

/** A formatted appointed reading for the detail card. */
data class ReadingRow(
    val kind: ReadingKind,
    val reference: String,
    val osisRef: String
)

/** The currently selected day, expanded for the detail card. */
data class SelectedDay(
    val date: LocalDate,
    val season: LiturgicalSeason,
    val color: LiturgicalColor,
    val isFast: Boolean,
    val feastId: String?,
    val osisRef: String?,
    val readings: List<ReadingRow> = emptyList()
)

/**
 * UI state for the liturgical Calendar tab.
 *
 * @property days the days of [year]/[month] in order (1st → last).
 * @property leadingBlanks empty cells before the 1st so the grid starts on Sunday.
 * @property selected the day whose detail card is shown.
 */
data class LiturgicalCalendarUiState(
    val year: Int = 0,
    val month: Int = 0,
    val days: List<CalendarDayCell> = emptyList(),
    val leadingBlanks: Int = 0,
    val selected: SelectedDay? = null,
    val today: LocalDate = LocalDate.now(),
    val isLoading: Boolean = true
)

/**
 * Drives the month-grid Calendar from the [LiturgicalCalendarProvider], coloured and
 * fast-marked for the user's chosen tradition (observed from setup). For a selected
 * day it also resolves the appointed [LectionaryReadingsProvider] readings and formats
 * each reference in the tradition's naming/numbering convention via
 * [ShareReferenceFormatter]. Feast readings can be opened in the reader.
 *
 * Uses only `androidx.lifecycle` + coroutines/flow — no Android framework types.
 */
@HiltViewModel
class LiturgicalCalendarViewModel @Inject constructor(
    private val provider: LiturgicalCalendarProvider,
    private val jesusEvents: JesusEventsProvider,
    private val lectionary: LectionaryReadingsProvider,
    private val referenceFormatter: ShareReferenceFormatter,
    private val canonEngine: CanonEngine,
    preferencesStore: PreferencesStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiturgicalCalendarUiState())
    val uiState: StateFlow<LiturgicalCalendarUiState> = _uiState.asStateFlow()

    private val shownMonth = MutableStateFlow(YearMonth.now())
    private val selectedDate = MutableStateFlow(LocalDate.now())

    private data class CalContext(
        val denomination: Denomination,
        val language: String,
        val profile: CanonProfile
    )

    private val context = preferencesStore.setupState
        .map { setup ->
            val denomination = setup.denomination ?: Denomination.PROTESTANT_OTHER
            val language = setup.bibleLanguage ?: DEFAULT_LANGUAGE
            CalContext(denomination, language, canonEngine.profileFor(denomination, language))
        }
        .distinctUntilChanged()

    init {
        viewModelScope.launch {
            combine(context, shownMonth, selectedDate) { ctx, ym, selected ->
                build(ctx, ym, selected)
            }.collect { _uiState.value = it }
        }
    }

    /** Selects [date] (updates the detail card). */
    fun selectDate(date: LocalDate) {
        selectedDate.value = date
    }

    /** Moves to the previous month, selecting its first day. */
    fun previousMonth() = goToMonth(shownMonth.value.minusMonths(1))

    /** Moves to the next month, selecting its first day. */
    fun nextMonth() = goToMonth(shownMonth.value.plusMonths(1))

    /** Jumps back to the current month and selects today. */
    fun goToToday() {
        val today = LocalDate.now()
        shownMonth.value = YearMonth.from(today)
        selectedDate.value = today
    }

    private fun goToMonth(ym: YearMonth) {
        shownMonth.value = ym
        selectedDate.value = ym.atDay(1)
    }

    private fun build(
        ctx: CalContext,
        ym: YearMonth,
        selected: LocalDate
    ): LiturgicalCalendarUiState {
        val today = LocalDate.now()
        val days = provider.month(ym.year, ym.monthValue, ctx.denomination).map { day ->
            CalendarDayCell(
                date = day.date,
                color = day.color,
                isToday = day.date == today,
                isSunday = day.isSunday,
                isFast = day.isFast,
                isFeast = day.feastId != null
            )
        }
        // Sunday-first grid: Monday=1 … Sunday=7 → 1 … 0.
        val leadingBlanks = ym.atDay(1).dayOfWeek.value % 7

        val selectedDay = provider.dayFor(selected, ctx.denomination)
        val osisRef = jesusEvents.entriesFor(selected.year)
            .firstOrNull { it.date == selected }
            ?.verseRefs?.firstOrNull()?.format()
        val readings = lectionary.readingsFor(selected, ctx.denomination)
            .map { formatReading(ctx, it) }

        return LiturgicalCalendarUiState(
            year = ym.year,
            month = ym.monthValue,
            days = days,
            leadingBlanks = leadingBlanks,
            selected = SelectedDay(
                date = selected,
                season = selectedDay.season,
                color = selectedDay.color,
                isFast = selectedDay.isFast,
                feastId = selectedDay.feastId,
                osisRef = osisRef,
                readings = readings
            ),
            today = today,
            isLoading = false
        )
    }

    private fun formatReading(ctx: CalContext, reading: LectionaryReading): ReadingRow {
        val reference = if (reading.endVerse != null) {
            referenceFormatter.formatRange(
                ctx.profile, ctx.language, reading.osisId, reading.chapter, reading.verse, reading.endVerse
            )
        } else {
            referenceFormatter.formatReference(
                ctx.profile, ctx.language, reading.osisId, reading.chapter, reading.verse
            )
        }
        return ReadingRow(
            kind = reading.kind,
            reference = reference,
            osisRef = ReadingRef(reading.osisId, reading.chapter, reading.verse).format()
        )
    }

    private companion object {
        const val DEFAULT_LANGUAGE = "en"
    }
}
