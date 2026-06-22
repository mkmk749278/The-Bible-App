package com.manna.bible.ui.settings

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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** Unit tests for [AppSettingsViewModel] — appearance, text size, audio speed, Elder Mode. */
class AppSettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeEach fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterEach fun tearDown() { Dispatchers.resetMain() }

    @Test
    @DisplayName("changing the theme persists the stored value")
    fun setTheme() = runTest {
        val store = FakePreferencesStore()
        val vm = AppSettingsViewModel(store)

        vm.setTheme(ThemeChoice.DARK)
        advanceUntilIdle()
        assertEquals(PreferencesStore.THEME_DARK, store.darkModeValue())

        vm.uiState.test {
            assertEquals(ThemeChoice.DARK, awaitItem().theme)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("text scale and TTS speed are persisted")
    fun setScaleAndSpeed() = runTest {
        val store = FakePreferencesStore()
        val vm = AppSettingsViewModel(store)

        vm.setTextScale(1.5f)
        vm.setTtsSpeed(1.25f)
        advanceUntilIdle()

        assertEquals(1.5f, store.textScaleValue())
        assertEquals(1.25f, store.ttsSpeedValue())
    }

    @Test
    @DisplayName("enabling Simplified Mode also turns on continuous play")
    fun simplifiedEnablesContinuous() = runTest {
        val store = FakePreferencesStore()
        val vm = AppSettingsViewModel(store)

        vm.setSimplifiedMode(true)
        advanceUntilIdle()

        assertTrue(store.simplifiedValue())
        assertTrue(store.continuousValue())
    }

    @Test
    @DisplayName("an unknown stored theme falls back to System")
    fun unknownThemeFallsBack() {
        assertEquals(ThemeChoice.SYSTEM, ThemeChoice.fromStored("nonsense"))
        assertEquals(ThemeChoice.LIGHT, ThemeChoice.fromStored("light"))
    }

    private class FakePreferencesStore : PreferencesStore {
        private val theme = MutableStateFlow(PreferencesStore.THEME_SYSTEM)
        private val scale = MutableStateFlow(1.0f)
        private val speed = MutableStateFlow(1.0f)
        private val simplified = MutableStateFlow(false)
        private val continuous = MutableStateFlow(false)

        fun darkModeValue() = theme.value
        fun textScaleValue() = scale.value
        fun ttsSpeedValue() = speed.value
        fun simplifiedValue() = simplified.value
        fun continuousValue() = continuous.value

        override val darkMode: Flow<String> = theme
        override val textScale: Flow<Float> = scale
        override val ttsSpeed: Flow<Float> = speed
        override val simplifiedMode: Flow<Boolean> = simplified
        override val continuousPlay: Flow<Boolean> = continuous

        override suspend fun setDarkMode(value: String) { theme.value = value }
        override suspend fun setTextScale(value: Float) { scale.value = value }
        override suspend fun setTtsSpeed(value: Float) { speed.value = value }
        override suspend fun setSimplifiedMode(value: Boolean) { simplified.value = value }
        override suspend fun setContinuousPlay(value: Boolean) { continuous.value = value }

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
