package com.manna.bible.data.liturgy

import kotlinx.serialization.Serializable

/**
 * Serialization DTOs matching the bundled liturgy JSON schema under `assets/liturgy/`.
 *
 * Two asset shapes live there, mirroring the `assets/bibles/` and `assets/canon/`
 * conventions:
 *  - `manifest.json` — an index of available liturgies ([LiturgyManifestDto]).
 *  - `{id}.json` — one full order of service ([LiturgyDto]).
 *
 * Localized content fields are `Map<String, String>` (language tag → authored text);
 * they map onto [com.manna.bible.domain.liturgy.LocalizedText] in [LiturgyMapper].
 * Internal to the data layer — the rest of the app consumes the mapped domain
 * [com.manna.bible.domain.liturgy.Liturgy] instead.
 */

/** Top-level `manifest.json` listing every bundled liturgy and its asset file. */
@Serializable
internal data class LiturgyManifestDto(
    val liturgies: List<LiturgyManifestEntryDto> = emptyList()
)

/** One entry within [LiturgyManifestDto.liturgies]. */
@Serializable
internal data class LiturgyManifestEntryDto(
    val id: String,
    val title: String,
    val tradition: String,
    val denominations: List<String> = emptyList(),
    val languages: List<String> = listOf("en"),
    val assetFile: String
)

/** The full content of a `{id}.json` asset: one order of service. */
@Serializable
internal data class LiturgyDto(
    val id: String,
    val title: Map<String, String>,
    val tradition: String,
    val denominations: List<String> = emptyList(),
    val languages: List<String> = listOf("en"),
    val sourceNote: Map<String, String>,
    val sections: List<LiturgySectionDto> = emptyList()
)

/** A major division of the service within a [LiturgyDto]. */
@Serializable
internal data class LiturgySectionDto(
    val title: Map<String, String>,
    val parts: List<LiturgyPartDto> = emptyList()
)

/** One step of the service within a [LiturgySectionDto]. */
@Serializable
internal data class LiturgyPartDto(
    val role: String,
    val title: Map<String, String>? = null,
    val text: Map<String, String>? = null,
    val rubric: Map<String, String>? = null,
    val osisRef: String? = null,
    val needsOfficialText: Boolean = false
)
