package com.manna.bible.data

import com.manna.bible.domain.translation.Translation
import kotlinx.coroutines.flow.Flow

/**
 * Local (on-device) source of truth for the translation catalog.
 *
 * Abstracted as an interface so repositories remain pure-Kotlin and JVM-testable;
 * the Room-backed implementation lives in [com.manna.bible.data.local].
 */
interface TranslationLocalDataSource {

    /** Observes the cached translation catalog. */
    fun catalog(): Flow<List<Translation>>

    /** Inserts or updates the cached catalog. */
    suspend fun upsertAll(translations: List<Translation>)

    /** Marks a translation downloaded and records its size. */
    suspend fun setDownloaded(id: String, sizeBytes: Long)
}
