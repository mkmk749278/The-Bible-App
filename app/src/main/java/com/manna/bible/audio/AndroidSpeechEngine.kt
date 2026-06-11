package com.manna.bible.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.manna.bible.domain.audio.SpeechEngine
import com.manna.bible.domain.audio.SpeechListener
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android [TextToSpeech]-backed [SpeechEngine] (Requirement 9). The synthesizer
 * initializes asynchronously, so calls made before it is ready are buffered and
 * replayed on init; this keeps [com.manna.bible.domain.audio.DefaultTtsReader]
 * free of any platform timing concerns.
 *
 * The only Android-coupled piece of the audio stack — all verse-advance logic lives
 * in the pure-Kotlin reader and is unit-tested with a fake engine.
 */
@Singleton
class AndroidSpeechEngine @Inject constructor(
    @ApplicationContext context: Context
) : SpeechEngine {

    private var listener: SpeechListener? = null

    @Volatile
    private var ready = false

    /** Action queued because it arrived before the engine finished initializing. */
    @Volatile
    private var pending: (() -> Unit)? = null

    private val tts = TextToSpeech(context) { status ->
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            tts.setOnUtteranceProgressListener(progressListener)
            pending?.invoke()
        }
        pending = null
    }

    private val progressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) = Unit

        override fun onDone(utteranceId: String?) {
            utteranceId?.let { listener?.onDone(it) }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            utteranceId?.let { listener?.onError(it) }
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            utteranceId?.let { listener?.onError(it) }
        }
    }

    override fun setListener(listener: SpeechListener?) {
        this.listener = listener
    }

    override fun selectLanguage(languageTag: String): Boolean {
        if (!ready) return false
        val locale = Locale.forLanguageTag(languageTag)
        val result = tts.isLanguageAvailable(locale)
        val available = result == TextToSpeech.LANG_AVAILABLE ||
            result == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
            result == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
        if (available) {
            tts.language = locale
        }
        return available
    }

    override fun setSpeed(speed: Float) {
        tts.setSpeechRate(speed)
    }

    override fun speak(utteranceId: String, text: String, flush: Boolean) {
        val action = {
            val mode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            tts.speak(text, mode, null, utteranceId)
            Unit
        }
        if (ready) action() else pending = action
    }

    override fun stop() {
        pending = null
        if (ready) tts.stop()
    }

    override fun shutdown() {
        pending = null
        ready = false
        tts.shutdown()
    }
}
