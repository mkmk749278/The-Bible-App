package com.manna.bible.data.remote

import com.manna.bible.domain.model.CanonType
import com.manna.bible.domain.translation.Translation
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Retrofit-backed [HelloAoRemoteDataSource] for the Free Use Bible API
 * (`https://bible.helloao.org/api/`, MIT-licensed, key-less, cleared for
 * commercial use — Task 2.2 / 14.1).
 *
 * Mapping decisions:
 *  - **Catalog scope** ([fetchCatalog]) is limited to *complete* (66-book)
 *    editions. The public catalog is dominated by partial editions (e.g.
 *    New-Testament-only) which would clutter the picker and cannot back an
 *    offline whole-Bible reader.
 *  - **Language codes** are normalized from the API's ISO 639-3 codes
 *    (`eng`, `tel`, …) to the app's ISO 639-1 codes (`en`, `te`, …) so they match
 *    the setup language selection. Unknown codes pass through unchanged.
 *  - **Canon**: every complete helloao edition is a 66-book Protestant Bible, so
 *    [CanonType.PROTESTANT_66] with no deuterocanon is assigned.
 *  - **Verse text** is flattened to plain UTF-8: string segments are joined with a
 *    single space and inline footnote/markup objects are dropped (canonical
 *    numbering is preserved).
 *
 * [downloadTranslation] is intentionally a no-op (returns 0): bulk content
 * acquisition goes through the `DownloadManager`, which streams books/chapters via
 * [books] / [chapter]. The method only exists to satisfy the legacy
 * [com.manna.bible.data.TranslationRemoteDataSource] catalog seam.
 */
@Singleton
class DefaultHelloAoRemoteDataSource @Inject constructor(
    private val api: HelloAoApi
) : HelloAoRemoteDataSource {

    override suspend fun fetchCatalog(): List<Translation> =
        api.translations().translations
            .filter { (it.numberOfBooks ?: 0) >= COMPLETE_BOOK_COUNT }
            .map { it.toDomain() }

    override suspend fun downloadTranslation(id: String): Long = 0L

    override suspend fun books(id: String): List<RemoteBook> =
        api.books(id).books.mapIndexed { index, dto -> dto.toRemoteBook(index) }

    override suspend fun chapter(id: String, osisId: String, chapter: Int): RemoteChapter {
        val items = api.chapter(id, osisId, chapter).chapter?.content.orEmpty()
        val verses = items
            .filter { it.type == "verse" && it.number != null }
            .map { RemoteVerse(verse = it.number!!, text = flattenVerseText(it.content)) }
        return RemoteChapter(osisId = osisId, chapter = chapter, verses = verses)
    }

    private fun TranslationDto.toDomain(): Translation = Translation(
        id = id,
        name = englishName?.takeIf { it.isNotBlank() } ?: name,
        languageCode = mapLanguageCode(language),
        canonType = CanonType.PROTESTANT_66,
        hasDeuterocanon = (numberOfBooks ?: 0) > COMPLETE_BOOK_COUNT,
        isDownloaded = false,
        isDefaultForCanon = false
    )

    private fun BookDto.toRemoteBook(index: Int): RemoteBook = RemoteBook(
        osisId = id,
        name = commonName?.takeIf { it.isNotBlank() }
            ?: name?.takeIf { it.isNotBlank() }
            ?: id,
        testament = if (id in NEW_TESTAMENT_OSIS_IDS) "NEW" else "OLD",
        orderIndex = order ?: index,
        chapterCount = numberOfChapters
    )

    companion object {
        /** Number of books in a complete Protestant Bible. */
        const val COMPLETE_BOOK_COUNT = 66

        /** OSIS ids of the 27 New Testament books, used to infer testament. */
        private val NEW_TESTAMENT_OSIS_IDS = setOf(
            "MAT", "MRK", "LUK", "JHN", "ACT", "ROM", "1CO", "2CO", "GAL", "EPH",
            "PHP", "COL", "1TH", "2TH", "1TI", "2TI", "TIT", "PHM", "HEB", "JAS",
            "1PE", "2PE", "1JN", "2JN", "3JN", "JUD", "REV"
        )

        /** ISO 639-3 (API) → ISO 639-1 (app) for the languages Manna surfaces. */
        private val LANGUAGE_CODE_MAP = mapOf(
            "eng" to "en", "hin" to "hi", "tel" to "te", "tam" to "ta", "mal" to "ml",
            "ben" to "bn", "mar" to "mr", "guj" to "gu", "kan" to "kn", "pan" to "pa",
            "urd" to "ur", "ori" to "or", "asm" to "as", "nep" to "ne"
        )

        /**
         * Normalizes an API language code to the app's code, passing through any
         * code that has no explicit mapping so distinct languages stay distinct.
         */
        fun mapLanguageCode(apiCode: String): String =
            LANGUAGE_CODE_MAP[apiCode.lowercase()] ?: apiCode

        /**
         * Flattens a helloao verse `content` array to plain text. String segments
         * (and `{ "text": "…" }` objects) are joined with single spaces; footnote
         * references and other non-text objects are dropped. Collapses runs of
         * whitespace and trims.
         */
        fun flattenVerseText(content: List<JsonElement>): String {
            val builder = StringBuilder()
            for (element in content) {
                val segment = when (element) {
                    is JsonPrimitive -> if (element.isString) element.content else null
                    is JsonObject -> (element["text"] as? JsonPrimitive)
                        ?.takeIf { it.isString }
                        ?.content
                    else -> null
                }
                if (!segment.isNullOrEmpty()) {
                    if (builder.isNotEmpty()) builder.append(' ')
                    builder.append(segment)
                }
            }
            return builder.toString().replace(WHITESPACE, " ").trim()
        }

        private val WHITESPACE = Regex("\\s+")
    }
}
