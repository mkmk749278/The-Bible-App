package com.manna.bible.domain.audio

/**
 * Playback lifecycle of the offline read-aloud engine (Requirement 9.3).
 *
 * - [IDLE]: nothing queued; the reader shows the play affordance.
 * - [PLAYING]: a verse is actively being spoken.
 * - [PAUSED]: playback is suspended on the current verse and can resume.
 */
enum class TtsStatus { IDLE, PLAYING, PAUSED }

/**
 * A single verse handed to the [TtsReader] for narration.
 *
 * @property verse Canonical (Masoretic) verse number — the stable identity the
 *   reader uses to highlight the line currently being read (Req 9.2).
 * @property text Plain-text verse content to speak.
 */
data class TtsVerse(val verse: Int, val text: String)

/**
 * Immutable snapshot of the read-aloud engine, observed by the reader to drive
 * the audio bar and the spoken-verse indicator (Requirements 9.2, 9.3, 9.4, 9.6).
 *
 * @property status Current playback lifecycle.
 * @property currentVerse Canonical verse number being read, or null when idle.
 * @property speed Active playback speed in 0.5x–2.0x (Req 9.4).
 * @property voiceUnavailable True when no on-device voice matched the requested
 *   language and the engine fell back to the device default (Req 9.6).
 */
data class TtsState(
    val status: TtsStatus = TtsStatus.IDLE,
    val currentVerse: Int? = null,
    val speed: Float = TtsReader.DEFAULT_SPEED,
    val voiceUnavailable: Boolean = false
)
