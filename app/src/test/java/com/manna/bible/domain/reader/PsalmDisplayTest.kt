package com.manna.bible.domain.reader

import com.manna.bible.domain.model.CanonProfile
import com.manna.bible.domain.model.CanonType
import com.manna.bible.domain.model.Denomination
import com.manna.bible.domain.model.NumberingScheme
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Unit tests for [PsalmDisplay].
 *
 * Runs on the JVM (JUnit 5) without an emulator. Verifies that reader-facing
 * Psalm numbering and reference formatting honour the profile's numbering
 * scheme by delegating to [com.manna.bible.domain.canon.PsalmNumberingMapper].
 *
 * Validates: Requirements 7
 */
class PsalmDisplayTest {

    private fun profile(scheme: NumberingScheme) =
        CanonProfile(
            denomination = if (scheme == NumberingScheme.SEPTUAGINT) {
                Denomination.CATHOLIC
            } else {
                Denomination.PROTESTANT_OTHER
            },
            canonType = if (scheme == NumberingScheme.SEPTUAGINT) {
                CanonType.CATHOLIC_73
            } else {
                CanonType.PROTESTANT_66
            },
            books = emptyList(),
            numberingScheme = scheme,
            namingConventionId = null,
            suggestedTranslationId = null,
            lectionaryId = null
        )

    private val septuagint = profile(NumberingScheme.SEPTUAGINT)
    private val masoretic = profile(NumberingScheme.MASORETIC)

    // --- displayPsalmNumber --------------------------------------------------

    @Test
    fun `septuagint shifts masoretic 11 to display 10`() {
        assertEquals(10, PsalmDisplay.displayPsalmNumber(septuagint, 11))
    }

    @Test
    fun `masoretic display number is unchanged`() {
        assertEquals(11, PsalmDisplay.displayPsalmNumber(masoretic, 11))
    }

    // --- canonicalPsalmNumber ------------------------------------------------

    @Test
    fun `septuagint canonical of display 10 is masoretic 11`() {
        assertEquals(11, PsalmDisplay.canonicalPsalmNumber(septuagint, 10))
    }

    @Test
    fun `masoretic canonical number is unchanged`() {
        assertEquals(10, PsalmDisplay.canonicalPsalmNumber(masoretic, 10))
    }

    // --- displayReference ----------------------------------------------------

    @Test
    fun `septuagint psalm reference uses display number`() {
        assertEquals("PSA 10:1", PsalmDisplay.displayReference(septuagint, "PSA", 11, 1))
    }

    @Test
    fun `masoretic psalm reference uses chapter as-is`() {
        assertEquals("PSA 11:1", PsalmDisplay.displayReference(masoretic, "PSA", 11, 1))
    }

    @Test
    fun `non-psalm reference is unaffected by septuagint scheme`() {
        assertEquals("GEN 2:3", PsalmDisplay.displayReference(septuagint, "GEN", 2, 3))
    }
}
