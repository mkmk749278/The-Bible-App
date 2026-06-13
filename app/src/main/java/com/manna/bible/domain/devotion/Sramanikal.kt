package com.manna.bible.domain.devotion

import com.manna.bible.domain.usecase.ReadingRef
import javax.inject.Inject

/**
 * The forty-day Sramanikal — the memorial observance of the Indian churches, in which
 * a family prays for a departed loved one across the forty days following death,
 * gathering on the fortieth in remembrance and the sure hope of the resurrection.
 *
 * This journey pairs each day with a passage of Scripture, moving gently from lament
 * and the nearness of God in loss, through comfort, to the hope of eternal life and a
 * closing commendation and blessing. Each passage sits within the shared 66-book canon
 * so it resolves in any downloaded translation; the daily reflections and the prayer of
 * commendation live in string resources at the presentation layer.
 *
 * Pure Kotlin — no Android dependencies — so it is fully JVM-testable.
 */
interface SramanikalJourney {

    /** Total days in the observance (forty). */
    val dayCount: Int

    /** The passage for [day] (1-based), or null when [day] is outside `1..dayCount`. */
    fun verseFor(day: Int): ReadingRef?
}

/** Default [SramanikalJourney] with a hand-curated forty-day progression (66-book canon). */
class DefaultSramanikalJourney @Inject constructor() : SramanikalJourney {

    override val dayCount: Int = VERSES.size

    override fun verseFor(day: Int): ReadingRef? = VERSES.getOrNull(day - 1)

    private companion object {
        val VERSES: List<ReadingRef> = listOf(
            // Lament & the nearness of God in loss (1–8)
            ReadingRef("PSA", 34, 18),  //  1 close to the broken-hearted
            ReadingRef("PSA", 23, 4),   //  2 through the valley
            ReadingRef("LAM", 3, 22),   //  3 his mercies never end
            ReadingRef("PSA", 147, 3),  //  4 he heals the broken-hearted
            ReadingRef("MAT", 5, 4),    //  5 blessed are those who mourn
            ReadingRef("PSA", 56, 8),   //  6 he keeps our tears
            ReadingRef("JOB", 1, 21),   //  7 the Lord gave and has taken away
            ReadingRef("PSA", 31, 9),   //  8 be gracious, I am in distress
            // The God of all comfort (9–16)
            ReadingRef("2CO", 1, 3),    //  9 the God of all comfort
            ReadingRef("ISA", 41, 10),  // 10 fear not, I am with you
            ReadingRef("PSA", 46, 1),   // 11 God is our refuge and strength
            ReadingRef("ISA", 43, 2),   // 12 when you pass through the waters
            ReadingRef("MAT", 11, 28),  // 13 come to me, all who are weary
            ReadingRef("PSA", 121, 1),  // 14 where does my help come from
            ReadingRef("PSA", 30, 5),   // 15 weeping for a night, joy in the morning
            ReadingRef("PHP", 4, 7),    // 16 the peace that surpasses understanding
            // The hope of resurrection (17–26)
            ReadingRef("JHN", 11, 25),  // 17 I am the resurrection and the life
            ReadingRef("1CO", 15, 54),  // 18 death swallowed up in victory
            ReadingRef("ROM", 6, 5),    // 19 united with him in resurrection
            ReadingRef("1TH", 4, 14),   // 20 God will bring with him those who sleep
            ReadingRef("JHN", 14, 1),   // 21 let not your hearts be troubled
            ReadingRef("JHN", 14, 2),   // 22 in my Father's house are many rooms
            ReadingRef("2CO", 5, 1),    // 23 an eternal house in the heavens
            ReadingRef("1CO", 15, 42),  // 24 sown perishable, raised imperishable
            ReadingRef("PHP", 3, 20),   // 25 our citizenship is in heaven
            ReadingRef("2TI", 4, 7),    // 26 the crown of righteousness
            // The communion of saints & the life to come (27–34)
            ReadingRef("REV", 21, 4),   // 27 he will wipe away every tear
            ReadingRef("REV", 7, 17),   // 28 the Lamb will be their shepherd
            ReadingRef("HEB", 12, 1),   // 29 surrounded by a cloud of witnesses
            ReadingRef("HEB", 11, 10),  // 30 the city whose builder is God
            ReadingRef("LUK", 23, 43),  // 31 today you will be with me in paradise
            ReadingRef("JHN", 10, 28),  // 32 none shall snatch them from his hand
            ReadingRef("ROM", 8, 38),   // 33 nothing can separate us
            ReadingRef("PSA", 116, 15), // 34 precious is the death of his saints
            // Commendation, peace & blessing (35–40)
            ReadingRef("1PE", 1, 3),    // 35 born again to a living hope
            ReadingRef("REV", 14, 13),  // 36 blessed are the dead who die in the Lord
            ReadingRef("PSA", 73, 26),  // 37 God is the strength of my heart forever
            ReadingRef("PSA", 16, 11),  // 38 in his presence is fullness of joy
            ReadingRef("LUK", 23, 46),  // 39 into your hands I commend the spirit
            ReadingRef("NUM", 6, 24)    // 40 the Lord bless you and keep you
        )
    }
}
