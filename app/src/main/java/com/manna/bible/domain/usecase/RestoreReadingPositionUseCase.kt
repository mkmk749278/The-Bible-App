package com.manna.bible.domain.usecase

import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.model.CanonProfile
import com.manna.bible.domain.reader.CanonBookOrdering
import com.manna.bible.domain.repository.BookSummary
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Resolves the position at which the reader should open (Requirement 7).
 *
 * It reads the persisted `Reading_Position` (`lastReadPosition`) and returns it
 * when it parses to a position that is valid for the active canon and available
 * in the translation's [BookSummary] set. Otherwise — no persisted position, an
 * unparseable one, or one whose book is outside the canon / absent from the
 * translation, or whose chapter is out of range — it falls back to the first
 * chapter of the first book in the active [CanonProfile] (Req 7.2–7.4).
 *
 * The active profile and the translation's book set are passed in as data, so the
 * use case is pure and JVM-unit-testable.
 *
 * Pure Kotlin — depends only on `javax.inject` for constructor injection.
 */
class RestoreReadingPositionUseCase @Inject constructor(
    private val preferencesStore: PreferencesStore
) {

    /**
     * Returns the persisted position when valid, else the nearest valid position
     * (first canon book, chapter 1) for [profile] and [books].
     */
    suspend operator fun invoke(profile: CanonProfile, books: List<BookSummary>): ReadingRef {
        val persisted = ReadingRef.parse(preferencesStore.lastReadPosition.first())
        if (persisted != null && isValid(profile, books, persisted)) {
            return persisted
        }
        return firstValidPosition(profile, books)
    }

    /** A position is valid when its book is in the canon and translation, and its chapter is in range. */
    private fun isValid(
        profile: CanonProfile,
        books: List<BookSummary>,
        ref: ReadingRef
    ): Boolean {
        if (!CanonBookOrdering.isBookInCanon(profile, ref.osisId)) return false
        val book = books.firstOrNull { it.osisId == ref.osisId } ?: return false
        return ref.chapter in 1..book.chapterCount
    }

    /**
     * The first chapter of the first canon book that also has content in [books];
     * falls back to the first canon book, then to the first available book.
     */
    private fun firstValidPosition(profile: CanonProfile, books: List<BookSummary>): ReadingRef {
        val ordered = CanonBookOrdering.orderedBooks(profile)
        val availableIds = books.mapTo(mutableSetOf()) { it.osisId }

        ordered.firstOrNull { it.osisId in availableIds }?.let {
            return ReadingRef(it.osisId, chapter = 1, verse = 1)
        }
        ordered.firstOrNull()?.let {
            return ReadingRef(it.osisId, chapter = 1, verse = 1)
        }
        return books.minByOrNull { it.orderIndex }
            ?.let { ReadingRef(it.osisId, chapter = 1, verse = 1) }
            ?: ReadingRef(osisId = "GEN", chapter = 1, verse = 1)
    }
}
