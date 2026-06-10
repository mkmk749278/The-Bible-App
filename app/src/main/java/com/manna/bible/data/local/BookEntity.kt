package com.manna.bible.data.local

import androidx.room.Entity
import androidx.room.Index

/**
 * Room persistence model for a single book of a Bible [TranslationEntity].
 *
 * Stored in the `books` table, keyed by ([translationId], [osisId]) so the same
 * book (e.g. `GEN`) can exist independently for every installed translation. The
 * [orderIndex] preserves the source order; canon-specific ordering is applied at
 * read time via `CanonBookOrdering`, not here.
 */
@Entity(
    tableName = "books",
    primaryKeys = ["translationId", "osisId"],
    indices = [Index("translationId")]
)
data class BookEntity(
    /** Id of the owning translation (FK-by-convention to `translations.id`). */
    val translationId: String,
    /** OSIS book id, e.g. `GEN`, `PSA`. */
    val osisId: String,
    /** Localized book name as supplied by the content source. */
    val name: String,
    /** Testament marker, `OLD` or `NEW`. */
    val testament: String,
    /** Source order of the book within the translation. */
    val orderIndex: Int,
    /** Number of chapters in this book. */
    val chapterCount: Int
)
