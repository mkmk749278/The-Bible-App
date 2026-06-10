package com.manna.bible.domain.share

import javax.inject.Inject

/**
 * Supplies a display name for a book, honouring the active denomination naming
 * convention and/or the translation's default naming (see Requirement 8 and
 * Requirement 17.3).
 *
 * This is a pluggable seam: a real implementation will back this with a
 * localized, convention-aware name table once one exists. Until then,
 * [DefaultBookNameProvider] returns the OSIS id verbatim so the share feature
 * has a stable, dependency-free default.
 *
 * Pure Kotlin — no Android dependencies.
 */
interface BookNameProvider {

    /**
     * Returns the display name for the given book.
     *
     * @param namingConventionId denomination-specific naming convention id, or
     *   null to fall back to the translation/language default.
     * @param bibleLanguage the active Bible text language code (e.g. "ml"), or
     *   null when unknown.
     * @param osisId the OSIS id of the book (e.g. "PSA", "GEN").
     * @return the name to display for the book.
     */
    fun displayName(namingConventionId: String?, bibleLanguage: String?, osisId: String): String
}

/**
 * Default [BookNameProvider] that returns the OSIS id unchanged.
 *
 * Acts as a placeholder until a real, convention-aware localized name table is
 * available. Keeping it injectable lets downstream features depend on the
 * interface today and swap in a richer implementation later without changes.
 *
 * Pure Kotlin — no Android dependencies.
 */
class DefaultBookNameProvider @Inject constructor() : BookNameProvider {

    override fun displayName(
        namingConventionId: String?,
        bibleLanguage: String?,
        osisId: String
    ): String = osisId
}
