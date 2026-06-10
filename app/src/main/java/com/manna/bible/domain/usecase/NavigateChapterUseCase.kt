package com.manna.bible.domain.usecase

import com.manna.bible.domain.model.CanonProfile
import com.manna.bible.domain.reader.CanonBookOrdering
import com.manna.bible.domain.repository.BookSummary
import javax.inject.Inject

/**
 * Computes the next/previous reading position within the active canon
 * (Requirement 3).
 *
 * Navigation stays within a book by stepping the chapter number, and crosses book
 * boundaries using the canon ordering from [CanonBookOrdering]: at a book's last
 * chapter, "next" moves to chapter 1 of the next canon book; at a book's first
 * chapter, "previous" moves to the last chapter of the previous canon book.
 * Returns null at the canon's first/last boundary (Req 3.3, 3.4).
 *
 * The use case is pure and side-effect free: the per-book chapter counts are
 * supplied as data (a list of [BookSummary]), not fetched from a live repository,
 * so it is fully JVM-unit-testable without Android or coroutines machinery.
 *
 * Pure Kotlin — depends only on `javax.inject` for constructor injection.
 */
class NavigateChapterUseCase @Inject constructor() {

    /**
     * Returns the position following [current] in [profile]'s ordering, or null
     * when [current] is the last chapter of the last canon book (or its book is
     * unknown/outside the canon).
     *
     * @param books book metadata (chapter counts) for the active translation.
     */
    fun next(
        profile: CanonProfile,
        current: ReadingRef,
        books: List<BookSummary>
    ): ReadingRef? {
        val chapterCount = chapterCountOf(books, current.osisId) ?: return null
        if (current.chapter < chapterCount) {
            return ReadingRef(current.osisId, current.chapter + 1, verse = 1)
        }
        val nextBook = CanonBookOrdering.nextBook(profile, current.osisId) ?: return null
        return ReadingRef(nextBook.osisId, chapter = 1, verse = 1)
    }

    /**
     * Returns the position preceding [current] in [profile]'s ordering, or null
     * when [current] is the first chapter of the first canon book (or there is no
     * previous canon book / its chapter count is unknown).
     *
     * @param books book metadata (chapter counts) for the active translation.
     */
    fun previous(
        profile: CanonProfile,
        current: ReadingRef,
        books: List<BookSummary>
    ): ReadingRef? {
        if (current.chapter > 1) {
            return ReadingRef(current.osisId, current.chapter - 1, verse = 1)
        }
        val previousBook = CanonBookOrdering.previousBook(profile, current.osisId) ?: return null
        val previousCount = chapterCountOf(books, previousBook.osisId) ?: return null
        return ReadingRef(previousBook.osisId, chapter = previousCount, verse = 1)
    }

    private fun chapterCountOf(books: List<BookSummary>, osisId: String): Int? =
        books.firstOrNull { it.osisId == osisId }?.chapterCount
}
