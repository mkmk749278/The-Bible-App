package com.manna.bible.domain.calendar

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Unit tests for [DefaultJesusEventsProvider] — Easter computus, derived movable
 * feasts, fixed feasts, ordering, and "next event" lookup.
 */
class JesusEventsProviderTest {

    private val provider = DefaultJesusEventsProvider()

    private fun entry(year: Int, id: String): JesusCalendarEntry =
        provider.entriesFor(year).first { it.id == id }

    @Test
    @DisplayName("computes known Western Easter dates")
    fun knownEasterDates() {
        // Well-known Gregorian Easter Sundays.
        assertEquals(LocalDate.of(2024, 3, 31), entry(2024, "easter").date)
        assertEquals(LocalDate.of(2025, 4, 20), entry(2025, "easter").date)
        assertEquals(LocalDate.of(2026, 4, 5), entry(2026, "easter").date)
        assertEquals(LocalDate.of(2027, 3, 28), entry(2027, "easter").date)
    }

    @Test
    @DisplayName("movable feasts are offset from Easter correctly")
    fun movableOffsets() {
        val easter = entry(2026, "easter").date
        assertEquals(easter.minusDays(2), entry(2026, "good_friday").date)
        assertEquals(easter.minusDays(7), entry(2026, "palm_sunday").date)
        assertEquals(easter.minusDays(46), entry(2026, "ash_wednesday").date)
        assertEquals(easter.plusDays(39), entry(2026, "ascension").date)
        assertEquals(easter.plusDays(49), entry(2026, "pentecost").date)
    }

    @Test
    @DisplayName("fixed feasts fall on their calendar dates")
    fun fixedFeasts() {
        assertEquals(LocalDate.of(2026, 12, 25), entry(2026, "nativity").date)
        assertEquals(LocalDate.of(2026, 1, 6), entry(2026, "epiphany").date)
        assertEquals(LocalDate.of(2026, 3, 25), entry(2026, "annunciation").date)
    }

    @Test
    @DisplayName("entries are sorted ascending and reference real passages")
    fun sortedAndValid() {
        val entries = provider.entriesFor(2026)
        assertTrue(entries.isNotEmpty())
        assertEquals(entries.sortedBy { it.date }, entries)
        entries.forEach { e ->
            assertTrue(e.verseRefs.isNotEmpty(), "${e.id} has no verses")
            e.verseRefs.forEach { ref ->
                assertTrue(ref.osisId.isNotBlank())
                assertTrue(ref.chapter >= 1)
                assertTrue(ref.verse >= 1)
            }
        }
    }

    @Test
    @DisplayName("nextEntry rolls into the following year once events have passed")
    fun nextEntryRollsOver() {
        // After the last event of 2026 (Holy Innocents, Dec 28), the next is in 2027.
        val next = provider.nextEntry(LocalDate.of(2026, 12, 31))
        assertNotNull(next)
        assertEquals(2027, next!!.date.year)

        // Mid-year, the next event is later the same year.
        val midYear = provider.nextEntry(LocalDate.of(2026, 1, 2))
        assertNotNull(midYear)
        assertEquals("epiphany", midYear!!.id)
    }
}
