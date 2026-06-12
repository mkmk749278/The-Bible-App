package com.manna.bible.domain.grief

import com.manna.bible.domain.usecase.ReadingRef
import javax.inject.Inject

/**
 * A gentle, 30-day Scripture journey through grief (Phase 2, blueprint P2-1).
 *
 * The arc moves from the rawness of loss (lament, God's nearness to the
 * broken-hearted) toward comfort, the hope of resurrection, and peace — never
 * rushing, never minimizing. Each day pairs a passage with a short reflection
 * (supplied as string resources at the presentation layer, indexed by day).
 *
 * Pure Kotlin — no Android dependencies — so it is fully JVM-testable.
 */
interface GriefJourney {

    /** Total number of days in the journey. */
    val dayCount: Int

    /**
     * The passage for [day] (1-based). Returns null when [day] is outside
     * `1..dayCount`.
     */
    fun verseFor(day: Int): ReadingRef?
}

/** Default [GriefJourney] with a hand-curated 30-day progression (66-book canon). */
class DefaultGriefJourney @Inject constructor() : GriefJourney {

    override val dayCount: Int = VERSES.size

    override fun verseFor(day: Int): ReadingRef? = VERSES.getOrNull(day - 1)

    private companion object {
        val VERSES: List<ReadingRef> = listOf(
            ReadingRef("PSA", 34, 18),  //  1 close to the broken-hearted
            ReadingRef("MAT", 5, 4),    //  2 blessed are those who mourn
            ReadingRef("PSA", 23, 4),   //  3 through the valley
            ReadingRef("LAM", 3, 22),   //  4 his mercies never come to an end
            ReadingRef("PSA", 147, 3),  //  5 he heals the broken-hearted
            ReadingRef("ISA", 41, 10),  //  6 fear not, I am with you
            ReadingRef("PSA", 56, 8),   //  7 you keep my tears in your bottle
            ReadingRef("2CO", 1, 3),    //  8 the God of all comfort
            ReadingRef("PSA", 30, 5),   //  9 weeping tarries for the night
            ReadingRef("JHN", 11, 25),  // 10 I am the resurrection and the life
            ReadingRef("ROM", 8, 28),   // 11 he works all things for good
            ReadingRef("PSA", 73, 26),  // 12 God is the strength of my heart
            ReadingRef("ISA", 43, 2),   // 13 when you pass through the waters
            ReadingRef("PSA", 62, 8),   // 14 pour out your heart before him
            ReadingRef("MAT", 11, 28),  // 15 come to me, all who are weary
            ReadingRef("PSA", 42, 11),  // 16 why are you cast down, my soul
            ReadingRef("2CO", 4, 17),   // 17 a light, momentary affliction
            ReadingRef("REV", 21, 4),   // 18 he will wipe away every tear
            ReadingRef("PSA", 31, 24),  // 19 be strong, take heart
            ReadingRef("ROM", 8, 38),   // 20 nothing can separate us
            ReadingRef("JOB", 19, 25),  // 21 I know that my Redeemer lives
            ReadingRef("PSA", 116, 15), // 22 precious is the death of his saints
            ReadingRef("1TH", 4, 13),   // 23 we do not grieve without hope
            ReadingRef("PSA", 121, 1),  // 24 where does my help come from
            ReadingRef("ISA", 40, 31),  // 25 they shall renew their strength
            ReadingRef("PHP", 4, 7),    // 26 the peace that surpasses understanding
            ReadingRef("PSA", 90, 12),  // 27 teach us to number our days
            ReadingRef("1PE", 5, 7),    // 28 cast all your anxiety on him
            ReadingRef("JHN", 14, 1),   // 29 let not your hearts be troubled
            ReadingRef("PSA", 16, 11)   // 30 in his presence is fullness of joy
        )
    }
}
