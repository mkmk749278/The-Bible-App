package com.manna.bible.domain.download

/**
 * Promotes an in-flight Bible download to a foreground service so it keeps running when
 * the app is backgrounded or the screen is off, and shows a progress notification
 * (Req 5). The download itself runs on the app-lifetime download scope; this only keeps
 * the process alive and visible while it works.
 *
 * Implementations must never throw — if a foreground service cannot be started (e.g. a
 * background-start restriction), the download still proceeds, just without a notification.
 */
interface DownloadForegroundController {
    /** Ensures the download foreground service is running. Safe to call repeatedly. */
    fun ensureRunning()
}
