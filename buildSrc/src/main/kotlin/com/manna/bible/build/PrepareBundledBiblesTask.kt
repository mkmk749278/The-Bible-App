package com.manna.bible.build

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.MessageDigest
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPOutputStream

/**
 * Generates the offline Bible assets consumed at runtime by
 * `com.manna.bible.data.bundled.BundledBibleAssetReader` /
 * `BundledBibleSeeder` (which seed Room on first launch).
 *
 * For each translation in [TRANSLATIONS] the task downloads the complete text
 * from the Free Use Bible API (`https://bible.helloao.org/api/`, MIT-licensed,
 * key-less, cleared for commercial use) and writes two asset shapes under
 * [outputDir] (`app/src/main/assets/bibles/`):
 *
 *  - `manifest.json` (uncompressed) — one entry per bundled translation.
 *  - `{id}.json.gz` (gzipped JSON) — the full book + verse data.
 *
 * The output JSON shapes match the runtime `BundledManifest` / `BundledBible`
 * serialization models exactly. Verse text is flattened with the **same** rule
 * the live download path uses (`DefaultHelloAoRemoteDataSource.flattenVerseText`)
 * so bundled and downloaded content are byte-identical.
 *
 * This is a developer/CI tool, **not** wired into `assembleDebug` — the
 * generated assets are committed to the repository, so normal builds (and CI)
 * never hit the network. Re-run it with:
 *
 * ```
 * ./gradlew :app:prepareBundledBibles
 * ```
 */
abstract class PrepareBundledBiblesTask : DefaultTask() {

    /** Destination directory for `manifest.json` and the `{id}.json.gz` files. */
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @Suppress("UNCHECKED_CAST")
    @TaskAction
    fun generate() {
        val dir = outputDir.get().asFile
        dir.mkdirs()

        val http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()
        val slurper = JsonSlurper()
        val pool = Executors.newFixedThreadPool(CONCURRENCY)

        try {
            val manifestEntries = TRANSLATIONS.map { spec ->
                logger.lifecycle("Bundling ${spec.id} (${spec.languageCode})…")
                val bible = downloadTranslation(http, slurper, pool, spec)

                val json = JsonOutput.toJson(
                    linkedMapOf(
                        "translationId" to spec.id,
                        "books" to bible.books.map { b ->
                            linkedMapOf(
                                "osisId" to b.osisId,
                                "name" to b.name,
                                "testament" to b.testament,
                                "orderIndex" to b.orderIndex,
                                "chapterCount" to b.chapterCount,
                            )
                        },
                        "verses" to bible.verses.map { v ->
                            linkedMapOf(
                                "osisId" to v.osisId,
                                "chapter" to v.chapter,
                                "verse" to v.verse,
                                "text" to v.text,
                            )
                        },
                    )
                )

                val assetFile = "${spec.id}.json.gz"
                gzipTo(dir.resolve(assetFile), json)
                val checksum = sha256(json)
                logger.lifecycle("  ${spec.id}: ${bible.verses.size} verses, ${bible.books.size} books → $assetFile")

                linkedMapOf(
                    "id" to spec.id,
                    "name" to spec.name,
                    "languageCode" to spec.languageCode,
                    "canonType" to spec.canonType,
                    "bundledContentVersion" to CONTENT_VERSION,
                    "verseCount" to bible.verses.size,
                    "checksum" to checksum,
                    "assetFile" to assetFile,
                    "isDeuterocanon" to spec.isDeuterocanon,
                )
            }

            val manifest = JsonOutput.prettyPrint(
                JsonOutput.toJson(linkedMapOf("translations" to manifestEntries))
            )
            dir.resolve(MANIFEST_FILE).writeText(manifest)
            logger.lifecycle("Wrote ${dir.resolve(MANIFEST_FILE)} with ${manifestEntries.size} translations.")
        } finally {
            pool.shutdown()
            pool.awaitTermination(1, TimeUnit.MINUTES)
        }
    }

    private fun downloadTranslation(
        http: HttpClient,
        slurper: JsonSlurper,
        pool: java.util.concurrent.ExecutorService,
        spec: TranslationSpec,
    ): Bible {
        val booksJson = getJson(http, slurper, "${spec.id}/books.json") as Map<String, Any?>
        val bookDtos = (booksJson["books"] as List<Map<String, Any?>>)

        val books = bookDtos.mapIndexed { index, dto ->
            val osisId = dto["id"] as String
            BundledBookOut(
                osisId = osisId,
                name = (dto["commonName"] as? String)?.takeIf { it.isNotBlank() }
                    ?: (dto["name"] as? String)?.takeIf { it.isNotBlank() }
                    ?: osisId,
                testament = if (osisId in NEW_TESTAMENT_OSIS_IDS) "NEW" else "OLD",
                orderIndex = (dto["order"] as? Number)?.toInt() ?: index,
                chapterCount = (dto["numberOfChapters"] as? Number)?.toInt() ?: 0,
            )
        }.sortedBy { it.orderIndex }

        // Fetch every chapter concurrently, preserving canonical order on assembly.
        val futures = books.flatMap { book ->
            (1..book.chapterCount).map { chapter ->
                pool.submit<List<BundledVerseOut>> {
                    val chJson = getJson(http, slurper, "${spec.id}/${book.osisId}/$chapter.json") as Map<String, Any?>
                    val content = ((chJson["chapter"] as? Map<String, Any?>)?.get("content")
                        as? List<Map<String, Any?>>).orEmpty()
                    content
                        .filter { it["type"] == "verse" && it["number"] != null }
                        .map { item ->
                            BundledVerseOut(
                                osisId = book.osisId,
                                chapter = chapter,
                                verse = (item["number"] as Number).toInt(),
                                text = flattenVerseText(item["content"] as? List<Any?>),
                            )
                        }
                }
            }
        }

        val verses = futures.flatMap { it.get() }
            .sortedWith(compareBy({ books.indexOfFirst { b -> b.osisId == it.osisId } }, { it.chapter }, { it.verse }))

        if (verses.isEmpty()) {
            throw GradleException("No verses downloaded for ${spec.id}; aborting to avoid shipping empty content.")
        }
        return Bible(books, verses)
    }

