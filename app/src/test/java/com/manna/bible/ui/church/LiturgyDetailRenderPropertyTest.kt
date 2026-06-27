package com.manna.bible.ui.church

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.manna.bible.data.liturgy.LiturgyArbs
import com.manna.bible.domain.liturgy.Liturgy
import com.manna.bible.domain.liturgy.LiturgyPart
import com.manna.bible.domain.liturgy.LiturgyRole
import com.manna.bible.domain.liturgy.LiturgySection
import com.manna.bible.domain.liturgy.LocalizedText
import com.manna.bible.ui.theme.MannaTheme
import io.kotest.common.ExperimentalKotest
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.robolectric.annotation.Config
import tech.apter.junit.jupiter.robolectric.RobolectricExtension

/**
 * Rendering property tests for [LiturgyDetailContent], rendered under Robolectric/Compose
 * on a large virtual screen so the whole order composes (no off-screen virtualization).
 *
 * Each test sets the Compose content **once** and drives the generated inputs through it
 * by mutating observable state (recomposing between iterations) — orders of magnitude
 * faster than standing up a fresh Compose environment per iteration. As RENDERING property
 * tests (each iteration boots Robolectric + renders a Compose tree) they run at the design's
 * reduced 20-iteration budget, while pure-domain properties stay at >=100.
 *
 * The `@Test` methods use a `void` block body (not an expression body) because the
 * `tech.apter` Robolectric extension runs them through a JUnit4 runner that requires `void`.
 */
@ExtendWith(RobolectricExtension::class)
@OptIn(ExperimentalKotest::class)
@Config(sdk = [34], qualifiers = "w2000dp-h4000dp")
class LiturgyDetailRenderPropertyTest {

    // The English role labels (values/strings_church.xml), as resolved when rendering in "en".
    private val presiderLabel = "Priest"
    private val peopleLabel = "People"
    private val allLabel = "All"
    private val readerLabel = "Reader"
    private val readInContext = "Read in context"
    private val officialNotice = "Prayer proper to today — follow your parish book / Missal."

