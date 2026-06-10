package com.manna.bible.domain.translation

import com.manna.bible.domain.model.CanonProfile

/**
 * Filters and ranks the translation catalog for a chosen [CanonProfile].
 *
 * Given the full catalog, the active canon profile, and the selected Bible
 * language, the filter returns the translations a user should be offered during
 * setup: those matching the language and compatible with the canon, ranked so the
 * most appropriate edition appears first. It also resolves a single suggested
 * default.
 *
 * Pure domain logic with no Android dependencies so it can be exercised in JVM
 * unit tests without an emulator.
 */
interface TranslationFilter {

    /**
     * Returns the catalog entries appropriate for [profile] in [bibleLanguage].
     *
     * Entries are kept when they match [bibleLanguage] and are canon-compatible
     * with [profile]. Results are ranked deterministically; for a `CATHOLIC_73`
     * profile, editions that include the deuterocanonical books rank first
     * (Requirement 5.4), then default editions, then by name.
     *
     * Returns an empty list when nothing matches both the language and the canon;
     * the closest-in-language fallback is handled by [suggestedDefault]
     * (Requirement 5.6).
     */
    fun filter(
        catalog: List<Translation>,
        profile: CanonProfile,
        bibleLanguage: String
    ): List<Translation>

    /**
     * Returns the translation to pre-select for [profile] in [bibleLanguage].
     *
     * Prefers a filtered entry flagged [Translation.isDefaultForCanon]; otherwise
     * a catalog entry matching [CanonProfile.suggestedTranslationId]; otherwise the
     * first filtered entry. When the filtered list is empty, falls back to the
     * closest in-language translation across all canons (default editions first,
     * then by name); returns null only when no translation matches the language at
     * all (Requirement 5.6).
     */
    fun suggestedDefault(
        catalog: List<Translation>,
        profile: CanonProfile,
        bibleLanguage: String
    ): Translation?
}
