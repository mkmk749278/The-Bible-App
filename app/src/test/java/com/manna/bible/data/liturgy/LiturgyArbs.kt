package com.manna.bible.data.liturgy

import com.manna.bible.domain.liturgy.Liturgy
import com.manna.bible.domain.liturgy.LiturgyPart
import com.manna.bible.domain.liturgy.LiturgyRole
import com.manna.bible.domain.liturgy.LiturgySection
import com.manna.bible.domain.liturgy.LocalizedText
import com.manna.bible.domain.model.Denomination
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.alphanumeric
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.set
import io.kotest.property.arbitrary.string

/**
 * Shared Kotest generators for liturgy property tests.
 *
 * Every generated [Liturgy] is **valid**: its declared `languages` set equals the set of
 * languages authored across its parts' localized fields, every [LocalizedText] carries an
 * `en` value, and ids/traditions/titles/source notes are non-blank — so serialize→parse
 * round-trips never trip schema validation. Generators intentionally constrain to the real
 * input space rather than emitting arbitrary noise.
 */
object LiturgyArbs {

    /** Non-blank alphanumeric token (no whitespace / control / JSON-breaking characters). */
    private val token: Arb<String> = Arb.string(1, 12, Codepoint.alphanumeric())

    /** The vernacular Bible languages that may accompany the always-present English. */
    private val vernacularLangs = listOf("ta", "te", "hi", "ml")

    /** A language set always containing "en" plus a random subset of the vernaculars. */
    val langSet: Arb<Set<String>> =
        Arb.set(Arb.of(vernacularLangs), 0..vernacularLangs.size).let { extras ->
            arbitrary { setOf("en") + extras.bind() }
        }

    /** A [LocalizedText] authored in exactly [langs] (each value non-blank). */
    private fun localized(langs: Set<String>): Arb<LocalizedText> = arbitrary {
        LocalizedText(langs.associateWith { "v_${token.bind()}" })
    }

    /**
     * A part with a random role, a guaranteed-present set of localized fields (all authored
     * in exactly [langs] so the liturgy's present-languages equal its declared set), and a
     * randomly-present `osisRef` / `needsOfficialText`.
     */
    private fun part(langs: Set<String>): Arb<LiturgyPart> = arbitrary {
        val role = Arb.enum<LiturgyRole>().bind()
        val includeTitle = Arb.boolean().bind()
        val includeText = Arb.boolean().bind()
        val includeRubric = Arb.boolean().bind()
        val title = if (includeTitle) localized(langs).bind() else null
        var text = if (includeText) localized(langs).bind() else null
        val rubric = if (includeRubric) localized(langs).bind() else null
        // Guarantee at least one localized field is present so present-languages cover `langs`.
        if (title == null && text == null && rubric == null) text = localized(langs).bind()
        val osisRef = Arb.of<String?>(null, "1CO.11.23", "JHN.3.16", "PSA.23.1").bind()
        val needsOfficialText = Arb.boolean().bind()
        LiturgyPart(role, title, text, rubric, osisRef, needsOfficialText)
    }

    private fun section(langs: Set<String>): Arb<LiturgySection> = arbitrary {
        LiturgySection(localized(langs).bind(), Arb.list(part(langs), 1..5).bind())
    }

    /** A complete, schema-valid [Liturgy]. */
    val liturgy: Arb<Liturgy> = arbitrary {
        val langs = langSet.bind()
        Liturgy(
            id = "lit_${token.bind()}",
            title = localized(langs).bind(),
            tradition = "trad_${token.bind()}",
            sections = Arb.list(section(langs), 1..4).bind(),
            sourceNote = localized(langs).bind(),
            denominations = Arb.set(Arb.enum<Denomination>(), 0..3).bind().toList(),
            languages = langs
        )
    }
}
