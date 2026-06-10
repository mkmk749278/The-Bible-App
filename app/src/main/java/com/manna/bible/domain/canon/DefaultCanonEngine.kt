package com.manna.bible.domain.canon

import com.manna.bible.data.canon.CanonDefinitionDataSource
import com.manna.bible.domain.model.CanonProfile
import com.manna.bible.domain.model.CanonType
import com.manna.bible.domain.model.Denomination
import javax.inject.Inject

/**
 * Default [CanonEngine] backed by a [CanonDefinitionDataSource].
 *
 * Pure Kotlin: the only non-stdlib dependency is `javax.inject` for constructor
 * injection. The engine resolves the [CanonType] for a denomination, loads the
 * matching [com.manna.bible.domain.model.CanonDefinition], and assembles a
 * [CanonProfile]. Naming convention, suggested translation, and lectionary are
 * resolved by later tasks and left null here.
 */
class DefaultCanonEngine @Inject constructor(
    private val dataSource: CanonDefinitionDataSource
) : CanonEngine {

    override fun canonTypeFor(denomination: Denomination): CanonType = when (denomination) {
        Denomination.CATHOLIC -> CanonType.CATHOLIC_73
        Denomination.CSI,
        Denomination.PROTESTANT_OTHER,
        Denomination.MAR_THOMA -> CanonType.PROTESTANT_66
        Denomination.ORTHODOX -> CanonType.ORTHODOX_EXPANDED
        Denomination.SHOW_EVERYTHING -> CanonType.ALL_CANONS
    }

    override suspend fun profileFor(
        denomination: Denomination,
        bibleLanguage: String
    ): CanonProfile {
        val canonType = canonTypeFor(denomination)
        val definition = dataSource.definitionFor(canonType)

        return CanonProfile(
            denomination = denomination,
            canonType = canonType,
            // The definition's books already carry deterministic, contiguous
            // orderIndex; return them ordered to guarantee stable presentation.
            books = definition.books.sortedBy { it.orderIndex },
            // Trust the definition's scheme (Requirement 7): SEPTUAGINT for
            // Catholic/Orthodox, MASORETIC otherwise.
            numberingScheme = definition.numberingScheme,
            // Resolved by later tasks; default to translation/built-in behaviour.
            namingConventionId = null,
            suggestedTranslationId = null,
            lectionaryId = null
        )
    }
}
