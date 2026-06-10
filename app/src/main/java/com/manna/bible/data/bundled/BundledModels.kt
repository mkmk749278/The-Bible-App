package com.manna.bible.data.bundled

import kotlinx.serialization.Serializable

/**
 * Serialization models for the bundled Bible assets produced by the
 * `:app:prepareBundledBibles` Gradle task (task 3.1) via
 * `buildSrc/.../BundledBibleGenerator.kt`.
 *
 * Two asset shapes live under `assets/bibles/`:
 *  - `manifest.json` (uncompressed) — see [BundledManifest].
 *  - `{assetFile}.gz` (gzipped JSON) — see [BundledBible].
 *
 * These shapes MUST match the generator's output exactly. They are internal to
 * the data layer; the rest of the app consumes the seeded Room content instead.
 */

/** Top-level `manifest.json` listing every bundled translation. */
@Serializable
data class BundledManifest(
    val translations: List<BundledTranslationManifest> = emptyList()
)

/**
 * A single entry within [BundledManifest.translations] describing one bundled
 * translation and the gzipped asset that holds its content.
 */
@Serializable
data class BundledTranslationManifest(
    val id: String,
    val name: String,
    val languageCode: String,
    val canonType: String,
    val bundledContentVersion: Int,
    val verseCount: Int,
    val checksum: String,
    val assetFile: String,
    val isDeuterocanon: Boolean
)

/**
 * The decompressed content of a `{assetFile}.gz` asset: the full book and verse
 * data for a single bundled translation.
 */
@Serializable
data class BundledBible(
    val translationId: String,
    val books: List<BundledBook> = emptyList(),
    val verses: List<BundledVerse> = emptyList()
)

/** Book metadata within a [BundledBible]. */
@Serializable
data class BundledBook(
    val osisId: String,
    val name: String,
    val testament: String,
    val orderIndex: Int,
    val chapterCount: Int
)

/** A single verse within a [BundledBible]. */
@Serializable
data class BundledVerse(
    val osisId: String,
    val chapter: Int,
    val verse: Int,
    val text: String
)
