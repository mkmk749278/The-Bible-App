package com.manna.bible.domain.reader

import com.manna.bible.domain.model.CanonBook
import com.manna.bible.domain.model.CanonProfile
import com.manna.bible.domain.model.CanonType
import com.manna.bible.domain.model.Denomination
import com.manna.bible.domain.model.NumberingScheme
import com.manna.bible.domain.model.Testament
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [CanonBookOrdering].
 *
 * Runs on the JVM (JUnit 5) without an emulator.
 *
 * Validates: Requirements 6
 */
class CanonBookOrderingTest {

    private fun book(osisId: String, orderIndex: Int) =
        CanonBook(
            osisId = osisId,
            testament = Testament.OLD,
            orderIndex = orderIndex,
            isDeuterocanonical = false
        )

    /** Profile whose [CanonProfile.books] are intentionally out of order. */
    private fun profile(books: List<CanonBook>) =
        CanonProfile(
            denomination = Denomination.PROTESTANT_OTHER,
            canonType = CanonType.PROTESTANT_66,
            books = books,
            numberingScheme = NumberingScheme.MASORETIC,
            namingConventionId = null,
            suggestedTranslationId = null,
            lectionaryId = null
        )

    private val unordered = profile(
        listOf(
            book("EXO", 1),
            book("LEV", 2),
            book("GEN", 0),
            book("NUM", 3)
        )
    )

    // --- orderedBooks --------------------------------------------------------

    @Test
    fun `orderedBooks returns books sorted by orderIndex`() {
        val result = CanonBookOrdering.orderedBooks(unordered).map { it.osisId }
        assertEquals(listOf("GEN", "EXO", "LEV", "NUM"), result)
    }

    @Test
    fun `orderedBooks does not mutate the source list`() {
        val before = unordered.books.map { it.osisId }
        CanonBookOrdering.orderedBooks(unordered)
        assertEquals(before, unordered.books.map { it.osisId })
    }

    @Test
    fun `orderedBooks is stable for equal orderIndex`() {
        val p = profile(
            listOf(
                book("AAA", 5),
                book("BBB", 5),
                book("CCC", 5)
            )
        )
        assertEquals(
            listOf("AAA", "BBB", "CCC"),
            CanonBookOrdering.orderedBooks(p).map { it.osisId }
        )
    }

    // --- orderIndexOf / isBookInCanon ----------------------------------------

    @Test
    fun `orderIndexOf returns the index for a present book`() {
        assertEquals(0, CanonBookOrdering.orderIndexOf(unordered, "GEN"))
        assertEquals(3, CanonBookOrdering.orderIndexOf(unordered, "NUM"))
    }

    @Test
    fun `orderIndexOf returns null for an absent book`() {
        assertNull(CanonBookOrdering.orderIndexOf(unordered, "TOB"))
    }

    @Test
    fun `isBookInCanon is true for present and false for absent`() {
        assertTrue(CanonBookOrdering.isBookInCanon(unordered, "LEV"))
        assertFalse(CanonBookOrdering.isBookInCanon(unordered, "TOB"))
    }

    // --- nextBook / previousBook ---------------------------------------------

    @Test
    fun `nextBook returns the following book in the middle`() {
        assertEquals("LEV", CanonBookOrdering.nextBook(unordered, "EXO")?.osisId)
    }

    @Test
    fun `nextBook returns null for the last book`() {
        assertNull(CanonBookOrdering.nextBook(unordered, "NUM"))
    }

    @Test
    fun `nextBook returns null for a book not in canon`() {
        assertNull(CanonBookOrdering.nextBook(unordered, "TOB"))
    }

    @Test
    fun `previousBook returns the preceding book in the middle`() {
        assertEquals("EXO", CanonBookOrdering.previousBook(unordered, "LEV")?.osisId)
    }

    @Test
    fun `previousBook returns null for the first book`() {
        assertNull(CanonBookOrdering.previousBook(unordered, "GEN"))
    }

    @Test
    fun `previousBook returns null for a book not in canon`() {
        assertNull(CanonBookOrdering.previousBook(unordered, "TOB"))
    }

    // --- visibleBooks --------------------------------------------------------

    @Test
    fun `visibleBooks returns ordered books regardless of toggle`() {
        val shown = CanonBookOrdering.visibleBooks(unordered, showDeuterocanonical = true)
        val hidden = CanonBookOrdering.visibleBooks(unordered, showDeuterocanonical = false)
        val expected = listOf("GEN", "EXO", "LEV", "NUM")
        assertEquals(expected, shown.map { it.osisId })
        assertEquals(expected, hidden.map { it.osisId })
    }
}
