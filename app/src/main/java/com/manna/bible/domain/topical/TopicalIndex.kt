package com.manna.bible.domain.topical

import com.manna.bible.domain.usecase.ReadingRef

/**
 * A browsable Bible topic (e.g. "Love", "Fear & Anxiety").
 *
 * Topics are the entry points of the offline topical search index: the user picks
 * a topic and is shown a curated set of well-known verses about it.
 *
 * @property id Stable, lowercase, machine-readable identifier (e.g. "love",
 *   "fear"). Unique within a [TopicalIndex] and safe to persist or pass in routes.
 * @property label Human-readable display name (e.g. "Love", "Fear & Anxiety").
 *   Localization of the label is handled at the presentation layer.
 *
 * Pure Kotlin — no Android dependencies.
 */
data class Topic(
    val id: String,
    val label: String
)

/**
 * Offline topical search index: a curated mapping from a small set of common life
 * topics to well-known Bible verses about each topic.
 *
 * This is a Phase 1 / Phase 3 feature enabling "topical search" without a network
 * connection or on-device model. Every verse reference is drawn from the 66-book
 * Protestant canon (USFM 3-letter UPPERCASE book ids), so it resolves in every
 * supported translation.
 *
 * Pure Kotlin — no Android dependencies.
 */
interface TopicalIndex {

    /** Returns all available topics, in a stable curated display order. */
    fun topics(): List<Topic>

    /**
     * Returns the curated verse references for [topicId].
     *
     * Returns an empty list when [topicId] is unknown, so callers can treat an
     * unrecognized topic as "no results" without special-casing.
     */
    fun versesFor(topicId: String): List<ReadingRef>
}
