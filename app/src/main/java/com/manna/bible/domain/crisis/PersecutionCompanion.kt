package com.manna.bible.domain.crisis

import com.manna.bible.domain.model.Denomination
import com.manna.bible.domain.usecase.ReadingRef
import javax.inject.Inject

/**
 * The kinds of persecution a believer in India may be facing (F-06). Each category maps
 * to a curated, offline set of Scripture passages chosen to speak honestly into that
 * specific kind of pressure — not generic comfort.
 *
 * @property id stable persisted identifier.
 */
enum class PersecutionCategory(val id: String) {
    /** Family pressure, forced marriage, disownment for the faith. */
    FAMILY_REJECTION("family_rejection"),

    /** Employment threat, economic exclusion, loss of livelihood. */
    JOB_LIVELIHOOD("job_livelihood"),

    /** Physical threat, violence, mob danger. */
    PHYSICAL_DANGER("physical_danger"),

    /** Ostracism, caste rejection, untouchability, social exclusion. */
    SOCIAL_EXCLUSION("social_exclusion"),

    /** Doubt under pressure, near apostasy, a faith in crisis. */
    FAITH_CRISIS("faith_crisis")
}

/**
 * Persecution-aware comfort (F-06): a curated, fully-offline tier of Scripture organised
 * by the kind of pressure a believer is under. Every passage is drawn from the 66-book
 * Protestant canon so it resolves in any bundled translation.
 *
 * Pure Kotlin — no Android dependencies — so it is fully JVM-testable.
 */
interface PersecutionCompanion {

    /**
     * The persecution categories to offer a reader of the given [denomination]. The
     * curated tier is universal, so every tradition sees the full taxonomy.
     */
    fun categoriesForDenomination(denomination: Denomination?): List<PersecutionCategory>

    /** The curated passages for [category], in display order. */
    fun versesFor(category: PersecutionCategory): List<ReadingRef>
}

/**
 * Default [PersecutionCompanion] with hand-curated passages (8–12 per category), all
 * within the 66-book canon. The sets are chosen to address each kind of pressure
 * directly and honestly rather than softening it — Jesus' own words about family
 * division, the early church under threat, the equality of all people in Christ.
 */
class DefaultPersecutionCompanion @Inject constructor() : PersecutionCompanion {

    override fun categoriesForDenomination(denomination: Denomination?): List<PersecutionCategory> =
        PersecutionCategory.entries.toList()

    override fun versesFor(category: PersecutionCategory): List<ReadingRef> = when (category) {
        PersecutionCategory.FAMILY_REJECTION -> FAMILY_REJECTION
        PersecutionCategory.JOB_LIVELIHOOD -> JOB_LIVELIHOOD
        PersecutionCategory.PHYSICAL_DANGER -> PHYSICAL_DANGER
        PersecutionCategory.SOCIAL_EXCLUSION -> SOCIAL_EXCLUSION
        PersecutionCategory.FAITH_CRISIS -> FAITH_CRISIS
    }

