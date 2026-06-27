package com.manna.bible.ui.church

import com.manna.bible.data.liturgy.LiturgyArbs
import com.manna.bible.domain.liturgy.LiturgyPart
import com.manna.bible.domain.liturgy.LiturgyRole
import com.manna.bible.domain.liturgy.LocalizedText
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.alphanumeric
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Pure-JVM property tests for the decision logic behind the Liturgy_Detail rendering
 * properties 8, 10, 12, and 13. Per design.md > Testing Strategy > "Verification level for
 * rendering properties", each of these properties is pure decision-making, so the decision
 * is extracted into a JVM-testable production function — [LocalizedText.resolve],
 * [liturgyRoleLabelRes], [shouldShowOfficialNotice], [openVerseRefFor] — that the composable
 * ([LiturgyDetailContent]/[PartView]) consumes directly. These tests exercise those same
 * functions at >=100 iterations on the JVM (no Compose render, no Robolectric, no emulator),
 * which both runs far faster and cannot hang on a Compose idle/frame signal. The "actually
 * shown in the UI" facet of these properties is covered by the Property 9 render test.
 */
class LiturgyDetailLogicPropertyTest {

    /** Authored content languages plus one ("fr") that is never authored, to force fallback. */
    private val langArb: Arb<String> = Arb.of("en", "ta", "te", "hi", "ml", "fr")

    private val token: Arb<String> = Arb.string(1, 12, Codepoint.alphanumeric())

    private fun localized(): Arb<LocalizedText> = arbitrary {
        LocalizedText(mapOf("en" to "v_${token.bind()}"))
    }

    /**
     * An arbitrary part: random role, independently-present title/text/rubric, an optional
     * osisRef drawn from real OSIS shapes plus null, and a random needsOfficialText flag.
     * This intentionally generates the bare-RUBRIC + needsOfficialText combination (RUBRIC
     * role, no title/text, needsOfficialText = true) — the case that previously dropped the
     * official-text notice on the rubric fast-path.
     */
    private val partArb: Arb<LiturgyPart> = arbitrary {
        val role = Arb.enum<LiturgyRole>().bind()
        val title = if (Arb.boolean().bind()) localized().bind() else null
        val text = if (Arb.boolean().bind()) localized().bind() else null
        val rubric = if (Arb.boolean().bind()) localized().bind() else null
        val osisRef = Arb.of<String?>(null, "1CO.11.23", "JHN.3.16", "PSA.23.1").bind()
        val needsOfficialText = Arb.boolean().bind()
        LiturgyPart(role, title, text, rubric, osisRef, needsOfficialText)
    }

    @Test
    fun `resolved source note is non-blank for any valid liturgy and language`(): Unit = runBlocking {
        // Feature: mass-liturgy-and-localization, Property 8: For any valid Liturgy, its resolved source note is non-blank and appears on the rendered Liturgy_Detail.
        // Pure facet (>=100 iters): the resolved source-note value is always non-blank for any
        // requested language (English fallback when the tag is unauthored). The "appears on
        // Liturgy_Detail" facet is covered by the Property 9 render test.
        checkAll(200, LiturgyArbs.liturgy, langArb) { liturgy, lang ->
            val resolved = liturgy.sourceNote.resolve(lang)
            assertTrue(resolved.isNotBlank(), "resolved source note must be non-blank for tag '$lang'")
        }
    }

    @Test
    fun `a speaker label key exists exactly for speaking roles and never for rubrics`(): Unit = runBlocking {
        // Feature: mass-liturgy-and-localization, Property 10: For any Liturgy_Part, a speaker role label is assigned exactly when the role is PRESIDER, PEOPLE, ALL, or READER; a RUBRIC part receives no speaker label and is styled as an instruction, regardless of role-referencing phrasing in its text.
        // Pure facet (>=100 iters): liturgyRoleLabelRes — the function PartView consumes to
        // decide whether to draw a speaker label — returns a non-null string-resource key for
        // exactly the four speaking roles and null for RUBRIC, independent of any text/phrasing.
        checkAll(200, partArb) { part ->
            val labelRes = liturgyRoleLabelRes(part.role)
            when (part.role) {
                LiturgyRole.PRESIDER, LiturgyRole.PEOPLE, LiturgyRole.ALL, LiturgyRole.READER ->
                    assertTrue(labelRes != null, "speaking role ${part.role} must have a label key")
                LiturgyRole.RUBRIC ->
                    assertNull(labelRes, "RUBRIC must have no speaker label key")
            }
        }
    }

    @Test
    fun `official notice is shown exactly for parts that need official text`(): Unit = runBlocking {
        // Feature: mass-liturgy-and-localization, Property 12: For any Liturgy_Part with needsOfficialText = true, Liturgy_Detail displays the official-text notice directing the user to the parish book, and does not present a fabricated proper-prayer body as authoritative text.
        // Pure facet (>=100 iters): shouldShowOfficialNotice — the function PartView consumes
        // to decide whether to draw the parish-book notice — is true iff part.needsOfficialText,
        // for EVERY role including a bare RUBRIC (so the notice is never dropped). The app shows
        // only this notice and never a fabricated proper-prayer body. The "rendered" facet is
        // covered by the Property 9 render test.
        // Regression guard: a bare RUBRIC that needs official text still triggers the notice.
        val bareRubricNeedingOfficial = LiturgyPart(
            role = LiturgyRole.RUBRIC,
            rubric = LocalizedText(mapOf("en" to "The Priest sings the Collect proper to the day.")),
            needsOfficialText = true
        )
        assertTrue(
            shouldShowOfficialNotice(bareRubricNeedingOfficial),
            "a bare RUBRIC part with needsOfficialText must still show the official notice"
        )
        checkAll(200, partArb) { part ->
            assertEquals(
                part.needsOfficialText,
                shouldShowOfficialNotice(part),
                "official-notice decision must equal needsOfficialText for role ${part.role}"
            )
        }
    }

    @Test
    fun `the open-verse action ref equals the part osisRef`(): Unit = runBlocking {
        // Feature: mass-liturgy-and-localization, Property 13: For any Liturgy_Part with a non-null osisRef, Liturgy_Detail exposes an action that, when invoked, calls onOpenVerse with exactly that osisRef.
        // Pure facet (>=100 iters): openVerseRefFor — the function ReadInContextAction consumes
        // to wire onOpenVerse — returns exactly part.osisRef, so the action (shown only when the
        // ref is non-null) opens exactly that reference. The click-through facet is covered by
        // the Property 9 render test rendering the action when a ref is present.
        checkAll(200, partArb) { part ->
            assertEquals(
                part.osisRef,
                openVerseRefFor(part),
                "open-verse action ref must equal the part's osisRef"
            )
            if (part.osisRef == null) {
                assertNull(openVerseRefFor(part), "no open-verse action when osisRef is null")
            } else {
                assertFalse(openVerseRefFor(part).isNullOrBlank(), "a present osisRef must open a non-blank ref")
            }
        }
    }
}
