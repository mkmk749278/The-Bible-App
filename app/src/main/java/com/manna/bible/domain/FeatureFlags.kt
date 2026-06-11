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

    /** On-device Gemini Nano AI; requires Android 16+ (Phase 3). */
    const val GEMINI_NANO_AI: Boolean = false

    /** Stealth / Persecution Mode (Phase 2). */
    const val STEALTH_MODE: Boolean = false
}
