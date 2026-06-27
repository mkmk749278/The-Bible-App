package com.manna.bible.ui.church

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.runComposeUiTest
import com.manna.bible.data.liturgy.LiturgyArbs
import com.manna.bible.domain.liturgy.Liturgy
import com.manna.bible.ui.theme.MannaTheme
import io.kotest.common.ExperimentalKotest
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.of
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.extension.ExtendWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import tech.apter.junit.jupiter.robolectric.RobolectricExtension
import java.util.concurrent.TimeUnit

/**
 * Render-required property test for [LiturgyDetailContent] (Property 9 only), rendered under
 * Robolectric/Compose on a large virtual screen so the whole order composes (no off-screen
 * virtualization).
 *
 * Per design.md > Testing Strategy:
 * - This is the only Liturgy_Detail property that genuinely needs a rendered tree (it asserts
 *   the resolved content actually appears in the UI). The pure-decision facets of Properties
 *   8, 10, 12, and 13 were moved to [LiturgyDetailLogicPropertyTest] (pure JVM, >=100 iters).
 * - It runs at the reduced **20-iteration** budget for rendering properties.
 * - **Hang-safety:** it carries a per-test [Timeout] so a stuck render fails fast instead of
 *   wedging CI, uses [GraphicsMode] NATIVE, and uses **bounded** waits (a `waitUntil` with an
 *   explicit timeout) rather than an unbounded `waitForIdle`, which is the call that can block
 *   forever on an idle/frame signal that never arrives in a headless CI runner.
 *
 * The `@Test` method uses a `void` block body (not an expression body) because the
 * `tech.apter` Robolectric extension runs it through a JUnit4 runner that requires `void`.
 */
@ExtendWith(RobolectricExtension::class)
@OptIn(ExperimentalKotest::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w2000dp-h4000dp")
class LiturgyDetailRenderPropertyTest {

    /** A language tag to resolve content in — authored ones plus one that forces fallback. */
    private val langArb: Arb<String> = Arb.of("en", "ta", "te", "hi", "ml", "fr")

    @OptIn(ExperimentalTestApi::class)
    @Test
    @Timeout(60, unit = TimeUnit.SECONDS)
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
                    // Bounded wait: block only until the first section heading for this
                    // generated liturgy is composed, with an explicit timeout so a stuck
                    // render fails fast instead of hanging on an idle signal that never comes.
                    val firstHeading = liturgy.sections.first().title.resolve(lang)
                    waitUntil(timeoutMillis = 5_000) {
                        onAllNodesWithText(firstHeading, substring = true).fetchSemanticsNodes().isNotEmpty()
                    }
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
}
