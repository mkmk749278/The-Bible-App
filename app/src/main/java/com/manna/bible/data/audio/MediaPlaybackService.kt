package com.manna.bible.data.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
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
 * playback — the app-scoped [TtsReader] / [NarratedAudioPlayer] engines do — it observes
 * them and routes the notification's transport controls (play/pause, stop) to whichever
 * engine is active, stopping itself once both are idle.
 *
 * It also holds **audio focus**, so playback pauses for a phone call or another app's audio
 * and resumes when focus returns. Richer lock-screen media controls (a Media3 MediaSession
 * with seek/album art) remain a follow-up.
 */
@AndroidEntryPoint
class MediaPlaybackService : Service() {

    @Inject lateinit var ttsReader: TtsReader
    @Inject lateinit var narratedPlayer: NarratedAudioPlayer

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var started = false

    private val audioManager by lazy { getSystemService(AudioManager::class.java) }
    private var focusRequest: AudioFocusRequest? = null
    /** True when audio focus loss auto-paused us, so focus regain can auto-resume. */
    private var pausedByFocus = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                ttsReader.stop()
                narratedPlayer.stop()
                stopNow()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE -> {
                togglePlayPause()
                return START_NOT_STICKY
            }
        }
        if (!started) {
            started = true
            startForegroundCompat(buildNotification())
            requestAudioFocus()
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

    /** Pauses the playing engine, or resumes the paused one — whichever applies. */
    private fun togglePlayPause() {
        val tts = ttsReader.state.value.status
        val narrated = narratedPlayer.state.value.status
        when {
            tts == TtsStatus.PLAYING -> ttsReader.pause()
            narrated == TtsStatus.PLAYING -> narratedPlayer.pause()
            tts == TtsStatus.PAUSED -> ttsReader.resume()
            narrated == TtsStatus.PAUSED -> narratedPlayer.resume()
        }
    }

    private fun pauseForFocusLoss() {
        val tts = ttsReader.state.value.status
        val narrated = narratedPlayer.state.value.status
        if (tts == TtsStatus.PLAYING) {
            ttsReader.pause(); pausedByFocus = true
        } else if (narrated == TtsStatus.PLAYING) {
            narratedPlayer.pause(); pausedByFocus = true
        }
    }

    private fun resumeAfterFocusGain() {
        if (!pausedByFocus) return
        pausedByFocus = false
        when {
            ttsReader.state.value.status == TtsStatus.PAUSED -> ttsReader.resume()
            narratedPlayer.state.value.status == TtsStatus.PAUSED -> narratedPlayer.resume()
        }
    }

    private fun requestAudioFocus() {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener { change ->
                when (change) {
                    AudioManager.AUDIOFOCUS_LOSS,
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> pauseForFocusLoss()
                    AudioManager.AUDIOFOCUS_GAIN -> resumeAfterFocusGain()
                }
            }
            .build()
        focusRequest = request
        runCatching { audioManager.requestAudioFocus(request) }
    }

    private fun abandonAudioFocus() {
        focusRequest?.let { runCatching { audioManager.abandonAudioFocusRequest(it) } }
        focusRequest = null
    }

    private fun stopNow() {
        abandonAudioFocus()
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
        val playing = ttsReader.state.value.status == TtsStatus.PLAYING ||
            narratedPlayer.state.value.status == TtsStatus.PLAYING
        val toggleLabel =
            if (playing) R.string.media_notification_pause else R.string.media_notification_play
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(applicationInfo.icon)
            .setContentTitle(getString(R.string.media_notification_title))
            .setContentText(getString(R.string.media_notification_text))
            .setOngoing(true)
            .setContentIntent(activityIntent())
            .addAction(action(getString(toggleLabel), ACTION_TOGGLE, requestCode = 2))
            .addAction(action(getString(R.string.media_notification_stop), ACTION_STOP, requestCode = 1))
            .build()
    }

    private fun activityIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun action(label: String, action: String, requestCode: Int): Notification.Action {
        val intent = PendingIntent.getService(
            this,
            requestCode,
            Intent(this, MediaPlaybackService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Action.Builder(null, label, intent).build()
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
        abandonAudioFocus()
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val CHANNEL_ID = "media_playback"
        const val NOTIFICATION_ID = 2003
        const val ACTION_STOP = "com.manna.bible.action.STOP_AUDIO"
        const val ACTION_TOGGLE = "com.manna.bible.action.TOGGLE_AUDIO"
    }
}
