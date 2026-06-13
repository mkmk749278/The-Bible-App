package com.manna.bible.domain.calendar

import com.manna.bible.domain.model.Denomination
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

/**
 * Default [LiturgicalCalendarProvider].
 *
 * Seasons are derived from the year's Easter (via [Computus]); feast markers reuse
 * [JesusEventsProvider] so a day's feast id, name, and readings stay consistent with
 * the Jesus-events surface. Fast days follow each tradition's rhythm:
 * - Western traditions (Catholic, CSI, Mar Thoma, other Protestant): Ash Wednesday,
 *   Good Friday, and the Fridays of Lent.
 * - Orthodox: the above plus the year-round Wednesday/Friday fast (simplified — real
 *   Orthodox practice has fast-free weeks).
 * - "Show everything" follows no tradition, so no fasts are marked.
 *
 * Pure Kotlin — depends only on `java.time` and `javax.inject`.
 */
class DefaultLiturgicalCalendarProvider @Inject constructor(
    private val jesusEvents: JesusEventsProvider
) : LiturgicalCalendarProvider {

    override fun dayFor(date: LocalDate, denomination: Denomination): LiturgicalDay {
        val year = date.year
        val easter = Computus.gregorianEaster(year)
        val ashWednesday = easter.minusDays(46)
        val palmSunday = easter.minusDays(7)
        val goodFriday = easter.minusDays(2)
        val pentecost = easter.plusDays(49)
        val epiphany = LocalDate.of(year, 1, 6)
        val christmas = LocalDate.of(year, 12, 25)
        val adventStart = Computus.adventStart(year)

        val season = when {
            date.isBefore(epiphany) -> LiturgicalSeason.CHRISTMAS          // Jan 1–5
            date.isBefore(ashWednesday) -> LiturgicalSeason.EPIPHANY       // Jan 6 → Shrove Tue
            date.isBefore(palmSunday) -> LiturgicalSeason.LENT             // Lent (pre Holy Week)
            date.isBefore(easter) -> LiturgicalSeason.HOLY_WEEK            // Palm Sun → Holy Sat
            !date.isAfter(pentecost) -> LiturgicalSeason.EASTER            // Easter → Pentecost
            date.isBefore(adventStart) -> LiturgicalSeason.ORDINARY        // after Pentecost
            date.isBefore(christmas) -> LiturgicalSeason.ADVENT            // Advent → Dec 24
            else -> LiturgicalSeason.CHRISTMAS                             // Dec 25–31
        }

        val color = when {
            date == goodFriday || date == palmSunday || date == pentecost -> LiturgicalColor.RED
            season == LiturgicalSeason.ADVENT ||
                season == LiturgicalSeason.LENT ||
                season == LiturgicalSeason.HOLY_WEEK -> LiturgicalColor.VIOLET
            season == LiturgicalSeason.CHRISTMAS || season == LiturgicalSeason.EASTER ->
                LiturgicalColor.WHITE
            season == LiturgicalSeason.EPIPHANY ->
                if (date == epiphany) LiturgicalColor.WHITE else LiturgicalColor.GREEN
            else -> LiturgicalColor.GREEN // Ordinary Time
        }

        val feastId = jesusEvents.entriesFor(year).firstOrNull { it.date == date }?.id

        return LiturgicalDay(
            date = date,
            season = season,
            color = color,
            isSunday = date.dayOfWeek == DayOfWeek.SUNDAY,
            isFast = isFast(date, easter, ashWednesday, goodFriday, denomination),
            feastId = feastId
        )
    }

    override fun month(year: Int, month: Int, denomination: Denomination): List<LiturgicalDay> {
        val first = LocalDate.of(year, month, 1)
        return (0 until first.lengthOfMonth()).map { offset ->
            dayFor(first.plusDays(offset.toLong()), denomination)
        }
    }

    private fun isFast(
        date: LocalDate,
        easter: LocalDate,
        ashWednesday: LocalDate,
        goodFriday: LocalDate,
        denomination: Denomination
    ): Boolean {
        if (denomination == Denomination.SHOW_EVERYTHING) return false
        val inLent = !date.isBefore(ashWednesday) && date.isBefore(easter)
        val lentenFast = date == ashWednesday ||
            date == goodFriday ||
            (inLent && date.dayOfWeek == DayOfWeek.FRIDAY)
        return when (denomination) {
            Denomination.ORTHODOX ->
                lentenFast ||
                    date.dayOfWeek == DayOfWeek.WEDNESDAY ||
                    date.dayOfWeek == DayOfWeek.FRIDAY
            else -> lentenFast
        }
    }
}
