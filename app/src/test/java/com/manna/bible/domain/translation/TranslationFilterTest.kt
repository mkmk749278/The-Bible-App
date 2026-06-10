package com.manna.bible.domain.translation

import com.manna.bible.domain.model.CanonProfile
import com.manna.bible.domain.model.CanonType
import com.manna.bible.domain.model.Denomination
import com.manna.bible.domain.model.NumberingScheme
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [DefaultTranslationFilter].
 *
 * Runs on the JVM (JUnit 5) without an emulator. Exercises language + canon
 * filtering for every [CanonType], Catholic deuterocanon ranking, suggested-default
 * resolution order, and the closest-in-language fallback.
 *
 * Validates: Requirements 5
 */
class TranslationFilterTest {

    private val filter = DefaultTranslationFilter()

    // --- helpers -------------------------------------------------------------

    private fun profile(
        canonType: CanonType,
        suggestedTranslationId: String? = null
    ): CanonProfile = CanonProfile(
        denomination = Denomination.SHOW_EVERYTHING,
        canonType = canonType,
        books = emptyList(),
        numberingScheme = NumberingScheme.MASORETIC,
        namingConventionId = null,
        suggestedTranslationId = suggestedTranslationId,
        lectionaryId = null
    )

    private fun translation(
        id: String,
        languageCode: String,
        canonType: CanonType,
        hasDeuterocanon: Boolean,
        name: String = id,
        isDefaultForCanon: Boolean = false
    ) = Translation(
        id = id,
        name = name,
        languageCode = languageCode,
        canonType = canonType,
        hasDeuterocanon = hasDeuterocanon,
        isDefaultForCanon = isDefaultForCanon
    )

    // --- filtering by language (Req 5.3) -------------------------------------

    @Test
    fun `filter keeps only matching language`() {
        val catalog = listOf(
            translation("en_prot", "en", CanonType.PROTESTANT_66, hasDeuterocanon = false),
            translation("ml_prot", "ml", CanonType.PROTESTANT_66, hasDeuterocanon = false)
        )

        val result = filter.filter(catalog, profile(CanonType.PROTESTANT_66), bibleLanguage = "en")

        assertEquals(listOf("en_prot"), result.map { it.id })
    }

    @Test
    fun `filter matches language case-insensitively`() {
        val catalog = listOf(
            translation("en", "EN", CanonType.PROTESTANT_66, hasDeuterocanon = false)
        )

        val result = filter.filter(catalog, profile(CanonType.PROTESTANT_66), bibleLanguage = "en")

        assertEquals(listOf("en"), result.map { it.id })
    }

    // --- filtering by canon for each CanonType (Req 5.3) ---------------------

    @Test
    fun `protestant profile keeps only protestant editions`() {
        val catalog = listOf(
            translation("prot", "en", CanonType.PROTESTANT_66, hasDeuterocanon = false),
            translation("cath", "en", CanonType.CATHOLIC_73, hasDeuterocanon = true),
            translation("orth", "en", CanonType.ORTHODOX_EXPANDED, hasDeuterocanon = true)
        )

        val result = filter.filter(catalog, profile(CanonType.PROTESTANT_66), bibleLanguage = "en")

        assertEquals(listOf("prot"), result.map { it.id })
    }

    @Test
    fun `catholic profile keeps catholic and deuterocanon-bearing editions`() {
        val catalog = listOf(
            translation("cath", "en", CanonType.CATHOLIC_73, hasDeuterocanon = false, name = "Cath"),
            translation("prot_dc", "en", CanonType.PROTESTANT_66, hasDeuterocanon = true, name = "ProtDC"),
            translation("prot_plain", "en", CanonType.PROTESTANT_66, hasDeuterocanon = false, name = "ProtPlain")
        )

        val result = filter.filter(catalog, profile(CanonType.CATHOLIC_73), bibleLanguage = "en")

        // Catholic edition (exact) and the deuterocanon-bearing Protestant edition
        // are kept; the plain Protestant edition is excluded.
        assertEquals(setOf("cath", "prot_dc"), result.map { it.id }.toSet())
    }

    @Test
    fun `orthodox profile keeps orthodox and catholic editions and prefers orthodox`() {
        val catalog = listOf(
            translation("cath", "en", CanonType.CATHOLIC_73, hasDeuterocanon = true, name = "Alpha"),
            translation("orth", "en", CanonType.ORTHODOX_EXPANDED, hasDeuterocanon = true, name = "Zeta"),
            translation("prot", "en", CanonType.PROTESTANT_66, hasDeuterocanon = true, name = "Beta")
        )

        val result = filter.filter(catalog, profile(CanonType.ORTHODOX_EXPANDED), bibleLanguage = "en")

        // Protestant excluded; Orthodox preferred ahead of the Catholic subset.
        assertEquals(listOf("orth", "cath"), result.map { it.id })
    }

    @Test
    fun `all_canons profile keeps every in-language edition`() {
        val catalog = listOf(
            translation("prot", "en", CanonType.PROTESTANT_66, hasDeuterocanon = false),
            translation("cath", "en", CanonType.CATHOLIC_73, hasDeuterocanon = true),
            translation("orth", "en", CanonType.ORTHODOX_EXPANDED, hasDeuterocanon = true),
            translation("ml_cath", "ml", CanonType.CATHOLIC_73, hasDeuterocanon = true)
        )

        val result = filter.filter(catalog, profile(CanonType.ALL_CANONS), bibleLanguage = "en")

        assertEquals(setOf("prot", "cath", "orth"), result.map { it.id }.toSet())
    }

    // --- catholic ranking: deuterocanon editions first (Req 5.4) -------------

