package com.manna.bible.domain.repository

/**
 * Outcome of a translation download attempt (Req 13).
 *
 * Pure Kotlin — no Android dependencies.
 */
sealed interface DownloadResult {
    /** The translation downloaded successfully and was marked available offline. */
    data object Success : DownloadResult

    /** The download failed; [reason] carries a non-PII diagnostic message. */
    data class Failure(val reason: String) : DownloadResult

    /** No connection was available; the translation was queued for later. */
    data object Offline : DownloadResult
}
