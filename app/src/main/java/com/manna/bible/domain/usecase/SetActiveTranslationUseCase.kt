package com.manna.bible.domain.usecase

import com.manna.bible.data.preferences.PreferencesStore
import javax.inject.Inject

/**
 * Sets and persists the `Active_Translation` (`bibleTranslationId`) via the
 * [PreferencesStore] (Requirement 6.2).
 *
 * Pure Kotlin — depends only on `javax.inject` for constructor injection.
 */
class SetActiveTranslationUseCase @Inject constructor(
    private val preferencesStore: PreferencesStore
) {

    /** Persists [translationId] as the active translation. */
    suspend operator fun invoke(translationId: String) {
        preferencesStore.setActiveTranslation(translationId)
    }
}
