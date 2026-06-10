package com.manna.bible.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.manna.bible.domain.model.Bookmark

/**
 * Room persistence model for a verse [Bookmark].
 *
 * Stored in the `bookmarks` table. [verseRef] is a canonical verse reference such
 * as `"TOB.3.2"`. Rows are never deleted on a canon change — only hidden at the
 * view layer (Req 12).
 */
@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val verseRef: String,
    val label: String?,
    val createdAt: Long
)

/** Maps this entity to its pure-domain [Bookmark]. */
fun BookmarkEntity.toDomain(): Bookmark = Bookmark(
    id = id,
    verseRef = verseRef,
    label = label,
    createdAt = createdAt
)

/** Maps a pure-domain [Bookmark] to a persistable [BookmarkEntity]. */
fun Bookmark.toEntity(): BookmarkEntity = BookmarkEntity(
    id = id,
    verseRef = verseRef,
    label = label,
    createdAt = createdAt
)
