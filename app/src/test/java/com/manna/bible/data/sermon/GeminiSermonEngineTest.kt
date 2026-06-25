package com.manna.bible.data.sermon

import com.manna.bible.data.remote.GeminiApi
import com.manna.bible.data.remote.GeminiCandidateDto
import com.manna.bible.data.remote.GeminiContentDto
import com.manna.bible.data.remote.GeminiPartDto
import com.manna.bible.data.remote.GeminiRequestDto
import com.manna.bible.data.remote.GeminiResponseDto
import com.manna.bible.domain.model.Denomination
import com.manna.bible.domain.sermon.CongregationType
import com.manna.bible.domain.sermon.SermonOutlineRequest
import com.manna.bible.domain.sermon.SermonOutlineResult
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
 * Unit tests for [GeminiSermonEngine] — outline generation, failure mapping, and prompt
 * construction (denomination + congregation type + outline structure). Uses a hand-rolled
 * [FakeApi] returning canned DTOs (no Mockito), mirroring the style of
 * [com.manna.bible.data.crisis.GeminiCrisisEngineTest].
 */
class GeminiSermonEngineTest {

    private fun successDto(text: String) = GeminiResponseDto(
        candidates = listOf(GeminiCandidateDto(GeminiContentDto(listOf(GeminiPartDto(text)))))
    )

    private fun request(
        reference: String = "John 4:1-26",
        denomination: Denomination? = null,
        languageCode: String = "ta",
        congregationType: CongregationType = CongregationType.GENERAL
    ) = SermonOutlineRequest(
        reference = reference,
        denomination = denomination,
        languageCode = languageCode,
        congregationType = congregationType
    )

    @Test
    @DisplayName("A blank key is not configured and returns Unavailable")
    fun unconfigured() = runTest {
        val engine = GeminiSermonEngine(FakeApi { successDto("outline") }, apiKey = "")
        assertFalse(engine.isConfigured)
        val result = engine.generateOutline(request())
        assertInstanceOf(SermonOutlineResult.Unavailable::class.java, result)
    }

    @Test
    @DisplayName("A non-blank outline response is returned as Success")
    fun successResponse() = runTest {
        val outline = "**Introduction** — A question about the village well..."
        val engine = GeminiSermonEngine(FakeApi { successDto(outline) }, apiKey = "key")
        val result = engine.generateOutline(request())
        assertEquals(SermonOutlineResult.Success(outline), result)
    }

    @Test
    @DisplayName("A blank outline response returns Unavailable")
    fun blankResponse() = runTest {
        val engine = GeminiSermonEngine(FakeApi { successDto("   ") }, apiKey = "key")
        val result = engine.generateOutline(request())
        assertInstanceOf(SermonOutlineResult.Unavailable::class.java, result)
    }

    @Test
    @DisplayName("A network failure maps to Offline")
    fun offline() = runTest {
        val engine = GeminiSermonEngine(FakeApi { throw IOException("no net") }, apiKey = "key")
        val result = engine.generateOutline(request())
        assertEquals(SermonOutlineResult.Offline, result)
    }

    @Test
    @DisplayName("An HTTP error maps to Unavailable carrying only the status code")
    fun httpError() = runTest {
        val engine = GeminiSermonEngine(
            FakeApi {
                val body = "blocked".toResponseBody("application/json".toMediaTypeOrNull())
                throw HttpException(Response.error<GeminiResponseDto>(429, body))
            },
            apiKey = "key"
        )
        val result = engine.generateOutline(request())
        assertInstanceOf(SermonOutlineResult.Unavailable::class.java, result)
        result as SermonOutlineResult.Unavailable
        assertTrue(result.reason.contains("429"))
    }

    @Test
    @DisplayName("The Catholic tradition is named in the prompt")
    fun catholicInPrompt() = runTest {
        val capture = CapturingApi { successDto("outline") }
        val engine = GeminiSermonEngine(capture, apiKey = "key")
        engine.generateOutline(request(denomination = Denomination.CATHOLIC))
        assertTrue(capture.lastPrompt.orEmpty().contains("Catholic", ignoreCase = true))
        assertTrue(capture.lastPrompt.orEmpty().contains("sacramental", ignoreCase = true))
    }

    @Test
    @DisplayName("The GRIEF congregation type shapes the prompt")
    fun griefInPrompt() = runTest {
        val capture = CapturingApi { successDto("outline") }
        val engine = GeminiSermonEngine(capture, apiKey = "key")
        engine.generateOutline(request(congregationType = CongregationType.GRIEF))
        val prompt = capture.lastPrompt.orEmpty()
        assertTrue(prompt.contains("funeral", ignoreCase = true) || prompt.contains("memorial", ignoreCase = true))
        assertTrue(prompt.contains("triumphalist", ignoreCase = true))
    }

    @Test
    @DisplayName("The outline structure instruction is present in the prompt")
    fun outlineFormatInPrompt() = runTest {
        val capture = CapturingApi { successDto("outline") }
        val engine = GeminiSermonEngine(capture, apiKey = "key")
        engine.generateOutline(request(languageCode = "ta"))
        val prompt = capture.lastPrompt.orEmpty()
        assertTrue(prompt.contains("Introduction", ignoreCase = true))
        assertTrue(prompt.contains("Cross-references", ignoreCase = true))
        assertTrue(prompt.contains("Illustration", ignoreCase = true))
        assertTrue(prompt.contains("Conclusion", ignoreCase = true))
        assertTrue(prompt.contains("'ta'"), "language ISO instruction should be present")
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
