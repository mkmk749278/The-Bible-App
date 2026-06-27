package com.manna.bible.ui.church

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.runComposeUiTest
import com.manna.bible.ui.theme.MannaTheme
import io.kotest.common.ExperimentalKotest
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.alphanumeric
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.robolectric.annotation.Config
import tech.apter.junit.jupiter.robolectric.RobolectricExtension

/**
 * Rendering property test for the Liturgy Library list, rendered under Robolectric/Compose.
 *
 * Sets the content once and drives the generated states through recomposition (fast),
 * rather than re-creating the Compose environment per iteration. As a RENDERING property
 * test it runs at the design's reduced 20-iteration budget.
 */
@ExtendWith(RobolectricExtension::class)
@OptIn(ExperimentalKotest::class)
@Config(sdk = [34], qualifiers = "w2000dp-h4000dp")
class LiturgyLibraryRenderPropertyTest {

    private val token: Arb<String> = Arb.string(1, 10, Codepoint.alphanumeric())

    /** A library state with a non-empty list of entries carrying unique ids + non-blank text. */
    private val stateArb: Arb<LiturgyLibraryUiState> = arbitrary {
        val count = Arb.int(1..5).bind()
        val hasMapping = Arb.boolean().bind()
        val entries = (0 until count).map { i ->
            LiturgyListItem(
                id = "id_$i",
                title = "T_${i}_${token.bind()}",
                tradition = "Tr_${i}_${token.bind()}"
            )
        }
        LiturgyLibraryUiState(isLoading = false, entries = entries, denominationHasMapping = hasMapping)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `every library row contains its title and tradition`() {
        // Feature: mass-liturgy-and-localization, Property 9: For any listed entry in Liturgy_Library, the row contains its title and tradition.
        runComposeUiTest {
            val stateHolder = mutableStateOf(LiturgyLibraryUiState(isLoading = false))
            setContent {
                MannaTheme {
                    LiturgyLibraryContent(
                        state = stateHolder.value,
                        bibleLanguage = "en",
                        simplified = false,
                        onBack = {},
                        onOpenLiturgy = {}
                    )
                }
            }
            runBlocking {
                checkAll(PropTestConfig(iterations = 20), stateArb) { state ->
                    stateHolder.value = state
                    waitForIdle()
                    state.entries.forEach { entry ->
                        assertTrue(
                            onAllNodesWithText(entry.title).fetchSemanticsNodes().isNotEmpty(),
                            "row for ${entry.id} is missing its title '${entry.title}'"
                        )
                        assertTrue(
                            onAllNodesWithText(entry.tradition).fetchSemanticsNodes().isNotEmpty(),
                            "row for ${entry.id} is missing its tradition '${entry.tradition}'"
                        )
                    }
                }
            }
        }
    }
}
