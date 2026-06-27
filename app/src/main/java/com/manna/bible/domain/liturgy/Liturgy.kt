package com.manna.bible.domain.liturgy

import com.manna.bible.domain.model.Denomination

/**
 * Who speaks/acts a part of the service. The UI labels each role and colours it, so a
 * worshipper can follow who says what — the presider, the people, everyone together,
 * a reader — or a plain instruction (rubric).
 */
enum class LiturgyRole { PRESIDER, PEOPLE, ALL, READER, RUBRIC }

/**
 * Authored, multilingual sacred text. English (`"en"`) is always present; other
 * languages appear only where an authoritative published translation exists — the app
 * never auto-translates liturgical text. [resolve] falls back to English deterministically
 * so a missing vernacular value can never leave the surface blank (Req 8.1/8.2).
 */
data class LocalizedText(private val values: Map<String, String>) {

    init { require(values.containsKey("en")) { "LocalizedText must contain 'en'" } }

    /** The required English value, used as the fallback for any missing language. */
    val english: String get() = values.getValue("en")

    /** The authored value for [languageTag] if present, else the English fallback. */
    fun resolve(languageTag: String): String = values[languageTag] ?: english

    /** The set of language tags this text was authored in (always includes `"en"`). */
    val languages: Set<String> get() = values.keys
}

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
    val title: LocalizedText? = null,
    val text: LocalizedText? = null,
    val rubric: LocalizedText? = null,
    val osisRef: String? = null,
    val needsOfficialText: Boolean = false
)

/** A major division of the service (e.g. "Introductory Rites"). */
data class LiturgySection(
    val title: LocalizedText,
    val parts: List<LiturgyPart>
)

/**
 * A complete order of worship for one tradition.
 *
 * Content is **structure + authentic short responses + public-domain ordinary texts**;
 * the longer presidential prayers are left as flagged rubrics so the official book is
 * followed (see [LiturgyPart.needsOfficialText]). [sourceNote] records where the order
 * comes from.
 *
 * @property denominations the traditions this order is mapped to (for surfacing it first).
 * @property languages the content languages authored across this order (always includes `"en"`).
 */
data class Liturgy(
    val id: String,
    val title: LocalizedText,
    val tradition: String,
    val sections: List<LiturgySection>,
    val sourceNote: LocalizedText,
    val denominations: List<Denomination> = emptyList(),
    val languages: Set<String> = setOf("en")
)

/**
 * Supplies the order(s) of worship the app can guide a congregation through. The order
 * shown is chosen from the tradition the user picked at setup; where a tradition's order
 * isn't available yet the caller can still offer the ones that are.
 *
 * Pure Kotlin — no Android dependencies — so the content is JVM-testable.
 */
interface LiturgyProvider {
    /** Every available order of worship. */
    fun all(): List<Liturgy>

    /** The order explicitly mapped to [denomination], or null when none is mapped/available. */
    fun defaultFor(denomination: Denomination): Liturgy?

    /**
     * Every available order, with the ones mapped to [denomination] surfaced first
     * (Req 5.3, 11.1). The returned list always contains the full library so a user
     * can still browse every order (Req 11.6). The default implementation orders by the
     * single [defaultFor] entry; asset-backed providers override it with a richer mapping.
     */
    fun forDenomination(denomination: Denomination): List<Liturgy> {
        val all = all()
        val default = defaultFor(denomination) ?: return all
        return listOf(default) + all.filter { it.id != default.id }
    }

    /**
     * A non-null selectable order whenever the library is non-empty: the mapped default,
     * else the first available order (Req 11.3, 11.6). Never returns null when [all] is
     * non-empty, so a denomination without an explicit mapping still gets an order.
     */
    fun resolvedDefaultFor(denomination: Denomination): Liturgy? =
        defaultFor(denomination) ?: all().firstOrNull()
}
