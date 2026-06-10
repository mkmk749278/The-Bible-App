package com.manna.bible.data.preferences

import com.manna.bible.domain.model.CanonType
import com.manna.bible.domain.model.Denomination
import com.manna.bible.domain.model.NumberingScheme
import com.manna.bible.domain.model.SetupState

/**
 * Pure, Android-free mapping between [SetupState] and a plain `key -> value` map mirroring the
 * DataStore Preferences layout used by [DataStorePreferencesStore].
 *
 * Extracting the mapping here keeps it unit-testable on the JVM (no Android Context / DataStore
 * file required) and lets [DataStorePreferencesStore] delegate (de)serialization to a single
 * source of truth, satisfying the persistence idempotence property (Req 10): for any
 * [SetupState] `s`, `fromMap(toMap(s)) == s`.
 *
 * Conventions:
 * - Null selection fields are absent from the map (so a round-trip preserves null).
 * - Booleans (`showDeuterocanonical`, `setupCompleted`) are always present and default to false
 *   when missing on read.
 */
object SetupPreferencesMapper {

    /** Preference key names — must match the keys declared in [DataStorePreferencesStore]. */
    object Keys {
        const val DENOMINATION = "denomination"
        const val CANON = "canon"
        const val UI_LANGUAGE = "uiLanguage"
        const val BIBLE_LANGUAGE = "bibleLanguage"
        const val NUMBERING_SCHEME = "numberingScheme"
        const val NAMING_CONVENTION = "namingConvention"
        const val BIBLE_TRANSLATION_ID = "bibleTranslationId"
        const val LECTIONARY = "lectionary"
        const val SETUP_COMPLETED = "setupCompleted"
        const val SHOW_DEUTEROCANONICAL = "showDeuterocanonical"

        /** Last-read `Reading_Position` (canonical `OSIS.CHAPTER.VERSE`); standalone, not part of [SetupState]. */
        const val LAST_READ_POSITION = "lastReadPosition"
    }

    /**
     * Serializes [state] to a plain map. Null selection fields are omitted; booleans are always
     * included.
     */
    fun toMap(state: SetupState): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        state.denomination?.let { map[Keys.DENOMINATION] = it.id }
        state.canonType?.let { map[Keys.CANON] = it.id }
        state.uiLanguage?.let { map[Keys.UI_LANGUAGE] = it }
        state.bibleLanguage?.let { map[Keys.BIBLE_LANGUAGE] = it }
        state.numberingScheme?.let { map[Keys.NUMBERING_SCHEME] = it.name }
        state.namingConventionId?.let { map[Keys.NAMING_CONVENTION] = it }
        state.bibleTranslationId?.let { map[Keys.BIBLE_TRANSLATION_ID] = it }
        state.lectionaryId?.let { map[Keys.LECTIONARY] = it }
        map[Keys.SETUP_COMPLETED] = state.setupCompleted
        map[Keys.SHOW_DEUTEROCANONICAL] = state.showDeuterocanonical
        return map
    }

    /**
     * Deserializes a plain map into a [SetupState]. Missing selection fields map to null; missing
     * booleans default to false. Enum values that cannot be resolved map to null (null-safe).
     */
    fun fromMap(prefs: Map<String, Any?>): SetupState =
        SetupState(
            denomination = (prefs[Keys.DENOMINATION] as? String)?.let { Denomination.fromId(it) },
            canonType = (prefs[Keys.CANON] as? String)?.let { CanonType.fromId(it) },
            uiLanguage = prefs[Keys.UI_LANGUAGE] as? String,
            bibleLanguage = prefs[Keys.BIBLE_LANGUAGE] as? String,
            numberingScheme = (prefs[Keys.NUMBERING_SCHEME] as? String)?.let { numberingSchemeOrNull(it) },
            namingConventionId = prefs[Keys.NAMING_CONVENTION] as? String,
            bibleTranslationId = prefs[Keys.BIBLE_TRANSLATION_ID] as? String,
            lectionaryId = prefs[Keys.LECTIONARY] as? String,
            showDeuterocanonical = prefs[Keys.SHOW_DEUTEROCANONICAL] as? Boolean ?: false,
            setupCompleted = prefs[Keys.SETUP_COMPLETED] as? Boolean ?: false
        )

    private fun numberingSchemeOrNull(name: String): NumberingScheme? =
        NumberingScheme.entries.firstOrNull { it.name == name }
}
