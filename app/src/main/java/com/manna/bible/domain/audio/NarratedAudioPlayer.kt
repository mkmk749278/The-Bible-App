package com.manna.bible.domain.audio

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Streams a single human-narrated chapter audio track (Requirement 9.8), as an
 * alternative to on-device [TtsReader]. The narration is chapter-level — there is no
 * per-verse timing — so [TtsState.currentVerse] stays null and the reader does not
 * highlight individual verses during narrated playback.
 *
 * The reader observes [state] to drive the same audio bar it uses for TTS, advances
 * to the next chapter on [completionEvents] when continuous play is on, and falls
 * back to TTS for the current chapter when [errorEvents] fires (e.g. the stream
 * could not be prepared). The Android `ExoPlayer` integration lives behind this seam
 * so the routing logic in the ViewModel stays JVM-testable.
 */
interface NarratedAudioPlayer {

    /** Current playback snapshot (status + speed). [TtsState.currentVerse] is always null. */
    val state: StateFlow<TtsState>

    /** Emits once when a chapter finishes naturally (not on [stop]) — drives continuous play. */
    val completionEvents: Flow<Unit>

    /**
     * Emits when the current track cannot be played (offline, bad URL, decode error).
     * The reader reacts by falling back to on-device TTS for the same chapter, so a
     * listening user always hears something.
     */
    val errorEvents: Flow<Unit>

    /** Streams [url] from the start at [speed] (0.5x–2.0x), interrupting any current track. */
    fun play(url: String, speed: Float = TtsReader.DEFAULT_SPEED)

    /** Pauses playback; safe to call when not playing. */
    fun pause()

    /** Resumes playback; safe to call when not paused. */
    fun resume()

    /** Stops playback and releases the stream without emitting completion. */
    fun stop()

    /** Sets the playback speed, clamped to [TtsReader.MIN_SPEED]..[TtsReader.MAX_SPEED]. */
    fun setSpeed(speed: Float)
}
