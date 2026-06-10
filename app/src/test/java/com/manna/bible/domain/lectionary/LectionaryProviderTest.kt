package com.manna.bible.domain.lectionary

import com.manna.bible.domain.model.Denomination
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit tests for [DefaultLectionaryProvider] covering the denomination -> lectionary
 * mapping (Requirements 9 and 18).
 */
class LectionaryProviderTest {

    private val provider: LectionaryProvider = DefaultLectionaryProvider()

    @Test
    @DisplayName("CSI maps to the CSI Almanac")
    fun csiMapsToCsiAlmanac() {
        assertEquals("csi_almanac", provider.lectionaryIdFor(Denomination.CSI))
    }

    @Test
    @DisplayName("Catholic maps to the Roman Catholic calendar")
    fun catholicMapsToRcCalendar() {
        assertEquals("rc_calendar", provider.lectionaryIdFor(Denomination.CATHOLIC))
    }

    @Test
    @DisplayName("Orthodox maps to the Orthodox calendar")
    fun orthodoxMapsToOrthodoxCalendar() {
        assertEquals("orthodox_calendar", provider.lectionaryIdFor(Denomination.ORTHODOX))
    }

    @Test
    @DisplayName("Mar Thoma maps to the general lectionary")
    fun marThomaMapsToGeneralLectionary() {
        assertEquals("general_lectionary", provider.lectionaryIdFor(Denomination.MAR_THOMA))
    }

    @Test
    @DisplayName("Other Protestant maps to the general lectionary")
    fun protestantOtherMapsToGeneralLectionary() {
        assertEquals("general_lectionary", provider.lectionaryIdFor(Denomination.PROTESTANT_OTHER))
    }

    @Test
    @DisplayName("Show everything has no specific lectionary (Requirement 9.5)")
    fun showEverythingMapsToNull() {
        assertNull(provider.lectionaryIdFor(Denomination.SHOW_EVERYTHING))
    }

    @Test
    @DisplayName("Every denomination has a defined mapping")
    fun everyDenominationIsMapped() {
        val expected = mapOf(
            Denomination.CSI to "csi_almanac",
            Denomination.CATHOLIC to "rc_calendar",
            Denomination.ORTHODOX to "orthodox_calendar",
            Denomination.MAR_THOMA to "general_lectionary",
            Denomination.PROTESTANT_OTHER to "general_lectionary",
            Denomination.SHOW_EVERYTHING to null,
        )
        Denomination.entries.forEach { denomination ->
            assertEquals(expected[denomination], provider.lectionaryIdFor(denomination))
        }
    }
}
