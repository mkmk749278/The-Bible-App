package com.manna.bible.domain.usecase

import com.manna.bible.data.preferences.PreferencesStore
import javax.inject.Inject

/**
 * Persists the current `Reading_Position` so the reader can resume there on the
 * next launch (Requirement 7.1).
 *
 * The position is stored in canonical `OSIS.CHAPTER.VERSE` form via
 * [ReadingRef.format], keeping it stable across denominations and translations.
 *
 * Pure Kotlin — depends only on `javax.inject` for constructor injection.
 */
class SaveReadingPositionUseCase @Inject constructor(
    private val preferencesStore: PreferencesStore
) {

    /** Persists [ref] as the last-read position. */
    suspend operator fun invoke(ref: ReadingRef) {
        preferencesStore.setLastReadPosition(ref.format())
    }
}
