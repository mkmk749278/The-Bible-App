package com.manna.bible.domain.audio

/**
 * Resolves a narrated-audio URL for a given book+chapter of a translation.
 *
 * This is the data seam for human-narrated playback (Requirement 9.8): a reader that
 * wants to play a chapter aloud asks for its audio URL and streams it. Returns null
 * when the translation has no audio, the chapter can't be resolved, or the device is
 * offline — callers fall back to on-device TTS in that case.
 *
 * Pure Kotlin contract — no Android dependencies — so it stays JVM-testable.
 */
interface ChapterAudioSource {

    /**
     * Returns a streamable audio URL for [translationId]/[osisId]/[chapter], or null
     * when none is available.
     */
    suspend fun audioUrl(translationId: String, osisId: String, chapter: Int): String?
}
