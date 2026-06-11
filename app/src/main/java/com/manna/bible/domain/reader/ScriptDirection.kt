package com.manna.bible.domain.reader

/**
 * Resolves the layout/text direction for a Bible language (Requirement 14.4).
 *
 * Bible text in right-to-left scripts — Urdu, Arabic, Persian, Hebrew and related
 * languages — must render and lay out right-to-left. This is a pure lookup over the
 * language's base code (the segment before any `-`/`_` region suffix), so both
 * `"ur"` and `"ur-PK"` resolve identically.
 *
 * Pure Kotlin — no Android dependencies.
 */
object ScriptDirection {

    /** Base language codes whose scripts are written right-to-left. */
    private val RIGHT_TO_LEFT = setOf(
        "ar", // Arabic
        "ur", // Urdu
        "fa", // Persian / Farsi
        "he", // Hebrew (modern)
        "iw", // Hebrew (legacy code)
        "ps", // Pashto
        "sd", // Sindhi
        "ug", // Uyghur
        "yi", // Yiddish
        "dv", // Divehi / Maldivian
        "ku", // Kurdish (Sorani)
        "ks"  // Kashmiri
    )

    /**
     * Returns true when [languageCode] is written right-to-left. Null/blank codes,
     * and any language not in the RTL set, resolve to left-to-right.
     */
    fun isRightToLeft(languageCode: String?): Boolean {
        if (languageCode.isNullOrBlank()) return false
        val base = languageCode.trim().lowercase()
            .substringBefore('-')
            .substringBefore('_')
        return base in RIGHT_TO_LEFT
    }
}
