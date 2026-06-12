package com.manna.bible.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Serialization DTOs matching the Free Use Bible API (`https://bible.helloao.org/api/`)
 * JSON shapes consumed by [HelloAoApi] / [HelloAoRemoteDataSource].
 *
 * These deliberately mirror only the fields Manna needs, and are written to be
 * **tolerant** of the real helloao payloads:
 *  - the injected [kotlinx.serialization.json.Json] enables `ignoreUnknownKeys`, so
 *    additive fields on the API don't break parsing;
 *  - optional/structural fields are nullable with sane defaults so a missing key is
 *    never fatal.
 *
 * Internal to the data layer — callers consume the mapped domain
 * [com.manna.bible.domain.translation.Translation] and the `Remote*` models instead.
 */

/** Response for `GET available_translations.json`. */
@Serializable
data class AvailableTranslationsResponseDto(
    val translations: List<TranslationDto> = emptyList()
)

/**
 * A single translation entry in the catalog.
 *
 * @property id stable translation id used as the path segment for books/chapters.
 * @property name native edition name.
 * @property englishName English edition name, when provided.
 * @property language ISO-639 language code (e.g. "eng").
 * @property textDirection "ltr"/"rtl", when provided.
 * @property numberOfBooks book count hint, when provided.
 */
@Serializable
data class TranslationDto(
    val id: String,
    val name: String,
    val englishName: String? = null,
    val language: String,
    val textDirection: String? = null,
    val numberOfBooks: Int? = null
)

/** Response for `GET {id}/books.json`. */
@Serializable
data class BooksResponseDto(
    val books: List<BookDto> = emptyList()
)

/**
 * A single book entry.
 *
 * @property id USFM/OSIS-like book id (e.g. "GEN", "PSA").
 * @property name source book name, when provided.
 * @property commonName common/display book name, when provided.
 * @property numberOfChapters chapter count for the book.
 * @property order source ordering index, when provided.
 */
@Serializable
data class BookDto(
    val id: String,
    val name: String? = null,
    val commonName: String? = null,
    val numberOfChapters: Int = 0,
    val order: Int? = null
)

/**
 * Response for `GET {id}/{book}/{chapter}.json`.
 *
 * @property chapter the chapter text payload.
 * @property audioLinks the chapter's narrated-audio links (`thisChapterAudioLinks`),
 *   present when the translation has audio — typically an object of
 *   `reader -> mp3 url`. Modeled as a raw [JsonElement] so its (provider-defined,
 *   occasionally varying) shape can never break parsing of the chapter text that
 *   downloads depend on.
 */
@Serializable
data class ChapterResponseDto(
    val chapter: ChapterDto? = null,
    @SerialName("thisChapterAudioLinks") val audioLinks: JsonElement? = null
)

/**
 * The chapter payload. `content` is a heterogeneous list of items (headings,
 * line breaks, verses, …); only items with [ChapterContentItemDto.type] == "verse"
 * carry verse text.
 */
@Serializable
data class ChapterDto(
    val number: Int? = null,
    val content: List<ChapterContentItemDto> = emptyList()
)

/**
 * A content item within a chapter.
 *
 * Verse items have `type == "verse"`, a [number], and a [content] list whose elements
 * are a mix of plain strings and objects (e.g. `{ "text": "…" }`, footnote refs).
 * Because the element shapes are heterogeneous, [content] is modeled as raw
 * [JsonElement]s and flattened to plain text in
 * [HelloAoRemoteDataSource.flattenVerseText].
 */
@Serializable
data class ChapterContentItemDto(
    val type: String? = null,
    val number: Int? = null,
    @SerialName("content") val content: List<JsonElement> = emptyList()
)
