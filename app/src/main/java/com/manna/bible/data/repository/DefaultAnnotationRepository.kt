package com.manna.bible.data.repository

import com.manna.bible.data.AnnotationLocalDataSource
import com.manna.bible.domain.canon.VerseRef
import com.manna.bible.domain.model.Bookmark
import com.manna.bible.domain.model.Highlight
import com.manna.bible.domain.model.Note
import com.manna.bible.domain.repository.AnnotationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Default [AnnotationRepository] backed by an [AnnotationLocalDataSource].
 *
 * All book-scoping derives the OSIS book id from each reference via
 * [VerseRef.bookId]. Visibility filtering is applied to the *observed* streams
 * only — the underlying data source always retains every row, so switching to a
 * narrower canon hides annotations without deleting them (Req 12).
 *
 * Pure Kotlin (javax.inject only) so it is JVM-unit-testable with a fake data
 * source.
 */
class DefaultAnnotationRepository @Inject constructor(
    private val dataSource: AnnotationLocalDataSource
) : AnnotationRepository {

    override suspend fun allAnnotatedBookIds(): Set<String> =
        dataSource.allVerseRefs()
            .mapNotNull { VerseRef.bookId(it) }
            .toSet()

    override suspend fun annotatedBookIdsOutside(visibleBookIds: Set<String>): Set<String> =
        allAnnotatedBookIds() - visibleBookIds

    override fun visibleHighlights(visibleBookIds: Set<String>): Flow<List<Highlight>> =
        dataSource.observeHighlights().map { highlights ->
            highlights.filter { isVisible(it.verseRef, visibleBookIds) }
        }

    override fun visibleBookmarks(visibleBookIds: Set<String>): Flow<List<Bookmark>> =
        dataSource.observeBookmarks().map { bookmarks ->
            bookmarks.filter { isVisible(it.verseRef, visibleBookIds) }
        }

    override fun visibleNotes(visibleBookIds: Set<String>): Flow<List<Note>> =
        dataSource.observeNotes().map { notes ->
            notes.filter { isVisible(it.verseRef ?: it.chapterRef, visibleBookIds) }
        }

    /**
     * A reference is visible when its book is in [visibleBookIds]. A reference with
     * no parseable book id has no book association and is treated as visible.
     */
    private fun isVisible(ref: String?, visibleBookIds: Set<String>): Boolean {
        val bookId = VerseRef.bookId(ref) ?: return true
        return bookId in visibleBookIds
    }
}
