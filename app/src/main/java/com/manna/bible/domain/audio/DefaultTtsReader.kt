package com.manna.bible.domain.audio

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pure-Kotlin [TtsReader] that drives a [SpeechEngine] one verse at a time
 * (Requirement 9). It speaks the first verse on [play], then advances on each
 * [SpeechListener.onDone] until the chapter ends, at which point it emits a
 * completion event for continuous-play handling (Req 9.2, 9.7).
 *
 * Utterance ids encode the queue index (`manna-verse-<index>`) so stale callbacks
 * — e.g. a late `onDone` arriving after [pause]/[stop] — are ignored. All state
 * mutation is guarded by a single lock because engine callbacks may arrive on a
 * synthesizer thread while control methods run on the main thread.
 */
@Singleton
class DefaultTtsReader @Inject constructor(
    private val engine: SpeechEngine
) : TtsReader, SpeechListener {

    private val lock = Any()

    private val _state = MutableStateFlow(TtsState())
    override val state: StateFlow<TtsState> = _state.asStateFlow()

    private val _completionEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val completionEvents: Flow<Unit> = _completionEvents.asSharedFlow()

    private var queue: List<TtsVerse> = emptyList()
    private var index = -1
    private var speed = TtsReader.DEFAULT_SPEED

    init {
        engine.setListener(this)
    }

    override fun play(verses: List<TtsVerse>, languageTag: String?) {
        synchronized(lock) {
            if (verses.isEmpty()) return
            val voiceOk = languageTag.isNullOrBlank() || engine.selectLanguage(languageTag)
            engine.setSpeed(speed)
            queue = verses
            index = 0
            _state.value = TtsState(
                status = TtsStatus.PLAYING,
                currentVerse = verses[0].verse,
                speed = speed,
                voiceUnavailable = !voiceOk
            )
            engine.speak(utteranceId(0), verses[0].text, flush = true)
        }
    }

    override fun pause() {
        synchronized(lock) {
            if (_state.value.status != TtsStatus.PLAYING) return
            engine.stop()
            _state.value = _state.value.copy(status = TtsStatus.PAUSED)
        }
    }

    override fun resume() {
        synchronized(lock) {
            if (_state.value.status != TtsStatus.PAUSED) return
            val verse = queue.getOrNull(index) ?: return
            _state.value = _state.value.copy(status = TtsStatus.PLAYING)
            engine.speak(utteranceId(index), verse.text, flush = true)
        }
    }

    override fun stop() {
        synchronized(lock) {
            engine.stop()
            queue = emptyList()
            index = -1
            _state.value = TtsState(status = TtsStatus.IDLE, currentVerse = null, speed = speed)
        }
    }

    override fun setSpeed(speed: Float) {
        synchronized(lock) {
            val clamped = speed.coerceIn(TtsReader.MIN_SPEED, TtsReader.MAX_SPEED)
            this.speed = clamped
            engine.setSpeed(clamped)
            _state.value = _state.value.copy(speed = clamped)
        }
    }

    override fun shutdown() {
        synchronized(lock) {
            engine.setListener(null)
            engine.shutdown()
        }
    }

    // --- SpeechListener (engine thread) -------------------------------------

    override fun onDone(utteranceId: String) = advance(utteranceId)

    /** A failed utterance is skipped so one bad verse can't halt playback. */
    override fun onError(utteranceId: String) = advance(utteranceId)

    /**
     * Advances from the just-finished verse to the next one, or finishes the
     * chapter when the queue is exhausted. Ignores callbacks that don't match the
     * active index (stale/duplicate) or arrive while not playing (paused/stopped).
     */
    private fun advance(finishedUtteranceId: String) {
        synchronized(lock) {
            if (_state.value.status != TtsStatus.PLAYING) return
            val finishedIndex = parseIndex(finishedUtteranceId) ?: return
            if (finishedIndex != index) return
            val next = index + 1
            if (next <= queue.lastIndex) {
                index = next
                val verse = queue[next]
                _state.value = _state.value.copy(currentVerse = verse.verse)
                engine.speak(utteranceId(next), verse.text, flush = false)
            } else {
                queue = emptyList()
                index = -1
                _state.value = _state.value.copy(status = TtsStatus.IDLE, currentVerse = null)
                _completionEvents.tryEmit(Unit)
            }
        }
    }

    private fun utteranceId(index: Int): String = "$UTTERANCE_PREFIX$index"

    private fun parseIndex(utteranceId: String): Int? =
        utteranceId.removePrefix(UTTERANCE_PREFIX).toIntOrNull()

    private companion object {
        const val UTTERANCE_PREFIX = "manna-verse-"
    }
}
