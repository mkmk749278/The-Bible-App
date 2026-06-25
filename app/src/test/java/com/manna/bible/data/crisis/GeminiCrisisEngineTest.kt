package com.manna.bible.data.crisis

import com.manna.bible.data.remote.GeminiApi
import com.manna.bible.data.remote.GeminiCandidateDto
import com.manna.bible.data.remote.GeminiContentDto
import com.manna.bible.data.remote.GeminiPartDto
import com.manna.bible.data.remote.GeminiRequestDto
import com.manna.bible.data.remote.GeminiResponseDto
import com.manna.bible.domain.crisis.CrisisAiResult
import com.manna.bible.domain.model.Denomination
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/**
 * Unit tests for [GeminiCrisisEngine] — response parsing, failure mapping, prompt
 * construction, and the privacy invariant that the situation text never leaks into a
 * result. Uses a hand-rolled [FakeApi] returning canned DTOs (no Mockito), mirroring
 * the style of [com.manna.bible.data.explain.GeminiExplanationEngineTest].
 */
class GeminiCrisisEngineTest {

    private fun successDto(text: String) = GeminiResponseDto(
        candidates = listOf(GeminiCandidateDto(GeminiContentDto(listOf(GeminiPartDto(text)))))
    )

    @Test
    @DisplayName("A blank key is not configured and returns Unavailable")
    fun unconfigured() = runTest {
        val engine = GeminiCrisisEngine(FakeApi { successDto("REF: PSA.34.18") }, apiKey = "")
        assertFalse(engine.isConfigured)
        val result = engine.respond("anything", "en", isNight = false, denomination = null)
        assertInstanceOf(CrisisAiResult.Unavailable::class.java, result)
    }

    @Test
    @DisplayName("A well-formed three-line reply is parsed into Success")
    fun validResponse() = runTest {
        val engine = GeminiCrisisEngine(
            FakeApi {
                successDto(
                    """
                    REF: PSA.34.18
                    DISPLAY: Psalm 34:18
                    MESSAGE: God is close to the broken-hearted.
                    """.trimIndent()
                )
            },
            apiKey = "key"
        )
        val result = engine.respond("I am grieving", "en", isNight = false, denomination = null)
        assertEquals(
            CrisisAiResult.Success(
                passageRef = "Psalm 34:18",
                osisRef = "PSA.34.18",
                explanation = "God is close to the broken-hearted."
            ),
            result
        )
    }

    @Test
    @DisplayName("A reply missing the REF line returns Unavailable with a parse message")
    fun missingRef() = runTest {
        val engine = GeminiCrisisEngine(
            FakeApi {
                successDto(
                    """
                    DISPLAY: Psalm 34:18
                    MESSAGE: God is close to the broken-hearted.
                    """.trimIndent()
                )
            },
            apiKey = "key"
        )
        val result = engine.respond("I am grieving", "en", isNight = false, denomination = null)
        assertInstanceOf(CrisisAiResult.Unavailable::class.java, result)
        result as CrisisAiResult.Unavailable
        assertTrue(result.reason.contains("REF"), "reason should name the missing line")
    }

    @Test
    @DisplayName("A network failure maps to Offline")
    fun offline() = runTest {
        val engine = GeminiCrisisEngine(FakeApi { throw IOException("no net") }, apiKey = "key")
        val result = engine.respond("I am grieving", "en", isNight = false, denomination = null)
        assertEquals(CrisisAiResult.Offline, result)
    }

    @Test
    @DisplayName("An HTTP error maps to Unavailable carrying only the status code")
    fun httpError() = runTest {
        val engine = GeminiCrisisEngine(
            FakeApi {
                val body = "blocked".toResponseBody("application/json".toMediaTypeOrNull())
                throw HttpException(Response.error<GeminiResponseDto>(403, body))
            },
            apiKey = "key"
        )
        val result = engine.respond("secret crisis text", "en", isNight = false, denomination = null)
        assertInstanceOf(CrisisAiResult.Unavailable::class.java, result)
        result as CrisisAiResult.Unavailable
        assertTrue(result.reason.contains("403"))
        assertFalse(result.reason.contains("secret crisis text"))
    }

    @Test
    @DisplayName("isNight=true prefaces the prompt with the night-window instruction")
    fun nightPrefix() = runTest {
        val capture = CapturingApi { successDto("REF: PSA.4.8\nDISPLAY: Psalm 4:8\nMESSAGE: rest") }
        val engine = GeminiCrisisEngine(capture, apiKey = "key")
        engine.respond("I can't sleep", "en", isNight = true, denomination = null)
        assertTrue(
            capture.lastPrompt.orEmpty().contains("middle of the night", ignoreCase = true),
            "night prompt should mention the night window"
        )
    }

    @Test
    @DisplayName("A day prompt does not carry the night-window instruction")
    fun dayHasNoNightPrefix() = runTest {
        val capture = CapturingApi { successDto("REF: PSA.4.8\nDISPLAY: Psalm 4:8\nMESSAGE: rest") }
        val engine = GeminiCrisisEngine(capture, apiKey = "key")
        engine.respond("I can't sleep", "en", isNight = false, denomination = null)
        assertFalse(capture.lastPrompt.orEmpty().contains("middle of the night", ignoreCase = true))
    }

    @Test
    @DisplayName("The Catholic tradition is named in the prompt")
    fun denominationInPrompt() = runTest {
        val capture = CapturingApi { successDto("REF: PSA.4.8\nDISPLAY: Psalm 4:8\nMESSAGE: rest") }
        val engine = GeminiCrisisEngine(capture, apiKey = "key")
        engine.respond("I am afraid", "en", isNight = false, denomination = Denomination.CATHOLIC)
        assertTrue(capture.lastPrompt.orEmpty().contains("catholic", ignoreCase = true))
    }

    @Test
    @DisplayName("The situation text never appears in the Unavailable reason on parse failure")
    fun situationNotLeakedOnParseFailure() = runTest {
        val secret = "my husband hit me tonight and I am terrified"
        val engine = GeminiCrisisEngine(
            FakeApi { successDto("this is not the expected format at all") },
            apiKey = "key"
        )
        val result = engine.respond(secret, "ml", isNight = true, denomination = null)
        assertInstanceOf(CrisisAiResult.Unavailable::class.java, result)
        result as CrisisAiResult.Unavailable
        assertFalse(result.reason.contains(secret), "situation text must never leak into the reason")
    }

    private class FakeApi(
        private val behavior: suspend () -> GeminiResponseDto
    ) : GeminiApi {
        override suspend fun generate(
            model: String,
            key: String,
            body: GeminiRequestDto
        ): GeminiResponseDto = behavior()
    }

    /** Captures the prompt text sent in the request body for assertion. */
    private class CapturingApi(
        private val behavior: suspend () -> GeminiResponseDto
    ) : GeminiApi {
        var lastPrompt: String? = null
            private set

        override suspend fun generate(
            model: String,
            key: String,
            body: GeminiRequestDto
        ): GeminiResponseDto {
            lastPrompt = body.contents.firstOrNull()?.parts?.firstOrNull()?.text
            return behavior()
        }
    }
}
