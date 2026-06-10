package com.manna.bible.domain.canon

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Unit tests for [VerseRef].
 *
 * Runs on the JVM (JUnit 5). Confirms OSIS book id extraction from verse and
 * chapter references and graceful handling of null/blank input.
 *
 * Validates: Requirements 12
 */
class VerseRefTest {

    @Test
    fun `bookId extracts the book from a verse reference`() {
        assertEquals("GEN", VerseRef.bookId("GEN.1.1"))
        assertEquals("TOB", VerseRef.bookId("TOB.3.2"))
    }

    @Test
    fun `bookId extracts the book from a chapter reference`() {
        assertEquals("PSA", VerseRef.bookId("PSA.23"))
    }

    @Test
    fun `bookId returns the whole string when there is no dot`() {
        assertEquals("REV", VerseRef.bookId("REV"))
    }

    @Test
    fun `bookId returns null for null input`() {
        assertNull(VerseRef.bookId(null))
    }

    @Test
    fun `bookId returns null for blank input`() {
        assertNull(VerseRef.bookId(""))
        assertNull(VerseRef.bookId("   "))
    }

    @Test
    fun `bookId returns null when the book segment is blank`() {
        assertNull(VerseRef.bookId(".1.1"))
    }
}
