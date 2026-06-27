package com.manna.bible.ui.church

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.liturgy.Liturgy
import com.manna.bible.domain.liturgy.LiturgyProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** The nav argument carrying the selected liturgy's id into the detail surface. */
const val LITURGY_ID_ARG = "liturgyId"

/**
 * UI state for the Liturgy Detail (expanded order of service) surface.
 *
 * @property liturgy the selected order, or null while loading / if the id is unknown.
 * @property bibleLanguage the live Bible-language tag; liturgy content resolves to it with
 *   an English fallback, and re-resolves when the preference changes (Req 8.5).
 */
data class LiturgyDetailUiState(
    val isLoading: Boolean = true,
    val liturgy: Liturgy? = null,
    val bibleLanguage: String = "en"
)

/**
 * Loads the selected order of worship by id from the [LiturgyProvider] and observes the
 * user's Bible language (Req 6.1, 8.5). Fully offline — the provider is backed by bundled
 * assets and performs no network request (Req 9.2, 9.3).
 *
 * Uses only `androidx.lifecycle` + coroutines/flow — no Android framework types.
 */
@HiltViewModel
class LiturgyDetailViewModel @Inject constructor(
    private val liturgyProvider: LiturgyProvider,
    preferencesStore: PreferencesStore,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val liturgyId: String? = savedStateHandle[LITURGY_ID_ARG]

    val uiState: StateFlow<LiturgyDetailUiState> =
        preferencesStore.setupState.map { setup ->
            val liturgy = liturgyId?.let { id -> liturgyProvider.all().firstOrNull { it.id == id } }
            LiturgyDetailUiState(
                isLoading = false,
                liturgy = liturgy,
                bibleLanguage = setup.bibleLanguage ?: "en"
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LiturgyDetailUiState()
        )
}
