package com.manna.bible.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import com.manna.bible.ui.util.SimplifiedScale
import com.manna.bible.ui.util.rememberSimplifiedMode
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.manna.bible.R
import com.manna.bible.domain.FeatureFlags
import com.manna.bible.ui.attribution.AttributionScreen
import com.manna.bible.ui.calendar.LiturgicalCalendarScreen
import com.manna.bible.ui.card.ScriptureCardScreen
import com.manna.bible.ui.catalog.TranslationCatalogScreen
import com.manna.bible.ui.crisis.CrisisModeScreen
import com.manna.bible.ui.daily.DailyVerseScreen
import com.manna.bible.ui.fasting.FastingScreen
import com.manna.bible.ui.grief.GriefScreen
import com.manna.bible.ui.more.MoreScreen
import com.manna.bible.ui.prayer.PrayerJournalScreen
import com.manna.bible.ui.prayers.PrayersHubScreen
import com.manna.bible.ui.prayers.jesus.JesusPrayerScreen
import com.manna.bible.ui.prayers.paraloka.ParalokaScreen
import com.manna.bible.ui.prayers.rosary.RosaryScreen
import com.manna.bible.ui.prayers.sramanikal.SramanikalScreen
import com.manna.bible.ui.prayers.stations.StationsScreen
import com.manna.bible.ui.reader.ReaderScreen
import com.manna.bible.ui.reminder.ReminderSettingsScreen
import com.manna.bible.ui.church.ChurchModeScreen
import com.manna.bible.ui.library.LibraryScreen
import com.manna.bible.ui.search.SearchScreen
import com.manna.bible.ui.sermon.SermonHelperScreen
import com.manna.bible.ui.settings.AppSettingsScreen
import com.manna.bible.ui.settings.DenominationSettingsScreen
import com.manna.bible.ui.setup.SetupHost
import com.manna.bible.ui.stealth.StealthLockScreen
import com.manna.bible.ui.stealth.StealthSettingsScreen

/**
 * Navigation routes. There are three primary tabs (Read · Calendar · More); the
 * reader *is* the Read tab. Everything else (search, daily verse, tools, settings)
 * is pushed above the tabs.
 */
private object Routes {
    const val SETUP = "setup"
    const val READ = "read"
    const val CALENDAR = "calendar"
    const val PRAYERS = "prayers"
    const val MORE = "more"
    const val STATIONS = "stations"
    const val ROSARY = "rosary"
    const val JESUS_PRAYER = "jesus_prayer"
    const val PARALOKA = "paraloka"
    const val SRAMANIKAL = "sramanikal"
    const val SEARCH = "search"
    const val CATALOG = "catalog"
    const val ATTRIBUTION = "attribution"
    const val DAILY = "daily"
    const val REMINDER = "reminder"
    const val CRISIS = "crisis"
    const val GRIEF = "grief"
    const val PRAYER = "prayer"
    const val FASTING = "fasting"
    const val CARD = "card"
    const val SERMON = "sermon"
    const val CHURCH = "church"
    const val LIBRARY = "library"
    const val SETTINGS = "settings"
    const val DENOMINATION = "denomination"
    const val STEALTH = "stealth"
}

/** Calm motion (UX directive): 300ms fades only — no bounce, no flashy transitions. */
private const val MOTION_MS = 300

private data class TabItem(val route: String, val labelRes: Int, val icon: ImageVector)

private val Tabs = listOfNotNull(
    TabItem(Routes.READ, R.string.nav_read, Icons.Filled.Home),
    TabItem(Routes.CALENDAR, R.string.nav_calendar, Icons.Filled.DateRange),
    if (FeatureFlags.PRAYERS_HUB) {
        TabItem(Routes.PRAYERS, R.string.nav_prayers, Icons.Filled.Favorite)
    } else null,
    TabItem(Routes.MORE, R.string.nav_more, Icons.Filled.Menu),
)

