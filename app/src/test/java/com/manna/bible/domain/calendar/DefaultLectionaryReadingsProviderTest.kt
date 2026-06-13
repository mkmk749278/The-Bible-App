package com.manna.bible.domain.calendar

import com.manna.bible.domain.model.Denomination
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** Unit tests for [DefaultLectionaryReadingsProvider] — principal-feast readings. */
class DefaultLectionaryReadingsProviderTest {

    private val provider = DefaultLectionaryReadingsProvider(DefaultJesusEventsProvider())

    private fun readings(date: LocalDate) = provider.readingsFor(date, Denomination.CSI)

    @Test
    @DisplayName("Christmas carries OT, Psalm, Epistle, and Gospel")
    fun christmas() {
        val r = readings(LocalDate.of(2026, 12, 25))
        assertEquals(
            listOf(ReadingKind.FIRST, ReadingKind.PSALM, ReadingKind.SECOND, ReadingKind.GOSPEL),
            r.map { it.kind }
        )
        val gospel = r.first { it.kind == ReadingKind.GOSPEL }
        assertEquals("LUK", gospel.osisId)
        assertEquals(2, gospel.chapter)
        // Psalms always use the PSA book id.
        assertEquals("PSA", r.first { it.kind == ReadingKind.PSALM }.osisId)
    }

    @Test
    @DisplayName("Easter (5 Apr 2026) carries its proper readings")
    fun easter() {
        val r = readings(LocalDate.of(2026, 4, 5))
        assertTrue(r.isNotEmpty())
        assertTrue(r.any { it.kind == ReadingKind.GOSPEL && it.osisId == "JHN" })
    }

    @Test
    @DisplayName("An ordinary day has no proper readings")
    fun ordinaryDay() {
        assertTrue(readings(LocalDate.of(2026, 7, 15)).isEmpty())
    }
}
