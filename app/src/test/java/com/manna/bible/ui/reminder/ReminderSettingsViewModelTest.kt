package com.manna.bible.ui.reminder

import app.cash.turbine.test
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.model.SetupState
import com.manna.bible.domain.reminder.ReminderTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit tests for [ReminderSettingsViewModel] — persists the enabled flag and time
 * to the preferences store and reflects them back in UI state.
 */
class ReminderSettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    @DisplayName("persists the enabled flag and add/remove of multiple times")
    fun persistsSettings() = runTest {
        val store = FakePreferencesStore()
        val vm = ReminderSettingsViewModel(store)

        vm.uiState.test {
            // Initial state matches the default (off, single 07:00) so no duplicate emit.
            assertEquals(ReminderSettingsUiState(), awaitItem())

            vm.setEnabled(true)
            advanceUntilIdle()
            assertTrue(awaitItem().enabled)

            // Add a second and third prayer-bell time.
            vm.addTime(12, 0)
            advanceUntilIdle()
            assertEquals(listOf(ReminderTime(7, 0), ReminderTime(12, 0)), awaitItem().times)

            vm.addTime(20, 0)
            advanceUntilIdle()
            assertEquals(
                listOf(ReminderTime(7, 0), ReminderTime(12, 0), ReminderTime(20, 0)),
                awaitItem().times
            )

            // Remove the morning one.
            vm.removeTime(ReminderTime(7, 0))
            advanceUntilIdle()
            assertEquals(listOf(ReminderTime(12, 0), ReminderTime(20, 0)), awaitItem().times)

            vm.setEnabled(false)
            advanceUntilIdle()
            assertFalse(awaitItem().enabled)

            cancelAndIgnoreRemainingEvents()
        }
    }

    private class FakePreferencesStore : PreferencesStore {
        private val enabled = MutableStateFlow(false)
        private val times = MutableStateFlow("07:00")

        override val setupState: Flow<SetupState> = MutableStateFlow(
            SetupState(
                denomination = null,
                canonType = null,
                uiLanguage = null,
                bibleLanguage = null,
                numberingScheme = null,
                namingConventionId = null,
                bibleTranslationId = null,
                lectionaryId = null
            )
        )
        override val lastReadPosition: Flow<String?> = MutableStateFlow(null)
        override val dailyReminderEnabled: Flow<Boolean> = enabled
        override val dailyReminderTimes: Flow<String> = times

        override suspend fun setDailyReminderEnabled(value: Boolean) { enabled.value = value }
        override suspend fun setDailyReminderTimes(value: String) { times.value = value }

        override suspend fun saveSetup(state: SetupState) {}
        override suspend fun setSetupCompleted(value: Boolean) {}
        override suspend fun updateDenomination(profile: com.manna.bible.domain.model.CanonProfile) {}
        override suspend fun setShowDeuterocanonical(value: Boolean) {}
        override suspend fun setActiveTranslation(translationId: String) {}
        override suspend fun setLastReadPosition(ref: String) {}
    }
}
