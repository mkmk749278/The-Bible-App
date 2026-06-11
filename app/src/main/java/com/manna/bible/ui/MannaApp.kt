package com.manna.bible.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.manna.bible.R
import com.manna.bible.domain.FeatureFlags
import com.manna.bible.ui.attribution.AttributionScreen
import com.manna.bible.ui.calendar.JesusCalendarScreen
import com.manna.bible.ui.catalog.TranslationCatalogScreen
import com.manna.bible.ui.daily.DailyVerseScreen
import com.manna.bible.ui.home.HomeScreen
import com.manna.bible.ui.listen.ListenScreen
import com.manna.bible.ui.pastor.PastorModeScreen
import com.manna.bible.ui.reader.ReaderScreen
import com.manna.bible.ui.reminder.ReminderSettingsScreen
import com.manna.bible.ui.search.SearchScreen
import com.manna.bible.ui.setup.SetupHost

/** Navigation routes. Tabs are top-level; the reader opens full-screen above them. */
private object Routes {
    const val SETUP = "setup"
    const val HOME = "home"
    const val LISTEN = "listen"
    const val SEARCH = "search"
    const val LIBRARY = "library"
    const val READER = "reader?ref={ref}&autoplay={autoplay}"
    const val CATALOG = "catalog"
    const val ATTRIBUTION = "attribution"
    const val DAILY = "daily"
    const val CALENDAR = "calendar"
    const val PASTOR = "pastor"
    const val REMINDER = "reminder"
}

/** Builds a concrete reader route, optionally opening at [ref] and auto-playing audio. */
private fun readerRoute(ref: String? = null, autoplay: Boolean = false): String =
    "reader?ref=${ref.orEmpty()}&autoplay=$autoplay"

/** SavedStateHandle key for handing a selected reference to the reader. */
private const val SCROLL_REF_KEY = "scrollToRef"

/** Calm motion (UX directive): 300ms fades only — no bounce, no flashy transitions. */
private const val MOTION_MS = 300

private data class TabItem(val route: String, val labelRes: Int, val icon: ImageVector)

private val Tabs = listOf(
    TabItem(Routes.HOME, R.string.nav_home, Icons.Filled.Home),
    TabItem(Routes.LISTEN, R.string.nav_listen, Icons.Filled.PlayArrow),
    TabItem(Routes.SEARCH, R.string.nav_search, Icons.Filled.Search),
    TabItem(Routes.LIBRARY, R.string.nav_library, Icons.Filled.List),
)

