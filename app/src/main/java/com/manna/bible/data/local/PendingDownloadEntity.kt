package com.manna.bible.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room persistence model for a translation that has been requested for download
 * while offline (or that failed and is awaiting retry).
 *
 * Stored in the `pending_downloads` table, keyed by [translationId] so requesting
 * the same translation twice is idempotent. [requestedAt] records when the request
 * was made (epoch millis) for ordering/diagnostics.
 */
@Entity(tableName = "pending_downloads")
data class PendingDownloadEntity(
    @PrimaryKey val translationId: String,
    val requestedAt: Long
)
