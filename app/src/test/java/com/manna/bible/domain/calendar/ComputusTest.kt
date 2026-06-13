package com.manna.bible.domain.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** Unit tests for [Computus] — Western Easter and the anchors derived from it. */
class ComputusTest {

    @Test
    @DisplayName("Gregorian Easter matches known dates")
    fun easterKnownDates() {
        assertEquals(LocalDate.of(2024, 3, 31), Computus.gregorianEaster(2024))
        assertEquals(LocalDate.of(2025, 4, 20), Computus.gregorianEaster(2025))
        assertEquals(LocalDate.of(2026, 4, 5), Computus.gregorianEaster(2026))
        assertEquals(LocalDate.of(2027, 3, 28), Computus.gregorianEaster(2027))
    }

    @Test
    @DisplayName("Lenten and Easter anchors are the right offsets from Easter")
    fun anchors() {
        val easter = Computus.gregorianEaster(2026)
        assertEquals(easter.minusDays(46), Computus.ashWednesday(2026))
        assertEquals(easter.minusDays(7), Computus.palmSunday(2026))
        assertEquals(easter.minusDays(2), Computus.goodFriday(2026))
        assertEquals(easter.plusDays(49), Computus.pentecost(2026))
        // Ash Wednesday is, by definition, a Wednesday.
        assertEquals(DayOfWeek.WEDNESDAY, Computus.ashWednesday(2026).dayOfWeek)
    }

    @Test
    @DisplayName("Advent starts on a Sunday, four Sundays before Christmas")
    fun adventStart() {
        val advent = Computus.adventStart(2026)
        assertEquals(DayOfWeek.SUNDAY, advent.dayOfWeek)
        // Advent 1 is 22–28 days before Christmas (inclusive of the 4 Sundays).
        val christmas = LocalDate.of(2026, 12, 25)
        val daysBefore = christmas.toEpochDay() - advent.toEpochDay()
        assertTrue(daysBefore in 22..28) { "Advent start $advent is $daysBefore days before Christmas" }
    }
}
