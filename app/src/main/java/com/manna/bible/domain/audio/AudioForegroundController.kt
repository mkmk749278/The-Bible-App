package com.manna.bible.domain.audio

/**
 * Keeps audio read-aloud playing while the app is backgrounded (Req 9). Read-aloud and
 * narrated playback run on app-scoped engines ([TtsReader], [NarratedAudioPlayer]); on
 * their own the OS may reclaim the process once the app is no longer visible. This
 * watches both engines and, while either is active, runs a `mediaPlayback` foreground
 * service with a notification so playback continues with the screen off.
 *
 * Implementations must never throw: if the foreground service cannot be started, audio
 * simply behaves as before (foreground only).
 */
interface AudioForegroundController {
    /** Begins watching playback state. Safe to call once from app startup. */
    fun start()
}
