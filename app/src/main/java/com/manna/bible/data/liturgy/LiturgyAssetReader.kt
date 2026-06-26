package com.manna.bible.data.liturgy

import com.manna.bible.domain.liturgy.Liturgy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** The outcome of reading + parsing a single manifest-listed liturgy asset. */
sealed interface LiturgyParseResult {
    /** The asset parsed and validated into a domain [Liturgy]. */
    data class Success(val liturgy: Liturgy) : LiturgyParseResult

    /** The asset was malformed or failed validation; [message] describes why. */
    data class Failure(val id: String, val message: String) : LiturgyParseResult
}

/**
 * Reads the bundled liturgy assets through a [LiturgyAssetSource] and parses each via
 * [LiturgyMapper]. Mirrors [com.manna.bible.data.canon.AssetCanonDefinitionDataSource]:
 *
 *  - [manifest] reads `assets/liturgy/manifest.json`, returning null when it is absent or
 *    itself malformed.
 *  - [readAll] reads ONLY the asset files listed in the manifest (Req 9.3, 10.4); each is
 *    parsed under `runCatching`, yielding [LiturgyParseResult.Success] or, on any failure,
 *    a descriptive [LiturgyParseResult.Failure] — it never throws, so one bad asset can
 *    never remove the others (Req 10.2).
 *
 * The injected [Json] is the lenient, unknown-key-ignoring instance shared with the canon
 * and bundled-Bible readers.
 */
@Singleton
class LiturgyAssetReader @Inject constructor(
    private val source: LiturgyAssetSource,
    private val json: Json
) {

    /** Parses the manifest, or returns null when it is missing or unparseable. */
    internal suspend fun manifest(): LiturgyManifestDto? = withContext(Dispatchers.IO) {
        val raw = source.readManifest() ?: return@withContext null
        runCatching { json.decodeFromString(LiturgyManifestDto.serializer(), raw) }.getOrNull()
    }

    /**
     * Reads and parses every manifest-listed liturgy. Returns an empty list when no manifest
     * is present. No file outside the manifest is ever opened.
     */
    suspend fun readAll(): List<LiturgyParseResult> = withContext(Dispatchers.IO) {
        val manifest = manifest() ?: return@withContext emptyList()
        manifest.liturgies.map { entry ->
            runCatching {
                val raw = source.readAsset(entry.assetFile)
                LiturgyMapper.parse(json, raw)
            }.fold(
                onSuccess = { LiturgyParseResult.Success(it) },
                onFailure = { error ->
                    LiturgyParseResult.Failure(entry.id, error.message ?: error.toString())
                }
            )
        }
    }
}
