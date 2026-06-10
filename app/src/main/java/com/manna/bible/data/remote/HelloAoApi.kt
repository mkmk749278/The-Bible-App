package com.manna.bible.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit interface for the Free Use Bible API (`https://bible.helloao.org/api/`).
 *
 * The API is key-less, rate-limit-free, and cleared for commercial use; no auth
 * header is required. The base URL is provided by Hilt/Retrofit wiring (Task 14.1).
 */
interface HelloAoApi {

    /** `GET available_translations.json` — the full translation catalog. */
    @GET("available_translations.json")
    suspend fun translations(): AvailableTranslationsResponseDto

    /** `GET {id}/books.json` — the book list for a translation. */
    @GET("{id}/books.json")
    suspend fun books(@Path("id") id: String): BooksResponseDto

    /** `GET {id}/{book}/{chapter}.json` — chapter content for a book+chapter. */
    @GET("{id}/{book}/{chapter}.json")
    suspend fun chapter(
        @Path("id") id: String,
        @Path("book") book: String,
        @Path("chapter") chapter: Int
    ): ChapterResponseDto
}
