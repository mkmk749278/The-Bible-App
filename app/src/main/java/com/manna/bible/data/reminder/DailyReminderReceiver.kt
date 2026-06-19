package com.manna.bible.data.reminder

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.manna.bible.MainActivity
import com.manna.bible.R
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.reminder.ReminderScheduler
import com.manna.bible.domain.reminder.ReminderTime
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Receives the daily reminder alarm and posts the "verse of the day" notification,
 * and re-arms the repeating alarm after a device reboot (which clears it).
 *
 * The notification intentionally carries no scripture text — it simply opens the
 * app, where Home shows today's verse. This keeps the receiver free of database
 * work and robust on constrained devices.
 *
 * Dependencies are pulled from Hilt via an [ReminderEntryPoint] accessor rather
 * than `@AndroidEntryPoint`, which keeps `onReceive` free of any base-class
 * coupling and works regardless of the Hilt plugin's transform ordering.
 */
class DailyReminderReceiver : BroadcastReceiver() {

    /** Hilt accessor for the dependencies needed inside [onReceive]. */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReminderEntryPoint {
        fun reminderScheduler(): ReminderScheduler
        fun preferencesStore(): PreferencesStore
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> rescheduleAfterBoot(context)
            ACTION_FIRE -> postReminder(context)
        }
    }

    /** Re-arms the repeating alarm on boot when the reminder is still enabled. */
    private fun rescheduleAfterBoot(context: Context) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            ReminderEntryPoint::class.java
        )
        val preferencesStore = entryPoint.preferencesStore()
        val scheduler = entryPoint.reminderScheduler()
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                if (preferencesStore.dailyReminderEnabled.first()) {
                    ReminderTime.parseList(preferencesStore.dailyReminderTimes.first())
                        .forEach { scheduler.schedule(it) }
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun postReminder(context: Context) {
        // On Android 13+ posting requires the runtime POST_NOTIFICATIONS grant.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = context.getString(R.string.reminder_channel_description) }
        )

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(context.applicationInfo.icon)
            .setContentTitle(context.getString(R.string.reminder_notification_title))
            .setContentText(context.getString(R.string.reminder_notification_text))
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        /** Explicit action used by the alarm's broadcast intent. */
        const val ACTION_FIRE = "com.manna.bible.action.DAILY_REMINDER"

        private const val CHANNEL_ID = "daily_reminder"
        private const val NOTIFICATION_ID = 2001
    }
}
