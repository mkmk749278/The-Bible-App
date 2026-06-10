package com.manna.bible.domain.translation

import com.manna.bible.domain.model.CanonProfile
import com.manna.bible.domain.model.CanonType
import javax.inject.Inject

/**
 * Default [TranslationFilter] implementation.
 *
 * Filtering is language + canon-compatibility driven (Requirement 5.3), with
 * deuterocanon-aware ranking for Catholic profiles (Requirement 5.4) and a
 * closest-in-language fallback in [suggestedDefault] when no compatible
 * translation exists (Requirement 5.6).
 *
 * Pure Kotlin — depends only on `javax.inject` for constructor injection.
 */
class DefaultTranslationFilter @Inject constructor() : TranslationFilter {

    override fun filter(
        catalog: List<Translation>,
        profile: CanonProfile,
        bibleLanguage: String
    ): List<Translation> =
        catalog
            .filter { it.matchesLanguage(bibleLanguage) && isCompatible(profile, it) }
            .sortedWith(rankComparator(profile))

    override fun suggestedDefault(
        catalog: List<Translation>,
        profile: CanonProfile,
        bibleLanguage: String
    ): Translation? {
        val filtered = filter(catalog, profile, bibleLanguage)

        // 1. Prefer an explicitly default edition from the filtered list.
        filtered.firstOrNull { it.isDefaultForCanon }?.let { return it }

        // 2. Otherwise the profile's suggestion when it exists in the catalog.
        profile.suggestedTranslationId?.let { suggestedId ->
            catalog.firstOrNull { it.id == suggestedId }?.let { return it }
        }

        // 3. Otherwise the first (best-ranked) compatible, in-language entry.
        filtered.firstOrNull()?.let { return it }

        // 4. Fallback (Req 5.6): the closest in-language translation across ALL
        //    canons — default editions first, then by name for determinism. Null
        //    when nothing matches the language at all.
        return catalog
            .filter { it.matchesLanguage(bibleLanguage) }
            .sortedWith(compareByDescending<Translation> { it.isDefaultForCanon }.thenBy { it.name })
            .firstOrNull()
    }

    /**
     * Decides whether [translation] is canon-compatible with [profile].
     *
     * - `ALL_CANONS`: every edition is compatible (multi-canon mode).
     * - `PROTESTANT_66`: strictly Protestant editions.
     * - `CATHOLIC_73`: editions whose canon is `CATHOLIC_73`, or that include the
     *   deuterocanon (a deuterocanon-bearing edition can serve the Catholic canon).
     * - `ORTHODOX_EXPANDED`: editions whose canon is `ORTHODOX_EXPANDED`, or
     *   `CATHOLIC_73` editions (accepted as a subset; Orthodox preferred via rank).
     */
    private fun isCompatible(profile: CanonProfile, translation: Translation): Boolean =
        when (profile.canonType) {
            CanonType.ALL_CANONS -> true
            CanonType.PROTESTANT_66 -> translation.canonType == CanonType.PROTESTANT_66
            CanonType.CATHOLIC_73 ->
                translation.canonType == CanonType.CATHOLIC_73 || translation.hasDeuterocanon
            CanonType.ORTHODOX_EXPANDED ->
                translation.canonType == CanonType.ORTHODOX_EXPANDED ||
                    translation.canonType == CanonType.CATHOLIC_73
        }

    /**
     * Deterministic ordering by primary canon rank, then default flag, then name.
     *
     * - `CATHOLIC_73`: editions with the deuterocanon sort first (Requirement 5.4).
     * - `ORTHODOX_EXPANDED`: native Orthodox editions sort ahead of Catholic
     *   subsets ("prefer ORTHODOX_EXPANDED").
     * - Other profiles: the primary rank is uniform, so default editions and then
     *   name decide the order.
     */
    private fun rankComparator(profile: CanonProfile): Comparator<Translation> =
        compareBy<Translation> { primaryRank(profile, it) }
            .thenByDescending { it.isDefaultForCanon }
            .thenBy { it.name }

    private fun primaryRank(profile: CanonProfile, translation: Translation): Int =
        when (profile.canonType) {
            CanonType.CATHOLIC_73 -> if (translation.hasDeuterocanon) 0 else 1
            CanonType.ORTHODOX_EXPANDED ->
                if (translation.canonType == CanonType.ORTHODOX_EXPANDED) 0 else 1
            else -> 0
        }

    private fun Translation.matchesLanguage(bibleLanguage: String): Boolean =
        languageCode.equals(bibleLanguage, ignoreCase = true)
}
