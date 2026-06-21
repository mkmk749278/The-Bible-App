package com.manna.bible.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.manna.bible.data.preferences.PreferencesStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Simplified / Elder Mode, exposed app-wide.
 *
 * Manna serves the elderly and low-vision; Simplified Mode enlarges text across the
 * whole app. Rather than re-sizing every `Text` on every screen, [SimplifiedScale] bumps
 * the composition's font scale, so all `sp` text grows together (and `dp` layout is left
 * untouched). The reading screen manages its own enlargement, so it opts out.
 */

/** Multiplier applied to the font scale in Simplified Mode (~140% of the user's size). */
const val SIMPLIFIED_FONT_SCALE = 1.4f

@HiltViewModel
class SimplifiedModeViewModel @Inject constructor(
    preferencesStore: PreferencesStore
) : ViewModel() {
    val simplifiedMode: StateFlow<Boolean> =
        preferencesStore.simplifiedMode
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
}

/** Whether Simplified / Elder Mode is on. */
@Composable
fun rememberSimplifiedMode(): Boolean =
    hiltViewModel<SimplifiedModeViewModel>().simplifiedMode.collectAsStateWithLifecycle().value

/**
 * Wraps [content] so that, when [enabled], all `sp` text is scaled up for Elder Mode.
 * A no-op (no extra provider) when disabled.
 */
@Composable
fun SimplifiedScale(enabled: Boolean, content: @Composable () -> Unit) {
    if (!enabled) {
        content()
        return
    }
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = density.density,
            fontScale = (density.fontScale * SIMPLIFIED_FONT_SCALE).coerceAtMost(2f)
        )
    ) {
        content()
    }
}
