package com.manna.bible.domain.grief

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** Unit tests for [DefaultGriefJourney]. */
class GriefJourneyTest {

    private val journey = DefaultGriefJourney()

    @Test
    @DisplayName("is a 30-day journey of structurally valid passages")
    fun thirtyValidDays() {
        assertEquals(30, journey.dayCount)
        for (day in 1..journey.dayCount) {
            val ref = journey.verseFor(day)!!
            assertTrue(ref.osisId.isNotBlank())
            assertTrue(ref.chapter >= 1 && ref.verse >= 1)
        }
    }

    @Test
    @DisplayName("returns null for out-of-range days")
    fun outOfRange() {
        assertNull(journey.verseFor(0))
        assertNull(journey.verseFor(31))
        assertNull(journey.verseFor(-1))
    }
}
