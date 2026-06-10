package com.manna.bible.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for the annotation tables: `highlights`, `bookmarks`, and `notes`.
 *
 * Provides Flow observers for each table, insert methods, and one-shot reference
 * getters used to compute which books carry annotations (Req 12). No delete
 * queries exist here by design — annotations survive canon changes and are only
 * hidden at the view layer.
 */
@Dao
interface AnnotationDao {

    // --- observers -----------------------------------------------------------

    /** Observes all highlights, emitting on every change. */
    @Query("SELECT * FROM highlights")
    fun observeHighlights(): Flow<List<HighlightEntity>>

    /** Observes all bookmarks, emitting on every change. */
    @Query("SELECT * FROM bookmarks")
    fun observeBookmarks(): Flow<List<BookmarkEntity>>

    /** Observes all notes, emitting on every change. */
    @Query("SELECT * FROM notes")
    fun observeNotes(): Flow<List<NoteEntity>>

    // --- inserts -------------------------------------------------------------

    /** Inserts a highlight, returning its generated row id. */
    @Insert
    suspend fun insertHighlight(entity: HighlightEntity): Long

    /** Inserts a bookmark, returning its generated row id. */
    @Insert
    suspend fun insertBookmark(entity: BookmarkEntity): Long

    /** Inserts a note, returning its generated row id. */
    @Insert
    suspend fun insertNote(entity: NoteEntity): Long

    // --- reference getters (for canon-switch impact, Req 12) -----------------

    /** Returns the verse references of every highlight. */
    @Query("SELECT verseRef FROM highlights")
    suspend fun highlightVerseRefs(): List<String>

    /** Returns the verse references of every bookmark. */
    @Query("SELECT verseRef FROM bookmarks")
    suspend fun bookmarkVerseRefs(): List<String>

    /**
     * Returns each note's reference, preferring its verse reference and falling
     * back to its chapter reference. May contain nulls for notes lacking both.
     */
    @Query("SELECT COALESCE(verseRef, chapterRef) FROM notes")
    suspend fun noteRefs(): List<String?>
}
