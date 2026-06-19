package com.manna.bible.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.manna.bible.domain.sermon.SermonNote

/**
 * Room persistence model for a [SermonNote]. Stored in the `sermon_notes` table.
 */
@Entity(tableName = "sermon_notes")
data class SermonNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val reference: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long
)

/** Maps this entity to its pure-domain [SermonNote]. */
fun SermonNoteEntity.toDomain(): SermonNote = SermonNote(
    id = id,
    title = title,
    reference = reference,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt
)

/** Maps a pure-domain [SermonNote] to a persistable [SermonNoteEntity]. */
fun SermonNote.toEntity(): SermonNoteEntity = SermonNoteEntity(
    id = id,
    title = title,
    reference = reference,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt
)