    @Test
    fun `catholic profile ranks deuterocanon editions above non-deuterocanon`() {
        val catalog = listOf(
            translation("ml_orth", "ml", CanonType.ORTHODOX_EXPANDED, hasDeuterocanon = true, name = "Zeta"),
            translation("ml_cath", "ml", CanonType.CATHOLIC_73, hasDeuterocanon = true, name = "Alpha"),
            translation("ml_exact", "ml", CanonType.CATHOLIC_73, hasDeuterocanon = false, name = "Beta")
        )

        val result = filter.filter(catalog, profile(CanonType.CATHOLIC_73), bibleLanguage = "ml")

        // Deuterocanon-bearing editions rank first (sorted by name: Alpha, Zeta),
        // then the exact-canon edition lacking deuterocanon (Beta).
        assertEquals(listOf("ml_cath", "ml_orth", "ml_exact"), result.map { it.id })

        val firstNonDeutero = result.indexOfFirst { !it.hasDeuterocanon }
        val lastDeutero = result.indexOfLast { it.hasDeuterocanon }
        assertTrue(lastDeutero < firstNonDeutero, "deuterocanon editions must precede non-deuterocanon")
    }

    @Test
    fun `catholic ranking breaks ties by default flag then name`() {
        val catalog = listOf(
            translation("plain", "en", CanonType.CATHOLIC_73, hasDeuterocanon = true, name = "Zulu"),
            translation("default", "en", CanonType.CATHOLIC_73, hasDeuterocanon = true, name = "Yankee", isDefaultForCanon = true)
        )

        val result = filter.filter(catalog, profile(CanonType.CATHOLIC_73), bibleLanguage = "en")

        // Both deuterocanon-bearing → default edition ranks first despite later name.
        assertEquals(listOf("default", "plain"), result.map { it.id })
    }

    // --- suggestedDefault resolution order (Req 5) ---------------------------

    @Test
    fun `suggestedDefault prefers the default-for-canon edition`() {
        val catalog = listOf(
            translation("en_a", "en", CanonType.CATHOLIC_73, hasDeuterocanon = true, name = "Alpha"),
            translation("en_b", "en", CanonType.CATHOLIC_73, hasDeuterocanon = true, name = "Beta", isDefaultForCanon = true)
        )

        // Even with a suggestion pointing elsewhere, the default edition wins.
        val result = filter.suggestedDefault(
            catalog,
            profile(CanonType.CATHOLIC_73, suggestedTranslationId = "en_a"),
            bibleLanguage = "en"
        )

        assertEquals("en_b", result?.id)
    }

    @Test
    fun `suggestedDefault uses profile suggestion when no default flag`() {
        val catalog = listOf(
            translation("en_a", "en", CanonType.CATHOLIC_73, hasDeuterocanon = true, name = "Alpha"),
            translation("en_b", "en", CanonType.CATHOLIC_73, hasDeuterocanon = true, name = "Beta")
        )

        val result = filter.suggestedDefault(
            catalog,
            profile(CanonType.CATHOLIC_73, suggestedTranslationId = "en_b"),
            bibleLanguage = "en"
        )

        assertEquals("en_b", result?.id)
    }

    @Test
    fun `suggestedDefault returns first filtered when no default and no suggestion`() {
        val catalog = listOf(
            translation("en_b", "en", CanonType.CATHOLIC_73, hasDeuterocanon = true, name = "Beta"),
            translation("en_a", "en", CanonType.CATHOLIC_73, hasDeuterocanon = true, name = "Alpha")
        )

        // No default, no suggestion → first of the ranked list (by name → Alpha).
        val result = filter.suggestedDefault(
            catalog,
            profile(CanonType.CATHOLIC_73),
            bibleLanguage = "en"
        )

        assertEquals("en_a", result?.id)
    }

    // --- fallback (Req 5.6) --------------------------------------------------

    @Test
    fun `suggestedDefault falls back to closest in-language edition when no canon match`() {
        val catalog = listOf(
            translation("en_prot", "en", CanonType.PROTESTANT_66, hasDeuterocanon = false, name = "Zulu"),
            translation("en_prot_default", "en", CanonType.PROTESTANT_66, hasDeuterocanon = false, name = "Mike", isDefaultForCanon = true),
            translation("ml_cath", "ml", CanonType.CATHOLIC_73, hasDeuterocanon = true)
        )

        // No Catholic-compatible edition in "en" → relax canon, keep language,
        // preferring the default edition.
        val result = filter.suggestedDefault(
            catalog,
            profile(CanonType.CATHOLIC_73),
            bibleLanguage = "en"
        )

        assertEquals("en_prot_default", result?.id)
    }

    @Test
    fun `suggestedDefault fallback picks by name when no default flag in language`() {
        val catalog = listOf(
            translation("en_prot_z", "en", CanonType.PROTESTANT_66, hasDeuterocanon = false, name = "Zulu"),
            translation("en_prot_a", "en", CanonType.PROTESTANT_66, hasDeuterocanon = false, name = "Alpha")
        )

        val result = filter.suggestedDefault(
            catalog,
            profile(CanonType.CATHOLIC_73),
            bibleLanguage = "en"
        )

        assertEquals("en_prot_a", result?.id)
    }

    @Test
    fun `suggestedDefault returns null when language absent entirely`() {
        val catalog = listOf(
            translation("ml_prot", "ml", CanonType.PROTESTANT_66, hasDeuterocanon = false),
            translation("te_cath", "te", CanonType.CATHOLIC_73, hasDeuterocanon = true)
        )

        val result = filter.suggestedDefault(
            catalog,
            profile(CanonType.CATHOLIC_73),
            bibleLanguage = "en"
        )

        assertNull(result)
    }
}
