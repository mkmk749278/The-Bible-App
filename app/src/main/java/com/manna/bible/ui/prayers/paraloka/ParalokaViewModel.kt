package com.manna.bible.ui.prayers.paraloka

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manna.bible.domain.devotion.ParalokaProvider
import com.manna.bible.domain.usecase.ResolveVerseTextUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A Paraloka passage prepared for display: its id (for theme/reflection prose) and
 * the resolved verse in the active translation.
 */
data class ParalokaPassageUi(
    val id: String,
    val reference: String?,
    val verseText: String?,
    val osisRef: String?
)

/**
 * A Paraloka prayer prepared for display: its id (for title/text prose) and the
 * resolved anchor verse in the active translation.
 */
data class ParalokaPrayerUi(
    val id: String,
    val reference: String?,
    val osisRef: String?
)

/**
 * UI state for the Paraloka collection.
 *
 * @property isLoading True until the passages and prayer anchors resolve.
 * @property passages The eternal-life passages with resolved text.
 * @property prayers The prayers of hope with their resolved anchor reference.
 */
data class ParalokaUiState(
    val isLoading: Boolean = true,
    val passages: List<ParalokaPassageUi> = emptyList(),
    val prayers: List<ParalokaPrayerUi> = emptyList()
)

/**
 * Drives the Paraloka collection: resolves the eternal-life passages and the prayer
 * anchor verses from the active translation (offline). The themes, reflections, and
 * prayer texts are supplied by the screen as string resources keyed on id.
 *
 * Uses only `androidx.lifecycle` + coroutines — no Android framework types.
 */
@HiltViewModel
class ParalokaViewModel @Inject constructor(
    private val paralokaProvider: ParalokaProvider,
    private val resolveVerseText: ResolveVerseTextUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ParalokaUiState())
    val uiState: StateFlow<ParalokaUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val passages = paralokaProvider.passages()
            val prayers = paralokaProvider.prayers()

            val resolvedPassages = resolveVerseText(passages.map { it.scripture })
                .associateBy { it.osisRef }
            val resolvedPrayers = resolveVerseText(prayers.map { it.scripture })
                .associateBy { it.osisRef }

            val passageUi = passages.map { passage ->
                val match = resolvedPassages[passage.scripture.format()]
                ParalokaPassageUi(
                    id = passage.id,
                    reference = match?.reference,
                    verseText = match?.text,
                    osisRef = match?.osisRef ?: passage.scripture.format()
                )
            }
            val prayerUi = prayers.map { prayer ->
                val match = resolvedPrayers[prayer.scripture.format()]
                ParalokaPrayerUi(
                    id = prayer.id,
                    reference = match?.reference,
                    osisRef = match?.osisRef ?: prayer.scripture.format()
                )
            }
            _uiState.value = ParalokaUiState(
                isLoading = false,
                passages = passageUi,
                prayers = prayerUi
            )
        }
    }
}
