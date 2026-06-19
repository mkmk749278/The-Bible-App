package com.manna.bible.ui.more

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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit tests for [MoreViewModel] — the Simplified / Elder Mode toggle and its
 * continuous-play coupling.
 */
class MoreViewModelTest {

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
    @DisplayName("enabling Simplified Mode persists it and turns on continuous play")
    fun enableTurnsOnContinuousPlay() = runTest {
        val store = FakePreferencesStore()
        val vm = MoreViewModel(store)

        vm.uiState.test {
            assertFalse(awaitItem().simplifiedMode)

            vm.setSimplifiedMode(true)
            advanceUntilIdle()
            assertTrue(awaitItem().simplifiedMode)
            assertTrue(store.continuousPlayValue(), "enabling Simplified Mode should enable continuous play")

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("disabling Simplified Mode leaves continuous play untouched")
    fun disableLeavesContinuousPlay() = runTest {
        val store = FakePreferencesStore()
        val vm = MoreViewModel(store)

        vm.setSimplifiedMode(true)
        advanceUntilIdle()
        assertTrue(store.continuousPlayValue())

        vm.setSimplifiedMode(false)
        advanceUntilIdle()

        vm.uiState.test {
            assertFalse(awaitItem().simplifiedMode)
            // Continuous play was not switched off by disabling Simplified Mode.
            assertTrue(store.continuousPlayValue())
            cancelAndIgnoreRemainingEvents()
        }
    }

    private class FakePreferencesStore : PreferencesStore {
        private val simplified = MutableStateFlow(false)
        private val continuous = MutableStateFlow(false)

        fun continuousPlayValue() = continuous.value

        override val simplifiedMode: Flow<Boolean> = simplified
        override val continuousPlay: Flow<Boolean> = continuous

        override suspend fun setSimplifiedMode(value: Boolean) { simplified.value = value }
        override suspend fun setContinuousPlay(value: Boolean) { continuous.value = value }

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
        override suspend fun saveSetup(state: SetupState) {}
        override suspend fun setSetupCompleted(value: Boolean) {}
        override suspend fun updateDenomination(profile: CanonProfile) {}
        override suspend fun setShowDeuterocanonical(value: Boolean) {}
        override suspend fun setActiveTranslation(translationId: String) {}
        override suspend fun setLastReadPosition(ref: String) {}
    }
}
