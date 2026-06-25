package com.manna.bible.ui.card

import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.model.CanonProfile
import com.manna.bible.domain.model.CanonType
import com.manna.bible.domain.model.Denomination
import com.manna.bible.domain.model.NumberingScheme
import com.manna.bible.domain.model.SetupState
import com.manna.bible.domain.share.VerseRecommendation
import com.manna.bible.domain.share.VerseRecommendationEngine
import com.manna.bible.domain.share.VerseRecommendationRequest
import kotlinx.coroutines.CompletableDeferred
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit tests for [VerseRecommendationViewModel] — gating, loading→result transitions,
 * clear, and the offline surface. Uses hand-rolled fakes (no Mockito).
 */
class VerseRecommendationViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeEach fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterEach fun tearDown() { Dispatchers.resetMain() }

    private fun vm(engine: FakeEngine) =
        VerseRecommendationViewModel(engine, FakePreferencesStore())

    private val success = VerseRecommendation.Success(
        osisRef = "EPH.5.25",
        reference = "Ephesians 5:25",
        verseText = "Husbands, love your wives...",
        personalMessage = "Wishing you a marriage full of grace."
    )

    @Test
    @DisplayName("engineConfigured reflects the engine when configured")
    fun configuredFlag() = runTest {
        val vm = vm(FakeEngine(configured = true))
        advanceUntilIdle()
        assertTrue(vm.uiState.value.engineConfigured)
    }

    @Test
    @DisplayName("recommend emits loading then a Success result")
    fun loadingThenResult() = runTest {
        val gate = CompletableDeferred<Unit>()
        val engine = FakeEngine(configured = true, result = success, gate = gate)
        val vm = vm(engine)
        advanceUntilIdle()

        vm.onSituationChange("wedding today")
        vm.recommend()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isLoading)

        gate.complete(Unit)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(success, state.result)
    }

    @Test
    @DisplayName("recommend is a no-op when the engine is not configured")
    fun noOpWhenUnconfigured() = runTest {
        val engine = FakeEngine(configured = false, result = success)
        val vm = vm(engine)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.engineConfigured)

        vm.onSituationChange("wedding today")
        vm.recommend()
        advanceUntilIdle()

        assertEquals(0, engine.callCount)
        assertNull(vm.uiState.value.result)
    }

    @Test
    @DisplayName("recommend is a no-op when the situation text is blank")
    fun noOpWhenBlank() = runTest {
        val engine = FakeEngine(configured = true, result = success)
        val vm = vm(engine)
        advanceUntilIdle()

        vm.onSituationChange("   ")
        vm.recommend()
        advanceUntilIdle()

        assertEquals(0, engine.callCount)
    }

    @Test
    @DisplayName("clear resets the situation text and the result")
    fun clearResets() = runTest {
        val engine = FakeEngine(configured = true, result = success)
        val vm = vm(engine)
        advanceUntilIdle()

        vm.onSituationChange("wedding today")
        vm.recommend()
        advanceUntilIdle()
        assertEquals(success, vm.uiState.value.result)

        vm.clear()
        assertEquals("", vm.uiState.value.situationText)
        assertNull(vm.uiState.value.result)
    }

    @Test
    @DisplayName("an offline result is surfaced as the Offline state")
    fun offlineSurfaced() = runTest {
        val engine = FakeEngine(configured = true, result = VerseRecommendation.Offline)
        val vm = vm(engine)
        advanceUntilIdle()

        vm.onSituationChange("failed exam")
        vm.recommend()
        advanceUntilIdle()

        assertEquals(VerseRecommendation.Offline, vm.uiState.value.result)
    }

    private class FakeEngine(
        private val configured: Boolean = true,
        private val result: VerseRecommendation = VerseRecommendation.Offline,
        private val gate: CompletableDeferred<Unit>? = null
    ) : VerseRecommendationEngine {
        var callCount = 0
            private set

        override val isConfigured: Boolean get() = configured

        override suspend fun recommend(request: VerseRecommendationRequest): VerseRecommendation {
            callCount++
            gate?.await()
            return result
        }
    }

    private class FakePreferencesStore : PreferencesStore {
        private val state = MutableStateFlow(
            SetupState(
                denomination = Denomination.PROTESTANT_OTHER,
                canonType = CanonType.PROTESTANT_66,
                uiLanguage = "en", bibleLanguage = "en",
                numberingScheme = NumberingScheme.MASORETIC,
                namingConventionId = null, bibleTranslationId = "web",
                lectionaryId = null, showDeuterocanonical = false, setupCompleted = true
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
