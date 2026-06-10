package com.manna.bible.domain.model

/**
 * A single book within a canon.
 *
 * @property osisId Stable OSIS-style book identifier (e.g. "GEN", "TOB").
 * @property testament Whether the book belongs to the Old or New Testament.
 * @property orderIndex Position of the book within the active canon's ordering.
 * @property isDeuterocanonical True if the book is a deuterocanonical book.
 *
 * Pure Kotlin — no Android dependencies.
 */
data class CanonBook(
    val osisId: String,
    val testament: Testament,
    val orderIndex: Int,
    val isDeuterocanonical: Boolean
)
