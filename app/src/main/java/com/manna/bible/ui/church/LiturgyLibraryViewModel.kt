package com.manna.bible.ui.church

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.liturgy.LiturgyProvider
import com.manna.bible.domain.model.Denomination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * One browsable entry in the Liturgy Library list. [title] is the liturgy's title resolved
 * in the user's Bible language (English fallback); [tradition] is the order's tradition
 * label.
 */
data class LiturgyListItem(
    val id: String,
    val title: String,
    val tradition: String
)

/**
 * UI state for the Liturgy Library (browse) surface.
 *
 * @property entries every available order of worship, with the ones mapped to the user's
 *   tradition surfaced first (Req 5.2, 5.3, 11.1); always the full library so the user can
 *   browse every order (Req 11.6).
 * @property denominationHasMapping true when the user's tradition has an explicitly mapped,
 *   available order; false drives the calm "an order for your tradition is being prepared"
 *   note shown above the still-complete list (Req 5.4).
 */
data class LiturgyLibraryUiState(
    val isLoading: Boolean = true,
    val entries: List<LiturgyListItem> = emptyList(),
    val denominationHasMapping: Boolean = true
)

/**
 * Drives the Liturgy Library: combines the user's chosen tradition (from
 * [PreferencesStore.setupState]) with [LiturgyProvider.forDenomination] to produce a
 * denomination-ordered list of every bundled order (Req 5.1–5.3). Entirely offline —
 * populated from bundled assets with no network request (Req 5.5, 9.2, 9.3).
 *
 * Uses only `androidx.lifecycle` + coroutines/flow — no Android framework types.
 */
@HiltViewModel
class LiturgyLibraryViewModel @Inject constructor(
    private val liturgyProvider: LiturgyProvider,
    preferencesStore: PreferencesStore
) : ViewModel() {

    val uiState: StateFlow<LiturgyLibraryUiState> =
        preferencesStore.setupState.map { setup ->
            val denomination = setup.denomination ?: Denomination.PROTESTANT_OTHER
            val bibleLanguage = setup.bibleLanguage ?: "en"
            val ordered = liturgyProvider.forDenomination(denomination)
            LiturgyLibraryUiState(
                isLoading = false,
                entries = ordered.map { liturgy ->
                    LiturgyListItem(
                        id = liturgy.id,
                        title = liturgy.title.resolve(bibleLanguage),
                        tradition = liturgy.tradition
                    )
                },
                // SHOW_EVERYTHING browses the whole library without a single "tradition";
                // every other denomination drives the note off its explicit mapped default.
                denominationHasMapping = denomination != Denomination.SHOW_EVERYTHING &&
                    liturgyProvider.defaultFor(denomination) != null
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LiturgyLibraryUiState()
        )
}
