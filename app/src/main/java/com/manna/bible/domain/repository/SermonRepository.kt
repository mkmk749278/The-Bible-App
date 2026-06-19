package com.manna.bible.domain.repository

import com.manna.bible.domain.sermon.SermonNote
import kotlinx.coroutines.flow.Flow

/**
 * Stores and observes the user's sermon notes (Village Pastor Sermon Helper,
 * Phase 3). All local — a pastor's sermon library lives on the device and works
 * fully offline.
 */
interface SermonRepository {

    /** Observes all sermon notes, most recently updated first. */
    fun observeSermons(): Flow<List<SermonNote>>

    /** Returns a single sermon note by id, or null when it does not exist. */
    suspend fun get(id: Long): SermonNote?

    /**
     * Creates ([id] <= 0) or updates a sermon note. A note with no title, reference,
     * and content is ignored and returns -1. Returns the saved note's row id. On
     * update, the original [SermonNote.createdAt] is preserved and `updatedAt` is
     * stamped with the current time.
     */
    suspend fun save(id: Long, title: String, reference: String, content: String): Long

    /** Deletes the sermon note with [id]. */
    suspend fun delete(id: Long)
}
