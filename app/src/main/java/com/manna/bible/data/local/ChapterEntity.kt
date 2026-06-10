package com.manna.bible.data.local

import androidx.room.Entity

/**
 * Room persistence model for chapter-level metadata of a [BookEntity].
 *
 * Stored in the `chapters` table, keyed by ([translationId], [osisId], [chapter]).
 * The [chapter] number is always canonical (Masoretic); Septuagint display
 * numbering is computed at render time via `PsalmDisplay`.
 */
@Entity(
    tableName = "chapters",
    primaryKeys = ["translationId", "osisId", "chapter"]
)
data class ChapterEntity(
    /** Id of the owning translation. */
    val translationId: String,
    /** OSIS book id this chapter belongs to. */
    val osisId: String,
    /** Canonical (Masoretic) chapter number. */
    val chapter: Int,
    /** Number of verses in this chapter. */
    val verseCount: Int
)
