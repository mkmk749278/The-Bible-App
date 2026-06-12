package com.manna.bible.domain

/**
 * Compile-time gates for in-progress features (see CLAUDE.md → Feature Flags).
 *
 * Navigation and UI consult these flags so unfinished features can land on the
 * default branch without being exposed to users. Flip a flag to `true` to enable
 * the feature; phase-gated features default to `false` until their phase begins.
 */
object FeatureFlags {
    /** Pastor Mode — guided 5-step sermon preparation (Phase 1). */
    const val PASTOR_MODE: Boolean = true

    /** Jesus Events Calendar — liturgical events of Jesus' life (Phase 1). */
    const val JESUS_CALENDAR: Boolean = true

    /** Topical search — curated topic → verses index (Phase 1). */
    const val TOPICAL_SEARCH: Boolean = true

    /** Daily verse reminder notification (Phase 1). */
    const val DAILY_REMINDER: Boolean = true

    /**
     * Human-narrated chapter audio (streamed) instead of on-device TTS (Phase 1,
     * Req 9.8). Off until the ExoPlayer playback path is wired and device-verified;
     * the [com.manna.bible.domain.audio.ChapterAudioSource] data seam is in place.
     */
    const val NARRATED_AUDIO: Boolean = false

    /** On-device Gemini Nano AI; requires Android 16+ (Phase 3). */
    const val GEMINI_NANO_AI: Boolean = false

    /** 3AM / Crisis Mode — compassionate companion for hard moments (Phase 2, P2-1). */
    const val CRISIS_MODE: Boolean = true

    /** Grief Companion — a gentle 30-day journey through loss (Phase 2, P2-1). */
    const val GRIEF_COMPANION: Boolean = true

    /** Prayer Journal + Faith Timeline (Phase 2). */
    const val PRAYER_JOURNAL: Boolean = true

    /** Fasting Companion — time-boxed fasts with focus Scripture (Phase 2). */
    const val FASTING_COMPANION: Boolean = true

    /** Stealth / Persecution Mode (Phase 2). */
    const val STEALTH_MODE: Boolean = false
}
