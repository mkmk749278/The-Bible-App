package com.manna.bible.ui.stealth

import app.cash.turbine.test
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.data.stealth.PinHasher
import com.manna.bible.domain.model.CanonProfile
import com.manna.bible.domain.model.SetupState
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

/** Unit tests for [StealthSettingsViewModel] — arming, changing, and disarming the PIN. */
class StealthSettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeEach fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterEach fun tearDown() { Dispatchers.resetMain() }

    @Test
    @DisplayName("enabling with a valid, confirmed PIN arms the mode and stores a hash")
    fun enableArms() = runTest {
        val store = FakePreferencesStore()
        val vm = StealthSettingsViewModel(store)

        vm.enable("4821", "4821")
        advanceUntilIdle()

        assertTrue(store.enabledValue())
        val credential = store.credentialValue()
        assertTrue(credential.isNotBlank())
        assertFalse(credential.contains("4821"), "PIN must not be stored in plaintext")
        assertTrue(PinHasher.verify("4821", credential))
        assertEquals(StealthMessage.ENABLED, vm.uiState.value.message)
    }

    @Test
    @DisplayName("a too-short PIN is rejected without persisting")
    fun rejectsShortPin() = runTest {
        val store = FakePreferencesStore()
        val vm = StealthSettingsViewModel(store)

        vm.enable("12", "12")
        advanceUntilIdle()

        assertFalse(store.enabledValue())
        assertEquals(StealthMessage.PIN_TOO_SHORT, vm.uiState.value.message)
    }

    @Test
    @DisplayName("a mismatched confirmation is rejected")
    fun rejectsMismatch() = runTest {
        val store = FakePreferencesStore()
        val vm = StealthSettingsViewModel(store)

        vm.enable("4821", "9999")
        advanceUntilIdle()

        assertFalse(store.enabledValue())
        assertEquals(StealthMessage.PIN_MISMATCH, vm.uiState.value.message)
    }

    @Test
    @DisplayName("disabling requires the correct PIN")
    fun disableRequiresPin() = runTest {
        val store = FakePreferencesStore()
        val vm = StealthSettingsViewModel(store)
        vm.uiState.test {
            awaitItem() // collect so the credential flow is observed
            vm.enable("4821", "4821")
            advanceUntilIdle()

            vm.disable("0000")
            advanceUntilIdle()
            assertTrue(store.enabledValue(), "wrong PIN must not disarm")
            assertEquals(StealthMessage.WRONG_PIN, vm.uiState.value.message)

            vm.disable("4821")
            advanceUntilIdle()
            assertFalse(store.enabledValue())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("changing the PIN requires the current PIN and updates the credential")
    fun changePin() = runTest {
        val store = FakePreferencesStore()
        val vm = StealthSettingsViewModel(store)
        vm.uiState.test {
            awaitItem()
            vm.enable("1111", "1111")
            advanceUntilIdle()

            vm.changePin("9999", "2222", "2222")
            advanceUntilIdle()
            assertTrue(PinHasher.verify("1111", store.credentialValue()), "wrong current PIN keeps the old one")

            vm.changePin("1111", "2222", "2222")
            advanceUntilIdle()
            assertTrue(PinHasher.verify("2222", store.credentialValue()))
            assertEquals(StealthMessage.PIN_CHANGED, vm.uiState.value.message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private class FakePreferencesStore : PreferencesStore {
        private val enabled = MutableStateFlow(false)
        private val credential = MutableStateFlow("")

        fun enabledValue() = enabled.value
        fun credentialValue() = credential.value

        override val stealthEnabled: Flow<Boolean> = enabled
        override val stealthPinCredential: Flow<String> = credential

        override suspend fun setStealthCredential(credential: String) {
            this.credential.value = credential
            enabled.value = true
        }

        override suspend fun clearStealth() {
            credential.value = ""
            enabled.value = false
        }

        override val setupState: Flow<SetupState> = MutableStateFlow(EMPTY_SETUP)
        override val lastReadPosition: Flow<String?> = MutableStateFlow(null)
        override suspend fun saveSetup(state: SetupState) {}
        override suspend fun setSetupCompleted(value: Boolean) {}
        override suspend fun updateDenomination(profile: CanonProfile) {}
        override suspend fun setShowDeuterocanonical(value: Boolean) {}
        override suspend fun setActiveTranslation(translationId: String) {}
        override suspend fun setLastReadPosition(ref: String) {}
    }

    private companion object {
        val EMPTY_SETUP = SetupState(
            denomination = null,
            canonType = null,
            uiLanguage = null,
            bibleLanguage = null,
            numberingScheme = null,
            namingConventionId = null,
            bibleTranslationId = null,
            lectionaryId = null,
        )
    }
}
