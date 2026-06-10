package com.manna.bible.domain.usecase

import com.manna.bible.domain.canon.CanonEngine
import com.manna.bible.domain.model.Denomination
import com.manna.bible.domain.repository.AnnotationRepository
import javax.inject.Inject

/**
 * The outcome of evaluating a prospective canon switch (Req 12).
 *
 * @property excludedBookIds OSIS ids of books that carry annotations but would no
 *   longer be visible under the candidate canon. Annotations are hidden, never
 *   deleted — this set drives the pre-switch warning dialog.
 * @property hasImpact True when at least one annotated book would be hidden.
 */
data class CanonSwitchImpact(
    val excludedBookIds: Set<String>,
    val hasImpact: Boolean
)

/**
 * Computes which annotated books would be hidden by switching to a candidate
 * canon, so the UI can warn the user before applying a canon change (Req 12).
 *
 * The use case derives the candidate [com.manna.bible.domain.model.CanonProfile]
 * via the [CanonEngine], reduces it to the set of visible OSIS book ids, and asks
 * the [AnnotationRepository] which annotated books fall outside that set.
 *
 * Pure Kotlin — no Android dependencies.
 */
class ResolveCanonSwitchImpactUseCase @Inject constructor(
    private val canonEngine: CanonEngine,
    private val annotationRepository: AnnotationRepository
) {

    /**
     * Evaluates the impact of switching to [targetDenomination] for the given
     * [bibleLanguage].
     *
     * @return a [CanonSwitchImpact] listing the annotated books that would be
     *   hidden by the candidate canon, with [CanonSwitchImpact.hasImpact] set when
     *   that set is non-empty.
     */
    suspend operator fun invoke(
        targetDenomination: Denomination,
        bibleLanguage: String
    ): CanonSwitchImpact {
        val profile = canonEngine.profileFor(targetDenomination, bibleLanguage)
        val visibleBookIds = profile.books.map { it.osisId }.toSet()
        val excluded = annotationRepository.annotatedBookIdsOutside(visibleBookIds)
        return CanonSwitchImpact(excluded, excluded.isNotEmpty())
    }
}
