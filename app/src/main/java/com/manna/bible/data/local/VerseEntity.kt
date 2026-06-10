package com.manna.bible.data.local

import androidx.room.Entity
import androidx.room.Index

/**
 * Room persistence model for a single verse.
 *
 * Stored in the `verses` table, keyed by
 * ([translationId], [osisId], [chapter], [verse]). Both [chapter] and [verse] are
 * canonical (Masoretic) numbers, matching the `Verse_Reference` contract used by
 * annotations so references stay stable across denominations and translations.
 *
 * The companion `verses_fts` table ([VerseFtsEntity]) mirrors [text] for fast
 * full-text `MATCH` search and shares this table's implicit `rowid`.
 */
@Entity(
    tableName = "verses",
    primaryKeys = ["translationId", "osisId", "chapter", "verse"],
    indices = [Index(value = ["translationId", "osisId", "chapter"])]
)
data class VerseEntity(
    /** Id of the owning translation. */
    val translationId: String,
    /** OSIS book id this verse belongs to. */
    val osisId: String,
    /** Canonical (Masoretic) chapter number. */
    val chapter: Int,
    /** Canonical (Masoretic) verse number. */
    val verse: Int,
    /** Plain UTF-8 verse text. */
    val text: String
)
