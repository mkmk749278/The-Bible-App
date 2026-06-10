package com.manna.bible.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.manna.bible.domain.model.Note

/**
 * Room persistence model for a [Note] attached to a verse or chapter.
 *
 * Stored in the `notes` table. Exactly one of [verseRef] (e.g. `"PSA.23.1"`) or
 * [chapterRef] (e.g. `"PSA.23"`) is expected to be set; both embed the OSIS book
 * id before the first `.`. Rows are never deleted on a canon change — only hidden
 * at the view layer (Req 12).
 */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val verseRef: String?,
    val chapterRef: String?,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long
)

/** Maps this entity to its pure-domain [Note]. */
fun NoteEntity.toDomain(): Note = Note(
    id = id,
    verseRef = verseRef,
    chapterRef = chapterRef,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt
)

/** Maps a pure-domain [Note] to a persistable [NoteEntity]. */
fun Note.toEntity(): NoteEntity = NoteEntity(
    id = id,
    verseRef = verseRef,
    chapterRef = chapterRef,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt
)
