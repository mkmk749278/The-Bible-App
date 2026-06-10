package com.manna.bible.domain.usecase

import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.canon.CanonEngine
import com.manna.bible.domain.lectionary.LectionaryProvider
import com.manna.bible.domain.model.CanonProfile
import com.manna.bible.domain.model.Denomination
import javax.inject.Inject

/**
 * Recomputes and persists the canon configuration when the user changes their denomination after
 * setup (Requirement 11).
 *
 * The use case re-derives the [CanonProfile] from the new [Denomination], enriches it with the
 * associated lectionary, and persists only the canon-related preferences via
 * [PreferencesStore.updateDenomination] — other preferences (languages, toggles) and stored
 * annotations are left untouched (Requirement 11.4). Failures surface as [Result.failure].
 *
 * Pure Kotlin — depends only on `javax.inject` for constructor injection.
 */
class ApplyDenominationChangeUseCase @Inject constructor(
    private val canonEngine: CanonEngine,
    private val lectionaryProvider: LectionaryProvider,
    private val preferencesStore: PreferencesStore
) {

    /**
     * Recomputes the [CanonProfile] for [denomination] / [bibleLanguage], persists the canon config,
     * and returns the recomputed profile. Returns [Result.failure] if persistence throws.
     */
    suspend operator fun invoke(
        denomination: Denomination,
        bibleLanguage: String
    ): Result<CanonProfile> = runCatching {
        val profile = canonEngine
            .profileFor(denomination, bibleLanguage)
            .copy(lectionaryId = lectionaryProvider.lectionaryIdFor(denomination))

        preferencesStore.updateDenomination(profile)
        profile
    }
}
