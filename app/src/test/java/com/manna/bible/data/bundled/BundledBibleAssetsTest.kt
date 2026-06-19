package com.manna.bible.data.bundled

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest
import java.util.zip.GZIPInputStream

/**
 * Guards the committed offline Bible assets under `src/main/assets/bibles/`,
 * produced by `:app:prepareBundledBibles`. Runs on the JVM (no device) so CI
 * catches a corrupt, truncated, or out-of-sync asset before it ever ships.
 *
 * For each manifest entry it verifies that the gzipped asset decompresses, parses
 * into the runtime [BundledBible] shape, contains the declared number of verses,
 * matches the recorded checksum, and holds a complete 66-book canon with non-blank
 * verse text — the same content the seeder will load into Room on first launch.
 */
class BundledBibleAssetsTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val biblesDir = File("src/main/assets/bibles")

    private fun readManifest(): BundledManifest {
        val file = File(biblesDir, "manifest.json")
        assertTrue(file.exists(), "Missing bundled manifest at ${file.absolutePath}")
        return json.decodeFromString(BundledManifest.serializer(), file.readText())
    }

    private fun readBible(assetFile: String): Pair<BundledBible, String> {
        val file = File(biblesDir, assetFile)
        assertTrue(file.exists(), "Missing bundled asset $assetFile")
        val raw = GZIPInputStream(file.inputStream()).bufferedReader().use { it.readText() }
        val checksum = MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return json.decodeFromString(BundledBible.serializer(), raw) to checksum
    }

    @Test
    fun `manifest lists the five launch translations`() {
        val ids = readManifest().translations.map { it.id }.toSet()
        assertEquals(
            setOf("ENGWEBP", "tam_irv", "HINIRV", "tel_irv", "mal_irv"),
            ids,
            "Bundled manifest must contain exactly the five launch translations",
        )
    }

    @TestFactory
    fun `each bundled translation is complete and matches its manifest entry`(): List<DynamicTest> =
        readManifest().translations.map { entry ->
            DynamicTest.dynamicTest(entry.id) {
                val (bible, checksum) = readBible(entry.assetFile)

                assertEquals(entry.id, bible.translationId, "translationId mismatch for ${entry.id}")
                assertEquals(entry.checksum, checksum, "checksum drift for ${entry.id} (regenerate assets)")
                assertEquals(
                    entry.verseCount,
                    bible.verses.size,
                    "verseCount in manifest disagrees with asset for ${entry.id}",
                )
                assertEquals(66, bible.books.size, "${entry.id} must be a complete 66-book canon")
                assertTrue(
                    bible.verses.size > 30_000,
                    "${entry.id} has only ${bible.verses.size} verses — looks truncated",
                )
                // A handful of verses are intentionally blank: passages omitted by
                // modern textual criticism (e.g. WEB's Acts 8:37) are kept as empty
                // entries to preserve canonical numbering — the download path does the
                // same. Allow that handful, but flag wholesale blanks (corruption).
                val blanks = bible.verses.count { it.text.isBlank() }
                assertTrue(
                    blanks < 30,
                    "${entry.id} has $blanks blank verses — looks corrupt (expected only a few omitted passages)",
                )
                // Spot-check a well-known reference resolves to real text.
                val john3_16 = bible.verses.firstOrNull {
                    it.osisId == "JHN" && it.chapter == 3 && it.verse == 16
                }
                assertTrue(john3_16 != null && john3_16.text.isNotBlank(), "${entry.id} missing JHN 3:16")
            }
        }
}
