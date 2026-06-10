package com.manna.bible.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Tracks translations queued for download while offline or awaiting retry (Req 13).
 *
 * Pure Kotlin — no Android dependencies.
 */
interface PendingDownloadRepository {

    /** Observes the ids of translations currently pending download. */
    fun pending(): Flow<List<String>>

    /** Queues the given translation id for download. Idempotent. */
    suspend fun add(id: String)

    /** Removes the given translation id from the pending queue. */
    suspend fun remove(id: String)

    /** Returns the pending translation ids as a one-shot snapshot. */
    suspend fun all(): List<String>
}
