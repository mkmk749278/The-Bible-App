package com.manna.bible.domain.audio

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit tests for [DefaultTtsReader]'s verse-advance logic (Requirement 9), driven
 * by a [SpeechEngine] fake — no Android `TextToSpeech`. Covers ordered playback,
 * pause/resume/stop, speed clamping, voice-availability flagging, chapter-end
 * completion, and resilience to stale engine callbacks.
 */
class DefaultTtsReaderTest {

    private val verses = listOf(
        TtsVerse(1, "alpha"),
        TtsVerse(2, "beta"),
        TtsVerse(3, "gamma")
    )

    @Test
    @DisplayName("play speaks the first verse and marks playback active (Req 9.1, 9.2)")
    fun playSpeaksFirstVerse() {
        val engine = FakeSpeechEngine()
        val reader = DefaultTtsReader(engine)

        reader.play(verses, "en")

        assertEquals(TtsStatus.PLAYING, reader.state.value.status)
        assertEquals(1, reader.state.value.currentVerse)
        assertEquals(listOf("manna-verse-0"), engine.spoken)
        assertFalse(reader.state.value.voiceUnavailable)
    }

    @Test
    @DisplayName("each completed utterance advances to the next verse in order (Req 9.2)")
    fun advancesThroughVersesInOrder() {
        val engine = FakeSpeechEngine()
        val reader = DefaultTtsReader(engine)

        reader.play(verses, "en")
        engine.finishCurrent()
        assertEquals(2, reader.state.value.currentVerse)
        engine.finishCurrent()
        assertEquals(3, reader.state.value.currentVerse)
        assertEquals(listOf("manna-verse-0", "manna-verse-1", "manna-verse-2"), engine.spoken)
    }

    @Test
    @DisplayName("reaching chapter end emits a completion event and returns to idle (Req 9.7)")
    fun chapterEndEmitsCompletion() = runTest {
        val engine = FakeSpeechEngine()
        val reader = DefaultTtsReader(engine)

        reader.completionEvents.test {
            reader.play(listOf(TtsVerse(1, "only")), null)
            engine.finishCurrent()
            awaitItem() // single completion at the natural end
            assertEquals(TtsStatus.IDLE, reader.state.value.status)
            assertEquals(null, reader.state.value.currentVerse)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("pause stops the engine and a late callback does not advance (Req 9.3)")
    fun pauseHaltsAndIgnoresStaleCallback() {
        val engine = FakeSpeechEngine()
        val reader = DefaultTtsReader(engine)

        reader.play(verses, "en")
        reader.pause()
        assertEquals(TtsStatus.PAUSED, reader.state.value.status)
        assertEquals(1, engine.stopCount)

        // A callback that arrives after pause must be ignored.
        engine.finishCurrent()
        assertEquals(TtsStatus.PAUSED, reader.state.value.status)
        assertEquals(1, reader.state.value.currentVerse)
    }

    @Test
    @DisplayName("resume re-speaks the current verse (Req 9.3)")
    fun resumeRepeatsCurrentVerse() {
        val engine = FakeSpeechEngine()
        val reader = DefaultTtsReader(engine)

        reader.play(verses, "en")
        engine.finishCurrent() // now on verse 2 (index 1)
        reader.pause()
        reader.resume()

        assertEquals(TtsStatus.PLAYING, reader.state.value.status)
        assertEquals(2, reader.state.value.currentVerse)
        assertEquals(listOf("manna-verse-0", "manna-verse-1", "manna-verse-1"), engine.spoken)
    }

    @Test
    @DisplayName("stop clears playback without emitting completion (Req 9.3)")
    fun stopClearsPlayback() {
        val engine = FakeSpeechEngine()
        val reader = DefaultTtsReader(engine)

        reader.play(verses, "en")
        reader.stop()

        assertEquals(TtsStatus.IDLE, reader.state.value.status)
        assertEquals(null, reader.state.value.currentVerse)
        assertEquals(1, engine.stopCount)
    }

    @Test
    @DisplayName("speed is clamped to the supported 0.5x..2.0x range (Req 9.4)")
    fun speedIsClamped() {
        val engine = FakeSpeechEngine()
        val reader = DefaultTtsReader(engine)

        reader.setSpeed(5.0f)
        assertEquals(2.0f, reader.state.value.speed)
        assertEquals(2.0f, engine.lastSpeed)

        reader.setSpeed(0.1f)
        assertEquals(0.5f, reader.state.value.speed)
        assertEquals(0.5f, engine.lastSpeed)
    }

    @Test
    @DisplayName("an unavailable language is flagged but playback still proceeds (Req 9.6)")
    fun unavailableVoiceIsFlagged() {
        val engine = FakeSpeechEngine(voiceAvailable = false)
        val reader = DefaultTtsReader(engine)

        reader.play(verses, "xx")

        assertTrue(reader.state.value.voiceUnavailable)
        assertEquals(TtsStatus.PLAYING, reader.state.value.status)
        assertEquals(listOf("manna-verse-0"), engine.spoken)
    }

    @Test
    @DisplayName("a callback for a non-current utterance is ignored (Req 9.2)")
    fun staleUtteranceCallbackIgnored() {
        val engine = FakeSpeechEngine()
        val reader = DefaultTtsReader(engine)

        reader.play(verses, "en")
        engine.finish("manna-verse-2") // not the active index 0

        assertEquals(1, reader.state.value.currentVerse)
        assertEquals(listOf("manna-verse-0"), engine.spoken)
    }

    /** Records engine interactions and lets tests trigger utterance callbacks. */
    private class FakeSpeechEngine(
        private val voiceAvailable: Boolean = true
    ) : SpeechEngine {
        private var listener: SpeechListener? = null
        val spoken = mutableListOf<String>()
        var lastSpeed = TtsReader.DEFAULT_SPEED
            private set
        var stopCount = 0
            private set
        var selectedLanguage: String? = null
            private set

        override fun setListener(listener: SpeechListener?) {
            this.listener = listener
        }

        override fun selectLanguage(languageTag: String): Boolean {
            selectedLanguage = languageTag
            return voiceAvailable
        }

        override fun isLanguageAvailable(languageTag: String): Boolean = voiceAvailable

        override fun setSpeed(speed: Float) {
            lastSpeed = speed
        }

        override fun speak(utteranceId: String, text: String, flush: Boolean) {
            spoken += utteranceId
        }

        override fun stop() {
            stopCount++
        }

        override fun shutdown() = Unit

        /** Completes the utterance currently at the head of [spoken]. */
        fun finishCurrent() {
            spoken.lastOrNull()?.let { listener?.onDone(it) }
        }

        /** Completes a specific utterance id (used to simulate stale callbacks). */
        fun finish(utteranceId: String) {
            listener?.onDone(utteranceId)
        }
    }
}
