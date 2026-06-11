package com.manna.bible.domain.calendar

import com.manna.bible.domain.usecase.ReadingRef
import java.time.LocalDate
import javax.inject.Inject

/**
 * A single event in the life of Jesus, resolved to a concrete date in some year.
 *
 * @property id Stable, machine-readable id (e.g. "christmas", "easter"). The
 *   presentation layer maps this to a localized name and description, so the domain
 *   stays free of UI strings.
 * @property date The event's date in the requested year. Fixed feasts fall on the
 *   same month/day every year; movable feasts are computed relative to Easter.
 * @property verseRefs Scripture passages associated with the event (the first verse
 *   of each passage), all within the 66-book canon so they resolve in any translation.
 *
 * Pure Kotlin — no Android dependencies.
 */
data class JesusCalendarEntry(
    val id: String,
    val date: LocalDate,
    val verseRefs: List<ReadingRef>
)

/**
 * Supplies the "Jesus Events Calendar" (Phase 1): the key events of Jesus' life
 * mapped onto the calendar for a given year. Fully offline and deterministic.
 *
 * Pure Kotlin — no Android dependencies.
 */
interface JesusEventsProvider {

    /** Returns every event resolved into [year], sorted ascending by date. */
    fun entriesFor(year: Int): List<JesusCalendarEntry>

    /**
     * Returns the next event on or after [today] (looking into next year when the
     * current year's events have all passed), or null only if the calendar is empty.
     */
    fun nextEntry(today: LocalDate): JesusCalendarEntry?
}

/**
 * Default [JesusEventsProvider]. Combines fixed-date feasts (same month/day each
 * year) with movable feasts computed from Western (Gregorian) Easter via the
 * Anonymous Gregorian algorithm (Meeus/Jones/Butcher).
 *
 * Pure Kotlin — depends only on `java.time` and `javax.inject`.
 */
class DefaultJesusEventsProvider @Inject constructor() : JesusEventsProvider {

    override fun entriesFor(year: Int): List<JesusCalendarEntry> {
        val easter = gregorianEaster(year)
        val fixed = FIXED_FEASTS.map { feast ->
            JesusCalendarEntry(feast.id, LocalDate.of(year, feast.month, feast.day), feast.verseRefs)
        }
        val movable = MOVABLE_FEASTS.map { feast ->
            JesusCalendarEntry(feast.id, easter.plusDays(feast.easterOffsetDays.toLong()), feast.verseRefs)
        }
        return (fixed + movable).sortedBy { it.date }
    }

    override fun nextEntry(today: LocalDate): JesusCalendarEntry? {
        val thisYear = entriesFor(today.year)
        return thisYear.firstOrNull { !it.date.isBefore(today) }
            ?: entriesFor(today.year + 1).firstOrNull()
    }

    private data class FixedFeast(
        val id: String,
        val month: Int,
        val day: Int,
        val verseRefs: List<ReadingRef>
    )

    private data class MovableFeast(
        val id: String,
        val easterOffsetDays: Int,
        val verseRefs: List<ReadingRef>
    )

    private companion object {

        /**
         * Computes the date of Western (Gregorian) Easter Sunday for [year] using the
         * Anonymous Gregorian algorithm. Valid for all Gregorian years.
         */
        fun gregorianEaster(year: Int): LocalDate {
            val a = year % 19
            val b = year / 100
            val c = year % 100
            val d = b / 4
            val e = b % 4
            val f = (b + 8) / 25
            val g = (b - f + 1) / 3
            val h = (19 * a + b - d - g + 15) % 30
            val i = c / 4
            val k = c % 4
            val l = (32 + 2 * e + 2 * i - h - k) % 7
            val m = (a + 11 * h + 22 * l) / 451
            val month = (h + l - 7 * m + 114) / 31
            val day = ((h + l - 7 * m + 114) % 31) + 1
            return LocalDate.of(year, month, day)
        }

        val FIXED_FEASTS: List<FixedFeast> = listOf(
            FixedFeast("holy_name", 1, 1, listOf(ReadingRef("LUK", 2, 21))),
            FixedFeast("epiphany", 1, 6, listOf(ReadingRef("MAT", 2, 1))),
            FixedFeast("presentation", 2, 2, listOf(ReadingRef("LUK", 2, 22))),
            FixedFeast("annunciation", 3, 25, listOf(ReadingRef("LUK", 1, 26))),
            FixedFeast("transfiguration", 8, 6, listOf(ReadingRef("MAT", 17, 1))),
            FixedFeast("nativity", 12, 25, listOf(ReadingRef("LUK", 2, 7), ReadingRef("MAT", 1, 18))),
            FixedFeast("holy_innocents", 12, 28, listOf(ReadingRef("MAT", 2, 16)))
        )

        val MOVABLE_FEASTS: List<MovableFeast> = listOf(
            MovableFeast("ash_wednesday", -46, listOf(ReadingRef("MAT", 4, 1))),
            MovableFeast("palm_sunday", -7, listOf(ReadingRef("MAT", 21, 1))),
            MovableFeast("maundy_thursday", -3, listOf(ReadingRef("JHN", 13, 1))),
            MovableFeast("good_friday", -2, listOf(ReadingRef("JHN", 19, 16))),
            MovableFeast("easter", 0, listOf(ReadingRef("MAT", 28, 1))),
            MovableFeast("ascension", 39, listOf(ReadingRef("ACT", 1, 9))),
            MovableFeast("pentecost", 49, listOf(ReadingRef("ACT", 2, 1)))
        )
    }
}
