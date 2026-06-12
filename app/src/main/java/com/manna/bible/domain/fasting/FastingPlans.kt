package com.manna.bible.domain.fasting

import com.manna.bible.domain.usecase.ReadingRef
import javax.inject.Inject

/**
 * A time-boxed fast the user can choose (Phase 2).
 *
 * @property id Stable id; the presentation layer maps it to a localized label and
 *   description.
 * @property hours Duration of the fast in hours.
 *
 * Pure Kotlin — no Android dependencies.
 */
data class FastPlan(val id: String, val hours: Int)

/**
 * Provides the available fasts and the Scripture that anchors the experience
 * (Fasting Companion, Phase 2). Curated and offline.
 *
 * Pure Kotlin — fully JVM-testable.
 */
interface FastingPlans {

    /** Available fasts, in display order. */
    fun plans(): List<FastPlan>

    /** Looks up a plan by id, or null. */
    fun planById(id: String): FastPlan?

    /** Verses to dwell on during a fast (hunger turned toward God). */
    fun focusVerses(): List<ReadingRef>
}

/** Default [FastingPlans] with time-boxed fasts and focus verses (66-book canon). */
class DefaultFastingPlans @Inject constructor() : FastingPlans {

    override fun plans(): List<FastPlan> = PLANS

    override fun planById(id: String): FastPlan? = PLANS.firstOrNull { it.id == id }

    override fun focusVerses(): List<ReadingRef> = FOCUS

    private companion object {
        val PLANS: List<FastPlan> = listOf(
            FastPlan("partial", 6),         // a partial / one-meal fast
            FastPlan("sunrise_sunset", 12), // sunrise to sunset
            FastPlan("full_day", 24),       // a full day
            FastPlan("three_day", 72)       // a three-day fast
        )

        val FOCUS: List<ReadingRef> = listOf(
            ReadingRef("MAT", 6, 16),  // when you fast, do not look gloomy
            ReadingRef("ISA", 58, 6),  // the fast that I have chosen
            ReadingRef("JOL", 2, 12),  // return to me with all your heart
            ReadingRef("MAT", 4, 4),   // man shall not live by bread alone
            ReadingRef("PSA", 42, 1),  // as the deer pants for water
            ReadingRef("MAT", 6, 33),  // seek first the kingdom of God
            ReadingRef("PSA", 63, 1)   // my soul thirsts for you
        )
    }
}
