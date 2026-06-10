package com.manna.bible.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.manna.bible.ui.catalog.TranslationCatalogScreen
import com.manna.bible.ui.reader.ReaderScreen
import com.manna.bible.ui.setup.SetupHost

/** Navigation routes for the app-level first-launch gate. */
private object Routes {
    const val SETUP = "setup"
    const val MAIN = "main"
    const val CATALOG = "catalog"
}

/**
 * Application navigation root and first-launch gate (Requirement 1).
 *
 * Observes the persisted setup state via [AppGateViewModel]:
 * - While [GateState.Loading], shows a centered progress indicator until DataStore emits.
 * - When [GateState.NeedsSetup] (Req 1.1, 1.4), the [NavHost] starts at the setup flow.
 * - When [GateState.Ready] (Req 1.2), the [NavHost] starts at the main reader.
 *
 * On setup completion the flow navigates to [Routes.MAIN], popping [Routes.SETUP] so the user
 * cannot navigate back into setup. Because the gate observes [PreferencesStore.setupState], the
 * `setupCompleted` flag persisted by the flow keeps the gate consistent across relaunches.
 */
@Composable
fun MannaApp(
    gateViewModel: AppGateViewModel = hiltViewModel(),
) {
    val gateState by gateViewModel.gateState.collectAsStateWithLifecycle()

    when (val state = gateState) {
        GateState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        GateState.NeedsSetup, GateState.Ready -> {
            val navController = rememberNavController()
            val startDestination =
                if (state == GateState.Ready) Routes.MAIN else Routes.SETUP

            NavHost(
                navController = navController,
                startDestination = startDestination,
            ) {
                composable(Routes.SETUP) {
                    SetupHost(
                        onSetupComplete = {
                            navController.navigate(Routes.MAIN) {
                                popUpTo(Routes.SETUP) { inclusive = true }
                            }
                        },
                    )
                }
                composable(Routes.MAIN) {
                    ReaderScreen(
                        onSwitchTranslation = { navController.navigate(Routes.CATALOG) }
                    )
                }
                composable(Routes.CATALOG) {
                    TranslationCatalogScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
