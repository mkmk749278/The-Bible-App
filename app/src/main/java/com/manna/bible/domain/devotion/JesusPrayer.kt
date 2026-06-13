package com.manna.bible.domain.devotion

import com.manna.bible.domain.usecase.ReadingRef
import javax.inject.Inject

/**
 * The three traditional depths of the Jesus Prayer ("Lord Jesus Christ, Son of God,
 * have mercy on me, a sinner"), as the Orthodox hesychast tradition describes its
 * growth — from spoken words, to silent attention, to unceasing prayer of the heart.
 */
enum class PrayerDepth { VOCAL, MENTAL, HEART }

/**
 * One depth of the Jesus Prayer, with the Scripture that grounds it.
 *
 * @property depth Which of the three stages this describes.
 * @property scripture A supporting passage within the 66-book canon (so it resolves
 *   in any downloaded translation). The presentation layer supplies the localized
 *   title, guidance, and the breathing cadence; the domain keeps only the structure
 *   and the verse anchor.
 *
 * Pure Kotlin — no Android dependencies.
 */
data class JesusPrayerStage(
    val depth: PrayerDepth,
    val scripture: ReadingRef
)

/**
 * Supplies the Jesus Prayer and its three depths (the Prayers hub). The prayer text
 * and the guidance for each depth live in string resources; the domain holds the
 * ordered stages and their Scripture anchors.
 *
 * Pure Kotlin — no Android dependencies — so it is fully JVM-testable.
 */
interface JesusPrayerProvider {

    /** The three depths, from vocal to prayer of the heart. */
    fun stages(): List<JesusPrayerStage>
}

/** Default [JesusPrayerProvider] grounding each depth in Scripture. */
class DefaultJesusPrayerProvider @Inject constructor() : JesusPrayerProvider {

    override fun stages(): List<JesusPrayerStage> = STAGES

    private companion object {
        val STAGES: List<JesusPrayerStage> = listOf(
            // "have mercy on me, a sinner" — the tax collector's prayer
            JesusPrayerStage(PrayerDepth.VOCAL, ReadingRef("LUK", 18, 13)),
            // "pray without ceasing"
            JesusPrayerStage(PrayerDepth.MENTAL, ReadingRef("1TH", 5, 17)),
            // "Christ lives in me" — the prayer settled in the heart
            JesusPrayerStage(PrayerDepth.HEART, ReadingRef("GAL", 2, 20))
        )
    }
}
