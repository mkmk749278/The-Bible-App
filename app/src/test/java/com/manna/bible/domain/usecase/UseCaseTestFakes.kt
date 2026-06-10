package com.manna.bible.domain.usecase

import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.canon.CanonEngine
import com.manna.bible.domain.lectionary.LectionaryProvider
import com.manna.bible.domain.model.CanonProfile
import com.manna.bible.domain.model.CanonType
import com.manna.bible.domain.model.Denomination
import com.manna.bible.domain.model.NumberingScheme
import com.manna.bible.domain.model.SetupState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Fake [CanonEngine] returning deterministic profiles matching the real engine's mapping. */
internal class FakeCanonEngine : CanonEngine {
    var lastBibleLanguage: String? = null

    override fun canonTypeFor(denomination: Denomination): CanonType = when (denomination) {
        Denomination.CATHOLIC -> CanonType.CATHOLIC_73
        Denomination.ORTHODOX -> CanonType.ORTHODOX_EXPANDED
        Denomination.SHOW_EVERYTHING -> CanonType.ALL_CANONS
        else -> CanonType.PROTESTANT_66
    }

    override suspend fun profileFor(denomination: Denomination, bibleLanguage: String): CanonProfile {
        lastBibleLanguage = bibleLanguage
        val canonType = canonTypeFor(denomination)
        val numbering = when (canonType) {
            CanonType.CATHOLIC_73, CanonType.ORTHODOX_EXPANDED -> NumberingScheme.SEPTUAGINT
            else -> NumberingScheme.MASORETIC
        }
        return CanonProfile(
            denomination = denomination,
            canonType = canonType,
            books = emptyList(),
            numberingScheme = numbering,
            namingConventionId = null,
            suggestedTranslationId = null,
            lectionaryId = null
        )
    }
}

/** Fake [LectionaryProvider] mirroring the real denomination -> lectionary mapping. */
internal class FakeLectionaryProvider : LectionaryProvider {
    override fun lectionaryIdFor(denomination: Denomination): String? = when (denomination) {
        Denomination.CSI -> "csi_almanac"
        Denomination.CATHOLIC -> "rc_calendar"
        Denomination.ORTHODOX -> "orthodox_calendar"
        Denomination.MAR_THOMA, Denomination.PROTESTANT_OTHER -> "general_lectionary"
        Denomination.SHOW_EVERYTHING -> null
    }
}

/** In-memory [PreferencesStore] that records the last saved state / updated profile. */
internal class FakePreferencesStore(private val failOnSave: Boolean = false) : PreferencesStore {
    var savedState: SetupState? = null
    var updatedProfile: CanonProfile? = null
    var activeTranslationId: String? = null
    private val lastReadPositionFlow = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    /** Seeds the persisted last-read position for restore tests. */
    fun seedLastReadPosition(ref: String?) {
        lastReadPositionFlow.value = ref
    }

    override val setupState: Flow<SetupState>
        get() = flowOf(savedState ?: error("no state"))

    override val lastReadPosition: Flow<String?>
        get() = lastReadPositionFlow

    override suspend fun saveSetup(state: SetupState) {
        if (failOnSave) throw RuntimeException("disk full")
        savedState = state
    }

    override suspend fun setSetupCompleted(value: Boolean) = Unit

    override suspend fun updateDenomination(profile: CanonProfile) {
        if (failOnSave) throw RuntimeException("disk full")
        updatedProfile = profile
    }

    override suspend fun setShowDeuterocanonical(value: Boolean) = Unit

    override suspend fun setActiveTranslation(translationId: String) {
        if (failOnSave) throw RuntimeException("disk full")
        activeTranslationId = translationId
    }

    override suspend fun setLastReadPosition(ref: String) {
        if (failOnSave) throw RuntimeException("disk full")
        lastReadPositionFlow.value = ref
    }
}
