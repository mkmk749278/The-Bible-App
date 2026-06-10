package com.manna.bible.data.remote

import com.manna.bible.domain.model.CanonType
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit tests for [DefaultHelloAoRemoteDataSource] mapping logic — verse-text
 * flattening, language-code normalization, and catalog mapping/filtering. Pure JVM,
 * no network (a hand-rolled [HelloAoApi] fake serves fixtures).
 */
class DefaultHelloAoRemoteDataSourceTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun content(arrayJson: String): List<kotlinx.serialization.json.JsonElement> =
        (json.parseToJsonElement(arrayJson) as JsonArray).toList()

    @Test
    @DisplayName("flattenVerseText joins string segments and drops footnote/markup objects")
    fun flattenJoinsStringsDropsObjects() {
        val flattened = DefaultHelloAoRemoteDataSource.flattenVerseText(
            content("""["In the beginning, God", {"noteId": 0}, "created the heavens and the earth."]""")
        )
        assertEquals("In the beginning, God created the heavens and the earth.", flattened)
    }

    @Test
    @DisplayName("flattenVerseText keeps text from {text} objects and collapses whitespace")
    fun flattenKeepsTextObjects() {
        val flattened = DefaultHelloAoRemoteDataSource.flattenVerseText(
            content("""["The   Lord", {"text": "is my"}, {"lineBreak": true}, "shepherd"]""")
        )
        assertEquals("The Lord is my shepherd", flattened)
    }

    @Test
    @DisplayName("mapLanguageCode normalizes ISO 639-3 to app codes and passes through unknowns")
    fun mapsLanguageCodes() {
        assertEquals("te", DefaultHelloAoRemoteDataSource.mapLanguageCode("tel"))
        assertEquals("en", DefaultHelloAoRemoteDataSource.mapLanguageCode("ENG"))
        assertEquals("ml", DefaultHelloAoRemoteDataSource.mapLanguageCode("mal"))
        assertEquals("xyz", DefaultHelloAoRemoteDataSource.mapLanguageCode("xyz"))
    }

    @Test
    @DisplayName("fetchCatalog keeps only complete (66-book) editions and normalizes them")
    fun fetchCatalogFiltersAndMaps() = runTest {
        val api = FakeHelloAoApi(
            translations = AvailableTranslationsResponseDto(
                translations = listOf(
                    TranslationDto("tel_irv", "ఇండియన్", "Telugu IRV Bible", "tel", numberOfBooks = 66),
                    TranslationDto("eng_nt", "NT Only", "English NT", "eng", numberOfBooks = 27)
                )
            )
        )
        val source = DefaultHelloAoRemoteDataSource(api)

        val catalog = source.fetchCatalog()

        assertEquals(1, catalog.size)
        val telugu = catalog.single()
        assertEquals("tel_irv", telugu.id)
        assertEquals("Telugu IRV Bible", telugu.name)
        assertEquals("te", telugu.languageCode)
        assertEquals(CanonType.PROTESTANT_66, telugu.canonType)
        assertFalse(telugu.hasDeuterocanon)
        assertFalse(telugu.isDownloaded)
    }

    @Test
    @DisplayName("books infers testament from OSIS id and preserves order/chapter counts")
    fun booksMapTestamentAndOrder() = runTest {
        val api = FakeHelloAoApi(
            books = BooksResponseDto(
                books = listOf(
                    BookDto(id = "GEN", commonName = "Genesis", numberOfChapters = 50, order = 1),
                    BookDto(id = "REV", commonName = "Revelation", numberOfChapters = 22, order = 66)
                )
            )
        )
        val source = DefaultHelloAoRemoteDataSource(api)

        val books = source.books("tel_irv")

        assertEquals("OLD", books.first { it.osisId == "GEN" }.testament)
        assertEquals("NEW", books.first { it.osisId == "REV" }.testament)
        assertTrue(books.all { it.chapterCount > 0 })
    }

    /** Minimal [HelloAoApi] returning canned fixtures. */
    private class FakeHelloAoApi(
        private val translations: AvailableTranslationsResponseDto = AvailableTranslationsResponseDto(),
        private val books: BooksResponseDto = BooksResponseDto(),
        private val chapter: ChapterResponseDto = ChapterResponseDto()
    ) : HelloAoApi {
        override suspend fun translations(): AvailableTranslationsResponseDto = translations
        override suspend fun books(id: String): BooksResponseDto = books
        override suspend fun chapter(id: String, book: String, chapter: Int): ChapterResponseDto =
            this.chapter
    }
}
