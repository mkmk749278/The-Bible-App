package com.manna.bible.domain.fasting

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** Unit tests for [DefaultFastingPlans] and [FastProgress]. */
class FastingTest {

    private val plans = DefaultFastingPlans()
    private val hourMillis = 3_600_000L

    @Test
    @DisplayName("offers fasts and valid focus verses; lookup works")
    fun plansAndFocus() {
        assertTrue(plans.plans().isNotEmpty())
        plans.plans().forEach { assertTrue(it.hours > 0) }
        assertEquals(12, plans.planById("sunrise_sunset")?.hours)
        assertNull(plans.planById("nope"))
        plans.focusVerses().forEach { ref ->
            assertTrue(ref.osisId.isNotBlank() && ref.chapter >= 1 && ref.verse >= 1)
        }
    }

    @Test
    @DisplayName("progress: fraction, remaining, and completion")
    fun progress() {
        val start = 1_000_000L
        // 12-hour fast, 3 hours in → 25%.
        val now = start + 3 * hourMillis
        assertEquals(0.25f, FastProgress.fraction(start, 12, now))
        assertEquals(9 * hourMillis, FastProgress.remainingMillis(start, 12, now))
        assertFalse(FastProgress.isComplete(start, 12, now))

        // At/after the end → complete, no remainder, fraction clamps to 1.
        val end = start + 12 * hourMillis
        assertTrue(FastProgress.isComplete(start, 12, end))
        assertEquals(0L, FastProgress.remainingMillis(start, 12, end + hourMillis))
        assertEquals(1f, FastProgress.fraction(start, 12, end + hourMillis))
    }

    @Test
    @DisplayName("elapsed never goes negative for a future start")
    fun noNegativeElapsed() {
        assertEquals(0L, FastProgress.elapsedMillis(startMillis = 2000L, nowMillis = 1000L))
    }
}
