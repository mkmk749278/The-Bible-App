package com.manna.bible.domain.repository

import com.manna.bible.domain.translation.Translation
import kotlinx.coroutines.flow.Flow

/**
 * Offline-first access to the translation catalog and downloads (Req 13).
 *
 * Pure Kotlin — no Android dependencies.
 */
interface TranslationRepository {

    /** Observes the locally cached translation catalog. */
    fun catalog(): Flow<List<Translation>>

    /** Refreshes the cached catalog from the network when a connection is available. */
    suspend fun refreshCatalog()

    /** Attempts to download a translation, queuing it when offline. */
    suspend fun download(id: String): DownloadResult

    /** Marks a translation for download once a connection becomes available. */
    suspend fun markPendingDownload(id: String)

    /** Retries all pending downloads when a connection is available. */
    suspend fun retryPendingDownloads()
}
