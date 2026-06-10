package com.manna.bible.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.manna.bible.domain.model.Highlight

/**
 * Room persistence model for a verse [Highlight].
 *
 * Stored in the `highlights` table. [verseRef] is a canonical verse reference such
 * as `"GEN.1.1"`; the book id is the segment before the first `.`. Rows are never
 * deleted on a canon change — only hidden at the view layer (Req 12).
 */
@Entity(tableName = "highlights")
data class HighlightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val verseRef: String,
    val colorArgb: Int,
    val createdAt: Long
)

/** Maps this entity to its pure-domain [Highlight]. */
fun HighlightEntity.toDomain(): Highlight = Highlight(
    id = id,
    verseRef = verseRef,
    colorArgb = colorArgb,
    createdAt = createdAt
)

/** Maps a pure-domain [Highlight] to a persistable [HighlightEntity]. */
fun Highlight.toEntity(): HighlightEntity = HighlightEntity(
    id = id,
    verseRef = verseRef,
    colorArgb = colorArgb,
    createdAt = createdAt
)
