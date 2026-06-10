package com.manna.bible.data

import com.manna.bible.domain.model.Bookmark
import com.manna.bible.domain.model.Highlight
import com.manna.bible.domain.model.Note
import kotlinx.coroutines.flow.Flow

/**
 * Local (on-device) source of truth for user annotations: highlights, bookmarks,
 * and notes (Req 11).
 *
 * Abstracted as an interface so the [com.manna.bible.domain.repository.AnnotationRepository]
 * stays pure-Kotlin and JVM-testable with fakes; the Room-backed implementation
 * lives in [com.manna.bible.data.local].
 *
 * Annotations are never deleted on a canon change — only their visibility changes
 * (Req 12). This data source therefore exposes the full stored set; filtering by
 * the active canon's visible book ids happens in the repository.
 */
interface AnnotationLocalDataSource {

    /** Observes all stored highlights, emitting on every change. */
    fun observeHighlights(): Flow<List<Highlight>>

    /** Observes all stored bookmarks, emitting on every change. */
    fun observeBookmarks(): Flow<List<Bookmark>>

    /** Observes all stored notes, emitting on every change. */
    fun observeNotes(): Flow<List<Note>>

    /** Inserts a highlight, returning its generated row id. */
    suspend fun insertHighlight(highlight: Highlight): Long

    /** Inserts a bookmark, returning its generated row id. */
    suspend fun insertBookmark(bookmark: Bookmark): Long

    /** Inserts a note, returning its generated row id. */
    suspend fun insertNote(note: Note): Long

    /**
     * Returns every reference (verse or chapter) across highlights, bookmarks, and
     * notes as a one-shot snapshot. Used to compute which books carry annotations
     * for the canon-switch impact warning (Req 12).
     */
    suspend fun allVerseRefs(): List<String>
}
