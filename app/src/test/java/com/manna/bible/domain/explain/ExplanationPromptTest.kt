package com.manna.bible.domain.explain

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
}
