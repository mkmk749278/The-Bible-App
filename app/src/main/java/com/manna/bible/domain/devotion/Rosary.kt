package com.manna.bible.domain.devotion

import com.manna.bible.domain.usecase.ReadingRef
import javax.inject.Inject

/**
 * The four sets of the Rosary's mysteries. Each set is prayed on its traditional
 * days; together they walk the whole Gospel from the Annunciation to the Coronation.
 */
enum class MysterySet { JOYFUL, SORROWFUL, GLORIOUS, LUMINOUS }

/**
 * A single mystery of the Rosary — one scene from the life of Christ (and His
 * mother) meditated on across a decade of Hail Marys.
 *
 * @property id Stable, machine-readable id (e.g. "joyful_1"); the presentation layer
 *   maps it to a localized title and "fruit of the mystery", keeping the domain free
 *   of UI strings.
 * @property set Which of the four sets the mystery belongs to.
 * @property number The mystery's place within its set (1..5).
 * @property scripture The Gospel passage the mystery is drawn from, within the
 *   66-book canon so it resolves in any downloaded translation.
 *
 * Pure Kotlin — no Android dependencies — so it is fully JVM-testable.
 */
data class Mystery(
    val id: String,
    val set: MysterySet,
    val number: Int,
    val scripture: ReadingRef
)

/**
 * Supplies the twenty mysteries of the Rosary (the Japamala of Indian Catholic
 * tradition). The mysteries, their order, and their Scripture anchoring are the
 * universal form; the prayers themselves (Our Father, Hail Mary, Glory Be, the
 * Apostles' Creed, the Hail Holy Queen) and the mystery titles live in string
 * resources so they localize.
 *
 * Fully offline and deterministic. Pure Kotlin — no Android dependencies.
 */
interface RosaryProvider {

    /** The five mysteries of [set], in order (1..5). */
    fun mysteries(set: MysterySet): List<Mystery>

    /** A single mystery by [set] and [number] (1..5), or null when out of range. */
    fun mystery(set: MysterySet, number: Int): Mystery?

    /**
     * The mystery set traditionally prayed on [dayOfWeek] (1 = Monday … 7 = Sunday,
     * matching `java.time.DayOfWeek.getValue`): Joyful on Mon/Sat, Sorrowful on
     * Tue/Fri, Glorious on Wed/Sun, Luminous on Thu.
     */
    fun setForDay(dayOfWeek: Int): MysterySet
}

/** Default [RosaryProvider] with the twenty canonical mysteries. */
class DefaultRosaryProvider @Inject constructor() : RosaryProvider {

    override fun mysteries(set: MysterySet): List<Mystery> = ALL.filter { it.set == set }

    override fun mystery(set: MysterySet, number: Int): Mystery? =
        ALL.firstOrNull { it.set == set && it.number == number }

    override fun setForDay(dayOfWeek: Int): MysterySet = when (dayOfWeek) {
        1, 6 -> MysterySet.JOYFUL      // Monday, Saturday
        2, 5 -> MysterySet.SORROWFUL   // Tuesday, Friday
        4 -> MysterySet.LUMINOUS       // Thursday
        else -> MysterySet.GLORIOUS    // Wednesday, Sunday
    }

    private companion object {
        val ALL: List<Mystery> = listOf(
            // Joyful — the Incarnation and childhood of Christ
            Mystery("joyful_1", MysterySet.JOYFUL, 1, ReadingRef("LUK", 1, 26)),  // Annunciation
            Mystery("joyful_2", MysterySet.JOYFUL, 2, ReadingRef("LUK", 1, 39)),  // Visitation
            Mystery("joyful_3", MysterySet.JOYFUL, 3, ReadingRef("LUK", 2, 6)),   // Nativity
            Mystery("joyful_4", MysterySet.JOYFUL, 4, ReadingRef("LUK", 2, 22)),  // Presentation
            Mystery("joyful_5", MysterySet.JOYFUL, 5, ReadingRef("LUK", 2, 46)),  // Finding in the Temple

            // Sorrowful — the Passion
            Mystery("sorrowful_1", MysterySet.SORROWFUL, 1, ReadingRef("LUK", 22, 39)), // Agony in the Garden
            Mystery("sorrowful_2", MysterySet.SORROWFUL, 2, ReadingRef("JHN", 19, 1)),  // Scourging
            Mystery("sorrowful_3", MysterySet.SORROWFUL, 3, ReadingRef("JHN", 19, 2)),  // Crowning with Thorns
            Mystery("sorrowful_4", MysterySet.SORROWFUL, 4, ReadingRef("JHN", 19, 17)), // Carrying of the Cross
            Mystery("sorrowful_5", MysterySet.SORROWFUL, 5, ReadingRef("JHN", 19, 18)), // Crucifixion

            // Glorious — the Resurrection and beyond
            Mystery("glorious_1", MysterySet.GLORIOUS, 1, ReadingRef("MAT", 28, 1)),  // Resurrection
            Mystery("glorious_2", MysterySet.GLORIOUS, 2, ReadingRef("ACT", 1, 9)),   // Ascension
            Mystery("glorious_3", MysterySet.GLORIOUS, 3, ReadingRef("ACT", 2, 1)),   // Descent of the Spirit
            Mystery("glorious_4", MysterySet.GLORIOUS, 4, ReadingRef("LUK", 1, 48)),  // Assumption
            Mystery("glorious_5", MysterySet.GLORIOUS, 5, ReadingRef("REV", 12, 1)),  // Coronation

            // Luminous — the public ministry
            Mystery("luminous_1", MysterySet.LUMINOUS, 1, ReadingRef("MAT", 3, 16)),  // Baptism in the Jordan
            Mystery("luminous_2", MysterySet.LUMINOUS, 2, ReadingRef("JHN", 2, 1)),   // Wedding at Cana
            Mystery("luminous_3", MysterySet.LUMINOUS, 3, ReadingRef("MRK", 1, 15)),  // Proclamation of the Kingdom
            Mystery("luminous_4", MysterySet.LUMINOUS, 4, ReadingRef("MAT", 17, 2)),  // Transfiguration
            Mystery("luminous_5", MysterySet.LUMINOUS, 5, ReadingRef("MAT", 26, 26))  // Institution of the Eucharist
        )
    }
}
