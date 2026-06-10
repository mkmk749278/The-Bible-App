package com.manna.bible.domain.usecase

import com.manna.bible.domain.model.CanonBook
import com.manna.bible.domain.model.CanonProfile
import com.manna.bible.domain.model.CanonType
import com.manna.bible.domain.model.Denomination
import com.manna.bible.domain.model.NumberingScheme
import com.manna.bible.domain.model.Testament
import com.manna.bible.domain.repository.BookSummary
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit tests for [RestoreReadingPositionUseCase] covering persisted-valid restore,
 * empty fallback, and outside-canon fallback (Requirement 7).
 *
 * Runs on the JVM (JUnit 5) without an emulator.
 *
 * Validates: Requirements 7
 */
class RestoreReadingPositionUseCaseTest {

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
            canonBook("EXO", 1)
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
        bookSummary("EXO", 1, 40)
    )

    @Test
    @DisplayName("a persisted valid position is returned as-is (Req 7.2)")
    fun returnsPersistedValidPosition() = runTest {
        val store = FakePreferencesStore().apply { seedLastReadPosition("EXO.2.3") }
        val result = RestoreReadingPositionUseCase(store).invoke(profile, books)
        assertEquals(ReadingRef("EXO", 2, 3), result)
    }

    @Test
    @DisplayName("no persisted position falls back to the first canon book chapter 1 (Req 7.3)")
    fun fallsBackToFirstBookWhenNone() = runTest {
        val store = FakePreferencesStore().apply { seedLastReadPosition(null) }
        val result = RestoreReadingPositionUseCase(store).invoke(profile, books)
        assertEquals(ReadingRef("GEN", 1, 1), result)
    }

    @Test
    @DisplayName("a position outside the canon falls back to the nearest valid position (Req 7.4)")
    fun fallsBackWhenOutsideCanon() = runTest {
        val store = FakePreferencesStore().apply { seedLastReadPosition("TOB.1.1") }
        val result = RestoreReadingPositionUseCase(store).invoke(profile, books)
        assertEquals(ReadingRef("GEN", 1, 1), result)
    }

    @Test
    @DisplayName("a position whose chapter is out of range falls back to nearest valid (Req 7.4)")
    fun fallsBackWhenChapterOutOfRange() = runTest {
        val store = FakePreferencesStore().apply { seedLastReadPosition("GEN.99.1") }
        val result = RestoreReadingPositionUseCase(store).invoke(profile, books)
        assertEquals(ReadingRef("GEN", 1, 1), result)
    }

    @Test
    @DisplayName("an unparseable persisted position falls back to nearest valid (Req 7.4)")
    fun fallsBackWhenUnparseable() = runTest {
        val store = FakePreferencesStore().apply { seedLastReadPosition("not-a-ref") }
        val result = RestoreReadingPositionUseCase(store).invoke(profile, books)
        assertEquals(ReadingRef("GEN", 1, 1), result)
    }
}
