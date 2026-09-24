package com.manna.bible

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.ui.AppGateViewModel
import com.manna.bible.ui.MannaApp
import com.manna.bible.ui.theme.MannaTheme
import com.manna.bible.ui.theme.ThemeViewModel
import com.manna.bible.ui.util.ReadingTextScale
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Same activity-scoped instance [MannaApp] obtains via `hiltViewModel()`. */
    private val gateViewModel: AppGateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        keepScriptureOffScreenWhileStealthArmed()
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
                        MannaApp(gateViewModel = gateViewModel)
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Stealth Mode: leaving the app (home, recents, screen off) re-arms the disguised
        // lock, so reopening shows the calculator again. Rotation is not "leaving".
        if (!isChangingConfigurations) gateViewModel.lock()
    }

    /**
     * While Stealth Mode is armed, mark the window secure: the Recents thumbnail shows a
     * blank card instead of the last scripture page, and screenshots/screen-casts are
     * blocked. Cleared again as soon as Stealth Mode is turned off.
     */
    private fun keepScriptureOffScreenWhileStealthArmed() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                gateViewModel.stealthArmed.collect { armed ->
                    if (armed) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
            }
        }
    }
}
