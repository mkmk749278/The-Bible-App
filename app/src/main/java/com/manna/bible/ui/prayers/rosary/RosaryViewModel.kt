package com.manna.bible.ui.prayers.rosary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manna.bible.domain.devotion.MysterySet
import com.manna.bible.domain.devotion.RosaryProvider
import com.manna.bible.domain.usecase.ResolveVerseTextUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Beads in one decade of the Rosary (ten Hail Marys). */
const val DECADE_BEADS = 10

/**
 * One mystery prepared for display: its id (for prose), its number, and the resolved
 * Gospel passage in the active translation.
 */
data class MysteryUi(
    val id: String,
    val number: Int,
    val reference: String?,
    val verseText: String?,
    val osisRef: String?
)

/**
 * UI state for the Rosary.
 *
 * @property isLoading True until the chosen set's mysteries and scripture resolve.
 * @property set The mystery set being prayed.
 * @property todaysSet The set traditionally prayed today (for the "Today" hint).
 * @property mysteries The five mysteries of [set] with resolved scripture.
 * @property currentIndex Zero-based index of the mystery being shown.
 * @property beadCount Hail Marys counted in the current decade (0..[DECADE_BEADS]).
 */
data class RosaryUiState(
    val isLoading: Boolean = true,
    val set: MysterySet = MysterySet.JOYFUL,
    val todaysSet: MysterySet = MysterySet.JOYFUL,
    val mysteries: List<MysteryUi> = emptyList(),
    val currentIndex: Int = 0,
    val beadCount: Int = 0
) {
    val current: MysteryUi? get() = mysteries.getOrNull(currentIndex)
    val total: Int get() = mysteries.size
    val canGoPrevious: Boolean get() = currentIndex > 0
    val canGoNext: Boolean get() = currentIndex < mysteries.size - 1
    val decadeComplete: Boolean get() = beadCount >= DECADE_BEADS
}

/**
 * Drives the Rosary: chooses a mystery set (defaulting to the one traditionally prayed
 * today), resolves each mystery's Gospel passage from the active translation (offline),
 * and tracks the bead count for the current decade. The prayers and mystery titles are
 * supplied by the screen from string resources keyed on mystery id and set.
 *
 * Moving between mysteries resets the bead count, so each decade starts fresh.
 *
 * Uses only `androidx.lifecycle` + coroutines — no Android framework types.
 */
@HiltViewModel
class RosaryViewModel @Inject constructor(
    private val rosaryProvider: RosaryProvider,
    private val resolveVerseText: ResolveVerseTextUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RosaryUiState())
    val uiState: StateFlow<RosaryUiState> = _uiState.asStateFlow()

    init {
        val today = rosaryProvider.setForDay(LocalDate.now().dayOfWeek.value)
        _uiState.value = _uiState.value.copy(todaysSet = today)
        selectSet(today)
    }

    /** Loads [set]'s mysteries, resolving their scripture, and resets to the first decade. */
    fun selectSet(set: MysterySet) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, set = set)
            val mysteries = rosaryProvider.mysteries(set)
            val resolved = resolveVerseText(mysteries.map { it.scripture })
            val byRef = resolved.associateBy { it.osisRef }
            val ui = mysteries.map { mystery ->
                val match = byRef[mystery.scripture.format()]
                MysteryUi(
                    id = mystery.id,
                    number = mystery.number,
                    reference = match?.reference,
                    verseText = match?.text,
                    osisRef = match?.osisRef ?: mystery.scripture.format()
                )
            }
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                mysteries = ui,
                currentIndex = 0,
                beadCount = 0
            )
        }
    }

    /** Moves to the next mystery, starting its decade fresh. */
    fun next() {
        val state = _uiState.value
        if (state.canGoNext) {
            _uiState.value = state.copy(currentIndex = state.currentIndex + 1, beadCount = 0)
        }
    }

    /** Moves to the previous mystery, starting its decade fresh. */
    fun previous() {
        val state = _uiState.value
        if (state.canGoPrevious) {
            _uiState.value = state.copy(currentIndex = state.currentIndex - 1, beadCount = 0)
        }
    }

    /** Counts one Hail Mary, up to the ten beads of the decade. */
    fun tapBead() {
        val state = _uiState.value
        if (state.beadCount < DECADE_BEADS) {
            _uiState.value = state.copy(beadCount = state.beadCount + 1)
        }
    }

    /** Clears the current decade's bead count. */
    fun resetBeads() {
        _uiState.value = _uiState.value.copy(beadCount = 0)
    }
}
