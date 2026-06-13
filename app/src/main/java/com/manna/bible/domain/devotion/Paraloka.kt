package com.manna.bible.domain.devotion

import com.manna.bible.domain.usecase.ReadingRef
import javax.inject.Inject

/**
 * One passage in the Paraloka collection — Scripture on *paraloka*, the "world
 * beyond": heaven, the resurrection, and the eternal life Christ promises. The word
 * carries weight across Tamil, Telugu, Hindi, and Malayalam Christianity, and these
 * verses are the ones believers most often return to in hope and in grief.
 *
 * @property id Stable id (e.g. "paraloka_01"); the presentation layer maps it to a
 *   localized theme and reflection, keeping the domain free of UI strings.
 * @property scripture The passage, within the 66-book canon so it resolves in any
 *   downloaded translation.
 *
 * Pure Kotlin — no Android dependencies.
 */
data class ParalokaPassage(
    val id: String,
    val scripture: ReadingRef
)

/**
 * A prayer of Christian hope in the Paraloka collection — drawn from the funeral and
 * memorial traditions of the Indian churches (CSI, Catholic, Orthodox). The prayer
 * text lives in string resources, indexed by [id]; the domain holds the structure
 * and the Scripture each prayer rests on.
 *
 * @property id Stable id (e.g. "paraloka_prayer_csi").
 * @property scripture The passage that anchors the prayer.
 */
data class ParalokaPrayer(
    val id: String,
    val scripture: ReadingRef
)

/**
 * Supplies the Paraloka collection: the eternal-life Scripture and the prayers of
 * Christian hope (the Prayers hub). Fully offline. Pure Kotlin — no Android deps.
 */
interface ParalokaProvider {

    /** The curated eternal-life passages, in reading order. */
    fun passages(): List<ParalokaPassage>

    /** The prayers of hope and commendation, in order. */
    fun prayers(): List<ParalokaPrayer>
}

/** Default [ParalokaProvider] with fifteen passages and five prayers of hope. */
class DefaultParalokaProvider @Inject constructor() : ParalokaProvider {

    override fun passages(): List<ParalokaPassage> = PASSAGES

    override fun prayers(): List<ParalokaPrayer> = PRAYERS

    private companion object {
        val PASSAGES: List<ParalokaPassage> = listOf(
            ParalokaPassage("paraloka_01", ReadingRef("JHN", 14, 2)),   // many rooms prepared
            ParalokaPassage("paraloka_02", ReadingRef("2CO", 5, 1)),    // an eternal house in heaven
            ParalokaPassage("paraloka_03", ReadingRef("REV", 21, 4)),   // he will wipe every tear
            ParalokaPassage("paraloka_04", ReadingRef("PHP", 3, 20)),   // our citizenship is in heaven
            ParalokaPassage("paraloka_05", ReadingRef("LUK", 23, 43)),  // today with me in paradise
            ParalokaPassage("paraloka_06", ReadingRef("1TH", 4, 16)),   // caught up to meet the Lord
            ParalokaPassage("paraloka_07", ReadingRef("1CO", 15, 42)),  // raised imperishable
            ParalokaPassage("paraloka_08", ReadingRef("JHN", 3, 16)),   // eternal life
            ParalokaPassage("paraloka_09", ReadingRef("JHN", 10, 28)),  // none shall snatch them
            ParalokaPassage("paraloka_10", ReadingRef("HEB", 11, 10)),  // the city whose builder is God
            ParalokaPassage("paraloka_11", ReadingRef("1PE", 1, 3)),    // an inheritance kept in heaven
            ParalokaPassage("paraloka_12", ReadingRef("ROM", 8, 38)),   // nothing can separate us
            ParalokaPassage("paraloka_13", ReadingRef("JHN", 11, 25)),  // I am the resurrection
            ParalokaPassage("paraloka_14", ReadingRef("PSA", 23, 4)),   // through the valley
            ParalokaPassage("paraloka_15", ReadingRef("2TI", 4, 7))     // the crown of righteousness
        )

        val PRAYERS: List<ParalokaPrayer> = listOf(
            ParalokaPrayer("paraloka_prayer_commendation", ReadingRef("LUK", 23, 46)), // into your hands
            ParalokaPrayer("paraloka_prayer_rest", ReadingRef("REV", 14, 13)),         // rest from their labours
            ParalokaPrayer("paraloka_prayer_comfort", ReadingRef("MAT", 5, 4)),        // blessed are those who mourn
            ParalokaPrayer("paraloka_prayer_hope", ReadingRef("1TH", 4, 13)),          // we do not grieve as without hope
            ParalokaPrayer("paraloka_prayer_light", ReadingRef("JHN", 8, 12))          // the light of life
        )
    }
}
