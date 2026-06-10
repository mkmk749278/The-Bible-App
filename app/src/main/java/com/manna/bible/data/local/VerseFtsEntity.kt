package com.manna.bible.data.local

import androidx.room.Entity
import androidx.room.Fts4

/**
 * FTS4 mirror of [VerseEntity.text] backing full-text scripture search.
 *
 * Declared as an external-content FTS table (`contentEntity = VerseEntity`), so
 * Room keeps it in sync with the `verses` table and the two share the same
 * implicit `rowid`. Search queries `MATCH` against [text] in `verses_fts` and join
 * back to `verses` on `rowid` to recover the full [VerseEntity] rows.
 */
@Fts4(contentEntity = VerseEntity::class)
@Entity(tableName = "verses_fts")
data class VerseFtsEntity(
    /** Indexed copy of [VerseEntity.text]. */
    val text: String
)
