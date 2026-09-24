package com.manna.bible.data.bundled

import com.manna.bible.domain.reader.MergedVerses
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.io.File
import java.util.zip.GZIPInputStream

/**
 * Checks [MergedVerses] against the real bundled Bibles: every verse of the English WEB
 * must resolve to some verse in each Indian edition, even where that edition merges
 * verses (the Telugu IRV merges 103, e.g. 1 Cor 4:15–16 and 2 Tim 3:16–17). The only
 * expected exception is Romans 14:24–26, which these editions print as Romans 16:25–27.
 *
 * Without the fallback, 103 Telugu references (daily verse, devotions, AI answers,
 * highlights made in English) would resolve to nothing.
 */
class BundledMergedVersesTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val biblesDir = File("src/main/assets/bibles")

    private fun chapters(assetFile: String): Map<Pair<String, Int>, List<Int>> {
        val raw = GZIPInputStream(File(biblesDir, assetFile).inputStream())
            .bufferedReader().use { it.readText() }
        return json.decodeFromString(BundledBible.serializer(), raw).verses
            .groupBy({ it.osisId to it.chapter }, { it.verse })
    }

    @TestFactory
    fun `every WEB verse resolves in each Indian edition`(): List<DynamicTest> {
        val english = chapters("ENGWEBP.json.gz")
        return listOf("tel_irv.json.gz", "tam_irv.json.gz", "HINIRV.json.gz", "mal_irv.json.gz").map { asset ->
            DynamicTest.dynamicTest(asset) {
                val edition = chapters(asset)
                val unresolved = english.flatMap { (key, verses) ->
                    val present = edition[key].orEmpty()
                    verses.filter { verse ->
                        MergedVerses.carrierOf(present, key.first, key.second, verse) == null
                    }.map { "${key.first}.${key.second}.$it" }
                }
                assertEquals(
                    listOf("ROM.14.24", "ROM.14.25", "ROM.14.26"),
                    unresolved.sorted(),
                    "$asset: only the relocated Romans doxology may stay unresolved"
                )
            }
        }
    }
}
