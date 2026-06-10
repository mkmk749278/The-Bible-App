package com.manna.bible.domain.lectionary

import com.manna.bible.domain.model.Denomination
import javax.inject.Inject

/**
 * Default [LectionaryProvider] implementation.
 *
 * Maps each [Denomination] to its lectionary id (Requirements 9 and 18):
 * - CSI uses the CSI Almanac (Requirement 9.2).
 * - Catholic uses the Roman Catholic liturgical calendar (Requirement 9.3).
 * - Orthodox uses the Orthodox calendar.
 * - Mar Thoma and other Protestant traditions fall back to a general lectionary.
 * - "Show everything" has no specific lectionary, so the user proceeds without
 *   one (Requirement 9.5), represented as `null`.
 *
 * Pure Kotlin — depends only on `javax.inject` for constructor injection.
 */
class DefaultLectionaryProvider @Inject constructor() : LectionaryProvider {

    override fun lectionaryIdFor(denomination: Denomination): String? =
        when (denomination) {
            Denomination.CSI -> CSI_ALMANAC
            Denomination.CATHOLIC -> RC_CALENDAR
            Denomination.ORTHODOX -> ORTHODOX_CALENDAR
            Denomination.MAR_THOMA -> GENERAL_LECTIONARY
            Denomination.PROTESTANT_OTHER -> GENERAL_LECTIONARY
            Denomination.SHOW_EVERYTHING -> null
        }

    companion object {
        /** CSI Almanac lectionary (Requirement 9.2). */
        const val CSI_ALMANAC: String = "csi_almanac"

        /** Roman Catholic liturgical calendar (Requirement 9.3). */
        const val RC_CALENDAR: String = "rc_calendar"

        /** Orthodox liturgical calendar. */
        const val ORTHODOX_CALENDAR: String = "orthodox_calendar"

        /** General / default lectionary for traditions without a specific one. */
        const val GENERAL_LECTIONARY: String = "general_lectionary"
    }
}
