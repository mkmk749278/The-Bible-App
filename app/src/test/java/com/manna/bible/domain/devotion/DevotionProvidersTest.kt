package com.manna.bible.domain.devotion

import com.manna.bible.domain.usecase.ReadingRef
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for the Prayers-hub domain providers. Every Scripture anchor must fall
 * within the shared 66-book canon (so it resolves in any downloaded translation) and
 * be structurally valid; each provider's counts and ordering are checked too.
 */
class DevotionProvidersTest {

    /** The 66-book Protestant canon — the lowest common denominator across traditions. */
    private val canon66 = setOf(
        "GEN", "EXO", "LEV", "NUM", "DEU", "JOS", "JDG", "RUT", "1SA", "2SA",
        "1KI", "2KI", "1CH", "2CH", "EZR", "NEH", "EST", "JOB", "PSA", "PRO",
        "ECC", "SNG", "ISA", "JER", "LAM", "EZK", "DAN", "HOS", "JOL", "AMO",
        "OBA", "JON", "MIC", "NAM", "HAB", "ZEP", "HAG", "ZEC", "MAL",
        "MAT", "MRK", "LUK", "JHN", "ACT", "ROM", "1CO", "2CO", "GAL", "EPH",
        "PHP", "COL", "1TH", "2TH", "1TI", "2TI", "TIT", "PHM", "HEB", "JAS",
        "1PE", "2PE", "1JN", "2JN", "3JN", "JUD", "REV"
    )

    private fun assertValidCanonRef(ref: ReadingRef) {
        assertTrue(ref.osisId in canon66, "Unknown OSIS id: ${ref.osisId}")
        assertTrue(ref.chapter >= 1, "Non-positive chapter in ${ref.format()}")
        assertTrue(ref.verse >= 1, "Non-positive verse in ${ref.format()}")
    }

    @Nested
    @DisplayName("Stations of the Cross")
    inner class Stations {
        private val provider = DefaultStationsProvider()

        @Test
        @DisplayName("has fourteen stations numbered 1..14 in order, anchored in the canon")
        fun fourteenOrderedStations() {
            val stations = provider.stations()
            assertEquals(14, stations.size)
            stations.forEachIndexed { index, station ->
                assertEquals(index + 1, station.number)
                assertValidCanonRef(station.scripture)
            }
            assertEquals(14, stations.map { it.id }.toSet().size, "station ids must be unique")
        }

        @Test
        @DisplayName("looks up a station by number, null when out of range")
        fun lookup() {
            assertEquals(1, provider.station(1)?.number)
            assertEquals(14, provider.station(14)?.number)
            assertNull(provider.station(0))
            assertNull(provider.station(15))
        }
    }

    @Nested
    @DisplayName("The Rosary")
    inner class Rosary {
        private val provider = DefaultRosaryProvider()

        @Test
        @DisplayName("each set has five mysteries numbered 1..5, anchored in the canon")
        fun fiveMysteriesPerSet() {
            MysterySet.entries.forEach { set ->
                val mysteries = provider.mysteries(set)
                assertEquals(5, mysteries.size, "$set should have 5 mysteries")
                mysteries.forEachIndexed { index, mystery ->
                    assertEquals(index + 1, mystery.number)
                    assertEquals(set, mystery.set)
                    assertValidCanonRef(mystery.scripture)
                }
            }
        }

        @Test
        @DisplayName("twenty mysteries with unique ids in all")
        fun twentyUnique() {
            val all = MysterySet.entries.flatMap { provider.mysteries(it) }
            assertEquals(20, all.size)
            assertEquals(20, all.map { it.id }.toSet().size)
        }

        @Test
        @DisplayName("assigns the traditional set for each day of the week")
        fun setForDay() {
            assertEquals(MysterySet.JOYFUL, provider.setForDay(1))     // Monday
            assertEquals(MysterySet.SORROWFUL, provider.setForDay(2))  // Tuesday
            assertEquals(MysterySet.GLORIOUS, provider.setForDay(3))   // Wednesday
            assertEquals(MysterySet.LUMINOUS, provider.setForDay(4))   // Thursday
            assertEquals(MysterySet.SORROWFUL, provider.setForDay(5))  // Friday
            assertEquals(MysterySet.JOYFUL, provider.setForDay(6))     // Saturday
            assertEquals(MysterySet.GLORIOUS, provider.setForDay(7))   // Sunday
        }

        @Test
        @DisplayName("looks up a mystery by set and number, null when out of range")
        fun lookup() {
            assertEquals("joyful_1", provider.mystery(MysterySet.JOYFUL, 1)?.id)
            assertEquals("glorious_5", provider.mystery(MysterySet.GLORIOUS, 5)?.id)
            assertNull(provider.mystery(MysterySet.LUMINOUS, 0))
            assertNull(provider.mystery(MysterySet.LUMINOUS, 6))
        }
    }

    @Nested
    @DisplayName("The Jesus Prayer")
    inner class JesusPrayer {
        private val provider = DefaultJesusPrayerProvider()

        @Test
        @DisplayName("has the three depths in order, each anchored in the canon")
        fun threeDepths() {
            val stages = provider.stages()
            assertEquals(
                listOf(PrayerDepth.VOCAL, PrayerDepth.MENTAL, PrayerDepth.HEART),
                stages.map { it.depth }
            )
            stages.forEach { assertValidCanonRef(it.scripture) }
        }
    }

    @Nested
    @DisplayName("Paraloka")
    inner class Paraloka {
        private val provider = DefaultParalokaProvider()

        @Test
        @DisplayName("has fifteen passages and five prayers, all anchored in the canon")
        fun passagesAndPrayers() {
            val passages = provider.passages()
            val prayers = provider.prayers()
            assertEquals(15, passages.size)
            assertEquals(5, prayers.size)
            passages.forEach { assertValidCanonRef(it.scripture) }
            prayers.forEach { assertValidCanonRef(it.scripture) }
            assertEquals(15, passages.map { it.id }.toSet().size, "passage ids must be unique")
            assertEquals(5, prayers.map { it.id }.toSet().size, "prayer ids must be unique")
        }
    }

    @Nested
    @DisplayName("Sramanikal")
    inner class Sramanikal {
        private val journey = DefaultSramanikalJourney()

        @Test
        @DisplayName("is a forty-day journey of canon-valid passages")
        fun fortyValidDays() {
            assertEquals(40, journey.dayCount)
            for (day in 1..journey.dayCount) {
                assertValidCanonRef(journey.verseFor(day)!!)
            }
        }

        @Test
        @DisplayName("returns null for out-of-range days")
        fun outOfRange() {
            assertNull(journey.verseFor(0))
            assertNull(journey.verseFor(41))
            assertNull(journey.verseFor(-1))
        }
    }
}
