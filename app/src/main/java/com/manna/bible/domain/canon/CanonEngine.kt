package com.manna.bible.domain.canon

import com.manna.bible.domain.model.CanonProfile
import com.manna.bible.domain.model.CanonType
import com.manna.bible.domain.model.Denomination

/**
 * Derives canon configuration from a chosen [Denomination].
 *
 * The engine maps a denomination to its [CanonType] and assembles the single
 * derived [CanonProfile] that every downstream consumer (reader, search, share,
 * lectionary) reads from. It is pure domain logic with no Android dependencies so
 * it can be exercised in JVM unit tests without an emulator.
 */
interface CanonEngine {

    /**
     * Maps a [Denomination] to its associated [CanonType] (Requirement 3).
     */
    fun canonTypeFor(denomination: Denomination): CanonType

    /**
     * Builds the [CanonProfile] for the given [denomination] and [bibleLanguage].
     *
     * The canon's book set, ordering, and numbering scheme are sourced from the
     * bundled canon definition; the profile's books are returned ordered by their
     * `orderIndex`.
     */
    suspend fun profileFor(
        denomination: Denomination,
        bibleLanguage: String
    ): CanonProfile
}
