package com.manna.bible.domain.attribution

/**
 * How a translation's text is licensed, as far as the app can assert it
 * (Requirement 12.2, 12.4). The UI maps each value to a localized notice.
 */
enum class TranslationLicense {
    /** Public domain — e.g. the bundled World English Bible (Req 12.2). */
    PUBLIC_DOMAIN,

    /**
     * Provided through the Free Use Bible API under its source license; shown for
     * catalog translations whose specific license the app does not itself capture
     * (Req 12.4).
     */
    SOURCE_PROVIDED
}

/**
 * Attribution for the active translation (Requirement 12.1). The Free Use Bible
 * API (MIT) acknowledgement is always shown by the surface regardless of this
 * value (Req 12.3), so it is not modeled here.
 *
 * @property translationName Active edition name, or null when none is active.
 * @property license The asserted license category, or null when no translation
 *   is active.
 */
data class Attribution(
    val translationName: String?,
    val license: TranslationLicense?
)
