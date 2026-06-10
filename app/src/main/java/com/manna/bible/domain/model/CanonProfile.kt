package com.manna.bible.domain.model

/**
 * The single derived configuration object produced from a [Denomination].
 *
 * Every downstream consumer (reader, search, share, lectionary) reads from the
 * persisted [CanonProfile] rather than re-deriving from the [Denomination].
 *
 * @property denomination The tradition this profile was derived from.
 * @property canonType The canon (book set) for the tradition.
 * @property books The ordered list of books for the canon.
 * @property numberingScheme The Psalm/verse numbering convention.
 * @property namingConventionId Denomination-specific naming convention id; null → translation default.
 * @property suggestedTranslationId Suggested translation id, if one is recommended.
 * @property lectionaryId Associated lectionary id, if available.
 *
 * Pure Kotlin — no Android dependencies.
 */
data class CanonProfile(
    val denomination: Denomination,
    val canonType: CanonType,
    val books: List<CanonBook>,
    val numberingScheme: NumberingScheme,
    val namingConventionId: String?,
    val suggestedTranslationId: String?,
    val lectionaryId: String?
)
