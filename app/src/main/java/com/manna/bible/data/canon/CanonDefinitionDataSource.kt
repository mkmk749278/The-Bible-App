package com.manna.bible.data.canon

import com.manna.bible.domain.model.CanonDefinition
import com.manna.bible.domain.model.CanonType

/**
 * Provides parsed [CanonDefinition]s for each [CanonType].
 *
 * Implementations source the canon metadata (book set, ordering, numbering scheme)
 * from bundled definitions and must never require a network connection.
 */
interface CanonDefinitionDataSource {

    /**
     * Returns the [CanonDefinition] for the given [canonType].
     *
     * For [CanonType.ALL_CANONS] the union of all bundled canons is computed at
     * runtime. Implementations fall back to a safe default rather than crashing
     * when an individual definition is missing or corrupt.
     */
    suspend fun definitionFor(canonType: CanonType): CanonDefinition
}
