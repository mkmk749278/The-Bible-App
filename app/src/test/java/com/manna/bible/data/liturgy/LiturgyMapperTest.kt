package com.manna.bible.data.liturgy

import io.kotest.property.Arb
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.set
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Property + unit tests for [LiturgyMapper] — the pure parse / serialize / validate logic.
 *
 * Mirrors the canon mapper tests: runs on the JVM (JUnit 5) with no emulator. Uses the same
 * lenient [Json] the app injects for assets.
 */
class LiturgyMapperTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun `serialize then parse yields an equivalent liturgy`(): Unit = runBlocking {
        // Feature: mass-liturgy-and-localization, Property 1: For any valid Liturgy, serializing to asset JSON and parsing the result produces an equivalent Liturgy (same id, title, tradition, source note, denominations, languages, and same sections/parts — role, localized title/text/rubric, osisRef, needsOfficialText — in the same order).
        checkAll(200, LiturgyArbs.liturgy) { liturgy ->
            val parsed = LiturgyMapper.parse(json, LiturgyMapper.serialize(json, liturgy))
            assertEquals(liturgy, parsed)
        }
    }

    @Test
    fun `parse preserves authored section and part order`(): Unit = runBlocking {
        // Feature: mass-liturgy-and-localization, Property 2: For any valid liturgy asset, parsing yields its sections in authored order and, within each section, its parts in authored order.
        checkAll(200, LiturgyArbs.liturgy) { liturgy ->
            val parsed = LiturgyMapper.parse(json, LiturgyMapper.serialize(json, liturgy))

            assertEquals(
                liturgy.sections.map { it.title.english },
                parsed.sections.map { it.title.english }
            )
            liturgy.sections.zip(parsed.sections).forEach { (original, roundTripped) ->
                assertEquals(
                    original.parts.map { Triple(it.role, it.title?.english, it.text?.english) },
                    roundTripped.parts.map { Triple(it.role, it.title?.english, it.text?.english) }
                )
            }
        }
    }

    @Test
    fun `validation flags a liturgy inconsistent iff declared languages differ from present languages`(): Unit = runBlocking {
        // Feature: mass-liturgy-and-localization, Property 6: For any liturgy asset, validation flags it inconsistent if and only if the set of declared languages differs from the set of languages actually present across its parts' localized fields.
        val presentArb: Arb<Set<String>> =
            Arb.set(Arb.of("ta", "te", "hi", "ml"), 0..4).let { extras ->
                io.kotest.property.arbitrary.arbitrary { setOf("en") + extras.bind() }
            }
        val declaredArb: Arb<Set<String>> = Arb.set(Arb.of("en", "ta", "te", "hi", "ml", "fr"), 0..6)

        checkAll(250, presentArb, declaredArb) { present, declared ->
            val dto = dtoWith(presentLanguages = present, declaredLanguages = declared)
            val flaggedInconsistent = LiturgyMapper.validate(dto)
                .any { it.contains("declared languages") }
            assertEquals(present != declared, flaggedInconsistent)
        }
    }

    // --- validate / parse error unit tests (task 3.6) -----------------------

    @Test
    fun `validate reports a missing en value`() {
        val dto = LiturgyDto(
            id = "x",
            title = mapOf("ta" to "தலைப்பு"),
            tradition = "Trad",
            languages = listOf("ta"),
            sourceNote = mapOf("en" to "Source"),
            sections = emptyList()
        )
        val errors = LiturgyMapper.validate(dto)
        assertTrue(errors.any { it.contains("title") && it.contains("en") }, errors.toString())
    }

    @Test
    fun `validate reports an unknown role token`() {
        val dto = LiturgyDto(
            id = "x",
            title = mapOf("en" to "T"),
            tradition = "Trad",
            languages = listOf("en"),
            sourceNote = mapOf("en" to "Source"),
            sections = listOf(
                LiturgySectionDto(
                    title = mapOf("en" to "Sec"),
                    parts = listOf(LiturgyPartDto(role = "BISHOP", text = mapOf("en" to "Hi")))
                )
            )
        )
        val errors = LiturgyMapper.validate(dto)
        assertTrue(errors.any { it.contains("unknown role") && it.contains("BISHOP") }, errors.toString())
    }

    @Test
    fun `validate reports a blank id`() {
        val dto = LiturgyDto(
            id = "   ",
            title = mapOf("en" to "T"),
            tradition = "Trad",
            languages = listOf("en"),
            sourceNote = mapOf("en" to "Source"),
            sections = emptyList()
        )
        assertTrue(LiturgyMapper.validate(dto).any { it.contains("id is blank") })
    }

    @Test
    fun `parse throws on an unknown role`() {
        val raw = """
            {
              "id": "x",
              "title": { "en": "T" },
              "tradition": "Trad",
              "languages": ["en"],
              "sourceNote": { "en": "S" },
              "sections": [
                { "title": { "en": "Sec" }, "parts": [ { "role": "DEACON", "text": { "en": "Hi" } } ] }
              ]
            }
        """.trimIndent()
        assertThrows(IllegalArgumentException::class.java) { LiturgyMapper.parse(json, raw) }
    }

    @Test
    fun `parse accepts a well-formed asset`() {
        val raw = """
            {
              "id": "x",
              "title": { "en": "T" },
              "tradition": "Trad",
              "languages": ["en"],
              "sourceNote": { "en": "S" },
              "sections": [
                { "title": { "en": "Sec" }, "parts": [ { "role": "PEOPLE", "text": { "en": "Amen." } } ] }
              ]
            }
        """.trimIndent()
        val liturgy = LiturgyMapper.parse(json, raw)
        assertEquals("x", liturgy.id)
        assertEquals("Amen.", liturgy.sections.single().parts.single().text?.english)
        assertFalse(liturgy.sections.single().parts.single().needsOfficialText)
    }

    /**
     * Builds a minimal, otherwise-valid DTO whose single part is authored in exactly
     * [presentLanguages] (which must include "en") and whose declared `languages` is
     * [declaredLanguages]. Only the language-consistency rule varies between the two sets.
     */
    private fun dtoWith(presentLanguages: Set<String>, declaredLanguages: Set<String>): LiturgyDto =
        LiturgyDto(
            id = "id",
            title = mapOf("en" to "Title"),
            tradition = "Trad",
            languages = declaredLanguages.toList(),
            sourceNote = mapOf("en" to "Source"),
            sections = listOf(
                LiturgySectionDto(
                    title = mapOf("en" to "Section"),
                    parts = listOf(
                        LiturgyPartDto(role = "PEOPLE", text = presentLanguages.associateWith { "v_$it" })
                    )
                )
            )
        )
}
