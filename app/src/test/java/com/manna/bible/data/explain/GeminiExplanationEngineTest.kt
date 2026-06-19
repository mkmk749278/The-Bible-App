package com.manna.bible.data.explain

import com.manna.bible.data.remote.GeminiApi
import com.manna.bible.data.remote.GeminiCandidateDto
import com.manna.bible.data.remote.GeminiContentDto
import com.manna.bible.data.remote.GeminiPartDto
import com.manna.bible.data.remote.GeminiRequestDto
import com.manna.bible.data.remote.GeminiResponseDto
import com.manna.bible.domain.explain.ExplainDepth
import com.manna.bible.domain.explain.ExplanationRequest
import com.manna.bible.domain.explain.ExplanationResult
import com.manna.bible.domain.explain.ExplanationUnavailableReason
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import retrofit2.HttpException
import retrofit2.Response
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.IOException

/** Unit tests for [GeminiExplanationEngine] — response mapping and failure modes. */
class GeminiExplanationEngineTest {

    private fun request() = ExplanationRequest(
        osisRef = "GEN.2.1",
        reference = "Genesis 2:1",
        passageText = "Thus the heavens…",
        uiLanguageCode = "en",
        depth = ExplainDepth.PLAIN
    )

    @Test
    @DisplayName("A blank key is not configured and returns NOT_CONFIGURED")
    fun blankKey() = runTest {
        val engine = GeminiExplanationEngine(FakeApi { GeminiResponseDto() }, apiKey = "")
        assertFalse(engine.isConfigured)
        assertEquals(
            ExplanationResult.Unavailable(ExplanationUnavailableReason.NOT_CONFIGURED),
            engine.explain(request())
        )
    }

    @Test
    @DisplayName("A candidate's text is mapped to Success")
    fun success() = runTest {
        val engine = GeminiExplanationEngine(
            FakeApi {
                GeminiResponseDto(
                    candidates = listOf(
                        GeminiCandidateDto(GeminiContentDto(listOf(GeminiPartDto("  Here is what it means.  "))))
                    )
                )
            },
            apiKey = "key"
        )
        assertEquals(
            ExplanationResult.Success("Here is what it means."),
            engine.explain(request())
        )
    }

    @Test
    @DisplayName("A network failure maps to OFFLINE and carries a diagnostic detail")
    fun offline() = runTest {
        val engine = GeminiExplanationEngine(FakeApi { throw IOException("no net") }, apiKey = "key")
        val result = engine.explain(request())
        assertInstanceOf(ExplanationResult.Unavailable::class.java, result)
        result as ExplanationResult.Unavailable
        assertEquals(ExplanationUnavailableReason.OFFLINE, result.reason)
        assertNotNull(result.detail)
    }

    @Test
    @DisplayName("An empty response maps to ERROR and carries a diagnostic detail")
    fun empty() = runTest {
        val engine = GeminiExplanationEngine(FakeApi { GeminiResponseDto() }, apiKey = "key")
        val result = engine.explain(request())
        assertInstanceOf(ExplanationResult.Unavailable::class.java, result)
        result as ExplanationResult.Unavailable
        assertEquals(ExplanationUnavailableReason.ERROR, result.reason)
        assertNotNull(result.detail)
    }

    @Test
    @DisplayName("An HTTP error maps to ERROR and reports the status code + body")
    fun httpError() = runTest {
        val engine = GeminiExplanationEngine(
            FakeApi {
                val body = "API_KEY_SERVICE_BLOCKED".toResponseBody("application/json".toMediaTypeOrNull())
                throw HttpException(Response.error<GeminiResponseDto>(403, body))
            },
            apiKey = "key"
        )
        val result = engine.explain(request())
        assertInstanceOf(ExplanationResult.Unavailable::class.java, result)
        result as ExplanationResult.Unavailable
        assertEquals(ExplanationUnavailableReason.ERROR, result.reason)
        assertTrue(result.detail!!.contains("403"), "detail should include the HTTP status")
        assertTrue(result.detail!!.contains("API_KEY_SERVICE_BLOCKED"), "detail should include the API reason")
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
}
