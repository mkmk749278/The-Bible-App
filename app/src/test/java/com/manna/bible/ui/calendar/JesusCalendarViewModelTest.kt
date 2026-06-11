package com.manna.bible.ui.calendar

import app.cash.turbine.test
import com.manna.bible.domain.calendar.JesusCalendarEntry
import com.manna.bible.domain.calendar.JesusEventsProvider
import com.manna.bible.domain.usecase.ReadingRef
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Unit tests for [JesusCalendarViewModel] — maps provider entries into display rows
 * and flags the next upcoming event.
 */
class JesusCalendarViewModelTest {

    private val easter = JesusCalendarEntry(
        id = "easter",
        date = LocalDate.now().withMonth(4).withDayOfMonth(5),
        verseRefs = listOf(ReadingRef("MAT", 28, 1))
    )
    private val nativity = JesusCalendarEntry(
        id = "nativity",
        date = LocalDate.now().withMonth(12).withDayOfMonth(25),
        verseRefs = listOf(ReadingRef("LUK", 2, 7))
    )

    @Test
    @DisplayName("maps entries to rows, formats the first ref, and flags the next event")
    fun mapsRowsAndFlagsNext() = runTestRows()

    private fun runTestRows() = kotlinx.coroutines.test.runTest {
        val vm = JesusCalendarViewModel(
            provider = FakeProvider(entries = listOf(easter, nativity), next = nativity)
        )
        vm.uiState.test {
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertEquals(2, state.rows.size)
            val easterRow = state.rows.first { it.id == "easter" }
            assertEquals("MAT.28.1", easterRow.osisRef)
            assertFalse(easterRow.isNext)
            assertTrue(state.rows.first { it.id == "nativity" }.isNext)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private class FakeProvider(
        private val entries: List<JesusCalendarEntry>,
        private val next: JesusCalendarEntry?
    ) : JesusEventsProvider {
        override fun entriesFor(year: Int): List<JesusCalendarEntry> = entries
        override fun nextEntry(today: LocalDate): JesusCalendarEntry? = next
    }
}
