package com.manna.bible.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.manna.bible.domain.model.Prayer
import com.manna.bible.domain.model.PrayerStatus

/**
 * Room persistence model for a [Prayer]. Stored in the `prayers` table.
 *
 * [status] holds the [PrayerStatus] name. [answeredAt] is null while active.
 */
@Entity(tableName = "prayers")
data class PrayerEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val status: String,
    val createdAt: Long,
    val answeredAt: Long?
)

/** Maps this entity to its pure-domain [Prayer] (unknown status falls back to ACTIVE). */
fun PrayerEntryEntity.toDomain(): Prayer = Prayer(
    id = id,
    content = content,
    status = runCatching { PrayerStatus.valueOf(status) }.getOrDefault(PrayerStatus.ACTIVE),
    createdAt = createdAt,
    answeredAt = answeredAt
)

/** Maps a pure-domain [Prayer] to a persistable [PrayerEntryEntity]. */
fun Prayer.toEntity(): PrayerEntryEntity = PrayerEntryEntity(
    id = id,
    content = content,
    status = status.name,
    createdAt = createdAt,
    answeredAt = answeredAt
)
