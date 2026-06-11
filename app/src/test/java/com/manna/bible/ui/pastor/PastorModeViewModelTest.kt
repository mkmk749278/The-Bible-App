package com.manna.bible.ui.pastor

import app.cash.turbine.test
import com.manna.bible.domain.pastor.SermonStep
import kotlinx.coroutines.Dispatchers
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

/**
 * Unit tests for [PastorModeViewModel] — the in-memory stepper that drives the
 * Pastor Mode sermon-preparation flow: step navigation, per-step note capture,
 * and reset.
 */
class PastorModeViewModelTest {

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
    @DisplayName("starts on PASSAGE as step 1 of 5 with isFirstStep true")
    fun initialState() = runTest {
        val vm = PastorModeViewModel()
        vm.uiState.test {
            val state = awaitItem()
            assertEquals(SermonStep.PASSAGE, state.currentStep)
            assertEquals(1, state.stepNumber)
            assertEquals(5, state.totalSteps)
            assertTrue(state.isFirstStep)
            assertFalse(state.isLastStep)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("next() advances the step and updates the display index")
    fun nextAdvances() = runTest {
        val vm = PastorModeViewModel()
        vm.uiState.test {
            assertEquals(SermonStep.PASSAGE, awaitItem().currentStep)
            vm.next()
            val state = awaitItem()
            assertEquals(SermonStep.OBSERVE, state.currentStep)
            assertEquals(2, state.stepNumber)
            assertFalse(state.isFirstStep)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("previous() on the first step is a no-op")
    fun previousAtFirstIsNoOp() = runTest {
        val vm = PastorModeViewModel()
        vm.previous()
        vm.uiState.test {
            val state = awaitItem()
            assertEquals(SermonStep.PASSAGE, state.currentStep)
            assertTrue(state.isFirstStep)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("next() past the last step is a no-op; APPLY reports isLastStep")
    fun nextPastLastIsNoOp() = runTest {
        val vm = PastorModeViewModel()
        // Walk to the final step.
        repeat(SermonStep.count - 1) { vm.next() }
        vm.uiState.test {
            val atLast = awaitItem()
            assertEquals(SermonStep.APPLY, atLast.currentStep)
            assertEquals(5, atLast.stepNumber)
            assertTrue(atLast.isLastStep)
            // An extra next() must not move past APPLY (no new emission expected).
            vm.next()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("updateNote stores text and it survives navigation")
    fun noteSurvivesNavigation() = runTest {
        val vm = PastorModeViewModel()
        vm.uiState.test {
            assertEquals(SermonStep.PASSAGE, awaitItem().currentStep)

            vm.updateNote(SermonStep.PASSAGE, "Psalm 23")
            assertEquals("Psalm 23", awaitItem().currentNote)

            // Move forward, then back — the note must still be there.
            vm.next()
            assertEquals(SermonStep.OBSERVE, awaitItem().currentStep)
            vm.previous()
            val back = awaitItem()
            assertEquals(SermonStep.PASSAGE, back.currentStep)
            assertEquals("Psalm 23", back.currentNote)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("reset() clears notes and returns to the first step")
    fun resetClearsState() = runTest {
        val vm = PastorModeViewModel()
        vm.updateNote(SermonStep.PASSAGE, "Psalm 23")
        vm.next()
        vm.updateNote(SermonStep.OBSERVE, "The Lord is my shepherd")

        vm.reset()
        vm.uiState.test {
            val state = awaitItem()
            assertEquals(SermonStep.PASSAGE, state.currentStep)
            assertTrue(state.isFirstStep)
            assertTrue(state.notes.isEmpty())
            assertEquals("", state.currentNote)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
