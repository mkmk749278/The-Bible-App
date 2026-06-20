package com.manna.bible.data.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.manna.bible.MainActivity
import com.manna.bible.R
import com.manna.bible.domain.download.DownloadManager
import com.manna.bible.domain.download.DownloadProgress
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Keeps an in-flight Bible download alive while the app is backgrounded by running as a
 * `dataSync` foreground service with a progress notification (Req 5.2). The download work
 * itself runs on the app-lifetime download scope inside [DownloadManager]; this service
 * only observes [DownloadManager.progress] to render the notification and stops itself
 * once the download finishes (or if none is active).
 */
@AndroidEntryPoint
class DownloadForegroundService : Service() {

    @Inject lateinit var downloadManager: DownloadManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var started = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!started) {
            started = true
            startForegroundCompat(buildNotification(null))
            observeProgress()
        }
        return START_NOT_STICKY
    }

    private fun observeProgress() {
        scope.launch {
            var sawActive = false
            // Don't linger as a foreground service if nothing actually starts downloading.
            val watchdog = launch {
                delay(IDLE_TIMEOUT_MS)
                if (!sawActive) stopNow()
            }
            downloadManager.progress().collect { progress ->
                when {
                    progress == null -> if (sawActive) stopNow()
                    progress.done -> {
                        sawActive = true
                        stopNow()
                    }
                    else -> {
                        sawActive = true
                        watchdog.cancel()
                        updateNotification(progress)
                    }
                }
            }
        }
    }

    private fun stopNow() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(progress: DownloadProgress) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(progress))
    }

    private fun buildNotification(progress: DownloadProgress?): Notification {
        ensureChannel()
        val tapIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(applicationInfo.icon)
            .setContentTitle(getString(R.string.download_notification_title))
            .setOngoing(true)
            .setContentIntent(tapIntent)
        if (progress != null && progress.totalChapters > 0 && !progress.done) {
            builder
                .setContentText(
                    getString(
                        R.string.download_notification_progress,
                        progress.completedChapters,
                        progress.totalChapters
                    )
                )
                .setProgress(progress.totalChapters, progress.completedChapters, false)
        } else {
            builder
                .setContentText(getString(R.string.download_notification_preparing))
                .setProgress(0, 0, true)
        }
        return builder.build()
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.download_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.download_channel_description)
            setSound(null, null)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val CHANNEL_ID = "downloads"
        const val NOTIFICATION_ID = 2002
        const val IDLE_TIMEOUT_MS = 15_000L
    }
}
