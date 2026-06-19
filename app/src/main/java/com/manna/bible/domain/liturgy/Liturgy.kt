package com.manna.bible.domain.liturgy

import com.manna.bible.domain.model.Denomination

/**
 * Who speaks/acts a part of the service. The UI labels each role and colours it, so a
 * worshipper can follow who says what — the presider, the people, everyone together,
 * a reader — or a plain instruction (rubric).
 */
enum class LiturgyRole { PRESIDER, PEOPLE, ALL, READER, RUBRIC }

/**
 * One step of an order of worship.
 *
 * @property role who says or does this part.
 * @property title short heading, e.g. "Greeting", "The Lord's Prayer" (null for a bare rubric).
 * @property text the spoken text, or null for a rubric-only / action step.
 * @property rubric an instruction shown in muted italics, e.g. "All stand".
 * @property osisRef a scripture this part points to, opened in the reader when tapped.
 * @property needsOfficialText true where a presidential prayer's exact words are
 *   proper to the day / the parish book and are intentionally not reproduced here —
 *   the UI flags it so nothing unofficial is presented as the authoritative text.
 */
data class LiturgyPart(
    val role: LiturgyRole,
    val title: String? = null,
    val text: String? = null,
    val rubric: String? = null,
    val osisRef: String? = null,
    val needsOfficialText: Boolean = false
)

/** A major division of the service (e.g. "Introductory Rites"). */
data class LiturgySection(
    val title: String,
    val parts: List<LiturgyPart>
)

/**
 * A complete order of worship for one tradition.
 *
 * Content is **structure + authentic short responses + public-domain ordinary texts**;
 * the longer presidential prayers are left as flagged rubrics so the official book is
 * followed (see [LiturgyPart.needsOfficialText]). [sourceNote] records where the order
 * comes from.
 */
data class Liturgy(
    val id: String,
    val title: String,
    val tradition: String,
    val sections: List<LiturgySection>,
    val sourceNote: String
)

/**
 * Supplies the order(s) of worship the app can guide a congregation through (Church
 * Mode, Phase 3). The order shown is chosen from the tradition the user picked at
 * setup; where a tradition's order isn't available yet the caller can still offer the
 * ones that are.
 *
 * Pure Kotlin — no Android dependencies — so the content is JVM-testable.
 */
interface LiturgyProvider {
    /** Every available order of worship. */
    fun all(): List<Liturgy>

    /** The order that matches [denomination], or null when none is available yet. */
    fun defaultFor(denomination: Denomination): Liturgy?
}
