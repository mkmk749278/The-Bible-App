package com.manna.bible.domain.crisis

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit tests for [DefaultCrisisCompanion] and [NightWindow].
 */
class CrisisCompanionTest {

    private val companion = DefaultCrisisCompanion()

    @Test
    @DisplayName("offers several structurally valid comfort passages")
    fun comfortVersesAreValid() {
        val verses = companion.comfortVerses()
        assertTrue(verses.size >= 8, "expected a generous set of comfort verses")
        verses.forEach { ref ->
            assertTrue(ref.osisId.isNotBlank())
            assertTrue(ref.chapter >= 1)
            assertTrue(ref.verse >= 1)
        }
    }

    @Test
    @DisplayName("listen passage is valid")
    fun listenPassageValid() {
        val ref = companion.listenPassage()
        assertTrue(ref.osisId.isNotBlank())
        assertTrue(ref.chapter >= 1 && ref.verse >= 1)
    }

    @Test
    @DisplayName("night window covers 11pm–5am only")
    fun nightWindow() {
        assertTrue(NightWindow.isNight(23))
        assertTrue(NightWindow.isNight(0))
        assertTrue(NightWindow.isNight(4))
        assertFalse(NightWindow.isNight(5))
        assertFalse(NightWindow.isNight(12))
        assertFalse(NightWindow.isNight(22))
    }
}
