package com.manna.bible.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.manna.bible.domain.model.CanonProfile
import com.manna.bible.domain.model.SetupState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Single DataStore<Preferences> instance for the app, backed by the `manna_prefs` file. */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "manna_prefs")

/**
 * Persists the user's setup choices ([SetupState]) to local storage (Req 10), without requiring
 * an account. Backed by DataStore Preferences.
 */
interface PreferencesStore {
    /** Emits the current persisted [SetupState], updating whenever preferences change. */
    val setupState: Flow<SetupState>

    /**
     * Emits the persisted last-read `Reading_Position` (canonical `OSIS.CHAPTER.VERSE`), or null
     * when none has been persisted yet (Req 7).
     */
    val lastReadPosition: Flow<String?>

    /** Persists every field of [state], removing keys for null selection fields. */
    suspend fun saveSetup(state: SetupState)

    /** Persists only the first-launch gate flag (Req 1). */
    suspend fun setSetupCompleted(value: Boolean)

    /** Persists the canon-related fields derived from [profile]; leaves language keys untouched. */
    suspend fun updateDenomination(profile: CanonProfile)

    /** Persists only the Protestant deuterocanonical visibility toggle (Req 15). */
    suspend fun setShowDeuterocanonical(value: Boolean)

    /** Persists the active translation id (`bibleTranslationId`) (Req 6.2). */
    suspend fun setActiveTranslation(translationId: String)

    /** Persists the last-read `Reading_Position` as a canonical `OSIS.CHAPTER.VERSE` string (Req 7.1). */
    suspend fun setLastReadPosition(ref: String)

    /**
     * Emits the audio continuous-play preference: when true, read-aloud advances to
     * the next chapter at a chapter's natural end (Req 9.7). Defaults to false.
     */
    val continuousPlay: Flow<Boolean>
        get() = flowOf(false)

    /** Persists the audio continuous-play preference (Req 9.7). */
    suspend fun setContinuousPlay(value: Boolean) {}

    /**
     * Emits the Simplified Mode (Elder / Oral Bible) preference: an audio-first
     * presentation with enlarged controls (Req 14.5). Defaults to false.
     */
    val simplifiedMode: Flow<Boolean>
        get() = flowOf(false)

    /** Persists the Simplified Mode preference (Req 14.5). */
    suspend fun setSimplifiedMode(value: Boolean) {}

    /**
     * Emits whether the daily verse reminder notification is enabled. Defaults to
     * false (no reminder until the user opts in during onboarding or settings).
     */
    val dailyReminderEnabled: Flow<Boolean>
        get() = flowOf(false)

    /** Persists the daily-reminder enabled flag. */
    suspend fun setDailyReminderEnabled(value: Boolean) {}

    /** Emits the daily-reminder time as an `HH:mm` string. Defaults to "07:00". */
    val dailyReminderTime: Flow<String>
        get() = flowOf("07:00")

    /** Persists the daily-reminder time as an `HH:mm` string. */
    suspend fun setDailyReminderTime(value: String) {}
}

/**
 * DataStore-backed [PreferencesStore]. (De)serialization is delegated to [SetupPreferencesMapper]
 * so the mapping logic stays pure and JVM-testable.
 */
