package com.manna.bible.data.canon

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Serialization DTO matching the bundled canon JSON schema in the `assets/canon`
 * directory (one JSON file per canon type).
 *
 * Internal to the data layer — the rest of the app consumes the mapped
 * [com.manna.bible.domain.model.CanonDefinition] instead.
 *
 * Example JSON:
 * ```json
 * {
 *   "canonType": "catholic_73",
 *   "numberingScheme": "septuagint",
 *   "books": [ { "osisId": "GEN", "testament": "OLD", "orderIndex": 0, "deuterocanonical": false } ]
 * }
 * ```
 */
@Serializable
internal data class CanonDefinitionDto(
    val canonType: String,
    val numberingScheme: String,
    val books: List<CanonBookDto>
)

/**
 * Serialization DTO for a single book entry within a canon definition.
 *
 * The JSON key `deuterocanonical` is mapped to [isDeuterocanonical] to match the
 * domain model's property naming.
 */
@Serializable
internal data class CanonBookDto(
    val osisId: String,
    val testament: String,
    val orderIndex: Int,
    @SerialName("deuterocanonical") val isDeuterocanonical: Boolean
)
