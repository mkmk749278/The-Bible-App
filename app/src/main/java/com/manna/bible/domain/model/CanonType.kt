package com.manna.bible.domain.model

/**
 * The set of biblical books associated with a [Denomination].
 *
 * Pure Kotlin — no Android dependencies.
 */
enum class CanonType(val id: String) {
    PROTESTANT_66("protestant_66"),
    CATHOLIC_73("catholic_73"),
    ORTHODOX_EXPANDED("orthodox_expanded"),
    ALL_CANONS("all_canons");

    companion object {
        /**
         * Resolves a [CanonType] from its persisted [id], or null if no match exists.
         */
        fun fromId(id: String): CanonType? = entries.firstOrNull { it.id == id }
    }
}
