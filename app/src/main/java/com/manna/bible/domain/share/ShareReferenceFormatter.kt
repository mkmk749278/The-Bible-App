package com.manna.bible.domain.share

import com.manna.bible.domain.model.CanonProfile
import com.manna.bible.domain.reader.PsalmDisplay
import javax.inject.Inject

/**
 * Formats verse references and share text in the active tradition's convention
 * (see Requirement 17).
 *
 * Two tradition-specific concerns are honoured:
 *
 * - **Psalm numbering** — for Psalms the displayed chapter follows the profile's
 *   [CanonProfile.numberingScheme]. The offset is never reimplemented here; it
 *   is delegated to [PsalmDisplay] (which in turn uses `PsalmNumberingMapper`),
 *   satisfying Requirement 17.1 and 17.2.
 * - **Book naming** — the book name comes from the pluggable [BookNameProvider]
 *   using the profile's [CanonProfile.namingConventionId] and the active Bible
 *   language, satisfying Requirement 17.3.
 *
 * Pure Kotlin — no Android dependencies.
 */
class ShareReferenceFormatter @Inject constructor(
    private val bookNameProvider: BookNameProvider
) {

    /** OSIS id of the Book of Psalms; the only book whose chapter is renumbered. */
    private val psalmsOsisId = "PSA"

    /**
     * The display chapter for [osisId] under [profile]: the display Psalm number
     * for Psalms, otherwise the chapter unchanged.
     */
    private fun displayChapter(profile: CanonProfile, osisId: String, chapter: Int): Int =
        if (osisId == psalmsOsisId) PsalmDisplay.displayPsalmNumber(profile, chapter) else chapter

    /**
     * Formats a single-verse reference of the form `"<Name> <chapter>:<verse>"`.
     *
     * @param profile the active canon profile.
     * @param bibleLanguage the active Bible text language code, or null.
     * @param osisId the OSIS id of the book.
     * @param chapter the canonical chapter number (Masoretic for Psalms).
     * @param verse the verse number.
     */
    fun formatReference(
        profile: CanonProfile,
        bibleLanguage: String?,
        osisId: String,
        chapter: Int,
        verse: Int
    ): String {
        val name = bookNameProvider.displayName(profile.namingConventionId, bibleLanguage, osisId)
        return "$name ${displayChapter(profile, osisId, chapter)}:$verse"
    }

    /**
     * Formats a verse-range reference of the form `"<Name> <chapter>:<start>-<end>"`.
     *
     * @param profile the active canon profile.
     * @param bibleLanguage the active Bible text language code, or null.
     * @param osisId the OSIS id of the book.
     * @param chapter the canonical chapter number (Masoretic for Psalms).
     * @param startVerse the first verse of the range.
     * @param endVerse the last verse of the range.
     */
    fun formatRange(
        profile: CanonProfile,
        bibleLanguage: String?,
        osisId: String,
        chapter: Int,
        startVerse: Int,
        endVerse: Int
    ): String {
        val name = bookNameProvider.displayName(profile.namingConventionId, bibleLanguage, osisId)
        return "$name ${displayChapter(profile, osisId, chapter)}:$startVerse-$endVerse"
    }

    /**
     * Composes a share-ready string of the verse text quoted, followed by an
     * em-dash and the formatted reference, e.g. (newline shown as ↵):
     *
     *     "In the beginning..."↵— GEN 1:1
     *
     * Suitable for messaging apps such as WhatsApp.
     *
     * @param profile the active canon profile.
     * @param bibleLanguage the active Bible text language code, or null.
     * @param osisId the OSIS id of the book.
     * @param chapter the canonical chapter number (Masoretic for Psalms).
     * @param verse the verse number.
     * @param verseText the verse text to quote.
     */
    fun formatShareText(
        profile: CanonProfile,
        bibleLanguage: String?,
        osisId: String,
        chapter: Int,
        verse: Int,
        verseText: String
    ): String {
        val reference = formatReference(profile, bibleLanguage, osisId, chapter, verse)
        return "\"$verseText\"\n— $reference"
    }
}
