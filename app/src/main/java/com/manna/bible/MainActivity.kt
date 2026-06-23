package com.manna.bible

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.ui.MannaApp
import com.manna.bible.ui.theme.MannaTheme
import com.manna.bible.ui.theme.ThemeViewModel
import com.manna.bible.ui.util.ReadingTextScale
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            // Honor the user's explicit theme choice; fall back to the OS setting.
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val themeMode by themeViewModel.darkMode.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                PreferencesStore.THEME_DARK -> true
                PreferencesStore.THEME_LIGHT -> false
                else -> isSystemInDarkTheme()
            }
            MannaTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ReadingTextScale {
                        MannaApp()
                    }
                }
            }
        }
    }
}
