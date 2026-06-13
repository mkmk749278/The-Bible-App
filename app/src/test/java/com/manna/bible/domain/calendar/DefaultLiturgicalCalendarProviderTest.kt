package com.manna.bible.domain.calendar

import com.manna.bible.domain.model.Denomination
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit tests for [DefaultLiturgicalCalendarProvider]. Anchored on 2026 (Easter is
 * 5 April 2026) so the season/colour/feast/fast results are concrete.
 */
class DefaultLiturgicalCalendarProviderTest {

    private val provider = DefaultLiturgicalCalendarProvider(DefaultJesusEventsProvider())

    private fun day(date: LocalDate, denom: Denomination = Denomination.CSI) =
        provider.dayFor(date, denom)

    @Test
    @DisplayName("Seasons are assigned correctly across the 2026 year")
    fun seasons() {
        assertEquals(LiturgicalSeason.CHRISTMAS, day(LocalDate.of(2026, 1, 1)).season)
        assertEquals(LiturgicalSeason.EPIPHANY, day(LocalDate.of(2026, 1, 6)).season)
        assertEquals(LiturgicalSeason.EPIPHANY, day(LocalDate.of(2026, 1, 20)).season)
        assertEquals(LiturgicalSeason.LENT, day(LocalDate.of(2026, 2, 18)).season)   // Ash Wed
        assertEquals(LiturgicalSeason.HOLY_WEEK, day(LocalDate.of(2026, 3, 29)).season) // Palm Sun
        assertEquals(LiturgicalSeason.HOLY_WEEK, day(LocalDate.of(2026, 4, 3)).season)   // Good Fri
        assertEquals(LiturgicalSeason.EASTER, day(LocalDate.of(2026, 4, 5)).season)      // Easter
        assertEquals(LiturgicalSeason.EASTER, day(LocalDate.of(2026, 5, 24)).season)     // Pentecost
        assertEquals(LiturgicalSeason.ORDINARY, day(LocalDate.of(2026, 7, 15)).season)
        assertEquals(LiturgicalSeason.CHRISTMAS, day(LocalDate.of(2026, 12, 25)).season)
    }

    @Test
    @DisplayName("Advent boundary: the day before Advent is Ordinary, Advent start is Advent")
    fun adventBoundary() {
        val advent = Computus.adventStart(2026)
        assertEquals(LiturgicalSeason.ORDINARY, day(advent.minusDays(1)).season)
        assertEquals(LiturgicalSeason.ADVENT, day(advent).season)
        assertEquals(LiturgicalColor.VIOLET, day(advent).color)
    }

    @Test
    @DisplayName("Liturgical colours follow the seasons, with Good Friday/Pentecost red")
    fun colors() {
        assertEquals(LiturgicalColor.WHITE, day(LocalDate.of(2026, 1, 1)).color)   // Christmas
        assertEquals(LiturgicalColor.WHITE, day(LocalDate.of(2026, 1, 6)).color)   // Epiphany feast
        assertEquals(LiturgicalColor.GREEN, day(LocalDate.of(2026, 1, 20)).color)  // Epiphany weeks
        assertEquals(LiturgicalColor.VIOLET, day(LocalDate.of(2026, 2, 18)).color) // Lent
        assertEquals(LiturgicalColor.RED, day(LocalDate.of(2026, 3, 29)).color)    // Palm Sunday
        assertEquals(LiturgicalColor.RED, day(LocalDate.of(2026, 4, 3)).color)     // Good Friday
        assertEquals(LiturgicalColor.WHITE, day(LocalDate.of(2026, 4, 5)).color)   // Easter
        assertEquals(LiturgicalColor.RED, day(LocalDate.of(2026, 5, 24)).color)    // Pentecost
        assertEquals(LiturgicalColor.GREEN, day(LocalDate.of(2026, 7, 15)).color)  // Ordinary
    }

    @Test
    @DisplayName("Feast ids match the Jesus-events surface")
    fun feasts() {
        assertEquals("holy_name", day(LocalDate.of(2026, 1, 1)).feastId)
        assertEquals("epiphany", day(LocalDate.of(2026, 1, 6)).feastId)
        assertEquals("good_friday", day(LocalDate.of(2026, 4, 3)).feastId)
        assertEquals("easter", day(LocalDate.of(2026, 4, 5)).feastId)
        assertEquals("pentecost", day(LocalDate.of(2026, 5, 24)).feastId)
        assertEquals("nativity", day(LocalDate.of(2026, 12, 25)).feastId)
        assertNull(day(LocalDate.of(2026, 7, 15)).feastId)
    }

    @Test
    @DisplayName("Western fasts: Ash Wednesday, Good Friday, and Fridays of Lent")
    fun westernFasts() {
        assertTrue(day(LocalDate.of(2026, 2, 18)).isFast)  // Ash Wednesday
        assertTrue(day(LocalDate.of(2026, 4, 3)).isFast)   // Good Friday
        assertTrue(day(LocalDate.of(2026, 2, 20)).isFast)  // First Friday of Lent
        assertFalse(day(LocalDate.of(2026, 2, 19)).isFast) // Thursday in Lent — not a fast
        assertFalse(day(LocalDate.of(2026, 7, 17)).isFast) // Ordinary Time — no Western fast
    }

    @Test
    @DisplayName("Orthodox adds the year-round Wednesday/Friday fast")
    fun orthodoxFasts() {
        // A Wednesday in Ordinary Time: a fast for Orthodox, not for Western traditions.
        val ordinaryWednesday = LocalDate.of(2026, 7, 1)
            .with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY))
        assertTrue(day(ordinaryWednesday, Denomination.ORTHODOX).isFast)
        assertFalse(day(ordinaryWednesday, Denomination.CSI).isFast)
    }

    @Test
    @DisplayName("\"Show everything\" marks no fasts")
    fun showEverythingNoFasts() {
        assertFalse(day(LocalDate.of(2026, 4, 3), Denomination.SHOW_EVERYTHING).isFast) // Good Fri
        assertFalse(day(LocalDate.of(2026, 2, 18), Denomination.SHOW_EVERYTHING).isFast) // Ash Wed
    }

    @Test
    @DisplayName("month() returns each day of the month in order")
    fun monthGrid() {
        val feb = provider.month(2026, 2, Denomination.CSI)
        assertEquals(28, feb.size)
        assertEquals(LocalDate.of(2026, 2, 1), feb.first().date)
        assertEquals(LocalDate.of(2026, 2, 28), feb.last().date)
        assertTrue(feb.zipWithNext().all { (a, b) -> a.date.isBefore(b.date) })
    }
}
