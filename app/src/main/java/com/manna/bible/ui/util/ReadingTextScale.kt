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
 * Global reading text size, exposed app-wide.
 *
 * The user's chosen text scale (Settings → Reading text size) multiplies the
 * composition's font scale so all `sp` text grows together — across every screen,
 * the reader included. Applied at the app shell, *above* where the reader captures
 * its base density, so the reader keeps the global size while still opting out of the
 * additional Elder-Mode enlargement layered on by [SimplifiedScale].
 */
@HiltViewModel
class ReadingTextScaleViewModel @Inject constructor(
    preferencesStore: PreferencesStore,
) : ViewModel() {
    val textScale: StateFlow<Float> =
        preferencesStore.textScale
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 1.0f)
}

/**
 * Wraps [content] so that all `sp` text is scaled by the user's global reading-size
 * preference. A no-op when the preference is the default 1.0.
 */
@Composable
fun ReadingTextScale(content: @Composable () -> Unit) {
    val scale by hiltViewModel<ReadingTextScaleViewModel>()
        .textScale.collectAsStateWithLifecycle()
    if (scale == 1.0f) {
        content()
        return
    }
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = density.density,
            fontScale = (density.fontScale * scale).coerceIn(0.8f, 2.0f),
        ),
    ) {
        content()
    }
}
