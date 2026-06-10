package com.manna.bible.data.remote

import com.manna.bible.data.TranslationRemoteDataSource
import com.manna.bible.domain.translation.Translation
import javax.inject.Inject

/**
 * Placeholder [TranslationRemoteDataSource] used to satisfy the dependency graph
 * until the Bible Brain Retrofit wiring lands.
 *
 * [fetchCatalog] returns an empty catalog so the offline-first repository simply
 * relies on the locally cached rows, and [downloadTranslation] reports a zero-byte
 * download. This keeps the app compilable and runnable while no real network layer
 * exists; replace it with the Retrofit-backed implementation in a later task.
 */
class StubTranslationRemoteDataSource @Inject constructor() : TranslationRemoteDataSource {

    override suspend fun fetchCatalog(): List<Translation> = emptyList()

    override suspend fun downloadTranslation(id: String): Long = 0L
}
