package com.manna.bible.ui.church

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.liturgy.Liturgy
import com.manna.bible.domain.liturgy.LiturgyProvider
import com.manna.bible.domain.model.Denomination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** A selectable order of worship, for the tradition switcher. */
data class LiturgyOption(val id: String, val title: String, val tradition: String)

/**
 * UI state for Church Mode.
 *
 * @property available every order of worship the app can show (the switcher).
 * @property selected the order currently displayed.
 * @property matchedTradition true when [selected] matches the tradition picked at
 *   setup; false when the user's tradition has no order yet and we're offering the
 *   available ones instead.
 */
data class ChurchModeUiState(
    val isLoading: Boolean = true,
    val available: List<LiturgyOption> = emptyList(),
    val selected: Liturgy? = null,
    val matchedTradition: Boolean = true
)

/**
 * Drives Church Mode: shows the order of worship for the tradition the user chose at
 * setup (Catholic → the Mass, CSI / other Protestant → Holy Communion), and lets the
 * user switch to any available order. Fully offline.
 *
 * Uses only `androidx.lifecycle` + coroutines/flow — no Android framework types.
 */
@HiltViewModel
class ChurchModeViewModel @Inject constructor(
    private val liturgyProvider: LiturgyProvider,
    preferencesStore: PreferencesStore
) : ViewModel() {

    private val selectedId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ChurchModeUiState> = combine(
        preferencesStore.setupState,
        selectedId
    ) { setup, chosenId ->
        val denomination = setup.denomination ?: Denomination.PROTESTANT_OTHER
        val default = liturgyProvider.defaultFor(denomination)
        val all = liturgyProvider.all()
        val selected = chosenId?.let { id -> all.firstOrNull { it.id == id } }
            ?: default
            ?: all.firstOrNull()
        ChurchModeUiState(
            isLoading = false,
            available = all.map { LiturgyOption(it.id, it.title.english, it.tradition) },
            selected = selected,
            matchedTradition = default != null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ChurchModeUiState()
    )

    /** Switches the displayed order of worship. */
    fun selectLiturgy(id: String) {
        selectedId.value = id
    }
}
