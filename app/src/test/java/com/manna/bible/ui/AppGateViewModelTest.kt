package com.manna.bible.ui

import app.cash.turbine.test
import com.manna.bible.data.preferences.PreferencesStore
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** Unit tests for [AppGateViewModel] — first-launch gate plus the Stealth-Mode lock. */
class AppGateViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeEach fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterEach fun tearDown() { Dispatchers.resetMain() }

    @Test
    @DisplayName("incomplete setup resolves to NeedsSetup")
    fun needsSetup() = runTest {
        val vm = AppGateViewModel(FakePreferencesStore(setupCompleted = false))
        vm.gateState.test {
            assertEquals(GateState.Loading, awaitItem())
            assertEquals(GateState.NeedsSetup, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("complete setup with no stealth resolves to Ready")
    fun ready() = runTest {
        val vm = AppGateViewModel(FakePreferencesStore(setupCompleted = true))
        vm.gateState.test {
            assertEquals(GateState.Loading, awaitItem())
            assertEquals(GateState.Ready, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("armed stealth mode locks the app until unlocked for the session")
    fun lockedUntilUnlocked() = runTest {
        val store = FakePreferencesStore(
            setupCompleted = true,
            stealthEnabled = true,
            stealthCredential = "salt:hash",
        )
        val vm = AppGateViewModel(store)
        vm.gateState.test {
            assertEquals(GateState.Loading, awaitItem())
            assertEquals(GateState.Locked, awaitItem())

            vm.unlock()
            advanceUntilIdle()
            assertEquals(GateState.Ready, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("stealth enabled but without a credential does not lock")
    fun noCredentialNoLock() = runTest {
        val store = FakePreferencesStore(
            setupCompleted = true,
            stealthEnabled = true,
            stealthCredential = "",
        )
        val vm = AppGateViewModel(store)
        vm.gateState.test {
            assertEquals(GateState.Loading, awaitItem())
            assertEquals(GateState.Ready, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    private class FakePreferencesStore(
        setupCompleted: Boolean,
        stealthEnabled: Boolean = false,
        stealthCredential: String = "",
    ) : PreferencesStore {
        override val setupState: Flow<SetupState> = MutableStateFlow(
            SetupState(
                denomination = null,
                canonType = null,
                uiLanguage = null,
                bibleLanguage = null,
                numberingScheme = null,
                namingConventionId = null,
                bibleTranslationId = null,
                lectionaryId = null,
                setupCompleted = setupCompleted,
            ),
        )
        override val stealthEnabled: Flow<Boolean> = MutableStateFlow(stealthEnabled)
        override val stealthPinCredential: Flow<String> = MutableStateFlow(stealthCredential)

        override val lastReadPosition: Flow<String?> = MutableStateFlow(null)
        override suspend fun saveSetup(state: SetupState) {}
        override suspend fun setSetupCompleted(value: Boolean) {}
        override suspend fun updateDenomination(profile: CanonProfile) {}
        override suspend fun setShowDeuterocanonical(value: Boolean) {}
        override suspend fun setActiveTranslation(translationId: String) {}
        override suspend fun setLastReadPosition(ref: String) {}
    }
}
