package com.manna.bible.domain.download

/**
 * Progress for an in-flight (or just-finished) translation download.
 *
 * Reported by [DownloadManager.progress] so the UI can render a determinate
 * progress indicator (Req 5.2). [completedChapters] advances as each chapter is
 * fetched and persisted; [done] is set once all chapters are committed and the
 * translation is marked downloaded.
 *
 * @property translationId the translation being downloaded.
 * @property completedChapters number of chapters fetched and stored so far.
 * @property totalChapters total chapters expected for the translation.
 * @property done true only after the download has fully committed.
 */
data class DownloadProgress(
    val translationId: String,
    val completedChapters: Int,
    val totalChapters: Int,
    val done: Boolean = false
)

/**
 * Terminal result of a [DownloadManager.download] call.
 *
 * - [Success]: all chapters were stored and the translation marked downloaded.
 * - [Failure]: the download failed or was cancelled mid-way; partial content was
 *   removed and the translation was NOT marked downloaded (Req 5.7).
 * - [Offline]: no network was available, so the request was queued for retry
 *   (Req 5.6).
 */
sealed interface DownloadOutcome {

    /** All chapters committed and the translation marked downloaded (Req 5.3). */
    data object Success : DownloadOutcome

    /**
     * The download failed or was cancelled; no partial content remains and the
     * translation stays un-downloaded.
     *
     * @property reason human-readable failure reason.
     */
    data class Failure(val reason: String) : DownloadOutcome

    /** No connectivity; the request was queued via the pending-download repository. */
    data object Offline : DownloadOutcome
}
