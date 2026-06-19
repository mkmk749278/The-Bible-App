package com.manna.bible.domain.sermon

/**
 * A sermon the user (a village pastor / lay preacher) is preparing or has preached.
 *
 * Deliberately simple and offline: a [title], the [reference] it is built on
 * (a free-text scripture reference such as "John 3:16" or "Romans 8"), and the
 * [content] — the outline, notes, and application the preacher writes. Stored
 * locally so a pastor with no connectivity keeps their whole sermon library on
 * the device.
 *
 * Pure Kotlin — no Android dependencies.
 *
 * @property id row id; 0 for a note that has not been saved yet.
 * @property createdAt epoch millis the note was first saved.
 * @property updatedAt epoch millis the note was last saved.
 */
data class SermonNote(
    val id: Long = 0,
    val title: String,
    val reference: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long
)
