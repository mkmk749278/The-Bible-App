package com.manna.bible.data.liturgy

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Smoke test for the bundled liturgy assets (Req 9.1, 9.4): the manifest plus both
 * `{id}.json` files exist under `src/main/assets/liturgy/`, and the manifest indexes
 * exactly the two shipped orders. Runs on the JVM, mirroring [com.manna.bible.data.bundled.BundledBibleAssetsTest].
 */
class LiturgyAssetsPresenceTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun assetsDir(): File =
        listOf(File("src/main/assets/liturgy"), File("app/src/main/assets/liturgy"))
            .firstOrNull { it.exists() }
            ?: error("Missing assets/liturgy dir (cwd=${File(".").absolutePath})")

    @Test
    fun `manifest and both liturgy assets exist and are indexed`() {
        val dir = assetsDir()
        val manifestFile = File(dir, "manifest.json")
        assertTrue(manifestFile.exists(), "manifest.json must exist")

        val manifest = json.decodeFromString(LiturgyManifestDto.serializer(), manifestFile.readText())
        val ids = manifest.liturgies.map { it.id }.toSet()
        assertEquals(setOf("roman_catholic_mass", "csi_holy_communion"), ids)

        manifest.liturgies.forEach { entry ->
            val assetFile = File(dir, entry.assetFile)
            assertTrue(assetFile.exists(), "Missing asset ${entry.assetFile} for ${entry.id}")
            // Each indexed asset parses cleanly and round-trips its id.
            val liturgy = LiturgyMapper.parse(json, assetFile.readText())
            assertEquals(entry.id, liturgy.id)
        }
    }
}
