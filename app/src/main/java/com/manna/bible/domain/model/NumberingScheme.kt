package com.manna.bible.domain.model

/**
 * The Psalm and verse numbering convention.
 *
 * - [MASORETIC] — Protestant numbering.
 * - [SEPTUAGINT] — Catholic/Orthodox (Septuagint/Vulgate) numbering.
 *
 * Pure Kotlin — no Android dependencies.
 */
enum class NumberingScheme {
    MASORETIC,
    SEPTUAGINT
}
