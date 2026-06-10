package com.manna.bible.data

import com.manna.bible.domain.translation.Translation

/**
 * Remote source for translation catalog metadata and downloads (Bible Brain API).
 *
 * Interface only — the Retrofit-backed implementation is deferred to a later task.
 */
interface TranslationRemoteDataSource {

    /** Fetches the current translation catalog from the network. */
    suspend fun fetchCatalog(): List<Translation>

    /**
     * Downloads the translation with the given id.
     *
     * @return the downloaded size in bytes.
     */
    suspend fun downloadTranslation(id: String): Long
}