    /** A language tag to resolve content in — authored ones plus one that forces fallback. */
    private val langArb: Arb<String> = Arb.of("en", "ta", "te", "hi", "ml", "fr")

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `the resolved source note is non-blank and rendered`() {
        // Feature: mass-liturgy-and-localization, Property 8: For any valid Liturgy, its resolved source note is non-blank and appears on the rendered Liturgy_Detail.
        runComposeUiTest {
            val liturgyState = mutableStateOf<Liturgy?>(null)
            val langState = mutableStateOf("en")
            setContent {
                MannaTheme {
                    LiturgyDetailContent(liturgyState.value, langState.value, onBack = {}, onOpenVerse = {})
                }
            }
            runBlocking {
                checkAll(PropTestConfig(iterations = 20), LiturgyArbs.liturgy, langArb) { liturgy, lang ->
                    liturgyState.value = liturgy
                    langState.value = lang
                    waitForIdle()
                    val resolvedNote = liturgy.sourceNote.resolve(lang)
                    assertTrue(resolvedNote.isNotBlank(), "source note resolved blank")
                    assertTrue(
                        onAllNodesWithText(resolvedNote, substring = true).fetchSemanticsNodes().isNotEmpty(),
                        "source note '$resolvedNote' not rendered"
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `every section heading, part title, and spoken text is rendered resolved`() {
        // Feature: mass-liturgy-and-localization, Property 9: For any Liturgy rendered in Liturgy_Detail, every section heading contains its resolved title, every titled part displays its resolved title, and every part with spoken text displays its resolved text.
        runComposeUiTest {
            val liturgyState = mutableStateOf<Liturgy?>(null)
            val langState = mutableStateOf("en")
            setContent {
                MannaTheme {
                    LiturgyDetailContent(liturgyState.value, langState.value, onBack = {}, onOpenVerse = {})
                }
            }
            runBlocking {
                checkAll(PropTestConfig(iterations = 20), LiturgyArbs.liturgy, langArb) { liturgy, lang ->
                    liturgyState.value = liturgy
                    langState.value = lang
                    waitForIdle()
                    liturgy.sections.forEach { section ->
                        val heading = section.title.resolve(lang)
                        assertTrue(
                            onAllNodesWithText(heading, substring = true).fetchSemanticsNodes().isNotEmpty(),
                            "section heading '$heading' not rendered"
                        )
                        section.parts.forEach { part ->
                            part.title?.resolve(lang)?.let { title ->
                                assertTrue(
                                    onAllNodesWithText(title, substring = true).fetchSemanticsNodes().isNotEmpty(),
                                    "part title '$title' not rendered"
                                )
                            }
                            part.text?.resolve(lang)?.let { text ->
                                assertTrue(
                                    onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty(),
                                    "part text '$text' not rendered"
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /** A single part wrapped in a minimal one-section liturgy, for the role-label property. */
    private fun singlePartLiturgy(part: LiturgyPart): Liturgy = Liturgy(
        id = "single",
        title = LocalizedText(mapOf("en" to "Order")),
        tradition = "Tradition",
        sections = listOf(LiturgySection(LocalizedText(mapOf("en" to "Section")), listOf(part))),
        sourceNote = LocalizedText(mapOf("en" to "Source")),
        languages = setOf("en")
    )

    private val tok: Arb<String> = Arb.string(1, 8, Codepoint.alphanumeric())

    /**
     * A single part with a random role. RUBRIC parts deliberately sometimes carry
     * role-referencing phrasing in their instruction text, to exercise that a rubric still
     * receives no speaker label.
     */
    private val singlePartArb: Arb<LiturgyPart> = arbitrary {
        val role = Arb.enum<LiturgyRole>().bind()
        if (role == LiturgyRole.RUBRIC) {
            val rolePhrasing = Arb.boolean().bind()
            val rubricText = if (rolePhrasing) {
                "The Priest and the People and All and the Reader proceed"
            } else {
                "instr_${tok.bind()}"
            }
            LiturgyPart(role = role, rubric = LocalizedText(mapOf("en" to rubricText)))
        } else {
            LiturgyPart(role = role, text = LocalizedText(mapOf("en" to "spoken_${tok.bind()}")))
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `a speaker label is shown exactly for speaking roles and never for rubrics`() {
        // Feature: mass-liturgy-and-localization, Property 10: For any Liturgy_Part, a speaker role label is assigned exactly when the role is PRESIDER, PEOPLE, ALL, or READER; a RUBRIC part receives no speaker label and is styled as an instruction, regardless of role-referencing phrasing in its text.
        runComposeUiTest {
            val liturgyState = mutableStateOf<Liturgy?>(null)
            setContent {
                MannaTheme {
                    LiturgyDetailContent(liturgyState.value, "en", onBack = {}, onOpenVerse = {})
                }
            }
            runBlocking {
                checkAll(PropTestConfig(iterations = 20), singlePartArb) { part ->
                    liturgyState.value = singlePartLiturgy(part)
                    waitForIdle()
                    fun shown(label: String) =
                        onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty()
                    val expectedLabel = when (part.role) {
                        LiturgyRole.PRESIDER -> presiderLabel
                        LiturgyRole.PEOPLE -> peopleLabel
                        LiturgyRole.ALL -> allLabel
                        LiturgyRole.READER -> readerLabel
                        LiturgyRole.RUBRIC -> null
                    }
                    if (expectedLabel != null) {
                        assertTrue(shown(expectedLabel), "missing label for role ${part.role}")
                    } else {
                        assertFalse(shown(presiderLabel), "rubric showed Priest label")
                        assertFalse(shown(peopleLabel), "rubric showed People label")
                        assertFalse(shown(allLabel), "rubric showed All label")
                        assertFalse(shown(readerLabel), "rubric showed Reader label")
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `official-text parts show the parish-book notice for each such part`() {
        // Feature: mass-liturgy-and-localization, Property 12: For any Liturgy_Part with needsOfficialText = true, Liturgy_Detail displays the official-text notice directing the user to the parish book, and does not present a fabricated proper-prayer body as authoritative text.
        runComposeUiTest {
            val liturgyState = mutableStateOf<Liturgy?>(null)
            setContent {
                MannaTheme {
                    LiturgyDetailContent(liturgyState.value, "en", onBack = {}, onOpenVerse = {})
                }
            }
            runBlocking {
                checkAll(PropTestConfig(iterations = 20), LiturgyArbs.liturgy) { liturgy ->
                    liturgyState.value = liturgy
                    waitForIdle()
                    val officialCount = liturgy.sections.sumOf { section ->
                        section.parts.count { it.needsOfficialText }
                    }
                    val notices = onAllNodesWithText(officialNotice).fetchSemanticsNodes().size
                    assertEquals(
                        officialCount, notices,
                        "official-text notice count should equal needsOfficialText part count"
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `each scripture reference exposes an action that opens exactly that osisRef`() {
        // Feature: mass-liturgy-and-localization, Property 13: For any Liturgy_Part with a non-null osisRef, Liturgy_Detail exposes an action that, when invoked, calls onOpenVerse with exactly that osisRef.
        runComposeUiTest {
            val liturgyState = mutableStateOf<Liturgy?>(null)
            val captured = mutableListOf<String>()
            setContent {
                MannaTheme {
                    LiturgyDetailContent(
                        liturgyState.value, "en", onBack = {}, onOpenVerse = { captured += it }
                    )
                }
            }
            runBlocking {
                checkAll(PropTestConfig(iterations = 20), LiturgyArbs.liturgy) { liturgy ->
                    captured.clear()
                    liturgyState.value = liturgy
                    waitForIdle()
                    val expectedRefs = liturgy.sections.flatMap { it.parts }.mapNotNull { it.osisRef }
                    val actions = onAllNodesWithText(readInContext)
                    val count = actions.fetchSemanticsNodes().size
                    assertEquals(
                        expectedRefs.size, count,
                        "one read-in-context action expected per osisRef part"
                    )
                    for (i in 0 until count) {
                        actions[i].performClick()
                    }
                    assertEquals(
                        expectedRefs.sorted(), captured.sorted(),
                        "clicking the actions must open exactly the parts' osisRefs"
                    )
                }
            }
        }
    }
}
