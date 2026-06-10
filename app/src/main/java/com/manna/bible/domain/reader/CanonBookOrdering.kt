package com.manna.bible.domain.reader

import com.manna.bible.domain.model.CanonBook
import com.manna.bible.domain.model.CanonProfile

/**
 * Reader-facing helper that applies a [CanonProfile]'s book ordering.
 *
 * Every downstream reader screen consumes the active [CanonProfile] rather than
 * re-deriving canon membership or ordering. This object centralises that logic
 * so book listing and navigation stay consistent with the tradition's canon
 * (see Requirement 6).
 *
 * All operations are pure functions over the profile's [CanonProfile.books];
 * no mutation, no Android dependencies.
 */
object CanonBookOrdering {

    /**
     * Returns the canon's books sorted ascending by [CanonBook.orderIndex].
     *
     * The sort is stable: books sharing an [CanonBook.orderIndex] retain their
     * relative position from [CanonProfile.books].
     *
     * @param profile the active canon profile.
     * @return a new list ordered for display; never the original instance.
     */
    fun orderedBooks(profile: CanonProfile): List<CanonBook> =
        profile.books.sortedBy { it.orderIndex }

    /**
     * Returns the [CanonBook.orderIndex] for [osisId] within [profile], or null
     * if the book is not part of the canon.
     */
    fun orderIndexOf(profile: CanonProfile, osisId: String): Int? =
        profile.books.firstOrNull { it.osisId == osisId }?.orderIndex

    /**
     * Returns true if [osisId] belongs to [profile]'s canon.
     */
    fun isBookInCanon(profile: CanonProfile, osisId: String): Boolean =
        profile.books.any { it.osisId == osisId }

    /**
     * Returns the book immediately following [osisId] in the canon's ordering,
     * or null if [osisId] is the last book or is not in the canon.
     */
    fun nextBook(profile: CanonProfile, osisId: String): CanonBook? {
        val ordered = orderedBooks(profile)
        val index = ordered.indexOfFirst { it.osisId == osisId }
        if (index < 0) return null
        return ordered.getOrNull(index + 1)
    }

    /**
     * Returns the book immediately preceding [osisId] in the canon's ordering,
     * or null if [osisId] is the first book or is not in the canon.
     */
    fun previousBook(profile: CanonProfile, osisId: String): CanonBook? {
        val ordered = orderedBooks(profile)
        val index = ordered.indexOfFirst { it.osisId == osisId }
        if (index <= 0) return null
        return ordered.getOrNull(index - 1)
    }

    /**
     * Returns the ordered books a reader should currently see.
     *
     * Deuterocanonical inclusion for the Protestant canon is governed by canon
     * selection (the `showDeuterocanonical` toggle swaps the active canon), not
     * by hiding books within a profile. This helper therefore simply returns the
     * ordered books of whichever canon is active; [showDeuterocanonical] is
     * accepted for call-site symmetry and currently has no filtering effect.
     */
    fun visibleBooks(profile: CanonProfile, showDeuterocanonical: Boolean): List<CanonBook> =
        orderedBooks(profile)
}
