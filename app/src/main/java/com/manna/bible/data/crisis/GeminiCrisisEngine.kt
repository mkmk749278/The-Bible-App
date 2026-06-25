package com.manna.bible.data.crisis

import com.manna.bible.data.remote.GeminiApi
import com.manna.bible.data.remote.GeminiContentDto
import com.manna.bible.data.remote.GeminiGenerationConfigDto
import com.manna.bible.data.remote.GeminiPartDto
import com.manna.bible.data.remote.GeminiRequestDto
import com.manna.bible.domain.crisis.CrisisAiEngine
import com.manna.bible.domain.crisis.CrisisAiResult
import com.manna.bible.domain.model.Denomination
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named

/**
 * Cloud [CrisisAiEngine] backed by Gemini Flash (F-03). Builds a compassionate,
 * single-turn prompt, calls [GeminiApi] with the injected key, and parses the model's
 * three-line response into a [CrisisAiResult.Success].
 *
 * Privacy is the defining constraint of this engine: the caller's `situation` text is
 * only ever placed in the request body. It is never logged, never persisted, and never
 * copied into an exception message or a [CrisisAiResult.Unavailable.reason]. Failures
 * map to fixed, situation-free results:
 * - [IOException] → [CrisisAiResult.Offline] (no message at all).
 * - [HttpException] → `Unavailable("HTTP <code>")` — only the status code.
 * - malformed output → `Unavailable("Malformed AI response …")` — derived from the
 *   parsed reply, never from the situation.
 * - [kotlinx.coroutines.CancellationException] is re-thrown so coroutine cancellation
 *   is never swallowed.
 *
 * The key is injected (from `BuildConfig.GEMINI_API_KEY`) so the engine stays
 * unit-testable without Android, mirroring [com.manna.bible.data.explain.GeminiExplanationEngine].
 */
class GeminiCrisisEngine @Inject constructor(
    private val api: GeminiApi,
    @Named("geminiApiKey") private val apiKey: String
) : CrisisAiEngine {

    override val isConfigured: Boolean get() = apiKey.isNotBlank()

    override suspend fun respond(
        situation: String,
        languageCode: String,
        isNight: Boolean,
        denomination: Denomination?
    ): CrisisAiResult {
        if (apiKey.isBlank()) return CrisisAiResult.Unavailable("No API key configured.")
        return try {
            val prompt = buildPrompt(situation, languageCode, isNight, denomination)
            val response = api.generate(
                model = MODEL,
                key = apiKey,
                body = GeminiRequestDto(
                    contents = listOf(
                        GeminiContentDto(parts = listOf(GeminiPartDto(prompt)))
                    ),
                    generationConfig = GeminiGenerationConfigDto(
                        temperature = 0.3f,
                        maxOutputTokens = 256
                    )
                )
            )
            val raw = response.candidates.firstOrNull()
                ?.content?.parts?.firstOrNull()?.text?.trim()
            if (raw.isNullOrBlank()) {
                CrisisAiResult.Unavailable("Empty response from AI.")
            } else {
                parseResponse(raw)
            }
        } catch (e: IOException) {
            // No diagnostic detail — an offline failure must not risk echoing anything.
            CrisisAiResult.Offline
        } catch (e: HttpException) {
            // Only the HTTP status — never the request body (which carries the situation).
            CrisisAiResult.Unavailable("HTTP ${e.code()}")
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            // The situation text is never passed to any throwable we construct or read,
            // so the class-name signal here cannot leak it.
            CrisisAiResult.Unavailable(e.javaClass.simpleName)
        }
    }

    private fun buildPrompt(
        situation: String,
        languageCode: String,
        isNight: Boolean,
        denomination: Denomination?
    ): String = buildString {
        if (isNight) {
            appendLine(
                "It is the middle of the night and this person is alone. Begin with quiet " +
                    "presence. Do not open with energy or an action item. Sit with them first."
            )
            appendLine()
        }
        appendLine(
            "You are a compassionate Christian pastor. Someone has come to you in crisis. " +
                "Your task: choose the single Bible passage most directly relevant to their " +
                "situation, then explain in 2–3 sentences why it speaks to them right now. " +
                "Lead with compassion, not theology. Do not minimise what they are experiencing."
        )
        if (denomination != null && denomination != Denomination.SHOW_EVERYTHING) {
            appendLine()
            appendLine("The person's tradition: ${denomination.id.replace('_', ' ')}.")
        }
        appendLine()
        appendLine("Their situation (treat this as a private confession — never repeat it back): $situation")
        appendLine()
        appendLine(
            "Respond ONLY in this exact format (no preamble, no greeting, just these three lines):\n" +
                "REF: <OSIS_ID>.<chapter>.<verse>\n" +
                "DISPLAY: <Book Name> <chapter>:<verse>\n" +
                "MESSAGE: <your 2–3 sentence compassionate explanation>\n" +
                "\nWrite the MESSAGE in the language with ISO code '$languageCode'."
        )
    }

    /**
     * Parses the model's three-line reply into [CrisisAiResult.Success], or returns
     * [CrisisAiResult.Unavailable] — never throws — when a required line is missing.
     * The Unavailable reason is derived only from which structural line was absent; it
     * never contains any model text beyond the fixed diagnostic, and never the situation.
     */
    private fun parseResponse(raw: String): CrisisAiResult {
        val lines = raw.lines().associate { line ->
            val colon = line.indexOf(':')
            if (colon < 0) return@associate "" to ""
            line.substring(0, colon).trim() to line.substring(colon + 1).trim()
        }
        val osisRef = lines["REF"]?.takeIf { it.isNotBlank() }
            ?: return CrisisAiResult.Unavailable("Malformed AI response (missing REF).")
        val display = lines["DISPLAY"]?.takeIf { it.isNotBlank() }
            ?: return CrisisAiResult.Unavailable("Malformed AI response (missing DISPLAY).")
        val message = lines["MESSAGE"]?.takeIf { it.isNotBlank() }
            ?: return CrisisAiResult.Unavailable("Malformed AI response (missing MESSAGE).")
        return CrisisAiResult.Success(
            passageRef = display,
            osisRef = osisRef,
            explanation = message
        )
    }

    private companion object {
        /** Fast, low-cost model suited to a single-turn crisis response. */
        const val MODEL = "gemini-2.5-flash"
    }
}
