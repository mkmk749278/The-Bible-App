package com.manna.bible.domain.usecase

import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.canon.CanonEngine
import com.manna.bible.domain.lectionary.LectionaryProvider
import com.manna.bible.domain.model.Denomination
import com.manna.bible.domain.model.SetupState
import javax.inject.Inject

/**
 * The user's setup choices gathered across the setup flow, prior to deriving the
 * canon configuration.
 *
 * All fields are nullable to model a skipped or partially completed flow. The use
 * case applies the appropriate defaults (Requirements 2 and 7) before persisting.
 *
 * @property denomination Chosen tradition, or null when the user skipped / chose "not sure".
 * @property uiLanguage Selected app UI language code, or null.
 * @property bibleLanguage Selected Bible text language code, or null (defaults to [uiLanguage], Req 7.4).
 * @property bibleTranslationId Selected translation id, or null to use the profile's suggestion.
 * @property showDeuterocanonical Protestant toggle to show deuterocanonical books.
 *
 * Pure Kotlin — no Android dependencies.
 */
data class SetupSelections(
    val denomination: Denomination?,
    val uiLanguage: String?,
    val bibleLanguage: String?,
    val bibleTranslationId: String?,
    val showDeuterocanonical: Boolean = false
)

/**
 * Completes the denomination-aware setup flow: derives the [com.manna.bible.domain.model.CanonProfile]
 * from the user's [SetupSelections] and persists the resulting [SetupState] (Requirements 2 and 10).
 *
 * A skipped denomination ("not sure") maps to [Denomination.SHOW_EVERYTHING], which resolves to the
 * all-canons configuration (Requirement 2.3). Bible language defaults to the UI language when unset
 * (Requirement 7.4). Persistence is wrapped so a storage failure surfaces as [Result.failure]
 * rather than throwing (Requirement 15.4).
 *
 * Pure Kotlin — depends only on `javax.inject` for constructor injection.
 */
class CompleteSetupUseCase @Inject constructor(
    private val canonEngine: CanonEngine,
    private val lectionaryProvider: LectionaryProvider,
    private val preferencesStore: PreferencesStore
) {

    /**
     * Derives and persists the [SetupState] for [input]. Returns [Result.success] once the state is
     * persisted, or [Result.failure] if persistence throws.
     */
    suspend operator fun invoke(input: SetupSelections): Result<Unit> {
        // A skipped / "not sure" denomination shows everything (Requirement 2.3).
        val effectiveDenomination = input.denomination ?: Denomination.SHOW_EVERYTHING

        // Bible language defaults to the UI language when unset (Requirement 7.4).
        val effectiveBibleLanguage = input.bibleLanguage ?: input.uiLanguage

        val profile = canonEngine
            .profileFor(effectiveDenomination, effectiveBibleLanguage ?: "")
            .copy(lectionaryId = lectionaryProvider.lectionaryIdFor(effectiveDenomination))

        val state = SetupState(
            denomination = effectiveDenomination,
            canonType = profile.canonType,
            uiLanguage = input.uiLanguage,
            bibleLanguage = effectiveBibleLanguage,
            numberingScheme = profile.numberingScheme,
            namingConventionId = profile.namingConventionId,
            bibleTranslationId = input.bibleTranslationId ?: profile.suggestedTranslationId,
            lectionaryId = profile.lectionaryId,
            showDeuterocanonical = input.showDeuterocanonical,
            setupCompleted = true
        )

        return runCatching { preferencesStore.saveSetup(state) }
    }
}