private fun NavController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Application navigation root and first-launch gate (Requirement 1 + UX directive).
 *
 * After setup, the app lands on the Home experience (Continue Reading · Today's
 * Verse · Continue Listening) with a calm bottom bar of four primary destinations:
 * Home, Listen, Search, Library. The reader opens full-screen above the bar so the
 * scripture text dominates; catalog, attribution, and daily-verse surfaces are
 * pushed the same way. Transitions are gentle 300ms fades.
 *
 * While [GateState.Loading], a centered progress indicator is shown; when
 * [GateState.NeedsSetup] the [NavHost] starts at the setup flow (Req 1.1, 1.4),
 * otherwise at Home (Req 1.2). Setup completion pops itself so back cannot
 * re-enter it.
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
                if (state == GateState.Ready) Routes.HOME else Routes.SETUP

            val currentRoute = navController.currentBackStackEntryAsState()
                .value?.destination?.route
            val showBottomBar = Tabs.any { it.route == currentRoute }

            Scaffold(
                bottomBar = {
                    if (showBottomBar) {
                        NavigationBar {
                            Tabs.forEach { tab ->
                                NavigationBarItem(
                                    selected = currentRoute == tab.route,
                                    onClick = { navController.navigateToTab(tab.route) },
                                    icon = { Icon(tab.icon, contentDescription = null) },
                                    label = { Text(stringResource(tab.labelRes)) }
                                )
                            }
                        }
                    }
                }
            ) { padding ->
                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier.padding(padding),
                    enterTransition = { fadeIn(tween(MOTION_MS)) },
                    exitTransition = { fadeOut(tween(MOTION_MS)) },
                    popEnterTransition = { fadeIn(tween(MOTION_MS)) },
                    popExitTransition = { fadeOut(tween(MOTION_MS)) },
                ) {
                    composable(Routes.SETUP) {
                        SetupHost(
                            onSetupComplete = {
                                navController.navigate(Routes.HOME) {
                                    popUpTo(Routes.SETUP) { inclusive = true }
                                }
                            },
                        )
                    }
                    composable(Routes.HOME) {
                        HomeScreen(
                            onContinueReading = { navController.navigate(readerRoute()) },
                            onOpenVerse = { ref -> navController.navigate(readerRoute(ref)) },
                            onContinueListening = {
                                navController.navigate(readerRoute(autoplay = true))
                            }
                        )
                    }
                    composable(Routes.LISTEN) {
                        ListenScreen(
                            onContinueListening = {
                                navController.navigate(readerRoute(autoplay = true))
                            }
                        )
                    }
                    composable(Routes.SEARCH) {
                        SearchScreen(
                            onBack = { navController.popBackStack() },
                            onResultSelected = { ref ->
                                navController.navigate(readerRoute(ref)) {
                                    popUpTo(Routes.HOME)
                                }
                            }
                        )
                    }
                    composable(Routes.LIBRARY) {
                        TranslationCatalogScreen(
                            onBack = null,
                            onOpenReminder = if (FeatureFlags.DAILY_REMINDER) {
                                { navController.navigate(Routes.REMINDER) }
                            } else null,
                            onOpenCalendar = if (FeatureFlags.JESUS_CALENDAR) {
                                { navController.navigate(Routes.CALENDAR) }
                            } else null,
                            onOpenPastorMode = if (FeatureFlags.PASTOR_MODE) {
                                { navController.navigate(Routes.PASTOR) }
                            } else null,
                            onOpenAttribution = { navController.navigate(Routes.ATTRIBUTION) }
                        )
                    }
                    composable(
                        route = Routes.READER,
                        arguments = listOf(
                            navArgument("ref") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                            navArgument("autoplay") {
                                type = NavType.BoolType
                                defaultValue = false
                            }
                        )
                    ) { backStackEntry ->
                        val handle = backStackEntry.savedStateHandle
                        val initialRef = backStackEntry.arguments
                            ?.getString("ref")?.takeIf { it.isNotBlank() }
                        val autoplay = backStackEntry.arguments
                            ?.getBoolean("autoplay") ?: false
                        val scrollRef by handle
                            .getStateFlow(SCROLL_REF_KEY, initialRef)
                            .collectAsStateWithLifecycle()
                        ReaderScreen(
                            onSwitchTranslation = { navController.navigate(Routes.CATALOG) },
                            onOpenAttribution = { navController.navigate(Routes.ATTRIBUTION) },
                            onOpenSearch = { navController.navigateToTab(Routes.SEARCH) },
                            onOpenDaily = { navController.navigate(Routes.DAILY) },
                            pendingScrollRef = scrollRef,
                            onScrollRefConsumed = { handle[SCROLL_REF_KEY] = null },
                            autoPlayAudio = autoplay
                        )
                    }
                    composable(Routes.CATALOG) {
                        TranslationCatalogScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Routes.ATTRIBUTION) {
                        AttributionScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Routes.DAILY) {
                        DailyVerseScreen(
                            onBack = { navController.popBackStack() },
                            onOpenVerse = { ref ->
                                navController.navigate(readerRoute(ref)) {
                                    popUpTo(Routes.HOME)
                                }
                            }
                        )
                    }
                    composable(Routes.CALENDAR) {
                        JesusCalendarScreen(
                            onBack = { navController.popBackStack() },
                            onOpenVerse = { ref ->
                                navController.navigate(readerRoute(ref)) {
                                    popUpTo(Routes.HOME)
                                }
                            }
                        )
                    }
                    composable(Routes.PASTOR) {
                        PastorModeScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Routes.REMINDER) {
                        ReminderSettingsScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