private fun NavController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(Routes.READ) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Application navigation root and first-launch gate (Requirement 1 + UX redesign).
 *
 * After setup the app lands on the **Read** tab (the reader, opened at the last
 * reading position) with a calm three-tab bar — **Read · Calendar · More**. Search,
 * the daily verse, and the care/practice tools are pushed above the tabs. The reader
 * carries its own search entry and audio mini-player, so reading, searching, and
 * listening live in one place. Transitions are gentle 300ms fades.
 *
 * A "verse to open" handed back by search / the daily verse / the calendar / the care
 * screens is held in app-scoped saveable state ([pendingRef]/[pendingAutoplay]) and
 * consumed by the long-lived Read tab — so those flows land in the same reader instead
 * of spawning a separate full-screen one.
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

        GateState.Locked -> {
            // Stealth Mode: a disguised lock stands in front of the app until the PIN is entered.
            StealthLockScreen(onUnlock = gateViewModel::unlock)
        }

        GateState.NeedsSetup, GateState.Ready -> {
            val navController = rememberNavController()
            val startDestination =
                if (state == GateState.Ready) Routes.READ else Routes.SETUP

            // A pending "open this verse (and maybe play it)" request for the Read tab.
            var pendingRef by rememberSaveable { mutableStateOf<String?>(null) }
            var pendingAutoplay by rememberSaveable { mutableStateOf(false) }
            val openInReader: (String?, Boolean) -> Unit = { ref, autoplay ->
                if (ref != null) pendingRef = ref
                if (autoplay) pendingAutoplay = true
                navController.navigateToTab(Routes.READ)
            }

            val currentRoute = navController.currentBackStackEntryAsState()
                .value?.destination?.route
            val showBottomBar = Tabs.any { it.route == currentRoute }

            // Elder Mode: enlarge text across every screen. The reader manages its own
            // enlargement, so it reverts to this base density below.
            val simplified = rememberSimplifiedMode()
            val baseDensity = LocalDensity.current

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
              SimplifiedScale(simplified) {
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
                                navController.navigate(Routes.READ) {
                                    popUpTo(Routes.SETUP) { inclusive = true }
                                }
                            },
                        )
                    }

                    // --- Read tab (the reader) -----------------------------------
                    composable(Routes.READ) {
                        // The reader sizes its own text (Elder Mode enlarges it there),
                        // so revert to the base density rather than double-scaling.
                        CompositionLocalProvider(LocalDensity provides baseDensity) {
                            ReaderScreen(
                                onSwitchTranslation = { navController.navigate(Routes.CATALOG) },
                                onOpenAttribution = { navController.navigate(Routes.ATTRIBUTION) },
                                onOpenSearch = { navController.navigate(Routes.SEARCH) },
                                onOpenCrisis = if (FeatureFlags.CRISIS_MODE) {
                                    { navController.navigate(Routes.CRISIS) }
                                } else null,
                                pendingScrollRef = pendingRef,
                                onScrollRefConsumed = { pendingRef = null },
                                autoPlayAudio = pendingAutoplay,
                                onAutoPlayConsumed = { pendingAutoplay = false }
                            )
                        }
                    }

                    // --- Calendar tab --------------------------------------------
                    composable(Routes.CALENDAR) {
                        LiturgicalCalendarScreen(
                            onOpenVerse = { ref -> openInReader(ref, false) }
                        )
                    }

                    // --- Prayers tab (devotional practices hub) ------------------
                    if (FeatureFlags.PRAYERS_HUB) {
                        composable(Routes.PRAYERS) {
                            PrayersHubScreen(
                                onOpenStations = if (FeatureFlags.STATIONS_OF_THE_CROSS) {
                                    { navController.navigate(Routes.STATIONS) }
                                } else null,
                                onOpenRosary = if (FeatureFlags.ROSARY) {
                                    { navController.navigate(Routes.ROSARY) }
                                } else null,
                                onOpenJesusPrayer = if (FeatureFlags.JESUS_PRAYER) {
                                    { navController.navigate(Routes.JESUS_PRAYER) }
                                } else null,
                                onOpenParaloka = if (FeatureFlags.PARALOKA) {
                                    { navController.navigate(Routes.PARALOKA) }
                                } else null,
                                onOpenSramanikal = if (FeatureFlags.SRAMANIKAL) {
                                    { navController.navigate(Routes.SRAMANIKAL) }
                                } else null
                            )
                        }
                    }

                    // --- More tab ------------------------------------------------
                    composable(Routes.MORE) {
                        MoreScreen(
                            onOpenLibrary = { navController.navigate(Routes.LIBRARY) },
                            onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                            onOpenTranslations = { navController.navigate(Routes.CATALOG) },
                            onOpenCalendar = null, // Calendar is a primary tab now.
                            onOpenReminder = if (FeatureFlags.DAILY_REMINDER) {
                                { navController.navigate(Routes.REMINDER) }
                            } else null,
                            onOpenPrayer = if (FeatureFlags.PRAYER_JOURNAL) {
                                { navController.navigate(Routes.PRAYER) }
                            } else null,
                            onOpenCrisis = if (FeatureFlags.CRISIS_MODE) {
                                { navController.navigate(Routes.CRISIS) }
                            } else null,
                            onOpenGrief = if (FeatureFlags.GRIEF_COMPANION) {
                                { navController.navigate(Routes.GRIEF) }
                            } else null,
                            onOpenFasting = if (FeatureFlags.FASTING_COMPANION) {
                                { navController.navigate(Routes.FASTING) }
                            } else null,
                            onOpenCard = if (FeatureFlags.SCRIPTURE_CARD) {
                                { navController.navigate(Routes.CARD) }
                            } else null,
                            onOpenSermon = if (FeatureFlags.SERMON_HELPER) {
                                { navController.navigate(Routes.SERMON) }
                            } else null,
                            onOpenChurch = if (FeatureFlags.CHURCH_MODE) {
                                { navController.navigate(Routes.CHURCH) }
                            } else null,
                            onOpenAttribution = { navController.navigate(Routes.ATTRIBUTION) }
                        )
                    }

                    // --- Pushed destinations -------------------------------------
                    composable(Routes.SEARCH) {
                        SearchScreen(
                            onBack = { navController.popBackStack() },
                            onResultSelected = { ref -> openInReader(ref, false) }
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
                            onOpenVerse = { ref -> openInReader(ref, false) }
                        )
                    }
                    composable(Routes.REMINDER) {
                        ReminderSettingsScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Routes.CRISIS) {
                        CrisisModeScreen(
                            onBack = { navController.popBackStack() },
                            onListen = { ref -> openInReader(ref, true) },
                            onOpenVerse = { ref -> openInReader(ref, false) }
                        )
                    }
                    composable(Routes.GRIEF) {
                        GriefScreen(
                            onBack = { navController.popBackStack() },
                            onOpenVerse = { ref -> openInReader(ref, false) }
                        )
                    }
                    composable(Routes.PRAYER) {
                        PrayerJournalScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // --- Prayers-hub practices -----------------------------------
                    composable(Routes.STATIONS) {
                        StationsScreen(
                            onBack = { navController.popBackStack() },
                            onOpenVerse = { ref -> openInReader(ref, false) }
                        )
                    }
                    composable(Routes.ROSARY) {
                        RosaryScreen(
                            onBack = { navController.popBackStack() },
                            onOpenVerse = { ref -> openInReader(ref, false) }
                        )
                    }
                    composable(Routes.JESUS_PRAYER) {
                        JesusPrayerScreen(
                            onBack = { navController.popBackStack() },
                            onOpenVerse = { ref -> openInReader(ref, false) }
                        )
                    }
                    composable(Routes.PARALOKA) {
                        ParalokaScreen(
                            onBack = { navController.popBackStack() },
                            onOpenVerse = { ref -> openInReader(ref, false) }
                        )
                    }
                    composable(Routes.SRAMANIKAL) {
                        SramanikalScreen(
                            onBack = { navController.popBackStack() },
                            onOpenVerse = { ref -> openInReader(ref, false) }
                        )
                    }
                    composable(Routes.FASTING) {
                        FastingScreen(
                            onBack = { navController.popBackStack() },
                            onOpenVerse = { ref -> openInReader(ref, false) }
                        )
                    }
                    composable(Routes.CARD) {
                        ScriptureCardScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    if (FeatureFlags.SERMON_HELPER) {
                        composable(Routes.SERMON) {
                            SermonHelperScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                    if (FeatureFlags.CHURCH_MODE) {
                        composable(Routes.CHURCH) {
                            ChurchModeScreen(
                                onBack = { navController.popBackStack() },
                                onOpenVerse = { ref -> openInReader(ref, false) }
                            )
                        }
                    }

                    // --- Library (saved highlights / bookmarks / notes) ----------
                    composable(Routes.LIBRARY) {
                        LibraryScreen(
                            onBack = { navController.popBackStack() },
                            onOpenVerse = { ref -> openInReader(ref, false) }
                        )
                    }

                    // --- Settings ------------------------------------------------
                    composable(Routes.SETTINGS) {
                        AppSettingsScreen(
                            onBack = { navController.popBackStack() },
                            onOpenDenomination = { navController.navigate(Routes.DENOMINATION) },
                            onOpenStealth = if (FeatureFlags.STEALTH_MODE) {
                                { navController.navigate(Routes.STEALTH) }
                            } else null
                        )
                    }
                    composable(Routes.DENOMINATION) {
                        DenominationSettingsScreen(
                            onReRunSetup = {
                                navController.navigate(Routes.SETUP) {
                                    popUpTo(Routes.READ) { inclusive = false }
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    if (FeatureFlags.STEALTH_MODE) {
                        composable(Routes.STEALTH) {
                            StealthSettingsScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
              }
            }
        }
    }
}
