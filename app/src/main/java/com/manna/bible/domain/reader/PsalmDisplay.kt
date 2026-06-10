package com.manna.bible.domain.reader

import com.manna.bible.domain.canon.PsalmNumberingMapper
import com.manna.bible.domain.model.CanonProfile

/**
 * Reader-facing Psalm number formatting that honours a [CanonProfile]'s
 * [CanonProfile.numberingScheme].
 *
 * Psalm content is stored under the canonical Masoretic numbering. When the
 * active scheme is Septuagint (Catholic/Orthodox), readers expect the displayed
 * Psalm number to follow that convention. This object converts at the
 * presentation layer by delegating to [PsalmNumberingMapper] — it never
 * reimplements the offset (see Requirement 7).
 *
 * Pure Kotlin — no Android dependencies.
 */
object PsalmDisplay {

    /** OSIS id of the Book of Psalms; the only book whose chapter is renumbered. */
    private const val PSALMS_OSIS_ID = "PSA"

    /**
     * Converts a canonical (Masoretic) Psalm number to the number to display
     * under [profile]'s numbering scheme.
     */
    fun displayPsalmNumber(profile: CanonProfile, masoreticPsalm: Int): Int =
        PsalmNumberingMapper.toDisplay(profile.numberingScheme, masoreticPsalm)

    /**
     * Converts a displayed Psalm number back to the canonical (Masoretic) number
     * used for storage and lookup.
     */
    fun canonicalPsalmNumber(profile: CanonProfile, displayPsalm: Int): Int =
        PsalmNumberingMapper.toCanonical(profile.numberingScheme, displayPsalm)

    /**
     * Formats a reader-facing reference of the form `"BOOK chapter:verse"`.
     *
     * For Psalms ([bookOsisId] == "PSA") the chapter is converted to the display
     * Psalm number under the profile's scheme. For every other book the chapter
     * is used unchanged.
     *
     * @param profile the active canon profile.
     * @param bookOsisId OSIS id of the book being referenced.
     * @param chapter the canonical chapter number (Masoretic for Psalms).
     * @param verse the verse number.
     * @return e.g. `"PSA 10:1"` for Septuagint, `"GEN 2:3"` for a non-Psalm book.
     */
    fun displayReference(
        profile: CanonProfile,
        bookOsisId: String,
        chapter: Int,
        verse: Int
    ): String {
        val displayChapter =
            if (bookOsisId == PSALMS_OSIS_ID) displayPsalmNumber(profile, chapter) else chapter
        return "$bookOsisId $displayChapter:$verse"
    }
}
