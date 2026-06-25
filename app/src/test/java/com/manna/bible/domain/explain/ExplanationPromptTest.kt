package com.manna.bible.domain.explain

import com.manna.bible.domain.model.Denomination
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** Unit tests for [ExplanationPrompt]. */
class ExplanationPromptTest {

    private fun request(depth: ExplainDepth) = ExplanationRequest(
        osisRef = "GEN.2.1",
        reference = "Genesis 2:1",
        passageText = "Thus the heavens and the earth were finished.",
        uiLanguageCode = "ta",
        depth = depth
    )

    private fun req(denomination: Denomination?, langCode: String = "te") =
        ExplanationRequest(
            osisRef = "JHN.3.16", reference = "John 3:16",
            passageText = "For God so loved the world...",
            uiLanguageCode = langCode, depth = ExplainDepth.PLAIN,
            denomination = denomination
        )

    @Test
    @DisplayName("Plain prompt includes the reference, text, language, and the three parts")
    fun plain() {
        val prompt = ExplanationPrompt.build(request(ExplainDepth.PLAIN))
        assertTrue(prompt.contains("Genesis 2:1"))
        assertTrue(prompt.contains("Thus the heavens and the earth were finished."))
        assertTrue(prompt.contains("'ta'"))
        assertTrue(prompt.contains("context"))
        assertTrue(prompt.contains("apply"))
        // Plain depth does not ask for preaching helps.
        assertFalse(prompt.contains("outline"))
    }

    @Test
    @DisplayName("Preaching prompt adds outline, cross-references, and an illustration")
    fun preaching() {
        val prompt = ExplanationPrompt.build(request(ExplainDepth.PREACHING))
        assertTrue(prompt.contains("outline"))
        assertTrue(prompt.contains("cross-reference"))
        assertTrue(prompt.contains("illustration"))
    }

    @Test
    @DisplayName("All prompts contain India cultural grounding")
    fun allPromptsContainIndiaCulturalGrounding() {
        Denomination.entries.forEach { den ->
            val prompt = ExplanationPrompt.build(req(den))
            assertTrue(
                prompt.contains("India", ignoreCase = true),
                "Expected 'India' in prompt for $den"
            )
        }
    }

    @Test
    @DisplayName("Null denomination contains cultural grounding but no denomination block")
    fun nullDenominationContainsCulturalGroundingButNoDenominationBlock() {
        val prompt = ExplanationPrompt.build(req(null))
        assertTrue(prompt.contains("India", ignoreCase = true))
        assertFalse(prompt.contains("sacramental", ignoreCase = true))
        assertFalse(prompt.contains("patristic", ignoreCase = true))
    }

    @Test
    @DisplayName("Catholic denomination includes sacramental note")
    fun catholicIncludesSacramentalNote() {
        val prompt = ExplanationPrompt.build(req(Denomination.CATHOLIC))
        assertTrue(prompt.contains("sacramental", ignoreCase = true))
    }

    @Test
    @DisplayName("Orthodox denomination includes patristic or liturgical")
    fun orthodoxIncludesPatristicOrLiturgical() {
        val prompt = ExplanationPrompt.build(req(Denomination.ORTHODOX))
        val hasEither = prompt.contains("patristic", ignoreCase = true) ||
            prompt.contains("liturgical", ignoreCase = true)
        assertTrue(hasEither)
    }

    @Test
    @DisplayName("CSI denomination names Church of South India")
    fun csiNamesChurchOfSouthIndia() {
        val prompt = ExplanationPrompt.build(req(Denomination.CSI))
        assertTrue(prompt.contains("Church of South India", ignoreCase = true))
    }

    @Test
    @DisplayName("Mar Thoma denomination names Mar Thoma")
    fun marThomaNamesMarThoma() {
        val prompt = ExplanationPrompt.build(req(Denomination.MAR_THOMA))
        assertTrue(prompt.contains("Mar Thoma", ignoreCase = true))
    }

    @Test
    @DisplayName("PROTESTANT_OTHER has no denomination-specific block")
    fun protestantOtherHasNoDenominationBlock() {
        val prompt = ExplanationPrompt.build(req(Denomination.PROTESTANT_OTHER))
        assertTrue(prompt.contains("India", ignoreCase = true))
        assertFalse(prompt.contains("sacramental", ignoreCase = true))
        assertFalse(prompt.contains("patristic", ignoreCase = true))
        assertFalse(prompt.contains("Church of South India", ignoreCase = true))
        assertFalse(prompt.contains("Mar Thoma", ignoreCase = true))
    }

    @Test
    @DisplayName("Language code ISO instruction present")
    fun languageCodeIsoInstructionPresent() {
        val prompt = ExplanationPrompt.build(req(null, "ml"))
        assertTrue(prompt.contains("'ml'"))
    }

    @Test
    @DisplayName("'do not invent facts' present")
    fun doNotInventFactsPresent() {
        val prompt = ExplanationPrompt.build(req(null))
        assertTrue(prompt.contains("do not invent facts", ignoreCase = true))
    }
}
