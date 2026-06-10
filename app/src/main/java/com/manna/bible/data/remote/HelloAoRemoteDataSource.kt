package com.manna.bible.data.remote

import com.manna.bible.data.TranslationRemoteDataSource

/**
 * Remote source for the Free Use Bible API (`https://bible.helloao.org/api/`).
 *
 * Extends the catalog-oriented [TranslationRemoteDataSource] seam with the
 * content-streaming methods the `DownloadManager` needs: per-translation book lists
 * and per-chapter verses, already normalized to the canon-agnostic [RemoteBook] /
 * [RemoteChapter] models (plain-text verses, canonical numbering).
 *
 * The Retrofit-backed implementation is provided by the data/wiring layer; this
 * interface keeps the download path JVM-testable with in-memory fakes.
 */
interface HelloAoRemoteDataSource : TranslationRemoteDataSource {

    /** Returns the books of the translation with the given id. */
    suspend fun books(id: String): List<RemoteBook>

    /** Returns the verses of one book+chapter of the translation with the given id. */
    suspend fun chapter(id: String, osisId: String, chapter: Int): RemoteChapter
}
