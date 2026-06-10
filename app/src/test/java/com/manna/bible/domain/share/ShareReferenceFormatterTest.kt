package com.manna.bible.domain.share

import com.manna.bible.domain.model.CanonProfile
import com.manna.bible.domain.model.CanonType
import com.manna.bible.domain.model.Denomination
import com.manna.bible.domain.model.NumberingScheme
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Unit tests for [ShareReferenceFormatter].
 *
 * Runs on the JVM (JUnit 5) without an emulator. Verifies that shared verse
 * references honour the profile's numbering scheme (delegating to
 * `PsalmNumberingMapper` via `PsalmDisplay`) and the active naming convention
 * (via the pluggable [BookNameProvider]).
 *
 * Validates: Requirements 17
 */
class ShareReferenceFormatterTest {

    private fun profile(scheme: NumberingScheme, namingConventionId: String? = null) =
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
            namingConventionId = namingConventionId,
            suggestedTranslationId = null,
            lectionaryId = null
        )

    /** A [BookNameProvider] that maps specific OSIS ids to custom display names. */
    private class FakeBookNameProvider(
        private val names: Map<String, String>
    ) : BookNameProvider {
        override fun displayName(
            namingConventionId: String?,
            bibleLanguage: String?,
            osisId: String
        ): String = names[osisId] ?: osisId
    }

    private val defaultFormatter = ShareReferenceFormatter(DefaultBookNameProvider())

    private val septuagint = profile(NumberingScheme.SEPTUAGINT)
    private val masoretic = profile(NumberingScheme.MASORETIC)

    // --- formatReference: numbering scheme (Req 17.1 / 17.2) -----------------

    @Test
    fun `septuagint psalm reference uses display number`() {
        assertEquals(
            "PSA 10:1",
            defaultFormatter.formatReference(septuagint, "ml", "PSA", 11, 1)
        )
    }

    @Test
    fun `masoretic psalm reference uses chapter as-is`() {
        assertEquals(
            "PSA 11:1",
            defaultFormatter.formatReference(masoretic, "ml", "PSA", 11, 1)
        )
    }

    @Test
    fun `non-psalm reference is unaffected by septuagint scheme`() {
        assertEquals(
            "GEN 2:3",
            defaultFormatter.formatReference(septuagint, "ml", "GEN", 2, 3)
        )
    }

    // --- formatReference: naming convention hookup (Req 17.3) ----------------

    @Test
    fun `book name comes from the naming-convention provider`() {
        val formatter = ShareReferenceFormatter(
            FakeBookNameProvider(mapOf("PSA" to "Sankeerthanangal"))
        )

        assertEquals(
            "Sankeerthanangal 10:1",
            formatter.formatReference(septuagint, "ml", "PSA", 11, 1)
        )
    }

    // --- formatRange ---------------------------------------------------------

    @Test
    fun `range reference renders start and end verses`() {
        assertEquals(
            "GEN 1:1-3",
            defaultFormatter.formatRange(masoretic, "ml", "GEN", 1, 1, 3)
        )
    }

    @Test
    fun `septuagint range reference uses display psalm number`() {
        assertEquals(
            "PSA 10:1-2",
            defaultFormatter.formatRange(septuagint, "ml", "PSA", 11, 1, 2)
        )
    }

    // --- formatShareText -----------------------------------------------------

    @Test
    fun `share text composes quote em-dash and reference`() {
        assertEquals(
            "\"In the beginning\"\n— GEN 1:1",
            defaultFormatter.formatShareText(masoretic, "ml", "GEN", 1, 1, "In the beginning")
        )
    }
}
