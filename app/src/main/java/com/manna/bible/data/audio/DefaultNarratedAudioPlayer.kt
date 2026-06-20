package com.manna.bible.data.audio

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.manna.bible.domain.audio.NarratedAudioPlayer
import com.manna.bible.domain.audio.TtsReader
import com.manna.bible.domain.audio.TtsState
import com.manna.bible.domain.audio.TtsStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [NarratedAudioPlayer] backed by Media3 [ExoPlayer] (Req 9.8). Streams one chapter
 * MP3 at a time and reflects playback into the same [TtsState] the reader's audio bar
 * already understands.
 *
 * Robustness is the priority: an [ExoPlayer] must be touched only from the thread that
 * created it, so the instance is built lazily on the main thread and every operation is
 * marshalled there via [onMain]. The player is released on stop, on the chapter's
 * natural end, and on any error. Construction or playback failure is swallowed and
 * surfaced through [errorEvents] so the reader can fall back to on-device TTS — narrated
 * playback can never leave a listener with silence.
 */
@Singleton
class DefaultNarratedAudioPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) : NarratedAudioPlayer {

    private val mainHandler = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow(TtsState())
    override val state: StateFlow<TtsState> = _state.asStateFlow()

    private val _completionEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val completionEvents: Flow<Unit> = _completionEvents.asSharedFlow()

    private val _errorEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val errorEvents: Flow<Unit> = _errorEvents.asSharedFlow()

    private var player: ExoPlayer? = null
    private var speed = TtsReader.DEFAULT_SPEED

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                _state.value = TtsState(status = TtsStatus.IDLE, speed = speed)
                _completionEvents.tryEmit(Unit)
                releaseInternal()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _state.value = TtsState(status = TtsStatus.IDLE, speed = speed)
            _errorEvents.tryEmit(Unit)
            releaseInternal()
        }
    }

    override fun play(url: String, speed: Float) {
        val clamped = speed.coerceIn(TtsReader.MIN_SPEED, TtsReader.MAX_SPEED)
        this.speed = clamped
        onMain {
            try {
                val p = player ?: ExoPlayer.Builder(context).build().also {
                    it.addListener(listener)
                    player = it
                }
                p.setPlaybackParameters(PlaybackParameters(clamped))
                p.setMediaItem(MediaItem.fromUri(url))
                p.prepare()
                p.playWhenReady = true
                // Buffering reads as "playing" (intended) — terminal states arrive via the listener.
                _state.value = TtsState(status = TtsStatus.PLAYING, speed = clamped)
            } catch (t: Throwable) {
                releaseInternal()
                _state.value = TtsState(status = TtsStatus.IDLE, speed = clamped)
                _errorEvents.tryEmit(Unit)
            }
        }
    }

    override fun pause() = onMain {
        player?.let { if (it.isPlaying) it.playWhenReady = false }
        if (_state.value.status == TtsStatus.PLAYING) {
            _state.value = _state.value.copy(status = TtsStatus.PAUSED)
        }
    }

    override fun resume() = onMain {
        if (_state.value.status == TtsStatus.PAUSED) {
            player?.playWhenReady = true
            _state.value = _state.value.copy(status = TtsStatus.PLAYING)
        }
    }

    override fun stop() = onMain {
        releaseInternal()
        _state.value = TtsState(status = TtsStatus.IDLE, speed = speed)
    }

    override fun setSpeed(speed: Float) {
        val clamped = speed.coerceIn(TtsReader.MIN_SPEED, TtsReader.MAX_SPEED)
        this.speed = clamped
        onMain {
            player?.setPlaybackParameters(PlaybackParameters(clamped))
            _state.value = _state.value.copy(speed = clamped)
        }
    }

    private fun releaseInternal() {
        player?.let {
            it.removeListener(listener)
            it.release()
        }
        player = null
    }

    private inline fun onMain(crossinline block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post { block() }
    }
}
