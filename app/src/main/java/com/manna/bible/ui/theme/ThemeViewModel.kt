package com.manna.bible.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manna.bible.data.preferences.PreferencesStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Exposes the persisted theme preference ("system" | "light" | "dark") so the app
 * shell can honor an explicit light/dark choice instead of only following the OS.
 */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    preferencesStore: PreferencesStore,
) : ViewModel() {
    val darkMode: StateFlow<String> =
        preferencesStore.darkMode
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                PreferencesStore.THEME_SYSTEM,
            )
}
