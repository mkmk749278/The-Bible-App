package com.manna.bible.data.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.manna.bible.domain.reminder.NextReminderTime
import com.manna.bible.domain.reminder.ReminderScheduler
import com.manna.bible.domain.reminder.ReminderTime
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [ReminderScheduler] backed by [AlarmManager].
 *
 * Uses `setInexactRepeating` with a daily interval: the OS re-fires automatically
 * each day, so the receiver only has to post a notification (no per-day
 * re-scheduling). Inexact alarms need no special permission — appropriate for a
 * gentle daily nudge, and battery-friendly on the low-end devices Manna targets.
 * Reboots clear repeating alarms, so [DailyReminderReceiver] re-arms on boot.
 */
@Singleton
class AlarmReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : ReminderScheduler {

    private val alarmManager: AlarmManager =
        context.getSystemService(AlarmManager::class.java)

    override fun schedule(time: ReminderTime) {
        val triggerAt = NextReminderTime.nextTriggerMillis(
            time = time,
            nowMillis = System.currentTimeMillis(),
            zone = ZoneId.systemDefault()
        )
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            AlarmManager.INTERVAL_DAY,
            pendingIntent()
        )
    }

    override fun cancel() {
        alarmManager.cancel(pendingIntent())
    }

    private fun pendingIntent(): PendingIntent {
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            action = DailyReminderReceiver.ACTION_FIRE
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private companion object {
        const val REQUEST_CODE = 1001
    }
}
