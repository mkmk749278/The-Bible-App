package com.manna.bible.data.liturgy

import com.manna.bible.domain.liturgy.Liturgy
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.set
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.FileNotFoundException

/**
 * Property tests for [LiturgyAssetReader] / [LiturgyRepository], exercised against a
 * recording in-memory [LiturgyAssetSource] (Properties 3, 4) and generated manifests
 * (Property 5). All pure JVM — no emulator.
 */
class LiturgyReaderPropertiesTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** Records which asset files the reader opens, serving them from an in-memory map. */
    private class RecordingAssetSource(
        private val manifestJson: String?,
        private val assets: Map<String, String>
    ) : LiturgyAssetSource {
        val requested = mutableListOf<String>()
        override suspend fun readManifest(): String? = manifestJson
        override suspend fun readAsset(assetFile: String): String {
            requested += assetFile
            return assets[assetFile] ?: throw FileNotFoundException("no asset $assetFile")
        }
    }

    private fun manifestJson(entries: List<LiturgyManifestEntryDto>): String =
        json.encodeToString(LiturgyManifestDto.serializer(), LiturgyManifestDto(entries))

    private fun entryFor(index: Int, liturgy: Liturgy? = null): LiturgyManifestEntryDto =
        LiturgyManifestEntryDto(
            id = liturgy?.id ?: "id_$index",
            title = liturgy?.title?.english ?: "Title $index",
            tradition = liturgy?.tradition ?: "Tradition $index",
            assetFile = "asset_$index.json"
        )

    @Test
    fun `the reader opens only asset files listed in the manifest`() = runBlocking {
        // Feature: mass-liturgy-and-localization, Property 4: For any Liturgy_Manifest, the set of asset files the reader opens is a subset of the assetFile values listed in that manifest (no unlisted file is loaded).
        checkAll(200, Arb.int(0..5), Arb.set(Arb.int(0..20), 0..6)) { listedCount, extraIndices ->
            val entries = (0 until listedCount).map { entryFor(it) }
            val manifestFiles = entries.map { it.assetFile }.toSet()

            // Asset map: a subset of listed files (valid JSON) + unlisted "extra" files.
            val assets = mutableMapOf<String, String>()
            entries.forEachIndexed { i, entry ->
                if (i % 2 == 0) assets[entry.assetFile] = validLiturgyJson("id_$i")
            }
            extraIndices.forEach { assets["extra_$it.json"] = validLiturgyJson("extra_$it") }

            val source = RecordingAssetSource(manifestJson(entries), assets)
            val reader = LiturgyAssetReader(source, json)
            reader.readAll()

            assertTrue(
                manifestFiles.containsAll(source.requested.toSet()),
                "reader opened ${source.requested} outside manifest $manifestFiles"
            )
        }
    }

    @Test
    fun `malformed assets fail safely and valid entries are retained`() = runBlocking {
        // Feature: mass-liturgy-and-localization, Property 3: For any input that is malformed JSON or fails schema validation, the parser/reader returns a descriptive failure result and does not throw, and the resulting Liturgy_Library excludes that entry while retaining all valid entries.
        val malformed = listOf(
            "{",
            "not json",
            """{"id":"x"}""",
            """{"id":"x","title":{"ta":"t"},"tradition":"T","sourceNote":{"en":"s"},"sections":[]}"""
        )
        checkAll(200, Arb.list(Arb.boolean(), 1..6)) { rawValidity ->
            // Guarantee at least one valid entry so the "retains valid entries" claim is exercised.
            val validity = if (rawValidity.any { it }) rawValidity else rawValidity.toMutableList().also { it[0] = true }

            val entries = validity.indices.map { entryFor(it) }
            val expectedValid = mutableListOf<Liturgy>()
            val assets = mutableMapOf<String, String>()
            entries.forEachIndexed { i, entry ->
                if (validity[i]) {
                    val liturgyJson = validLiturgyJson("id_$i")
                    assets[entry.assetFile] = liturgyJson
                    expectedValid += LiturgyMapper.parse(json, liturgyJson)
                } else {
                    assets[entry.assetFile] = malformed[i % malformed.size]
                }
            }

            val source = RecordingAssetSource(manifestJson(entries), assets)
            val reader = LiturgyAssetReader(source, json)

            // Never throws; failures are reported as Failure results.
            val results = reader.readAll()
            assertEquals(entries.size, results.size)
            results.forEachIndexed { i, result ->
                if (!validity[i]) {
                    assertTrue(result is LiturgyParseResult.Failure, "entry $i should fail")
                    assertTrue((result as LiturgyParseResult.Failure).message.isNotBlank())
                }
            }

            // The repository exposes exactly the valid entries, in order.
            val repository = LiturgyRepository(reader)
            assertEquals(expectedValid, repository.liturgies())
        }
    }

    @Test
    fun `complete manifest entries carry id, title, tradition, denominations, and en languages`() = runBlocking {
        // Feature: mass-liturgy-and-localization, Property 5: For any bundled liturgy referenced by the library, its manifest entry carries a non-blank id, title, tradition, a denominations field, and a non-empty languages list including en.
        checkAll(250, manifestEntryArb) { (entry, complete) ->
            val isComplete = LiturgyMapper.validateManifestEntry(entry).isEmpty()
            assertEquals(complete, isComplete)
            if (complete) {
                assertTrue(entry.id.isNotBlank())
                assertTrue(entry.title.isNotBlank())
                assertTrue(entry.tradition.isNotBlank())
                assertTrue(entry.languages.isNotEmpty() && "en" in entry.languages)
            }
        }
    }

    /** A manifest entry tagged with whether it is complete (valid) or deliberately broken. */
    private val manifestEntryArb: Arb<Pair<LiturgyManifestEntryDto, Boolean>> = arbitrary {
        val complete = Arb.boolean().bind()
        val extras = Arb.set(Arb.of("ta", "te", "hi", "ml"), 0..4).bind()
        val entry = if (complete) {
            LiturgyManifestEntryDto(
                id = "id",
                title = "Title",
                tradition = "Tradition",
                denominations = listOf("catholic"),
                languages = (listOf("en") + extras),
                assetFile = "a.json"
            )
        } else {
            // Break exactly one required invariant.
            when (Arb.int(0..3).bind()) {
                0 -> LiturgyManifestEntryDto("   ", "Title", "Trad", languages = listOf("en"), assetFile = "a.json")
                1 -> LiturgyManifestEntryDto("id", "  ", "Trad", languages = listOf("en"), assetFile = "a.json")
                2 -> LiturgyManifestEntryDto("id", "Title", "Trad", languages = emptyList(), assetFile = "a.json")
                else -> LiturgyManifestEntryDto("id", "Title", "Trad", languages = listOf("ta"), assetFile = "a.json")
            }
        }
        entry to complete
    }

    private fun validLiturgyJson(id: String): String = """
        {
          "id": "$id",
          "title": { "en": "Title" },
          "tradition": "Tradition",
          "languages": ["en"],
          "sourceNote": { "en": "Source" },
          "sections": [
            { "title": { "en": "Section" }, "parts": [ { "role": "PEOPLE", "text": { "en": "Amen." } } ] }
          ]
        }
    """.trimIndent()
}
