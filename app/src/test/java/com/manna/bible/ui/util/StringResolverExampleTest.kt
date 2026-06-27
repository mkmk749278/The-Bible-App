package com.manna.bible.ui.util

import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.runComposeUiTest
import com.manna.bible.R
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.extension.ExtendWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import tech.apter.junit.jupiter.robolectric.RobolectricExtension
import java.util.concurrent.TimeUnit

/**
 * Example tests for the String_Resolver (Requirements 1.1, 1.5, 2.1, 2.2): a translated
 * key resolves in the requested Bible language, an untranslated key falls back to the
 * English default, and string-array resolution works. Robolectric/Compose-backed.
 *
 * Hang-safety: explicit [GraphicsMode] NATIVE, matching every other Robolectric test class in
 * the suite. Robolectric does not clean up reliably when consecutive test classes in the same
 * JVM fork switch between NATIVE and the LEGACY default (robolectric/robolectric#8073) — that
 * mismatch, not a slow/stuck test, is what hung CI for 40+ minutes with zero output between
 * this class and the one that ran before it.
 *
 * A second, distinct hang lived inside this class itself: both tests called the **unbounded**
 * `waitForIdle()` after a `setContent` that renders no visible node (it only writes resolved
 * strings into a local map), so there was no frame/idle signal for Robolectric's headless
 * renderer to ever report — `waitForIdle()` could block forever. Fixed the same way as every
 * other Compose/Robolectric test in the suite: emit a marker [Text] node and wait on a
 * **bounded** `waitUntil`, backed by a per-test [Timeout] as a fail-fast safety net.
 */
@ExtendWith(RobolectricExtension::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class StringResolverExampleTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    @Timeout(60, unit = TimeUnit.SECONDS)
    fun `stringResourceIn resolves a values-ta key in Tamil and falls back to English when absent`() {
        runComposeUiTest {
            val captured = HashMap<String, String>()
            setContent {
                // A key translated in values-ta (crisis_title) resolves differently per language.
                captured["ta_translated"] = stringResourceIn("ta", R.string.crisis_title)
                captured["en_translated"] = stringResourceIn("en", R.string.crisis_title)
                // A key with no Tamil value (nav_home) must fall back to the English default.
                captured["ta_fallback"] = stringResourceIn("ta", R.string.nav_home)
                captured["en_fallback"] = stringResourceIn("en", R.string.nav_home)
                // Marker node: setContent above renders nothing visible on its own, so give the
                // bounded waitUntil below an actual node to key on instead of an unbounded wait.
                Text("seen")
            }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("seen").fetchSemanticsNodes().isNotEmpty()
            }

            // Req 1.1 / 2.1: the Tamil resolution differs from English for a translated key.
            assertNotEquals(
                captured["en_translated"], captured["ta_translated"],
                "crisis_title should resolve differently in Tamil vs English"
            )
            assertTrue(
                captured["ta_translated"]!!.isNotBlank(),
                "Tamil crisis_title resolved blank"
            )
            // Req 1.4 / 1.5: untranslated key falls back to the English default value.
            assertEquals(
                captured["en_fallback"], captured["ta_fallback"],
                "nav_home should fall back to the English value under tag 'ta'"
            )
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    @Timeout(60, unit = TimeUnit.SECONDS)
    fun `stringArrayResourceIn resolves a string-array in the requested language`() {
        runComposeUiTest {
            val sizes = mutableStateOf(0 to 0)
            setContent {
                val en = stringArrayResourceIn("en", R.array.calendar_weekdays)
                val ta = stringArrayResourceIn("ta", R.array.calendar_weekdays)
                sizes.value = en.size to ta.size
                // Marker node: see the first test above for why a bounded waitUntil needs one.
                Text("seen")
            }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("seen").fetchSemanticsNodes().isNotEmpty()
            }
            val (enSize, taSize) = sizes.value
            assertTrue(enSize > 0, "English weekday array resolved empty")
            // No Tamil array authored -> falls back to the default array (same size).
            assertEquals(enSize, taSize, "array resolution should fall back to the default")
        }
    }
}
