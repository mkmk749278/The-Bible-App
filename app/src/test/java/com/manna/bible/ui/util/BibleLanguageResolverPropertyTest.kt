package com.manna.bible.ui.util

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.manna.bible.R
import io.kotest.common.ExperimentalKotest
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.of
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.robolectric.annotation.Config
import tech.apter.junit.jupiter.robolectric.RobolectricExtension

/**
 * Rendering property test for the blank-tag behavior of the String_Resolver
 * (Requirement 2.3). Runs under Robolectric/Compose at the design's reduced 20-iteration
 * budget for rendering properties.
 */
@ExtendWith(RobolectricExtension::class)
@OptIn(ExperimentalKotest::class)
@Config(sdk = [34])
class BibleLanguageResolverPropertyTest {

    private val ids = listOf(
        R.string.crisis_title,
        R.string.sermon_title,
        R.string.card_title,
        R.string.verse_rec_title,
        R.string.explain_action,
        R.string.church_source,
        R.string.nav_home
    )

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `resolving with a blank tag matches resolving against the UI locale`() {
        // Feature: mass-liturgy-and-localization, Property 18: For any string resource, resolving it with a blank language tag produces the same value as resolving against the UI locale (the resolver returns the base context unchanged for a blank tag).
        runComposeUiTest {
            val idState = mutableStateOf(ids.first())
            // Captured outside composition; not read during composition, so no recompose loop.
            val captured = arrayOf("", "")
            setContent {
                val id = idState.value
                captured[0] = stringResourceIn("", id)
                captured[1] = stringResource(id)
            }
            runBlocking {
                checkAll(PropTestConfig(iterations = 20), Arb.of(ids)) { id ->
                    idState.value = id
                    waitForIdle()
                    assertEquals(
                        captured[1], captured[0],
                        "blank-tag resolution must equal the UI-locale value for id $id"
                    )
                }
            }
        }
    }
}
