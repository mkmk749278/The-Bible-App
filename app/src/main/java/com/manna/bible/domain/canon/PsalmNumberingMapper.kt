package com.manna.bible.domain.canon

import com.manna.bible.domain.model.NumberingScheme

/**
 * Pure-domain mapper that converts Psalm chapter numbers between the canonical
 * **Masoretic** numbering used for verse storage and the **Septuagint/Vulgate**
 * numbering used for display by Catholic and Orthodox traditions.
 *
 * ## Why a mapping is needed
 *
 * Manna stores every Psalm under one canonical numbering (Masoretic). When the
 * active [NumberingScheme] is [NumberingScheme.SEPTUAGINT], the numbers a reader
 * expects differ from the stored numbers for Psalms 9 through 147. Rather than
 * duplicate Psalm content per tradition, we convert numbers at the presentation
 * layer (see Requirements 7 and 17).
 *
 * ## The Masoretic ⇄ Septuagint correspondence (canonical = Masoretic)
 *
 * | Masoretic | Septuagint/Vulgate | Note                                   |
 * |-----------|--------------------|----------------------------------------|
 * | 1–8       | 1–8                | identical                              |
 * | 9, 10     | 9                  | LXX **joins** Masoretic 9 + 10         |
 * | 11–113    | 10–112             | LXX = Masoretic − 1                     |
 * | 114, 115  | 113                | LXX **joins** Masoretic 114 + 115      |
 * | 116       | 114 (+115)         | LXX **splits** Masoretic 116           |
 * | 117–146   | 116–145            | LXX = Masoretic − 1                     |
 * | 147       | 146 (+147)         | LXX **splits** Masoretic 147           |
 * | 148–150   | 148–150            | identical                              |
 *
 * ## Chapter-granularity convention
 *
 * Because some Masoretic psalms map to a single LXX number (joins) and some LXX
 * numbers arise from splits, a chapter-to-chapter function cannot be a perfect
 * bijection. We follow the widely-used simplified convention:
 *
 * - [toDisplay] maps each **join** to its single LXX number, and each **split**
 *   to the LXX **first part** (116 → 114, 147 → 146).
 * - [toCanonical] is the inverse over the *principal* branch (see below). For the
 *   two LXX display numbers that only ever arise as split second-parts (115 and
 *   147), it maps to the originating Masoretic psalm (116 and 147 respectively),
 *   matching the real-world split semantics.
 *
 * ## Round-trip guarantee (the tested correctness property)
 *
 * `toCanonical(SEPTUAGINT, toDisplay(SEPTUAGINT, m)) == m` holds for every
 * Masoretic psalm `m` in the **principal domain**:
 *
 *     1..150  EXCEPT  { 10, 115 }
 *
 * The two excluded values are the *second members* of the LXX joins (Masoretic 10
 * joins into LXX 9; Masoretic 115 joins into LXX 113). Both collapse onto a shared
 * LXX number whose canonical inverse is, by convention, the *first* member of the
 * join (9 → 9 and 113 → 114), so the round-trip cannot recover them. This is
 * inherent to the join and is documented, not a defect.
 *
 * For [NumberingScheme.MASORETIC] both directions are the identity.
 *
 * Pure Kotlin — no Android dependencies.
 */
object PsalmNumberingMapper {

    /** Valid Psalm chapter numbers in either numbering scheme. */
    private val PSALM_RANGE = 1..150

    /**
     * Masoretic psalm numbers for which the Septuagint round-trip is NOT an
     * identity, because they are the second members of an LXX join. Exposed so
     * tests (and callers) can reason about the exact invertible domain.
     */
    val SEPTUAGINT_NON_INVERTIBLE_MASORETIC: Set<Int> = setOf(10, 115)

    /**
     * Converts a canonical (Masoretic) Psalm number to the number to display
     * under [scheme].
     *
     * @param scheme the active numbering scheme.
     * @param masoreticPsalm the canonical Psalm number, in 1..150.
     * @return the Psalm number to display.
     * @throws IllegalArgumentException if [masoreticPsalm] is outside 1..150.
     */
    fun toDisplay(scheme: NumberingScheme, masoreticPsalm: Int): Int {
        require(masoreticPsalm in PSALM_RANGE) {
            "Psalm number out of range (1..150): $masoreticPsalm"
        }
        return when (scheme) {
            NumberingScheme.MASORETIC -> masoreticPsalm
            NumberingScheme.SEPTUAGINT -> when (masoreticPsalm) {
                in 1..8 -> masoreticPsalm        // identical
                in 9..10 -> 9                    // join: Masoretic 9 + 10 → LXX 9
                in 11..113 -> masoreticPsalm - 1 // offset by one
                in 114..115 -> 113               // join: Masoretic 114 + 115 → LXX 113
                116 -> 114                       // split: first part of Masoretic 116
                in 117..146 -> masoreticPsalm - 1
                147 -> 146                       // split: first part of Masoretic 147
                else -> masoreticPsalm           // 148..150 identical
            }
        }
    }

    /**
     * Converts a displayed Psalm number under [scheme] back to the canonical
     * (Masoretic) Psalm number used for storage.
     *
     * This is the left-inverse of [toDisplay] over the principal domain
     * (see the class docs and [SEPTUAGINT_NON_INVERTIBLE_MASORETIC]).
     *
     * @param scheme the active numbering scheme.
     * @param displayPsalm the displayed Psalm number, in 1..150.
     * @return the canonical (Masoretic) Psalm number.
     * @throws IllegalArgumentException if [displayPsalm] is outside 1..150.
     */
    fun toCanonical(scheme: NumberingScheme, displayPsalm: Int): Int {
        require(displayPsalm in PSALM_RANGE) {
            "Psalm number out of range (1..150): $displayPsalm"
        }
        return when (scheme) {
            NumberingScheme.MASORETIC -> displayPsalm
            NumberingScheme.SEPTUAGINT -> when (displayPsalm) {
                in 1..8 -> displayPsalm          // identical
                9 -> 9                           // principal of the 9/10 join
                in 10..112 -> displayPsalm + 1   // offset by one
                113 -> 114                        // principal of the 114/115 join
                114 -> 116                        // first split part of Masoretic 116
                115 -> 116                        // second split part of Masoretic 116
                in 116..145 -> displayPsalm + 1
                146 -> 147                        // first split part of Masoretic 147
                147 -> 147                        // second split part of Masoretic 147
                else -> displayPsalm              // 148..150 identical
            }
        }
    }
}
