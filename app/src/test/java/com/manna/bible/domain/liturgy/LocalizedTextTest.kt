package com.manna.bible.domain.liturgy

import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.set
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * Property + unit tests for [LocalizedText] — the deterministic English-fallback resolver
 * underpinning multilingual liturgy content (Req 1.4, 8.1, 8.2).
 */
class LocalizedTextTest {

    private val optionalLangs = listOf("ta", "te", "hi", "ml")

    /** Maps that always contain "en" plus a random subset of the vernacular Bible languages. */
    private val valuesArb: Arb<Map<String, String>> = arbitrary {
        val extras = Arb.set(Arb.of(optionalLangs), 0..optionalLangs.size).bind()
        (listOf("en") + extras).associateWith { Arb.string(1, 40).bind() }
    }

    /** Tags both inside and outside the authored set, including the always-present "en". */
    private val tagArb: Arb<String> = Arb.of("en", "ta", "te", "hi", "ml", "fr", "xx", "pt-BR")

    @Test
    fun `resolve returns authored value when present and English otherwise, and English is always present`(): Unit = runBlocking {
        // Feature: mass-liturgy-and-localization, Property 7: For any LocalizedText and any requested language tag, resolve(tag) returns the authored value when present and the English value otherwise; English is always present.
        checkAll(250, valuesArb, tagArb) { values, tag ->
            val text = LocalizedText(values)

            // English is always present and equals the authored "en" value.
            assertEquals(values.getValue("en"), text.english)

            val resolved = text.resolve(tag)
            if (values.containsKey(tag)) {
                assertEquals(values.getValue(tag), resolved)
            } else {
                assertEquals(text.english, resolved)
            }

            // languages reflects exactly the authored keys.
            assertEquals(values.keys, text.languages)
        }
    }

    @Test
    fun `init requires an en value`() {
        assertThrows(IllegalArgumentException::class.java) {
            LocalizedText(mapOf("ta" to "வணக்கம்"))
        }
    }

    @Test
    fun `resolve returns the authored value for a present language`() {
        val text = LocalizedText(mapOf("en" to "Peace", "ta" to "சமாதானம்"))
        assertEquals("சமாதானம்", text.resolve("ta"))
    }

    @Test
    fun `resolve falls back to English for a missing language`() {
        val text = LocalizedText(mapOf("en" to "Peace"))
        assertEquals("Peace", text.resolve("te"))
    }

    @Test
    fun `languages reflects the authored keys`() {
        val text = LocalizedText(mapOf("en" to "a", "hi" to "b"))
        assertEquals(setOf("en", "hi"), text.languages)
    }
}
