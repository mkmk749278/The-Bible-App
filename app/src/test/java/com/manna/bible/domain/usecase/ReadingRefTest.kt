package com.manna.bible.domain.usecase

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit tests for [ReadingRef] parse/format round-tripping and invalid-input
 * handling (Requirement 7).
 *
 * Runs on the JVM (JUnit 5) without an emulator.
 */
class ReadingRefTest {

    @Test
    @DisplayName("parse reads a full OSIS.CHAPTER.VERSE reference")
    fun parsesFullReference() {
        assertEquals(ReadingRef("GEN", 1, 1), ReadingRef.parse("GEN.1.1"))
        assertEquals(ReadingRef("PSA", 23, 4), ReadingRef.parse("PSA.23.4"))
    }

    @Test
    @DisplayName("parse defaults the verse to 1 when omitted")
    fun parseDefaultsVerse() {
        assertEquals(ReadingRef("PSA", 23, 1), ReadingRef.parse("PSA.23"))
    }

    @Test
    @DisplayName("format produces the canonical OSIS.CHAPTER.VERSE string")
    fun formatsCanonicalString() {
        assertEquals("GEN.1.1", ReadingRef("GEN", 1, 1).format())
        assertEquals("TOB.3.2", ReadingRef("TOB", 3, 2).format())
    }

    @Test
    @DisplayName("parse and format round-trip for valid references")
    fun roundTrips() {
        val refs = listOf(
            ReadingRef("GEN", 1, 1),
            ReadingRef("EXO", 40, 38),
            ReadingRef("REV", 22, 21)
        )
        refs.forEach { ref ->
            assertEquals(ref, ReadingRef.parse(ref.format()))
        }
    }

    @Test
    @DisplayName("parse returns null for invalid input")
    fun rejectsInvalidInput() {
        val invalid = listOf(
            null,
            "",
            "   ",
            "GEN",                 // missing chapter
            "GEN.x.1",             // non-numeric chapter
            "GEN.1.y",             // non-numeric verse
            "GEN.0.1",             // chapter below 1
            "GEN.1.0",             // verse below 1
            ".1.1",                // blank book
            "GEN.1.1.1"            // too many segments
        )
        invalid.forEach { input ->
            assertNull(ReadingRef.parse(input), "expected null for input='$input'")
        }
    }
}
