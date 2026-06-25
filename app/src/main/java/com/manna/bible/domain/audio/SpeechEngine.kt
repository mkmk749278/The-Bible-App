package com.manna.bible.domain.audio

/**
 * Thin, testable abstraction over the platform speech synthesizer (Android
 * `TextToSpeech`). Keeping the engine behind an interface lets the verse-advance
 * logic in [DefaultTtsReader] be exercised on the JVM with a fake (Req 9.2, 9.6).
 *
 * Utterances are spoken asynchronously; the engine reports progress back through a
 * [SpeechListener] keyed by the caller-supplied utterance id.
 */
interface SpeechEngine {

    /** Registers (or clears with null) the progress listener. */
    fun setListener(listener: SpeechListener?)

    /**
     * Selects an on-device voice for [languageTag] (BCP-47). Returns true when a
     * matching voice is available and selected; false when none exists, in which
     * case the engine keeps the device default voice (Req 9.5, 9.6).
     */
    fun selectLanguage(languageTag: String): Boolean

    /** Sets the speech rate; [speed] is already clamped to the supported range. */
    fun setSpeed(speed: Float)

    /**
     * Returns true when the synthesizer has an on-device voice for [languageTag]
     * (BCP-47) ready to speak — i.e. the language is supported and its data is
     * installed. Returns false when no such voice exists or the engine has not yet
     * finished initializing. Unlike [selectLanguage] this is a pure query: it does not
     * change the currently selected voice (F-02, Req 9.6).
     */
    fun isLanguageAvailable(languageTag: String): Boolean

    /**
     * Speaks [text], reporting completion via [SpeechListener.onDone] with
     * [utteranceId]. When [flush] is true any in-progress utterance is interrupted;
     * otherwise [text] is queued after it.
     */
    fun speak(utteranceId: String, text: String, flush: Boolean)

    /** Stops the current utterance and clears the engine's own queue. */
    fun stop()

    /** Releases native resources held by the synthesizer. */
    fun shutdown()
}

/** Asynchronous per-utterance progress callbacks from a [SpeechEngine]. */
interface SpeechListener {
    /** The utterance with [utteranceId] finished speaking naturally. */
    fun onDone(utteranceId: String)

    /** The utterance with [utteranceId] failed; the reader skips ahead. */
    fun onError(utteranceId: String)
}
