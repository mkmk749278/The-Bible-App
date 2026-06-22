package com.manna.bible.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.audio.TtsReader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The three theme choices offered in Appearance settings. */
enum class ThemeChoice(val storedValue: String) {
    SYSTEM(PreferencesStore.THEME_SYSTEM),
    LIGHT(PreferencesStore.THEME_LIGHT),
    DARK(PreferencesStore.THEME_DARK);

    companion object {
        fun fromStored(value: String): ThemeChoice =
            entries.firstOrNull { it.storedValue == value } ?: SYSTEM
    }
}

/**
 * Immutable UI state for the main app Settings screen (Appearance, Reading text size,
 * Audio speed, Accessibility).
 */
data class AppSettingsUiState(
    val theme: ThemeChoice = ThemeChoice.SYSTEM,
    val textScale: Float = 1.0f,
    val ttsSpeed: Float = TtsReader.DEFAULT_SPEED,
    val simplifiedMode: Boolean = false,
)

/**
 * Backs the consolidated Settings screen — the single home for appearance (light /
 * dark / system theme), global reading text size, read-aloud speed, and the
 * Simplified (Elder) Mode toggle. Each change is persisted immediately to
 * [PreferencesStore] so it survives restarts and takes effect app-wide.
 */
@HiltViewModel
class AppSettingsViewModel @Inject constructor(
    private val preferencesStore: PreferencesStore,
) : ViewModel() {

    val uiState: StateFlow<AppSettingsUiState> =
        combine(
            preferencesStore.darkMode,
            preferencesStore.textScale,
            preferencesStore.ttsSpeed,
            preferencesStore.simplifiedMode,
        ) { theme, scale, speed, simplified ->
            AppSettingsUiState(
                theme = ThemeChoice.fromStored(theme),
                textScale = scale,
                ttsSpeed = speed,
                simplifiedMode = simplified,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppSettingsUiState(),
        )

    fun setTheme(choice: ThemeChoice) {
        viewModelScope.launch { preferencesStore.setDarkMode(choice.storedValue) }
    }

    fun setTextScale(value: Float) {
        viewModelScope.launch { preferencesStore.setTextScale(value) }
    }

    fun setTtsSpeed(value: Float) {
        viewModelScope.launch { preferencesStore.setTtsSpeed(value) }
    }

    fun setSimplifiedMode(value: Boolean) {
        viewModelScope.launch {
            preferencesStore.setSimplifiedMode(value)
            // Mirror the More-screen coupling: Elder Mode reads continuously.
            if (value) preferencesStore.setContinuousPlay(true)
        }
    }
}
