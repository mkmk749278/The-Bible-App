package com.manna.bible.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.repository.TranslationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Re-renders the home-screen [DailyVerseWidget] whenever what it shows could change: the
 * active translation, which translations have content (e.g. the bundled Bibles finishing
 * their first-launch install), and Stealth Mode being armed or disarmed. Without this the
 * widget only refreshed on the system's 6-hourly cycle — so it could keep showing a verse
 * for hours after Stealth Mode was switched on.
 *
 * Started once from the Application. `updateAll` is a cheap no-op when no widget is placed.
 */
@Singleton
class WidgetRefresher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesStore: PreferencesStore,
    private val translationRepository: TranslationRepository
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Begins observing; safe to call once at startup. */
    fun start() {
        scope.launch {
            combine(
                preferencesStore.setupState.map { it.bibleTranslationId },
                preferencesStore.stealthEnabled,
                preferencesStore.stealthPinCredential.map { it.isNotBlank() },
                translationRepository.catalog().map { catalog ->
                    catalog.filter { it.isDownloaded }.map { it.id }.toSet()
                }
            ) { translationId, stealthOn, hasPin, downloaded ->
                WidgetInputs(translationId, stealthOn && hasPin, downloaded)
            }
                .distinctUntilChanged()
                .collect { refresh() }
        }
    }

    private suspend fun refresh() {
        try {
            DailyVerseWidget().updateAll(context)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // A widget refresh failure must never affect the app; the system's periodic
            // update will try again.
        }
    }

    private data class WidgetInputs(
        val translationId: String?,
        val stealthArmed: Boolean,
        val downloaded: Set<String>
    )
}
