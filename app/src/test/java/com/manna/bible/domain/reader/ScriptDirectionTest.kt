package com.manna.bible.domain.reader

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit tests for [ScriptDirection] (Requirement 14.4).
 */
class ScriptDirectionTest {

    @Test
    @DisplayName("RTL languages resolve right-to-left")
    fun rtlLanguages() {
        assertTrue(ScriptDirection.isRightToLeft("ur"))   // Urdu
        assertTrue(ScriptDirection.isRightToLeft("ar"))   // Arabic
        assertTrue(ScriptDirection.isRightToLeft("fa"))   // Persian
        assertTrue(ScriptDirection.isRightToLeft("he"))   // Hebrew
    }

    @Test
    @DisplayName("region suffixes and casing are ignored")
    fun normalizesTag() {
        assertTrue(ScriptDirection.isRightToLeft("UR-PK"))
        assertTrue(ScriptDirection.isRightToLeft("ar_EG"))
        assertTrue(ScriptDirection.isRightToLeft("  he  "))
    }

    @Test
    @DisplayName("LTR languages and null/blank resolve left-to-right")
    fun ltrAndEmpty() {
        assertFalse(ScriptDirection.isRightToLeft("en"))
        assertFalse(ScriptDirection.isRightToLeft("ta"))
        assertFalse(ScriptDirection.isRightToLeft("hi"))
        assertFalse(ScriptDirection.isRightToLeft(null))
        assertFalse(ScriptDirection.isRightToLeft(""))
    }
}
