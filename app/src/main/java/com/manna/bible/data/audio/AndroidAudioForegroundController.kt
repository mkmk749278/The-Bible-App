package com.manna.bible.data.audio

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.manna.bible.domain.audio.AudioForegroundController
import com.manna.bible.domain.audio.NarratedAudioPlayer
import com.manna.bible.domain.audio.TtsReader
import com.manna.bible.domain.audio.TtsStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Watches both audio engines and starts [MediaPlaybackService] whenever playback becomes
 * active, so audio keeps running while the app is backgrounded. The service stops itself
 * once both engines are idle. The start is guarded — audio is begun while the app is in
 * the foreground (the user tapped play), so the foreground-service start is permitted; if
 * it is ever refused, playback continues as before without a notification.
 */
@Singleton
class AndroidAudioForegroundController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ttsReader: TtsReader,
    private val narratedPlayer: NarratedAudioPlayer
) : AudioForegroundController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var started = false

    override fun start() {
        if (started) return
        started = true
        scope.launch {
            combine(ttsReader.state, narratedPlayer.state) { tts, narrated ->
                tts.status != TtsStatus.IDLE || narrated.status != TtsStatus.IDLE
            }
                .distinctUntilChanged()
                .collect { active ->
                    if (active) {
                        runCatching {
                            ContextCompat.startForegroundService(
                                context,
                                Intent(context, MediaPlaybackService::class.java)
                            )
                        }
                    }
                }
        }
    }
}
