package com.manna.bible.domain.repository

import com.manna.bible.domain.model.Prayer
import kotlinx.coroutines.flow.Flow

/**
 * Stores and observes prayer-journal entries (Phase 2). The "Faith Timeline" is
 * simply this list over time, with answered prayers carrying the date they were
 * answered.
 *
 * Local-first and private; pure-domain contract so it stays JVM-testable.
 */
interface PrayerRepository {

    /** Observes all prayers, newest first, emitting on every change. */
    fun observePrayers(): Flow<List<Prayer>>

    /** Records a new active prayer with [content], returning its row id. */
    suspend fun add(content: String): Long

    /** Marks the prayer [id] answered, stamping the moment it was answered. */
    suspend fun markAnswered(id: Long)

    /** Returns an answered prayer [id] to the active list. */
    suspend fun reopen(id: Long)

    /** Permanently deletes the prayer [id]. */
    suspend fun delete(id: Long)
}
