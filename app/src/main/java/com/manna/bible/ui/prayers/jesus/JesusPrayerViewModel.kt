package com.manna.bible.ui.prayers.jesus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manna.bible.domain.devotion.JesusPrayerProvider
import com.manna.bible.domain.devotion.PrayerDepth
import com.manna.bible.domain.usecase.ResolveVerseTextUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * One depth of the Jesus Prayer prepared for display: the depth itself plus its
 * resolved supporting passage in the active translation.
 */
data class PrayerDepthUi(
    val depth: PrayerDepth,
    val reference: String?,
    val verseText: String?,
    val osisRef: String?
)

/**
 * UI state for the Jesus Prayer.
 *
 * @property isLoading True until the supporting passages resolve.
 * @property stages The three depths (vocal, mental, heart) with resolved scripture.
 */
data class JesusPrayerUiState(
    val isLoading: Boolean = true,
    val stages: List<PrayerDepthUi> = emptyList()
)

/**
 * Drives the Jesus Prayer screen: resolves the supporting passage for each of the
 * three depths from the active translation (offline). The prayer text and the
 * guidance for each depth are supplied by the screen as string resources; the
 * breathing cadence is purely a presentation animation.
 *
 * Uses only `androidx.lifecycle` + coroutines — no Android framework types.
 */
@HiltViewModel
class JesusPrayerViewModel @Inject constructor(
    private val jesusPrayerProvider: JesusPrayerProvider,
    private val resolveVerseText: ResolveVerseTextUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(JesusPrayerUiState())
    val uiState: StateFlow<JesusPrayerUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val stages = jesusPrayerProvider.stages()
            val resolved = resolveVerseText(stages.map { it.scripture })
            val byRef = resolved.associateBy { it.osisRef }
            val ui = stages.map { stage ->
                val match = byRef[stage.scripture.format()]
                PrayerDepthUi(
                    depth = stage.depth,
                    reference = match?.reference,
                    verseText = match?.text,
                    osisRef = match?.osisRef ?: stage.scripture.format()
                )
            }
            _uiState.value = JesusPrayerUiState(isLoading = false, stages = ui)
        }
    }
}
