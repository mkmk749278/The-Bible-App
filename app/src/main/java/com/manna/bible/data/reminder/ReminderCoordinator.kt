package com.manna.bible.data.reminder

import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.reminder.ReminderScheduler
import com.manna.bible.domain.reminder.ReminderTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the OS alarm in sync with the persisted reminder preferences.
 *
 * Started once from the Application, it observes the enabled flag and time and
 * (re)schedules or cancels the [ReminderScheduler] accordingly — so toggling the
 * reminder or changing its time in settings takes effect immediately, and the
 * alarm is re-armed on every cold start (covering app updates and force-stops).
 */
@Singleton
class ReminderCoordinator @Inject constructor(
    private val scheduler: ReminderScheduler,
    private val preferencesStore: PreferencesStore
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Begins observing reminder preferences. Safe to call once at startup. */
    fun start() {
        scope.launch {
            combine(
                preferencesStore.dailyReminderEnabled,
                preferencesStore.dailyReminderTimes
            ) { enabled, csv -> if (enabled) ReminderTime.parseList(csv) else emptyList() }
                .distinctUntilChanged()
                .collect { desired -> sync(desired) }
        }
    }

    /**
     * Brings the OS alarms in line with [desired]: cancels any time that was armed
     * before but is no longer wanted (read from the persisted "scheduled" list so it
     * works across restarts), (re)schedules every desired time (idempotent), then
     * records the new armed set. [scheduledReminderTimes] is read one-shot — not part
     * of the observed combine — so persisting it here doesn't re-trigger the loop.
     */
    private suspend fun sync(desired: List<ReminderTime>) {
        val previouslyArmed = ReminderTime.parseList(preferencesStore.scheduledReminderTimes.first())
        val wanted = desired.toSet()
        previouslyArmed.filterNot { it in wanted }.forEach { scheduler.cancel(it) }
        desired.forEach { scheduler.schedule(it) }
        preferencesStore.setScheduledReminderTimes(ReminderTime.formatList(desired))
    }
}
