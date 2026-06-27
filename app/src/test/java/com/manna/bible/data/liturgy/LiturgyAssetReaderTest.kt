package com.manna.bible.data.liturgy

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.FileNotFoundException

/**
 * Plain-JVM unit tests for [LiturgyAssetReader] / [LiturgyRepository] using fake sources
 * (no network collaborator exists, Req 9.3). Robolectric-backed reading of the real
 * bundled assets lives in [LiturgyAssetReaderRobolectricTest].
 */
class LiturgyAssetReaderTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private class FakeSource(
        private val manifestJson: String?,
        private val assets: Map<String, String>
    ) : LiturgyAssetSource {
        override suspend fun readManifest(): String? = manifestJson
        override suspend fun readAsset(assetFile: String): String =
            assets[assetFile] ?: throw FileNotFoundException("missing $assetFile")
    }

    private fun validJson(id: String) = """
        {"id":"$id","title":{"en":"T"},"tradition":"Trad","languages":["en"],
         "sourceNote":{"en":"S"},
         "sections":[{"title":{"en":"Sec"},"parts":[{"role":"PEOPLE","text":{"en":"Amen."}}]}]}
    """.trimIndent()

    @Test
    fun `missing manifest yields null and an empty library`(): Unit = runBlocking {
        val reader = LiturgyAssetReader(FakeSource(manifestJson = null, assets = emptyMap()), json)
        assertNull(reader.manifest())
        assertTrue(reader.readAll().isEmpty())
        assertTrue(LiturgyRepository(reader).liturgies().isEmpty())
    }

    @Test
    fun `a missing listed asset becomes a descriptive Failure while others are retained`(): Unit = runBlocking {
        val manifest = """
            {"liturgies":[
              {"id":"a","title":"A","tradition":"TA","assetFile":"a.json"},
              {"id":"b","title":"B","tradition":"TB","assetFile":"b.json"}
            ]}
        """.trimIndent()
        // Only a.json is present; b.json is listed but missing.
        val reader = LiturgyAssetReader(
            FakeSource(manifest, mapOf("a.json" to validJson("a"))),
            json
        )

        val results = reader.readAll()
        assertEquals(2, results.size)
        assertTrue(results[0] is LiturgyParseResult.Success)
        val failure = results[1]
        assertTrue(failure is LiturgyParseResult.Failure)
        assertEquals("b", (failure as LiturgyParseResult.Failure).id)
        assertTrue(failure.message.isNotBlank())

        // The repository keeps the one valid order.
        val liturgies = LiturgyRepository(reader).liturgies()
        assertEquals(listOf("a"), liturgies.map { it.id })
    }

    @Test
    fun `a malformed listed asset becomes a Failure without affecting valid entries`(): Unit = runBlocking {
        val manifest = """
            {"liturgies":[
              {"id":"good","title":"G","tradition":"TG","assetFile":"good.json"},
              {"id":"bad","title":"B","tradition":"TB","assetFile":"bad.json"}
            ]}
        """.trimIndent()
        val reader = LiturgyAssetReader(
            FakeSource(manifest, mapOf("good.json" to validJson("good"), "bad.json" to "{ not json")),
            json
        )
        val liturgies = LiturgyRepository(reader).liturgies()
        assertEquals(listOf("good"), liturgies.map { it.id })
    }

    @Test
    fun `repository caches the parsed liturgies (parse once)`(): Unit = runBlocking {
        val manifest = """{"liturgies":[{"id":"a","title":"A","tradition":"TA","assetFile":"a.json"}]}"""
        val reader = LiturgyAssetReader(FakeSource(manifest, mapOf("a.json" to validJson("a"))), json)
        val repository = LiturgyRepository(reader)
        val first = repository.liturgies()
        val second = repository.liturgies()
        assertEquals(first, second)
    }
}
