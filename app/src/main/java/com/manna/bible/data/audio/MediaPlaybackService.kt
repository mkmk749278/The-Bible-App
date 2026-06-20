package com.manna.bible.data.audio

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
import com.manna.bible.domain.audio.NarratedAudioPlayer
import com.manna.bible.domain.audio.TtsReader
import com.manna.bible.domain.audio.TtsStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Runs while read-aloud or narrated audio is active so the OS keeps the process alive and
 * playback continues with the app backgrounded or the screen off (Req 9). It does not own
 * playback — the app-scoped [TtsReader] / [NarratedAudioPlayer] engines do — it only holds
 * a `mediaPlayback` foreground notification (with a Stop action) and observes the engines,
 * stopping itself once both are idle.
 *
 * Richer lock-screen transport controls (play/pause/seek via a Media3 MediaSession) are a
 * follow-up; this delivers the core "keep playing in the background" behaviour.
 */
@AndroidEntryPoint
class MediaPlaybackService : Service() {

    @Inject lateinit var ttsReader: TtsReader
    @Inject lateinit var narratedPlayer: NarratedAudioPlayer

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var started = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            ttsReader.stop()
            narratedPlayer.stop()
            stopNow()
            return START_NOT_STICKY
        }
        if (!started) {
            started = true
            startForegroundCompat(buildNotification())
            observe()
        }
        return START_NOT_STICKY
    }

    private fun observe() {
        scope.launch {
            combine(ttsReader.state, narratedPlayer.state) { tts, narrated ->
                tts.status != TtsStatus.IDLE || narrated.status != TtsStatus.IDLE
            }.collect { active ->
                if (active) updateNotification() else stopNow()
            }
        }
    }

    private fun stopNow() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification() {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        ensureChannel()
        val tapIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, MediaPlaybackService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(applicationInfo.icon)
            .setContentTitle(getString(R.string.media_notification_title))
            .setContentText(getString(R.string.media_notification_text))
            .setOngoing(true)
            .setContentIntent(tapIntent)
            .addAction(
                Notification.Action.Builder(
                    null,
                    getString(R.string.media_notification_stop),
                    stopIntent
                ).build()
            )
            .build()
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.media_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.media_channel_description)
            setSound(null, null)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val CHANNEL_ID = "media_playback"
        const val NOTIFICATION_ID = 2003
        const val ACTION_STOP = "com.manna.bible.action.STOP_AUDIO"
    }
}
