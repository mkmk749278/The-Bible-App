package com.manna.bible.data.liturgy

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supplies the raw text of the bundled liturgy assets. Abstracting the `AssetManager`
 * behind this seam keeps [LiturgyAssetReader]'s manifest-only-loading and fail-safe logic
 * pure and JVM-testable (Properties 3 & 4) — a fake source can record exactly which asset
 * files the reader requests, against a generated manifest and in-memory asset map.
 */
interface LiturgyAssetSource {

    /** Raw `assets/liturgy/manifest.json` text, or null when the manifest is absent. */
    suspend fun readManifest(): String?

    /** Raw text of a manifest-listed `assets/liturgy/{assetFile}`; throws if it is missing. */
    suspend fun readAsset(assetFile: String): String
}

/**
 * [LiturgyAssetSource] backed by the bundled `assets/liturgy/` files, mirroring
 * [com.manna.bible.data.bundled.BundledBibleAssetReader] / [com.manna.bible.data.canon.AssetCanonDefinitionDataSource].
 * All I/O happens on [Dispatchers.IO]. A missing manifest yields null (the feature simply
 * shows nothing) rather than crashing.
 */
@Singleton
class AndroidLiturgyAssetSource @Inject constructor(
    @ApplicationContext private val context: Context
) : LiturgyAssetSource {

    override suspend fun readManifest(): String? = withContext(Dispatchers.IO) {
        try {
            context.assets.open("$LITURGY_DIR/$MANIFEST_FILE").bufferedReader().use { it.readText() }
        } catch (_: FileNotFoundException) {
            null
        } catch (_: IOException) {
            // AssetManager.open throws a plain IOException when the asset is absent.
            null
        }
    }

    override suspend fun readAsset(assetFile: String): String = withContext(Dispatchers.IO) {
        context.assets.open("$LITURGY_DIR/$assetFile").bufferedReader().use { it.readText() }
    }

    private companion object {
        const val LITURGY_DIR = "liturgy"
        const val MANIFEST_FILE = "manifest.json"
    }
}
