package com.manna.bible.domain.repository

import com.manna.bible.domain.model.Bookmark
import com.manna.bible.domain.model.Highlight
import com.manna.bible.domain.model.Note
import kotlinx.coroutines.flow.Flow

/**
 * Book-scoped access to user annotations: highlights, bookmarks, and notes
 * (Req 11, 12).
 *
 * Annotations are keyed by canonical verse reference (e.g. `"GEN.1.1"`) whose book
 * id is the segment before the first `.`. Annotations are **never deleted** when
 * the active canon changes; only their *visibility* changes based on the active
 * canon's set of visible book ids (Req 12). The `visible*` observers therefore
 * filter out — but never remove — annotations for books outside the active canon.
 *
 * Pure Kotlin — no Android dependencies.
 */
interface AnnotationRepository {

    /** Returns the distinct OSIS book ids that carry any annotation. */
    suspend fun allAnnotatedBookIds(): Set<String>

    /**
     * Returns the annotated book ids that are **not** in [visibleBookIds] — the
     * books whose annotations would be hidden by switching to a canon with that
     * visible set. Used to drive the canon-switch warning (Req 12.1).
     */
    suspend fun annotatedBookIdsOutside(visibleBookIds: Set<String>): Set<String>

    /**
     * Observes highlights whose book is in [visibleBookIds] (Req 12.4). Highlights
     * for books outside the set are hidden, not deleted.
     */
    fun visibleHighlights(visibleBookIds: Set<String>): Flow<List<Highlight>>

    /**
     * Observes bookmarks whose book is in [visibleBookIds] (Req 12.4). Bookmarks
     * for books outside the set are hidden, not deleted.
     */
    fun visibleBookmarks(visibleBookIds: Set<String>): Flow<List<Bookmark>>

    /**
     * Observes notes whose book is in [visibleBookIds] (Req 12.4). Notes for books
     * outside the set are hidden, not deleted. A note carrying no parseable
     * reference has no book association and stays visible.
     */
    fun visibleNotes(visibleBookIds: Set<String>): Flow<List<Note>>
}
