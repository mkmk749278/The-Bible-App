package com.manna.bible.domain.audio

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Offline scripture read-aloud (Requirement 9). Reads a chapter's verses in order
 * using on-device Android TextToSpeech, with play/pause/stop and a 0.5x–2.0x speed
 * control. The reader observes [state] to indicate the verse being read and reacts
 * to [completionEvents] to honor the user's continuous-play preference (Req 9.7).
 *
 * Pure Kotlin — no Android dependencies. The Android `TextToSpeech` integration is
 * isolated behind [SpeechEngine] so the verse-advance logic stays JVM-testable.
 */
interface TtsReader {

    /** Current playback snapshot (status, spoken verse, speed, voice availability). */
    val state: StateFlow<TtsState>

    /**
     * Emits once each time playback reaches the natural end of a queued chapter
     * (Req 9.7). Does not emit on an explicit [stop]. The reader uses this to
     * continue to the next chapter when continuous play is enabled.
     */
    val completionEvents: Flow<Unit>

    /**
     * Begins reading [verses] in order (Req 9.1), interrupting any current playback.
     * [languageTag] (BCP-47, e.g. `"ta"`) selects a matching on-device voice where
     * available; when none exists the engine falls back to the default voice and
     * flags it via [TtsState.voiceUnavailable] (Req 9.5, 9.6). No-op when empty.
     */
    fun play(verses: List<TtsVerse>, languageTag: String?)

    /** Pauses on the current verse; safe to call when not playing (Req 9.3). */
    fun pause()

    /** Resumes the current verse from its start; safe to call when not paused (Req 9.3). */
    fun resume()

    /** Stops playback and clears the queue without emitting completion (Req 9.3). */
    fun stop()

    /** Sets the playback speed, clamped to [MIN_SPEED]..[MAX_SPEED] (Req 9.4). */
    fun setSpeed(speed: Float)

    /** Releases engine resources. Called when the app no longer needs audio. */
    fun shutdown()

    companion object {
        const val MIN_SPEED = 0.5f
        const val MAX_SPEED = 2.0f
        const val DEFAULT_SPEED = 1.0f
    }
}
