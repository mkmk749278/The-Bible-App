package com.manna.bible.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for offline scripture content: the `books`, `chapters`, `verses`, and
 * `verses_fts` tables.
 *
 * The DAO is intentionally canon-agnostic: it stores and returns canonical
 * (Masoretic) numbering and the source book order. Canon membership, ordering, and
 * Psalm display numbering are applied above this layer (repository / use cases).
 *
 * Full-text search runs against the FTS4 [VerseFtsEntity] table and joins back to
 * [VerseEntity] on the shared implicit `rowid` to recover full verse rows.
 */
@Dao
interface BibleContentDao {

    // --- reads ---------------------------------------------------------------

    /** Observes the books of a translation in source order, emitting on change. */
    @Query("SELECT * FROM books WHERE translationId = :translationId ORDER BY orderIndex")
    fun observeBooks(translationId: String): Flow<List<BookEntity>>

    /** Returns a single book of a translation, or null if absent. */
    @Query("SELECT * FROM books WHERE translationId = :translationId AND osisId = :osisId")
    suspend fun getBook(translationId: String, osisId: String): BookEntity?

    /** Returns the verses of a chapter ordered by verse number. */
    @Query(
        "SELECT * FROM verses WHERE translationId = :t AND osisId = :b AND chapter = :c " +
            "ORDER BY verse"
    )
    suspend fun getChapter(t: String, b: String, c: Int): List<VerseEntity>

    /** Returns chapter-level metadata for a chapter, or null if absent. */
    @Query(
        "SELECT * FROM chapters WHERE translationId = :translationId AND osisId = :osisId " +
            "AND chapter = :chapter"
    )
    suspend fun getChapterMeta(translationId: String, osisId: String, chapter: Int): ChapterEntity?

    /** True when any verse content exists for the translation. */
    @Query("SELECT EXISTS(SELECT 1 FROM verses WHERE translationId = :t)")
    suspend fun hasAnyContent(t: String): Boolean

    /** Counts the verses stored for a translation. */
    @Query("SELECT COUNT(*) FROM verses WHERE translationId = :translationId")
    suspend fun countVerses(translationId: String): Int

    /** Sum of stored verse text lengths for a translation (used as the size on disk). */
    @Query("SELECT IFNULL(SUM(LENGTH(text)), 0) FROM verses WHERE translationId = :translationId")
    suspend fun sumTextLength(translationId: String): Long

    // --- search --------------------------------------------------------------

    /**
     * Full-text search within a translation. Matches [query] against the FTS index
     * and joins back to `verses` on `rowid`, capped at [limit] rows.
     *
     * Canon-book restriction is applied by the caller; use [searchInBooks] to push
     * an explicit book-id filter into SQL.
     */
    @Query(
        "SELECT v.* FROM verses v JOIN verses_fts f ON v.rowid = f.rowid " +
            "WHERE f.text MATCH :query AND v.translationId = :t LIMIT :limit"
    )
    suspend fun search(t: String, query: String, limit: Int): List<VerseEntity>

    /**
     * Full-text search within a translation restricted to the given [osisIds]
     * (e.g. the visible canon books), capped at [limit] rows.
     */
    @Query(
        "SELECT v.* FROM verses v JOIN verses_fts f ON v.rowid = f.rowid " +
            "WHERE f.text MATCH :query AND v.translationId = :t " +
            "AND v.osisId IN (:osisIds) LIMIT :limit"
    )
    suspend fun searchInBooks(
        t: String,
        query: String,
        osisIds: List<String>,
        limit: Int
    ): List<VerseEntity>

    // --- inserts -------------------------------------------------------------

    /** Inserts or replaces book rows. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(list: List<BookEntity>)

    /** Inserts or replaces chapter rows. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(list: List<ChapterEntity>)

    /** Inserts or replaces verse rows (the FTS mirror updates automatically). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerses(list: List<VerseEntity>)

    /**
     * Inserts books, chapters, and verses for a translation atomically so a chapter
     * is never observable without its verses (and vice versa).
     */
    @Transaction
    suspend fun insertContent(
        books: List<BookEntity>,
        chapters: List<ChapterEntity>,
        verses: List<VerseEntity>
    ) {
        insertBooks(books)
        insertChapters(chapters)
        insertVerses(verses)
    }

    // --- deletes -------------------------------------------------------------

    /** Deletes all book rows for a translation. */
    @Query("DELETE FROM books WHERE translationId = :t")
    suspend fun deleteBooks(t: String)

    /** Deletes all chapter rows for a translation. */
    @Query("DELETE FROM chapters WHERE translationId = :t")
    suspend fun deleteChapters(t: String)

    /** Deletes all verse rows for a translation (the FTS mirror updates too). */
    @Query("DELETE FROM verses WHERE translationId = :t")
    suspend fun deleteVerses(t: String)

    /**
     * Removes every books/chapters/verses row for a translation atomically, leaving
     * no partial content behind (Req 5.4, 5.7, 15.4).
     */
    @Transaction
    suspend fun deleteTranslationContent(translationId: String) {
        deleteVerses(translationId)
        deleteChapters(translationId)
        deleteBooks(translationId)
    }
}
