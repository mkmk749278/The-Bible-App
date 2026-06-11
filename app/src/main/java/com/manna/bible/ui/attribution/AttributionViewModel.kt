package com.manna.bible.ui.attribution

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.attribution.Attribution
import com.manna.bible.domain.attribution.AttributionProvider
import com.manna.bible.domain.repository.TranslationRepository
import com.manna.bible.domain.translation.Translation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the attribution/about surface (Requirement 12).
 *
 * @property attribution Active-translation attribution; license is null when no
 *   translation is active.
 * @property isLoading True until the first catalog/preferences snapshot resolves.
 */
data class AttributionUiState(
    val attribution: Attribution = Attribution(translationName = null, license = null),
    val isLoading: Boolean = true
)

/**
 * Resolves the active translation from the persisted selection (falling back to the
 * first downloaded/bundled edition, mirroring the reader) and exposes its
 * [Attribution] (Req 12.1, 12.2, 12.4). The Free Use Bible API (MIT) acknowledgement
 * is rendered unconditionally by the screen (Req 12.3).
 *
 * Uses only `androidx.lifecycle` + coroutines/flow — no Android framework types.
 */
@HiltViewModel
class AttributionViewModel @Inject constructor(
    private val preferencesStore: PreferencesStore,
    private val translationRepository: TranslationRepository,
    private val attributionProvider: AttributionProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(AttributionUiState())
    val uiState: StateFlow<AttributionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                preferencesStore.setupState,
                translationRepository.catalog()
            ) { setup, catalog ->
                val active = resolveActiveTranslation(setup.bibleTranslationId, catalog)
                attributionProvider.attributionFor(active)
            }.collect { attribution ->
                _uiState.value = AttributionUiState(attribution = attribution, isLoading = false)
            }
        }
    }

    /** Picks the persisted translation, else the first downloaded/bundled one available. */
    private fun resolveActiveTranslation(
        persistedId: String?,
        catalog: List<Translation>
    ): Translation? {
        if (catalog.isEmpty()) return null
        if (!persistedId.isNullOrBlank()) {
            catalog.firstOrNull { it.id == persistedId }?.let { return it }
        }
        return catalog.firstOrNull { it.isDownloaded } ?: catalog.firstOrNull()
    }
}
