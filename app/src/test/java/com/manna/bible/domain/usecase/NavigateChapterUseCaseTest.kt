package com.manna.bible.domain.usecase

import com.manna.bible.domain.model.CanonBook
import com.manna.bible.domain.model.CanonProfile
import com.manna.bible.domain.model.CanonType
import com.manna.bible.domain.model.Denomination
import com.manna.bible.domain.model.NumberingScheme
import com.manna.bible.domain.model.Testament
import com.manna.bible.domain.repository.BookSummary
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit tests for [NavigateChapterUseCase] covering within-book stepping, book
 * boundary crossing, and canon first/last boundaries (Requirement 3).
 *
 * Runs on the JVM (JUnit 5) without an emulator.
 *
 * Validates: Requirements 3
 */
class NavigateChapterUseCaseTest {

    private val useCase = NavigateChapterUseCase()

    // A small three-book canon: GEN(50) -> EXO(40) -> LEV(27).
    private fun canonBook(osisId: String, orderIndex: Int) =
        CanonBook(
            osisId = osisId,
            testament = Testament.OLD,
            orderIndex = orderIndex,
            isDeuterocanonical = false
        )

    private val profile = CanonProfile(
        denomination = Denomination.PROTESTANT_OTHER,
        canonType = CanonType.PROTESTANT_66,
        books = listOf(
            canonBook("GEN", 0),
            canonBook("EXO", 1),
            canonBook("LEV", 2)
        ),
        numberingScheme = NumberingScheme.MASORETIC,
        namingConventionId = null,
        suggestedTranslationId = null,
        lectionaryId = null
    )

    private fun bookSummary(osisId: String, orderIndex: Int, chapterCount: Int) =
        BookSummary(
            osisId = osisId,
            name = osisId,
            testament = Testament.OLD,
            orderIndex = orderIndex,
            chapterCount = chapterCount
        )

    private val books = listOf(
        bookSummary("GEN", 0, 50),
        bookSummary("EXO", 1, 40),
        bookSummary("LEV", 2, 27)
    )

    @Test
    @DisplayName("next advances the chapter within a book (Req 3.1)")
    fun nextWithinBook() {
        assertEquals(
            ReadingRef("GEN", 2, 1),
            useCase.next(profile, ReadingRef("GEN", 1), books)
        )
    }

    @Test
    @DisplayName("next at a book's last chapter advances to the next book chapter 1 (Req 3.1)")
    fun nextAtBookEndCrossesToNextBook() {
        assertEquals(
            ReadingRef("EXO", 1, 1),
            useCase.next(profile, ReadingRef("GEN", 50), books)
        )
    }

    @Test
    @DisplayName("next at the last chapter of the last book returns null (Req 3.4)")
    fun nextAtCanonEndIsNull() {
        assertNull(useCase.next(profile, ReadingRef("LEV", 27), books))
    }

    @Test
    @DisplayName("previous steps back within a book (Req 3.2)")
    fun previousWithinBook() {
        assertEquals(
            ReadingRef("EXO", 4, 1),
            useCase.previous(profile, ReadingRef("EXO", 5), books)
        )
    }

    @Test
    @DisplayName("previous at a book's first chapter moves to the prior book's last chapter (Req 3.2)")
    fun previousAtBookStartCrossesToPriorBookLastChapter() {
        assertEquals(
            ReadingRef("GEN", 50, 1),
            useCase.previous(profile, ReadingRef("EXO", 1), books)
        )
    }

    @Test
    @DisplayName("previous at the first chapter of the first book returns null (Req 3.3)")
    fun previousAtCanonStartIsNull() {
        assertNull(useCase.previous(profile, ReadingRef("GEN", 1), books))
    }

    @Test
    @DisplayName("navigation from a book outside the canon returns null")
    fun navigationOutsideCanonIsNull() {
        assertNull(useCase.next(profile, ReadingRef("TOB", 1), books))
        assertNull(useCase.previous(profile, ReadingRef("TOB", 1), books))
    }
}
