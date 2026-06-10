package com.manna.bible.domain.model

/**
 * The user's selected Christian tradition.
 *
 * The chosen [Denomination] drives the derived [CanonProfile] (book set, ordering,
 * numbering scheme, naming convention, suggested translation, and lectionary).
 *
 * Pure Kotlin — no Android dependencies.
 */
enum class Denomination(val id: String) {
    CATHOLIC("catholic"),
    CSI("csi"),
    PROTESTANT_OTHER("protestant_other"),
    ORTHODOX("orthodox"),
    MAR_THOMA("mar_thoma"),
    SHOW_EVERYTHING("show_everything");

    companion object {
        /**
         * Resolves a [Denomination] from its persisted [id], or null if no match exists.
         */
        fun fromId(id: String): Denomination? = entries.firstOrNull { it.id == id }
    }
}
