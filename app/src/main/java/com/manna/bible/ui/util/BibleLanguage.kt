package com.manna.bible.ui.util

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.manna.bible.data.preferences.PreferencesStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Exposes the user's Bible language so any screen can render sacred text in it via
 * [stringResourceIn], without each feature ViewModel having to plumb the preference
 * through itself.
 */
@HiltViewModel
class BibleLanguageViewModel @Inject constructor(
    preferencesStore: PreferencesStore
) : ViewModel() {
    val bibleLanguage: StateFlow<String> =
        preferencesStore.setupState
            .map { it.bibleLanguage ?: "en" }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "en")
}

/** The user's Bible-language tag (BCP-47), for rendering recited prayer/scripture text. */
@Composable
fun rememberBibleLanguage(): String =
    hiltViewModel<BibleLanguageViewModel>().bibleLanguage.collectAsStateWithLifecycle().value
