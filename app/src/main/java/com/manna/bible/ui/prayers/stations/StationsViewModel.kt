package com.manna.bible.ui.prayers.stations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manna.bible.domain.devotion.StationsProvider
import com.manna.bible.domain.usecase.ResolveVerseTextUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * One station prepared for display: its structural id and number (for mapping prose
 * from string resources) plus the resolved scripture in the active translation.
 *
 * @property id Stable id, e.g. "station_01" — the key for title/reflection/prayer strings.
 * @property number Place in the sequence (1..14).
 * @property reference Display reference for the scripture, or null when unavailable.
 * @property verseText Scripture text in the active translation, or null when unavailable.
 * @property osisRef Canonical reference for "read in context", or null when unavailable.
 */
data class StationUi(
    val id: String,
    val number: Int,
    val reference: String?,
    val verseText: String?,
    val osisRef: String?
)

/**
 * UI state for the Stations of the Cross.
 *
 * @property isLoading True until the stations and their scripture resolve.
 * @property stations The fourteen stations with resolved scripture.
 * @property currentIndex Zero-based index of the station being shown.
 */
data class StationsUiState(
    val isLoading: Boolean = true,
    val stations: List<StationUi> = emptyList(),
    val currentIndex: Int = 0
) {
    val current: StationUi? get() = stations.getOrNull(currentIndex)
    val total: Int get() = stations.size
    val canGoPrevious: Boolean get() = currentIndex > 0
    val canGoNext: Boolean get() = currentIndex < stations.size - 1
}

/**
 * Drives the Stations of the Cross: loads the fourteen stations and resolves each
 * station's Gospel passage from the active translation (offline). The devotional
 * prose — the versicle, each reflection and prayer — is supplied by the screen as
 * string resources keyed on [StationUi.id], so the domain stays language-free.
 *
 * Uses only `androidx.lifecycle` + coroutines — no Android framework types.
 */
@HiltViewModel
class StationsViewModel @Inject constructor(
    private val stationsProvider: StationsProvider,
    private val resolveVerseText: ResolveVerseTextUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StationsUiState())
    val uiState: StateFlow<StationsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val stations = stationsProvider.stations()
            val resolved = resolveVerseText(stations.map { it.scripture })
            val byRef = resolved.associateBy { it.osisRef }
            val ui = stations.map { station ->
                val match = byRef[station.scripture.format()]
                StationUi(
                    id = station.id,
                    number = station.number,
                    reference = match?.reference,
                    verseText = match?.text,
                    osisRef = match?.osisRef ?: station.scripture.format()
                )
            }
            _uiState.value = StationsUiState(isLoading = false, stations = ui, currentIndex = 0)
        }
    }

    /** Moves to the next station, if any. */
    fun next() {
        val state = _uiState.value
        if (state.canGoNext) {
            _uiState.value = state.copy(currentIndex = state.currentIndex + 1)
        }
    }

    /** Moves to the previous station, if any. */
    fun previous() {
        val state = _uiState.value
        if (state.canGoPrevious) {
            _uiState.value = state.copy(currentIndex = state.currentIndex - 1)
        }
    }

    /** Jumps directly to the station at [index], if in range. */
    fun goTo(index: Int) {
        val state = _uiState.value
        if (index in state.stations.indices) {
            _uiState.value = state.copy(currentIndex = index)
        }
    }
}
