package com.manna.bible.ui.pastor

import androidx.lifecycle.ViewModel
import com.manna.bible.domain.pastor.SermonStep
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * UI state for Pastor Mode's guided sermon-preparation stepper.
 *
 * @property currentStep The step currently being edited.
 * @property notes The pastor's working notes, keyed by [SermonStep]. Steps with
 *   no entry are treated as empty.
 * @property stepNumber One-based position of [currentStep] for display (1..5).
 * @property totalSteps Total number of steps in the flow.
 * @property isFirstStep True when [currentStep] is the first step (Previous disabled).
 * @property isLastStep True when [currentStep] is the last step (Next becomes "Done").
 */
data class PastorModeUiState(
    val currentStep: SermonStep = SermonStep.first,
    val notes: Map<SermonStep, String> = emptyMap(),
    val stepNumber: Int = SermonStep.first.displayNumber,
    val totalSteps: Int = SermonStep.count,
    val isFirstStep: Boolean = true,
    val isLastStep: Boolean = false
) {
    /** The note text for [currentStep], or an empty string when none has been typed. */
    val currentNote: String get() = notes[currentStep].orEmpty()
}

/**
 * Drives the in-memory state of the Pastor Mode stepper: which step is active and
 * the notes captured for each step. The flow is intentionally ephemeral — there is
 * no persistence, so closing the screen discards the draft.
 *
 * Uses only `androidx.lifecycle` + coroutines/flow — no Android framework types.
 */
@HiltViewModel
class PastorModeViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(stateFor(SermonStep.first, emptyMap()))
    val uiState: StateFlow<PastorModeUiState> = _uiState.asStateFlow()

    /**
     * Records [text] as the note for [step], replacing any previous content for it.
     * Does not change the active step.
     */
    fun updateNote(step: SermonStep, text: String) {
        val updatedNotes = _uiState.value.notes + (step to text)
        _uiState.value = stateFor(_uiState.value.currentStep, updatedNotes)
    }

    /** Advances to the next step. No-op when already on the last step. */
    fun next() {
        val current = _uiState.value.currentStep
        if (current == SermonStep.last) return
        goTo(SermonStep.ordered[current.index + 1])
    }

    /** Moves back to the previous step. No-op when already on the first step. */
    fun previous() {
        val current = _uiState.value.currentStep
        if (current == SermonStep.first) return
        goTo(SermonStep.ordered[current.index - 1])
    }

    /** Jumps directly to [step], preserving all notes. */
    fun goTo(step: SermonStep) {
        _uiState.value = stateFor(step, _uiState.value.notes)
    }

    /** Clears all notes and returns to the first step, starting a fresh sermon. */
    fun reset() {
        _uiState.value = stateFor(SermonStep.first, emptyMap())
    }

    /** Builds a fully-derived [PastorModeUiState] from the raw step + notes. */
    private fun stateFor(step: SermonStep, notes: Map<SermonStep, String>) =
        PastorModeUiState(
            currentStep = step,
            notes = notes,
            stepNumber = step.displayNumber,
            totalSteps = SermonStep.count,
            isFirstStep = step == SermonStep.first,
            isLastStep = step == SermonStep.last
        )
}
