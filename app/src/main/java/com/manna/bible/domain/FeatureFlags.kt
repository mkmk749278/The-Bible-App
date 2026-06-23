package com.manna.bible.domain

/**
 * Compile-time gates for in-progress features (see CLAUDE.md → Feature Flags).
 *
 * Navigation and UI consult these flags so unfinished features can land on the
 * default branch without being exposed to users. Flip a flag to `true` to enable
 * the feature; phase-gated features default to `false` until their phase begins.
 */
object FeatureFlags {

    /** Jesus Events Calendar — liturgical events of Jesus' life (Phase 1). */
    const val JESUS_CALENDAR: Boolean = true

    /** Topical search — curated topic → verses index (Phase 1). */
    const val TOPICAL_SEARCH: Boolean = true

    /** Daily verse reminder notification (Phase 1). */
    const val DAILY_REMINDER: Boolean = true

    /**
     * Human-narrated chapter audio (streamed) instead of on-device TTS (Phase 1,
     * Req 9.8). Wired through Media3 ExoPlayer ([com.manna.bible.domain.audio
     * .NarratedAudioPlayer]); when online and a narrated track exists for the chapter
     * it streams that, otherwise it falls back to on-device TTS — and any stream error
     * falls back too, so a listener always hears something.
     */
    const val NARRATED_AUDIO: Boolean = true

    /**
     * On-device Gemini Nano AI via the AICore Prompt API (Phase 3). When on, the
     * "Explain this passage" engine prefers offline on-device generation on capable
     * devices (Android 14+ with AICore) and falls back to the cloud Gemini engine.
     * Safe to leave on across the device range: unsupported devices fall back.
     */
    const val GEMINI_NANO_AI: Boolean = true

    /**
     * "Explain this passage" — tap a verse for a plain, pastoral explanation
     * (cloud-primary hybrid: Gemini Flash + Room cache, Nano upgrade later). Replaces
     * the Pastor Mode notebook as the study surface (Phase 3, P3-1).
     */
    const val EXPLAIN_PASSAGE: Boolean = true

    /** 3AM / Crisis Mode — compassionate companion for hard moments (Phase 2, P2-1). */
    const val CRISIS_MODE: Boolean = true

    /** Grief Companion — a gentle 30-day journey through loss (Phase 2, P2-1). */
    const val GRIEF_COMPANION: Boolean = true

    /** Prayer Journal + Faith Timeline (Phase 2). */
    const val PRAYER_JOURNAL: Boolean = true

    /** Fasting Companion — time-boxed fasts with focus Scripture (Phase 2). */
    const val FASTING_COMPANION: Boolean = true

    /** Scripture Card Generator — share a verse as an image (Phase 2). */
    const val SCRIPTURE_CARD: Boolean = true

    /**
     * Village Pastor Sermon Helper — an offline library where a preacher writes,
     * keeps, and revisits sermon notes (title · scripture reference · outline) on
     * the device, no connectivity required (Phase 3).
     */
    const val SERMON_HELPER: Boolean = true

    /**
     * Stealth / Persecution Mode (Phase 2). When armed with a PIN, the app opens to a
     * disguised calculator lock and only reveals scripture once the PIN is entered; the
     * PIN is stored as a PBKDF2 hash (never plaintext). Configured in Settings → Privacy.
     */
    const val STEALTH_MODE: Boolean = true

    /**
     * Church Mode — a guided, denomination-aware order of worship (the Holy Mass /
     * Holy Communion) that the user can follow step by step, chosen from the tradition
     * picked at setup (Phase 3).
     */
    const val CHURCH_MODE: Boolean = true

    /**
     * Prayers tab — a grouped hub of guided devotional practices rooted in Indian
     * Christian tradition: the Stations of the Cross, the Rosary (Japamala), the
     * Jesus Prayer, and Paraloka (eternal-life) Scripture. Surfaces as a primary tab
     * (Read · Calendar · Prayers · More) and links every passage back to the reader.
     */
    const val PRAYERS_HUB: Boolean = true

    /** Stations of the Cross — 14-station Via Crucis meditation (Prayers hub). */
    const val STATIONS_OF_THE_CROSS: Boolean = true

    /** The Rosary / Japamala — Joyful, Sorrowful, Glorious & Luminous mysteries. */
    const val ROSARY: Boolean = true

    /** The Jesus Prayer — the Orthodox prayer of the heart, three depths. */
    const val JESUS_PRAYER: Boolean = true

    /** Paraloka — eternal-life Scripture and prayers of Christian hope. */
    const val PARALOKA: Boolean = true

    /** Sramanikal — the 40-day memorial journey for a departed loved one. */
    const val SRAMANIKAL: Boolean = true
}