    private fun getJson(http: HttpClient, slurper: JsonSlurper, path: String): Any? {
        var lastError: Exception? = null
        repeat(MAX_RETRIES) { attempt ->
            try {
                val request = HttpRequest.newBuilder()
                    .uri(URI.create("$API_BASE/$path"))
                    .timeout(Duration.ofSeconds(45))
                    .header("Accept", "application/json")
                    .GET()
                    .build()
                val response = http.send(request, HttpResponse.BodyHandlers.ofString())
                if (response.statusCode() != 200) {
                    throw GradleException("HTTP ${response.statusCode()} for $path")
                }
                val body = response.body()
                if (body.isBlank() || body.trimStart().startsWith("<")) {
                    // The API serves an HTML SPA-fallback page (not JSON) for a
                    // transiently unavailable resource; retry rather than fail.
                    throw GradleException("Non-JSON body for $path")
                }
                return slurper.parseText(body)
            } catch (e: Exception) {
                lastError = e
                Thread.sleep(BACKOFF_MS * (attempt + 1))
            }
        }
        throw GradleException("Failed to fetch $path after $MAX_RETRIES attempts", lastError)
    }

    private fun gzipTo(file: java.io.File, content: String) {
        GZIPOutputStream(file.outputStream().buffered()).use { gz ->
            gz.write(content.toByteArray(Charsets.UTF_8))
        }
    }

    private fun sha256(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(content.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private data class TranslationSpec(
        val id: String,
        val name: String,
        val languageCode: String,
        val canonType: String,
        val isDeuterocanon: Boolean,
    )

    private data class Bible(val books: List<BundledBookOut>, val verses: List<BundledVerseOut>)
    private data class BundledBookOut(
        val osisId: String,
        val name: String,
        val testament: String,
        val orderIndex: Int,
        val chapterCount: Int,
    )

    private data class BundledVerseOut(
        val osisId: String,
        val chapter: Int,
        val verse: Int,
        val text: String,
    )

    private companion object {
        const val API_BASE = "https://bible.helloao.org/api"
        const val MANIFEST_FILE = "manifest.json"
        const val CONTENT_VERSION = 1
        const val CONCURRENCY = 8
        const val MAX_RETRIES = 8
        const val BACKOFF_MS = 1000L

        /**
         * The bundled set: World English Bible (English) + the Indian Revised
         * Version family (Tamil, Hindi, Telugu, Malayalam) — a consistent,
         * freely-licensed 66-book canon across the app's five launch languages.
         */
        val TRANSLATIONS = listOf(
            TranslationSpec("ENGWEBP", "World English Bible", "en", "protestant_66", false),
            TranslationSpec("tam_irv", "Tamil Indian Revised Version (IRV)", "ta", "protestant_66", false),
            TranslationSpec("HINIRV", "Hindi Indian Revised Version (IRV)", "hi", "protestant_66", false),
            TranslationSpec("tel_irv", "Telugu Indian Revised Version (IRV)", "te", "protestant_66", false),
            TranslationSpec("mal_irv", "Malayalam Indian Revised Version (IRV)", "ml", "protestant_66", false),
        )

        /** OSIS ids of the 27 New Testament books (mirrors the runtime data source). */
        val NEW_TESTAMENT_OSIS_IDS = setOf(
            "MAT", "MRK", "LUK", "JHN", "ACT", "ROM", "1CO", "2CO", "GAL", "EPH",
            "PHP", "COL", "1TH", "2TH", "1TI", "2TI", "TIT", "PHM", "HEB", "JAS",
            "1PE", "2PE", "1JN", "2JN", "3JN", "JUD", "REV",
        )

        private val WHITESPACE = Regex("\\s+")

        /**
         * Flattens a helloao verse `content` array to plain text using the same
         * rule as `DefaultHelloAoRemoteDataSource.flattenVerseText`: string
         * segments (and `{ "text": "…" }` objects) are joined with single spaces;
         * footnote references and other non-text objects are dropped.
         */
        fun flattenVerseText(content: List<Any?>?): String {
            if (content == null) return ""
            val builder = StringBuilder()
            for (element in content) {
                val segment = when (element) {
                    is String -> element
                    is Map<*, *> -> element["text"] as? String
                    else -> null
                }
                if (!segment.isNullOrEmpty()) {
                    if (builder.isNotEmpty()) builder.append(' ')
                    builder.append(segment)
                }
            }
            return builder.toString().replace(WHITESPACE, " ").trim()
        }
    }
}