    private companion object {
        // Family pressure, forced marriage, disownment — Jesus is honest that following
        // him can divide a household, yet promises a hundredfold family in him.
        val FAMILY_REJECTION: List<ReadingRef> = listOf(
            ReadingRef("MAT", 10, 34),  // not peace, but a sword
            ReadingRef("MAT", 10, 35),  // to set a man against his father
            ReadingRef("MAT", 10, 36),  // a person's enemies — his own household
            ReadingRef("MAT", 10, 37),  // whoever loves father or mother more than me
            ReadingRef("LUK", 14, 26),  // unless he hates his own family
            ReadingRef("LUK", 12, 53),  // they will be divided, father against son
            ReadingRef("GEN", 12, 1),   // go from your country and your father's house
            ReadingRef("PSA", 27, 10),  // though my father and mother forsake me
            ReadingRef("MAT", 19, 29),  // everyone who has left houses or family
            ReadingRef("MRK", 10, 29),  // no one who has left home for my sake
            ReadingRef("MRK", 10, 30)   // will receive a hundredfold now
        )

        // Employment threat, economic exclusion — God's provision and the worth of
        // suffering loss for Christ over earthly security.
        val JOB_LIVELIHOOD: List<ReadingRef> = listOf(
            ReadingRef("PHP", 4, 19),   // my God will supply every need of yours
            ReadingRef("MAT", 6, 33),   // seek first the kingdom
            ReadingRef("MAT", 6, 31),   // do not be anxious, what shall we eat
            ReadingRef("MAT", 6, 25),   // do not be anxious about your life
            ReadingRef("HEB", 11, 25),  // chose to be mistreated with God's people
            ReadingRef("HEB", 11, 26),  // the reproach of Christ greater wealth
            ReadingRef("PRO", 3, 5),    // trust in the LORD with all your heart
            ReadingRef("PSA", 37, 25),  // I have not seen the righteous forsaken
            ReadingRef("PSA", 34, 10),  // those who seek the LORD lack no good thing
            ReadingRef("PHP", 4, 11),   // I have learned to be content
            ReadingRef("JOS", 1, 9)     // be strong and courageous, the LORD is with you
        )

        // Physical threat, violence, mob — the LORD as light, refuge, and presence in fire.
        val PHYSICAL_DANGER: List<ReadingRef> = listOf(
            ReadingRef("PSA", 27, 1),   // the LORD is my light and my salvation
            ReadingRef("PSA", 27, 2),   // when evildoers assail me
            ReadingRef("PSA", 27, 3),   // though an army encamp against me
            ReadingRef("ISA", 43, 2),   // when you pass through the waters / fire
            ReadingRef("ACT", 5, 41),   // rejoicing to suffer dishonour for the name
            ReadingRef("PSA", 91, 1),   // shelter of the Most High
            ReadingRef("PSA", 91, 2),   // my refuge and my fortress
            ReadingRef("PSA", 56, 3),   // when I am afraid, I put my trust in you
            ReadingRef("PSA", 46, 1),   // God is our refuge and strength
            ReadingRef("ROM", 8, 31),   // if God is for us, who can be against us
            ReadingRef("2TI", 1, 7)     // not a spirit of fear, but of power
        )

        // Ostracism, caste rejection, untouchability — the radical equality and belonging
        // of every person in Christ, and the blessing on the excluded.
        val SOCIAL_EXCLUSION: List<ReadingRef> = listOf(
            ReadingRef("GAL", 3, 28),   // neither Jew nor Greek, slave nor free
            ReadingRef("JHN", 15, 18),  // if the world hates you
            ReadingRef("JHN", 15, 19),  // because you are not of the world
            ReadingRef("1PE", 4, 14),   // if you are insulted for the name of Christ
            ReadingRef("1PE", 2, 9),    // a chosen race, a royal priesthood
            ReadingRef("HEB", 13, 13),  // go to him outside the camp, bearing reproach
            ReadingRef("LUK", 6, 22),   // blessed when they exclude you
            ReadingRef("MAT", 5, 10),   // blessed are those persecuted for righteousness
            ReadingRef("JAS", 2, 1),    // show no partiality
            ReadingRef("PSA", 27, 10)   // the LORD will take me in
        )

        // Doubt under pressure, near apostasy — honest laments and the faithfulness of
        // God when our own faith is failing.
        val FAITH_CRISIS: List<ReadingRef> = listOf(
            ReadingRef("MRK", 9, 24),   // I believe; help my unbelief!
            ReadingRef("HEB", 12, 1),   // run with endurance the race
            ReadingRef("HEB", 12, 2),   // looking to Jesus, the founder of our faith
            ReadingRef("PSA", 22, 1),   // my God, why have you forsaken me
            ReadingRef("PSA", 22, 2),   // I cry by day, but you do not answer
            ReadingRef("PSA", 42, 11),  // why are you cast down, O my soul
            ReadingRef("PSA", 73, 26),  // my flesh and heart may fail
            ReadingRef("2TI", 2, 13),   // if we are faithless, he remains faithful
            ReadingRef("JUD", 1, 22),   // have mercy on those who doubt
            ReadingRef("LAM", 3, 22),   // his mercies never come to an end
            ReadingRef("LAM", 3, 23),   // they are new every morning
            ReadingRef("MAT", 11, 28)   // come to me, all who are weary
        )
    }
}
