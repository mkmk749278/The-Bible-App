package com.manna.bible.domain.attribution

import com.manna.bible.domain.model.CanonType
import com.manna.bible.domain.translation.Translation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit tests for [DefaultAttributionProvider] (Requirement 12.1, 12.2, 12.4).
 */
class AttributionProviderTest {

    private val provider = DefaultAttributionProvider()

    private fun translation(
        id: String = "web",
        name: String = "World English Bible",
        isBundled: Boolean = false
    ) = Translation(
        id = id,
        name = name,
        languageCode = "en",
        canonType = CanonType.PROTESTANT_66,
        hasDeuterocanon = false,
        isDownloaded = true,
        isBundled = isBundled
    )

    @Test
    @DisplayName("null translation yields an empty attribution (Req 12.1)")
    fun nullTranslation() {
        val attribution = provider.attributionFor(null)
        assertNull(attribution.translationName)
        assertNull(attribution.license)
    }

    @Test
    @DisplayName("the bundled World English Bible is public domain (Req 12.2)")
    fun bundledWebIsPublicDomain() {
        val attribution = provider.attributionFor(translation(name = "World English Bible", isBundled = true))
        assertEquals("World English Bible", attribution.translationName)
        assertEquals(TranslationLicense.PUBLIC_DOMAIN, attribution.license)
    }

    @Test
    @DisplayName("a bundled but licensed edition (e.g. IRV) is source-provided, not public domain (Req 12.4)")
    fun bundledLicensedEditionIsSourceProvided() {
        val attribution = provider.attributionFor(
            translation(id = "tam_irv", name = "Tamil Indian Revised Version (IRV)", isBundled = true)
        )
        assertEquals("Tamil Indian Revised Version (IRV)", attribution.translationName)
        assertEquals(TranslationLicense.SOURCE_PROVIDED, attribution.license)
    }

    @Test
    @DisplayName("the World English Bible is public domain by name even when not bundled (Req 12.2)")
    fun webByNameIsPublicDomain() {
        val attribution = provider.attributionFor(translation(name = "World English Bible", isBundled = false))
        assertEquals(TranslationLicense.PUBLIC_DOMAIN, attribution.license)
    }

    @Test
    @DisplayName("other catalog editions are reported as source-provided (Req 12.4)")
    fun otherEditionsAreSourceProvided() {
        val attribution = provider.attributionFor(
            translation(id = "kjv", name = "King James Version", isBundled = false)
        )
        assertEquals("King James Version", attribution.translationName)
        assertEquals(TranslationLicense.SOURCE_PROVIDED, attribution.license)
    }
}
