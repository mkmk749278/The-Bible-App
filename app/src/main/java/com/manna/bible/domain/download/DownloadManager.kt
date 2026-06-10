package com.manna.bible.domain.download

import kotlinx.coroutines.flow.Flow

/**
 * Downloads, tracks, cancels, and deletes on-demand translations for offline use
 * (Req 5).
 *
 * Implementations are offline-first and integrity-preserving:
 *  - a translation is marked downloaded only after every chapter is committed
 *    (Req 5.3, 15.4);
 *  - cancel or failure mid-way removes partial content so nothing partial is ever
 *    presented as complete (Req 5.4, 5.7);
 *  - a download requested while offline is queued and retried on reconnect
 *    (Req 5.6, 11.5).
 */
interface DownloadManager {

    /**
     * The current (or last) download progress, or `null` when idle.
     *
     * Emits a new [DownloadProgress] as chapters complete and a final value with
     * `done = true` on success.
     */
    fun progress(): Flow<DownloadProgress?>

    /**
     * Downloads the translation with the given id.
     *
     * Offline: queues the request and returns [DownloadOutcome.Offline]. Online:
     * fetches books and chapters, stores them, marks the translation downloaded,
     * and returns [DownloadOutcome.Success]; on failure removes any partial content
     * and returns [DownloadOutcome.Failure].
     */
    suspend fun download(translationId: String): DownloadOutcome

    /**
     * Cancels an in-flight download for [translationId] and removes any content
     * stored so far, leaving the translation un-downloaded (Req 5.4).
     */
    suspend fun cancel(translationId: String)

    /**
     * Deletes a downloaded translation's stored content and clears its downloaded
     * marker (Req 5.5).
     *
     * Note: switching the active translation to a bundled fallback when the deleted
     * translation was active is handled by the catalog / active-translation layer,
     * not here. This method only clears content and the downloaded marker.
     */
    suspend fun delete(translationId: String)

    /**
     * Attempts every pending download recorded while offline, if connectivity is
     * now available (Req 5.6, 11.5).
     */
    suspend fun retryPending()
}
