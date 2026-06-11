package com.manna.bible.domain.topical

import com.manna.bible.domain.usecase.ReadingRef
import javax.inject.Inject

/**
 * Default, fully offline [TopicalIndex].
 *
 * The topic list and each topic's verses are hand-curated constants — no network,
 * no database, and no on-device model are required, so topical search works in
 * airplane mode from a cold start. References use USFM 3-letter UPPERCASE book ids
 * within the 66-book Protestant canon (e.g. "JHN", "ROM", "PSA"), so they resolve
 * in every supported translation including the bundled World English Bible.
 *
 * Where a passage spans multiple verses (e.g. 1 John 4:7-8), the curated entry
 * points at the first verse of the range; callers expand the passage at the
 * presentation layer. Topics are returned in a stable, intentional display order.
 *
 * Pure Kotlin — no Android dependencies.
 */
class DefaultTopicalIndex @Inject constructor() : TopicalIndex {

    override fun topics(): List<Topic> = TOPICS

    override fun versesFor(topicId: String): List<ReadingRef> =
        VERSES_BY_TOPIC[topicId].orEmpty()

    private companion object {

        /** Curated topics, in display order. Ids are stable and lowercase. */
        val TOPICS: List<Topic> = listOf(
            Topic("love", "Love"),
            Topic("faith", "Faith"),
            Topic("hope", "Hope"),
            Topic("fear", "Fear & Anxiety"),
            Topic("forgiveness", "Forgiveness"),
            Topic("peace", "Peace"),
            Topic("prayer", "Prayer"),
            Topic("healing", "Healing"),
            Topic("salvation", "Salvation"),
            Topic("comfort", "Comfort"),
            Topic("grief", "Grief"),
            Topic("strength", "Strength"),
            Topic("patience", "Patience"),
            Topic("joy", "Joy"),
            Topic("wisdom", "Wisdom"),
            Topic("money", "Money & Provision"),
            Topic("marriage", "Marriage"),
            Topic("temptation", "Temptation"),
            Topic("guidance", "Guidance"),
            Topic("thankfulness", "Thankfulness")
        )

        /**
         * Curated verses per topic id. Each list holds 4–8 well-known, correctly
         * numbered references from the 66-book canon. Multi-verse passages point at
         * the first verse of the range.
         */
        val VERSES_BY_TOPIC: Map<String, List<ReadingRef>> = mapOf(
            "love" to listOf(
                ReadingRef("JHN", 3, 16),
                ReadingRef("1CO", 13, 4),
                ReadingRef("ROM", 8, 38),
                ReadingRef("1JN", 4, 7),
                ReadingRef("ROM", 5, 8),
                ReadingRef("1JN", 4, 19)
            ),
            "faith" to listOf(
                ReadingRef("HEB", 11, 1),
                ReadingRef("HEB", 11, 6),
                ReadingRef("ROM", 10, 17),
                ReadingRef("MAT", 17, 20),
                ReadingRef("EPH", 2, 8),
                ReadingRef("JAS", 2, 17)
            ),
            "hope" to listOf(
                ReadingRef("ROM", 15, 13),
                ReadingRef("JER", 29, 11),
                ReadingRef("ROM", 5, 5),
                ReadingRef("ISA", 40, 31),
                ReadingRef("HEB", 6, 19),
                ReadingRef("PSA", 39, 7)
            ),
            "fear" to listOf(
                ReadingRef("ISA", 41, 10),
                ReadingRef("PHP", 4, 6),
                ReadingRef("PSA", 23, 4),
                ReadingRef("2TI", 1, 7),
                ReadingRef("MAT", 6, 34),
                ReadingRef("JOS", 1, 9)
            ),
            "forgiveness" to listOf(
                ReadingRef("1JN", 1, 9),
                ReadingRef("EPH", 4, 32),
                ReadingRef("COL", 3, 13),
                ReadingRef("MAT", 6, 14),
                ReadingRef("PSA", 103, 12),
                ReadingRef("MAT", 18, 21)
            ),
            "peace" to listOf(
                ReadingRef("JHN", 14, 27),
                ReadingRef("PHP", 4, 7),
                ReadingRef("ISA", 26, 3),
                ReadingRef("ROM", 5, 1),
                ReadingRef("COL", 3, 15),
                ReadingRef("PSA", 4, 8)
            ),
            "prayer" to listOf(
                ReadingRef("PHP", 4, 6),
                ReadingRef("MAT", 6, 9),
                ReadingRef("1TH", 5, 17),
                ReadingRef("JAS", 5, 16),
                ReadingRef("MAT", 7, 7),
                ReadingRef("1JN", 5, 14)
            ),
            "healing" to listOf(
                ReadingRef("JER", 17, 14),
                ReadingRef("PSA", 147, 3),
                ReadingRef("ISA", 53, 5),
                ReadingRef("JAS", 5, 15),
                ReadingRef("EXO", 15, 26),
                ReadingRef("2CH", 7, 14)
            ),
            "salvation" to listOf(
                ReadingRef("ROM", 10, 9),
                ReadingRef("EPH", 2, 8),
                ReadingRef("JHN", 3, 16),
                ReadingRef("ACT", 4, 12),
                ReadingRef("ROM", 6, 23),
                ReadingRef("TIT", 3, 5)
            ),
            "comfort" to listOf(
                ReadingRef("2CO", 1, 3),
                ReadingRef("PSA", 23, 4),
                ReadingRef("MAT", 5, 4),
                ReadingRef("PSA", 34, 18),
                ReadingRef("ISA", 41, 10),
                ReadingRef("JHN", 14, 1)
            ),
            "grief" to listOf(
                ReadingRef("PSA", 34, 18),
                ReadingRef("MAT", 5, 4),
                ReadingRef("REV", 21, 4),
                ReadingRef("PSA", 147, 3),
                ReadingRef("1TH", 4, 13),
                ReadingRef("JHN", 11, 25)
            ),
            "strength" to listOf(
                ReadingRef("PHP", 4, 13),
                ReadingRef("ISA", 40, 31),
                ReadingRef("ISA", 41, 10),
                ReadingRef("PSA", 46, 1),
                ReadingRef("2CO", 12, 9),
                ReadingRef("NEH", 8, 10)
            ),
            "patience" to listOf(
                ReadingRef("JAS", 1, 4),
                ReadingRef("ROM", 12, 12),
                ReadingRef("GAL", 6, 9),
                ReadingRef("PSA", 37, 7),
                ReadingRef("ECC", 7, 8),
                ReadingRef("ROM", 5, 3)
            ),
            "joy" to listOf(
                ReadingRef("NEH", 8, 10),
                ReadingRef("PHP", 4, 4),
                ReadingRef("PSA", 16, 11),
                ReadingRef("JHN", 15, 11),
                ReadingRef("ROM", 15, 13),
                ReadingRef("JAS", 1, 2)
            ),
            "wisdom" to listOf(
                ReadingRef("JAS", 1, 5),
                ReadingRef("PRO", 3, 5),
                ReadingRef("PRO", 9, 10),
                ReadingRef("PRO", 2, 6),
                ReadingRef("COL", 3, 16),
                ReadingRef("PSA", 111, 10)
            ),
            "money" to listOf(
                ReadingRef("PHP", 4, 19),
                ReadingRef("MAT", 6, 33),
                ReadingRef("PRO", 3, 9),
                ReadingRef("1TI", 6, 10),
                ReadingRef("HEB", 13, 5),
                ReadingRef("MAL", 3, 10)
            ),
            "marriage" to listOf(
                ReadingRef("GEN", 2, 24),
                ReadingRef("EPH", 5, 25),
                ReadingRef("1CO", 13, 4),
                ReadingRef("COL", 3, 14),
                ReadingRef("ECC", 4, 9),
                ReadingRef("MRK", 10, 9)
            ),
            "temptation" to listOf(
                ReadingRef("1CO", 10, 13),
                ReadingRef("JAS", 1, 12),
                ReadingRef("MAT", 26, 41),
                ReadingRef("HEB", 4, 15),
                ReadingRef("JAS", 4, 7),
                ReadingRef("1PE", 5, 8)
            ),
            "guidance" to listOf(
                ReadingRef("PRO", 3, 5),
                ReadingRef("PSA", 32, 8),
                ReadingRef("PSA", 119, 105),
                ReadingRef("ISA", 30, 21),
                ReadingRef("JER", 29, 11),
                ReadingRef("JAS", 1, 5)
            ),
            "thankfulness" to listOf(
                ReadingRef("1TH", 5, 18),
                ReadingRef("PSA", 100, 4),
                ReadingRef("COL", 3, 17),
                ReadingRef("PHP", 4, 6),
                ReadingRef("PSA", 107, 1),
                ReadingRef("EPH", 5, 20)
            )
        )
    }
}
