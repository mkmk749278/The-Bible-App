package com.manna.bible.domain.daily

import com.manna.bible.domain.usecase.ReadingRef
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Unit tests for [DefaultDailyVerseProvider] — deterministic, offline daily verse.
 */
class DailyVerseProviderTest {

    private val provider = DefaultDailyVerseProvider()

    @Test
    @DisplayName("the same date always yields the same verse")
    fun deterministicForDate() {
        val date = LocalDate.of(2026, 6, 11)
        assertEquals(provider.verseForDate(date), provider.verseForDate(date))
    }

    @Test
    @DisplayName("rotation is keyed by epoch day")
    fun rotatesByEpochDay() {
        // Epoch day 0 maps to the first curated verse; the next day differs.
        assertEquals(ReadingRef("JHN", 3, 16), provider.verseForDate(LocalDate.ofEpochDay(0)))
        assertNotEquals(
            provider.verseForDate(LocalDate.ofEpochDay(0)),
            provider.verseForDate(LocalDate.ofEpochDay(1))
        )
    }

    @Test
    @DisplayName("every selected reference is structurally valid")
    fun referencesAreValid() {
        for (offset in 0 until 400) {
            val ref = provider.verseForDate(LocalDate.ofEpochDay(offset.toLong()))
            assertTrue(ref.osisId.isNotBlank(), "blank book id")
            assertTrue(ref.chapter >= 1, "non-positive chapter")
            assertTrue(ref.verse >= 1, "non-positive verse")
        }
    }
}
