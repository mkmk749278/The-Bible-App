package com.manna.bible.domain.liturgy

import com.manna.bible.domain.model.Denomination
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** Unit tests for [DefaultLiturgyProvider] — denomination mapping and content sanity. */
class DefaultLiturgyProviderTest {

    private val provider = DefaultLiturgyProvider()

    @Test
    @DisplayName("the tradition picked at setup chooses its order of worship")
    fun defaultMapping() {
        assertEquals("roman_catholic_mass", provider.defaultFor(Denomination.CATHOLIC)?.id)
        assertEquals("csi_holy_communion", provider.defaultFor(Denomination.CSI)?.id)
        assertEquals("csi_holy_communion", provider.defaultFor(Denomination.PROTESTANT_OTHER)?.id)
        // Rites we don't yet ship faithfully have no default (the screen offers the rest).
        assertNull(provider.defaultFor(Denomination.ORTHODOX))
        assertNull(provider.defaultFor(Denomination.MAR_THOMA))
        assertNull(provider.defaultFor(Denomination.SHOW_EVERYTHING))
    }

    @Test
    @DisplayName("every order is complete, attributed, and well-formed")
    fun contentSanity() {
        val all = provider.all()
        assertEquals(2, all.size)
        all.forEach { liturgy ->
            assertTrue(liturgy.title.isNotBlank(), "${liturgy.id} has a title")
            assertTrue(liturgy.tradition.isNotBlank(), "${liturgy.id} names a tradition")
            assertTrue(liturgy.sourceNote.isNotBlank(), "${liturgy.id} cites a source")
            assertTrue(liturgy.sections.size >= 4, "${liturgy.id} has the major sections")
            val parts = liturgy.sections.flatMap { it.parts }
            assertTrue(parts.size >= 20, "${liturgy.id} has a full order")
            // Every non-rubric part actually says something.
            assertTrue(
                parts.all { it.role == LiturgyRole.RUBRIC || !it.text.isNullOrBlank() },
                "${liturgy.id}: spoken parts must carry text"
            )
            // The Lord's Prayer is prayed in both orders.
            assertTrue(
                parts.any { it.text?.contains("Our Father", ignoreCase = true) == true },
                "${liturgy.id} includes the Lord's Prayer"
            )
            // The presidential prayers are flagged, not invented.
            assertTrue(
                parts.any { it.needsOfficialText },
                "${liturgy.id} flags the prayers proper to the day"
            )
        }
    }

    @Test
    @DisplayName("the CSI order links the Words of Institution to scripture")
    fun csiInstitutionLinksScripture() {
        val csi = provider.defaultFor(Denomination.CSI)
        assertNotNull(csi)
        val institution = csi!!.sections.flatMap { it.parts }.firstOrNull { it.osisRef != null }
        assertNotNull(institution)
        assertEquals("1CO.11.23", institution!!.osisRef)
    }
}
