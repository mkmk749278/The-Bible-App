package com.manna.bible.data.remote

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.manna.bible.data.crisis.GeminiCrisisEngine
import com.manna.bible.data.explain.GeminiExplanationEngine
import com.manna.bible.data.sermon.GeminiSermonEngine
import com.manna.bible.domain.explain.ExplainDepth
import com.manna.bible.domain.explain.ExplanationRequest
import com.manna.bible.domain.sermon.CongregationType
import com.manna.bible.domain.sermon.SermonOutlineRequest
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import retrofit2.Retrofit

/**
 * Pins the wire contract every cloud AI engine relies on:
 *  - thinking is disabled (`thinkingBudget = 0`) so Gemini 2.5 Flash's thought tokens can't
 *    consume the small `maxOutputTokens` budgets and return empty/truncated answers;
 *  - the API key is sent in the `x-goog-api-key` header, never in the URL;
 *  - the app-identity headers are attached so the key can be restricted to this app.
 */
class GeminiRequestContractTest {

    /** Same configuration as the app's shared parser (AppModule.provideJson). */
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val okResponse = """{"candidates":[{"content":{"parts":[{"text":"ok"}]}}]}"""

    @Test
    @DisplayName("The serialized request disables thinking even though defaults are not encoded")
    fun thinkingOffIsSerialized() {
        val body = GeminiRequestDto(
            contents = listOf(GeminiContentDto(parts = listOf(GeminiPartDto("hi")))),
            generationConfig = GeminiGenerationConfigDto(
                temperature = 0.4f,
                maxOutputTokens = 256,
                thinkingConfig = GeminiThinkingConfigDto.OFF
            )
        )
        val encoded = json.encodeToString(body)
        assertTrue(
            encoded.contains("\"thinkingConfig\":{\"thinkingBudget\":0}"),
            "thinkingConfig missing from $encoded"
        )
    }

    @Test
    @DisplayName("The explanation, crisis and sermon engines all send thinking OFF")
    fun enginesDisableThinking() = runTest {
        val api = CapturingApi(okResponse)

        GeminiExplanationEngine(api, apiKey = "k").explain(
            ExplanationRequest(
                osisRef = "JHN.3.16",
                reference = "John 3:16",
                passageText = "For God so loved the world",
                uiLanguageCode = "en",
                depth = ExplainDepth.PLAIN
            )
        )
        GeminiCrisisEngine(api, apiKey = "k")
            .respond("I feel alone", languageCode = "en", isNight = true, denomination = null)
        GeminiSermonEngine(api, apiKey = "k").generateOutline(
            SermonOutlineRequest(
                reference = "John 3:16",
                denomination = null,
                languageCode = "en",
                congregationType = CongregationType.GENERAL
            )
        )

        assertEquals(3, api.bodies.size)
        api.bodies.forEach { body ->
            assertEquals(GeminiThinkingConfigDto.OFF, body.generationConfig?.thinkingConfig)
        }
    }

    @Test
    @DisplayName("The key travels in a header, and the app-identity headers are attached")
    fun keyInHeaderWithAppIdentity() = runTest {
        var captured: Request? = null
        val client = OkHttpClient.Builder()
            .addInterceptor(GoogleApiAppIdentityInterceptor("com.manna.bible") { "AB12CD" })
            .addInterceptor(Interceptor { chain ->
                captured = chain.request()
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(okResponse.toResponseBody("application/json".toMediaType()))
                    .build()
            })
            .build()
        val api = Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiApi::class.java)

        api.generate(
            model = "gemini-2.5-flash",
            key = "secret-key",
            body = GeminiRequestDto(contents = listOf(GeminiContentDto(listOf(GeminiPartDto("hi")))))
        )

        val request = captured!!
        assertNull(request.url.queryParameter("key"), "the key must not be in the URL")
        assertFalse(request.url.toString().contains("secret-key"))
        assertEquals("secret-key", request.header("x-goog-api-key"))
        assertEquals("com.manna.bible", request.header(GoogleApiAppIdentityInterceptor.HEADER_PACKAGE))
        assertEquals("AB12CD", request.header(GoogleApiAppIdentityInterceptor.HEADER_CERT))
        val sent = Buffer().also { request.body!!.writeTo(it) }.readUtf8()
        assertFalse(sent.contains("secret-key"), "the key must not be in the body")
    }

    @Test
    @DisplayName("A missing signing certificate omits only the certificate header")
    fun missingCertificateOmitsHeader() = runTest {
        var captured: Request? = null
        val interceptor = GoogleApiAppIdentityInterceptor("com.manna.bible") { null }
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .addInterceptor(Interceptor { chain ->
                captured = chain.request()
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(204)
                    .message("No Content")
                    .body("".toResponseBody(null))
                    .build()
            })
            .build()
        client.newCall(Request.Builder().url("https://example.invalid/").build()).execute().close()

        assertEquals("com.manna.bible", captured!!.header(GoogleApiAppIdentityInterceptor.HEADER_PACKAGE))
        assertNull(captured!!.header(GoogleApiAppIdentityInterceptor.HEADER_CERT))
    }

    @Test
    @DisplayName("Certificate fingerprints use Google's uppercase, colon-free hex format")
    fun sha1Format() {
        // SHA-1("abc") = a9993e36 4706816a ba3e2571 7850c26c 9cd0d89d
        assertEquals(
            "A9993E364706816ABA3E25717850C26C9CD0D89D",
            GoogleApiAppIdentityInterceptor.sha1Hex("abc".toByteArray())
        )
    }

    /** Records every request body and answers with a fixed JSON response. */
    private inner class CapturingApi(private val responseJson: String) : GeminiApi {
        val bodies = mutableListOf<GeminiRequestDto>()

        override suspend fun generate(
            model: String,
            key: String,
            body: GeminiRequestDto
        ): GeminiResponseDto {
            bodies += body
            return json.decodeFromString(GeminiResponseDto.serializer(), responseJson)
        }
    }
}
