package com.manna.bible.domain.reader

import com.manna.bible.domain.repository.ChapterContent
import com.manna.bible.domain.repository.VerseLine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** Unit tests for [MergedVerses] — resolving verses an edition merges into a neighbour. */
class MergedVersesTest {

    @Test
    @DisplayName("a verse that exists is its own carrier")
    fun existingVerse() {
        assertEquals(15, MergedVerses.carrierOf(listOf(14, 15, 17), "1CO", 4, 15))
    }

    @Test
    @DisplayName("a verse merged inside the chapter resolves to the verse before it (1 Cor 4:15-16)")
    fun mergedInsideChapter() {
        assertEquals(15, MergedVerses.carrierOf(listOf(14, 15, 17, 18), "1CO", 4, 16))
        // A three-verse merge (2 Sam 4:6-8 in the Telugu IRV).
        assertEquals(6, MergedVerses.carrierOf(listOf(5, 6, 9), "2SA", 4, 8))
    }

    @Test
    @DisplayName("a verse merged into the last verse resolves to it (2 Tim 3:16-17)")
    fun mergedAtChapterEnd() {
        assertEquals(16, MergedVerses.carrierOf((1..16).toList(), "2TI", 3, 17))
    }

    @Test
    @DisplayName("a verse far past the chapter's end does not resolve")
    fun farPastEnd() {
        assertNull(MergedVerses.carrierOf((1..36).toList(), "JHN", 3, 36 + MergedVerses.MAX_MERGED_AFTER_LAST + 1))
    }

    @Test
    @DisplayName("Romans 14:24-26 never falls back to 14:23 (the doxology is printed at 16:25-27)")
    fun romansDoxology() {
        assertNull(MergedVerses.carrierOf((1..23).toList(), "ROM", 14, 24))
        assertEquals(23..23, MergedVerses.spans((1..23).toList(), "ROM", 14).getValue(23))
    }

    @Test
    @DisplayName("a verse before the first one does not resolve")
    fun beforeFirst() {
        assertNull(MergedVerses.carrierOf(listOf(2, 3), "GEN", 1, 1))
    }

    @Test
    @DisplayName("spans list the verses each row carries")
    fun spans() {
        val spans = MergedVerses.spans(listOf(17, 14, 15), "1CO", 4)
        assertEquals(14..14, spans.getValue(14))
        assertEquals(15..16, spans.getValue(15))
        assertEquals(17..17 + MergedVerses.MAX_MERGED_AFTER_LAST, spans.getValue(17))
    }

    @Test
    @DisplayName("lineCarrying returns the merged line's text")
    fun lineCarrying() {
        val chapter = ChapterContent(
            translationId = "tel_irv",
            osisId = "1CO",
            chapter = 4,
            verses = listOf(VerseLine(15, "15-16 text"), VerseLine(17, "17 text"))
        )
        assertEquals("15-16 text", chapter.lineCarrying(16)?.text)
        assertEquals("17 text", chapter.lineCarrying(17)?.text)
        assertNull(chapter.lineCarrying(14))
    }
}
