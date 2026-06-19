package com.manna.bible.ui.church

import app.cash.turbine.test
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.liturgy.DefaultLiturgyProvider
import com.manna.bible.domain.model.CanonProfile
import com.manna.bible.domain.model.Denomination
import com.manna.bible.domain.model.SetupState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
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

/** Unit tests for [ChurchModeViewModel] — denomination-driven order + switching. */
class ChurchModeViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(denomination: Denomination) =
        ChurchModeViewModel(DefaultLiturgyProvider(), FakePreferencesStore(denomination))

    @Test
    @DisplayName("Catholic sees the Mass; both orders are available")
    fun catholic() = runTest {
        val vm = viewModel(Denomination.CATHOLIC)
        vm.uiState.test {
            // Skip the initial loading emission.
            awaitItem()
            val state = awaitItem()
            assertEquals("roman_catholic_mass", state.selected?.id)
            assertTrue(state.matchedTradition)
            assertEquals(2, state.available.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("a tradition without an order yet is offered the available ones")
    fun unmatchedTradition() = runTest {
        val vm = viewModel(Denomination.ORTHODOX)
        vm.uiState.test {
            awaitItem()
            val state = awaitItem()
            assertFalse(state.matchedTradition)
            // Still shows something to follow (the first available order).
            assertEquals("roman_catholic_mass", state.selected?.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("selecting a different tradition switches the order")
    fun switch() = runTest {
        val vm = viewModel(Denomination.CATHOLIC)
        vm.uiState.test {
            awaitItem()
            assertEquals("roman_catholic_mass", awaitItem().selected?.id)
            vm.selectLiturgy("csi_holy_communion")
            assertEquals("csi_holy_communion", awaitItem().selected?.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private class FakePreferencesStore(denomination: Denomination) : PreferencesStore {
        private val state = MutableStateFlow(
            SetupState(
                denomination = denomination,
                canonType = null,
                uiLanguage = "en",
                bibleLanguage = "en",
                numberingScheme = null,
                namingConventionId = null,
                bibleTranslationId = null,
                lectionaryId = null
            )
        )
        override val setupState: Flow<SetupState> = state
        override val lastReadPosition: Flow<String?> = MutableStateFlow(null)
        override suspend fun saveSetup(state: SetupState) {}
        override suspend fun setSetupCompleted(value: Boolean) {}
        override suspend fun updateDenomination(profile: CanonProfile) {}
        override suspend fun setShowDeuterocanonical(value: Boolean) {}
        override suspend fun setActiveTranslation(translationId: String) {}
        override suspend fun setLastReadPosition(ref: String) {}
    }
}
