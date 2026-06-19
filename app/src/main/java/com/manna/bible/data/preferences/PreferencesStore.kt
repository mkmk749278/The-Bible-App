package com.manna.bible.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
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

    /**
     * Emits the reminder times as a comma-separated `HH:mm` list (e.g.
     * "08:00,12:00,20:00") — the user's prayer-bell schedule. Defaults to the single
     * legacy reminder time so existing users keep their one reminder.
     */
    val dailyReminderTimes: Flow<String>
        get() = flowOf("07:00")

    /** Persists the reminder times as a comma-separated `HH:mm` list. */
    suspend fun setDailyReminderTimes(value: String) {}

    /**
     * Emits the comma-separated `HH:mm` list the scheduler last armed, so the
     * coordinator can cancel alarms that have since been removed (even across a
     * process restart). Defaults to empty.
     */
    val scheduledReminderTimes: Flow<String>
        get() = flowOf("")

    /** Records the comma-separated `HH:mm` list currently armed with the OS. */
    suspend fun setScheduledReminderTimes(value: String) {}

    /**
     * Emits the epoch day on which the 30-day grief journey began, or -1 when it has
     * not been started. The current day is derived from this and today's date.
     */
    val griefStartEpochDay: Flow<Long>
        get() = flowOf(-1L)

    /** Persists the grief journey's start epoch day (begins the journey). */
    suspend fun setGriefStartEpochDay(value: Long) {}

    /** Emits the active fast's start epoch millis, or -1 when no fast is active. */
    val fastStartMillis: Flow<Long>
        get() = flowOf(-1L)

    /** Emits the active fast's plan id, or "" when no fast is active. */
    val fastPlanId: Flow<String>
        get() = flowOf("")

    /** Starts/clears the active fast (start millis < 0 and blank id clear it). */
    suspend fun setActiveFast(startMillis: Long, planId: String) {}

    /**
     * Emits the epoch day on which the active 40-day Sramanikal (memorial) began, or
     * -1 when none is active. The current day is derived from this and today's date.
     */
    val sramanikalStartEpochDay: Flow<Long>
        get() = flowOf(-1L)

    /** Emits the name of the one being remembered in the active Sramanikal, or "". */
    val sramanikalName: Flow<String>
        get() = flowOf("")

    /** Starts a Sramanikal for [name] beginning on [startEpochDay]. */
    suspend fun setSramanikal(startEpochDay: Long, name: String) {}

    /** Clears the active Sramanikal. */
    suspend fun clearSramanikal() {}
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
        val DAILY_REMINDER_TIMES = stringPreferencesKey("daily_reminder_times")
        val SCHEDULED_REMINDER_TIMES = stringPreferencesKey("scheduled_reminder_times")
        val GRIEF_START_EPOCH_DAY = longPreferencesKey("grief_start_epoch_day")
        val FAST_START_MILLIS = longPreferencesKey("fast_start_millis")
        val FAST_PLAN_ID = stringPreferencesKey("fast_plan_id")
        val SRAMANIKAL_START_EPOCH_DAY = longPreferencesKey("sramanikal_start_epoch_day")
        val SRAMANIKAL_NAME = stringPreferencesKey("sramanikal_name")
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

    override val dailyReminderTimes: Flow<String> =
        dataStore.data.map { prefs ->
            // Fall back to the legacy single time so a pre-existing reminder survives.
            prefs[Keys.DAILY_REMINDER_TIMES] ?: prefs[Keys.DAILY_REMINDER_TIME] ?: "07:00"
        }

    override val scheduledReminderTimes: Flow<String> =
        dataStore.data.map { prefs -> prefs[Keys.SCHEDULED_REMINDER_TIMES] ?: "" }

    override val griefStartEpochDay: Flow<Long> =
        dataStore.data.map { prefs -> prefs[Keys.GRIEF_START_EPOCH_DAY] ?: -1L }

    override val fastStartMillis: Flow<Long> =
        dataStore.data.map { prefs -> prefs[Keys.FAST_START_MILLIS] ?: -1L }

    override val fastPlanId: Flow<String> =
        dataStore.data.map { prefs -> prefs[Keys.FAST_PLAN_ID] ?: "" }

    override val sramanikalStartEpochDay: Flow<Long> =
        dataStore.data.map { prefs -> prefs[Keys.SRAMANIKAL_START_EPOCH_DAY] ?: -1L }

    override val sramanikalName: Flow<String> =
        dataStore.data.map { prefs -> prefs[Keys.SRAMANIKAL_NAME] ?: "" }

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

    override suspend fun setDailyReminderTimes(value: String) {
        dataStore.edit { prefs -> prefs[Keys.DAILY_REMINDER_TIMES] = value }
    }

    override suspend fun setScheduledReminderTimes(value: String) {
        dataStore.edit { prefs -> prefs[Keys.SCHEDULED_REMINDER_TIMES] = value }
    }

    override suspend fun setGriefStartEpochDay(value: Long) {
        dataStore.edit { prefs -> prefs[Keys.GRIEF_START_EPOCH_DAY] = value }
    }

    override suspend fun setActiveFast(startMillis: Long, planId: String) {
        dataStore.edit { prefs ->
            prefs[Keys.FAST_START_MILLIS] = startMillis
            prefs[Keys.FAST_PLAN_ID] = planId
        }
    }

    override suspend fun setSramanikal(startEpochDay: Long, name: String) {
        dataStore.edit { prefs ->
            prefs[Keys.SRAMANIKAL_START_EPOCH_DAY] = startEpochDay
            prefs[Keys.SRAMANIKAL_NAME] = name
        }
    }

    override suspend fun clearSramanikal() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.SRAMANIKAL_START_EPOCH_DAY)
            prefs.remove(Keys.SRAMANIKAL_NAME)
        }
    }

    private fun putOrRemove(prefs: MutablePreferences, key: Preferences.Key<String>, value: String?) {
        if (value == null) prefs.remove(key) else prefs[key] = value
    }
}
