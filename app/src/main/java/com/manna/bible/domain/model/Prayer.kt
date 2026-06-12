package com.manna.bible.domain.model

/** Lifecycle of a [Prayer]. */
enum class PrayerStatus { ACTIVE, ANSWERED }

/**
 * A single prayer-journal entry (Phase 2).
 *
 * @property id Stable row id (0 until persisted).
 * @property content What the person is praying about.
 * @property status Whether the prayer is still being prayed or has been answered.
 * @property createdAt Epoch millis when the prayer was first recorded.
 * @property answeredAt Epoch millis when it was marked answered, or null while active.
 *
 * Pure Kotlin — no Android dependencies.
 */
data class Prayer(
    val id: Long = 0,
    val content: String,
    val status: PrayerStatus,
    val createdAt: Long,
    val answeredAt: Long? = null
)