class DataStorePreferencesStore @Inject constructor(
    @ApplicationContext private val context: Context
) : PreferencesStore {

    private val dataStore: DataStore<Preferences> = context.dataStore

    private object Keys {
        val DENOMINATION = stringPreferencesKey(SetupPreferencesMapper.Keys.DENOMINATION)
        val CANON = stringPreferencesKey(SetupPreferencesMapper.Keys.CANON)
        val UI_LANGUAGE = stringPreferencesKey(SetupPreferencesMapper.Keys.UI_LANGUAGE)
        val BIBLE_LANGUAGE = stringPreferencesKey(SetupPreferencesMapper.Keys.BIBLE_LANGUAGE)
        val NUMBERING_SCHEME = stringPreferencesKey(SetupPreferencesMapper.Keys.NUMBERING_SCHEME)
        val NAMING_CONVENTION = stringPreferencesKey(SetupPreferencesMapper.Keys.NAMING_CONVENTION)
        val BIBLE_TRANSLATION_ID = stringPreferencesKey(SetupPreferencesMapper.Keys.BIBLE_TRANSLATION_ID)
        val LECTIONARY = stringPreferencesKey(SetupPreferencesMapper.Keys.LECTIONARY)
        val SETUP_COMPLETED = booleanPreferencesKey(SetupPreferencesMapper.Keys.SETUP_COMPLETED)
        val SHOW_DEUTEROCANONICAL = booleanPreferencesKey(SetupPreferencesMapper.Keys.SHOW_DEUTEROCANONICAL)
        val LAST_READ_POSITION = stringPreferencesKey(SetupPreferencesMapper.Keys.LAST_READ_POSITION)
        val CONTINUOUS_PLAY = booleanPreferencesKey("continuous_play")
        val SIMPLIFIED_MODE = booleanPreferencesKey("simplified_mode")
        val DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
        val DAILY_REMINDER_TIME = stringPreferencesKey("daily_reminder_time")
    }

    override val setupState: Flow<SetupState> =
        dataStore.data.map { prefs -> SetupPreferencesMapper.fromMap(prefs.asMap().mapKeys { it.key.name }) }

    override val lastReadPosition: Flow<String?> =
        dataStore.data.map { prefs -> prefs[Keys.LAST_READ_POSITION] }

    override val continuousPlay: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[Keys.CONTINUOUS_PLAY] ?: false }

    override val simplifiedMode: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[Keys.SIMPLIFIED_MODE] ?: false }

    override val dailyReminderEnabled: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[Keys.DAILY_REMINDER_ENABLED] ?: false }

    override val dailyReminderTime: Flow<String> =
        dataStore.data.map { prefs -> prefs[Keys.DAILY_REMINDER_TIME] ?: "07:00" }

    override suspend fun saveSetup(state: SetupState) {
        dataStore.edit { prefs ->
            putOrRemove(prefs, Keys.DENOMINATION, state.denomination?.id)
            putOrRemove(prefs, Keys.CANON, state.canonType?.id)
            putOrRemove(prefs, Keys.UI_LANGUAGE, state.uiLanguage)
            putOrRemove(prefs, Keys.BIBLE_LANGUAGE, state.bibleLanguage)
            putOrRemove(prefs, Keys.NUMBERING_SCHEME, state.numberingScheme?.name)
            putOrRemove(prefs, Keys.NAMING_CONVENTION, state.namingConventionId)
            putOrRemove(prefs, Keys.BIBLE_TRANSLATION_ID, state.bibleTranslationId)
            putOrRemove(prefs, Keys.LECTIONARY, state.lectionaryId)
            prefs[Keys.SETUP_COMPLETED] = state.setupCompleted
            prefs[Keys.SHOW_DEUTEROCANONICAL] = state.showDeuterocanonical
        }
    }

    override suspend fun setSetupCompleted(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.SETUP_COMPLETED] = value }
    }

    override suspend fun updateDenomination(profile: CanonProfile) {
        dataStore.edit { prefs ->
            prefs[Keys.DENOMINATION] = profile.denomination.id
            prefs[Keys.CANON] = profile.canonType.id
            prefs[Keys.NUMBERING_SCHEME] = profile.numberingScheme.name
            putOrRemove(prefs, Keys.NAMING_CONVENTION, profile.namingConventionId)
            putOrRemove(prefs, Keys.BIBLE_TRANSLATION_ID, profile.suggestedTranslationId)
            putOrRemove(prefs, Keys.LECTIONARY, profile.lectionaryId)
        }
    }

    override suspend fun setShowDeuterocanonical(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.SHOW_DEUTEROCANONICAL] = value }
    }

    override suspend fun setActiveTranslation(translationId: String) {
        dataStore.edit { prefs -> prefs[Keys.BIBLE_TRANSLATION_ID] = translationId }
    }

    override suspend fun setLastReadPosition(ref: String) {
        dataStore.edit { prefs -> prefs[Keys.LAST_READ_POSITION] = ref }
    }

    override suspend fun setContinuousPlay(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.CONTINUOUS_PLAY] = value }
    }

    override suspend fun setSimplifiedMode(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.SIMPLIFIED_MODE] = value }
    }

    override suspend fun setDailyReminderEnabled(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.DAILY_REMINDER_ENABLED] = value }
    }

    override suspend fun setDailyReminderTime(value: String) {
        dataStore.edit { prefs -> prefs[Keys.DAILY_REMINDER_TIME] = value }
    }

    private fun putOrRemove(prefs: MutablePreferences, key: Preferences.Key<String>, value: String?) {
        if (value == null) prefs.remove(key) else prefs[key] = value
    }
}
