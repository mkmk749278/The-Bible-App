package com.manna.bible.domain.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Computus — the calculation of Easter and the liturgical anchor dates derived from
 * it. Western (Gregorian) reckoning, valid for all Gregorian years.
 *
 * Pure Kotlin (`java.time` only) so it is fully unit-testable without an emulator and
 * shared by every calendar feature (the Jesus feasts and the liturgical seasons).
 */
object Computus {

    /**
     * Date of Western (Gregorian) Easter Sunday for [year], via the Anonymous
     * Gregorian algorithm (Meeus/Jones/Butcher).
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

    /** Ash Wednesday — 46 days before Easter (start of Lent). */
    fun ashWednesday(year: Int): LocalDate = gregorianEaster(year).minusDays(46)

    /** Palm Sunday — the Sunday before Easter (start of Holy Week). */
    fun palmSunday(year: Int): LocalDate = gregorianEaster(year).minusDays(7)

    /** Good Friday — two days before Easter. */
    fun goodFriday(year: Int): LocalDate = gregorianEaster(year).minusDays(2)

    /** Pentecost — 49 days after Easter (close of Eastertide). */
    fun pentecost(year: Int): LocalDate = gregorianEaster(year).plusDays(49)

    /**
     * The First Sunday of Advent for the Advent that *precedes Christmas in [year]* —
     * i.e. the fourth Sunday before 25 December (the latest Sunday on/before 24 Dec,
     * minus three weeks). This is the start of the liturgical year ending in [year].
     */
    fun adventStart(year: Int): LocalDate {
        val christmasEve = LocalDate.of(year, 12, 24)
        val fourthSunday = christmasEve.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        return fourthSunday.minusWeeks(3)
    }
}
