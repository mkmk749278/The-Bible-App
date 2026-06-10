package com.manna.bible.domain.lectionary

import com.manna.bible.domain.model.Denomination

/**
 * Resolves the lectionary associated with a chosen [Denomination].
 *
 * The setup flow uses this to pre-select / offer the tradition's liturgical
 * calendar (Requirement 9) and to drive denomination-aware daily readings
 * (Requirement 18). When a denomination has no specific lectionary, the user is
 * still allowed to proceed without one (Requirement 9.5), which is represented
 * here by a `null` result.
 *
 * Pure domain logic with no Android dependencies so it can be exercised in JVM
 * unit tests without an emulator.
 */
interface LectionaryProvider {

    /**
     * Returns the lectionary id for [denomination], or `null` when no specific
     * lectionary applies and the user may proceed without one (Requirement 9.5).
     */
    fun lectionaryIdFor(denomination: Denomination): String?
}
