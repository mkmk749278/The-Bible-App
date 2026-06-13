package com.manna.bible.domain.calendar

import com.manna.bible.domain.model.Denomination
import java.time.LocalDate

/**
 * A season of the (Western) liturgical year. Ordinary Time is represented as a single
 * season; the presentation layer maps each to a localized name.
 */
enum class LiturgicalSeason {
    ADVENT,
    CHRISTMAS,
    EPIPHANY,
    LENT,
    HOLY_WEEK,
    EASTER,
    ORDINARY
}

/**
 * The liturgical colour of a day — the traditional vestment colour. The UI maps each
 * to a palette token (no hardcoded hex in the domain).
 */
enum class LiturgicalColor {
    VIOLET, // Advent, Lent
    WHITE,  // Christmas, Easter, feasts of the Lord
    GREEN,  // Ordinary Time, Epiphany weeks
    RED     // Good Friday, Pentecost, Palm Sunday
}

/**
 * The resolved liturgical character of a single calendar date: its season and colour,
 * whether it is a Sunday, whether it is a fast day for the chosen tradition, and the
 * feast that falls on it (if any) — keyed by [feastId], which matches the ids from
 * [JesusEventsProvider] so the UI can reuse the same localized names and readings.
 *
 * Pure data — no Android dependencies.
 */
data class LiturgicalDay(
    val date: LocalDate,
    val season: LiturgicalSeason,
    val color: LiturgicalColor,
    val isSunday: Boolean,
    val isFast: Boolean,
    val feastId: String? = null
)

/**
 * Resolves the liturgical character of dates for a chosen [Denomination] — seasons,
 * colours, feast markers, and fast days — so the Calendar tab can render a real
 * month grid (highlighted feasts, marked fasting days) and day details.
 *
 * Western (Gregorian) reckoning. Orthodox dating differs (often Julian, with its own
 * fasting structure); for now the Orthodox tradition is approximated with the Western
 * seasons plus its Wednesday/Friday fasting rhythm.
 *
 * Pure Kotlin — no Android dependencies, fully unit-testable.
 */
interface LiturgicalCalendarProvider {

    /** The liturgical character of [date] for [denomination]. */
    fun dayFor(date: LocalDate, denomination: Denomination): LiturgicalDay

    /**
     * Every day of the given calendar month (1-based [month]) for [denomination],
     * in date order — ready to lay out as a grid.
     */
    fun month(year: Int, month: Int, denomination: Denomination): List<LiturgicalDay>
}
