package com.manna.bible.data.canon

import android.content.Context
import com.manna.bible.domain.model.CanonDefinition
import com.manna.bible.domain.model.CanonType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [CanonDefinitionDataSource] backed by the bundled JSON assets under
 * `assets/canon/{canonType.id}.json`.
 *
 * Parsing happens on [Dispatchers.IO]. Individual asset reads are guarded so a
 * missing or corrupt file falls back to the `protestant_66` safe default
 * (per the design's error-handling table) instead of crashing.
 *
 * The [Json] instance is injected; a Hilt module provides it (task 11.1).
 */
@Singleton
class AssetCanonDefinitionDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) : CanonDefinitionDataSource {

    override suspend fun definitionFor(canonType: CanonType): CanonDefinition =
        withContext(Dispatchers.IO) {
            when (canonType) {
                // ALL_CANONS has no bundled file: compute the union at runtime.
                CanonType.ALL_CANONS -> loadUnion()
                else -> loadFromAsset(canonType) ?: protestantFallback()
            }
        }

    /**
     * Builds the union of every bundled single-tradition canon. The most
     * expansive canon is listed first so the de-duplicated ordering interleaves
     * deuterocanonical books sensibly.
     */
    private fun loadUnion(): CanonDefinition {
        val definitions = UNION_SOURCES.mapNotNull { loadFromAsset(it) }
        return if (definitions.isEmpty()) {
            protestantFallback()
        } else {
            CanonDefinitionMapper.buildUnion(definitions)
        }
    }

    private fun protestantFallback(): CanonDefinition =
        loadFromAsset(CanonType.PROTESTANT_66)
            ?: error("Bundled protestant_66 canon asset is missing or invalid")

    /** Loads and parses a single canon asset, returning null on any failure. */
    private fun loadFromAsset(canonType: CanonType): CanonDefinition? =
        runCatching {
            val raw = context.assets
                .open("canon/${canonType.id}.json")
                .bufferedReader()
                .use { it.readText() }
            CanonDefinitionMapper.parse(json, raw)
        }.getOrNull()

    private companion object {
        /** Single-tradition canons, most expansive first, used to build ALL_CANONS. */
        val UNION_SOURCES = listOf(
            CanonType.ORTHODOX_EXPANDED,
            CanonType.CATHOLIC_73,
            CanonType.PROTESTANT_66
        )
    }
}
