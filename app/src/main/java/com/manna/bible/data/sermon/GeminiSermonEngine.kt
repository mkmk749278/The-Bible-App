package com.manna.bible.data.sermon

import com.manna.bible.data.remote.GeminiApi
import com.manna.bible.data.remote.GeminiContentDto
import com.manna.bible.data.remote.GeminiGenerationConfigDto
import com.manna.bible.data.remote.GeminiPartDto
import com.manna.bible.data.remote.GeminiRequestDto
import com.manna.bible.domain.model.Denomination
import com.manna.bible.domain.sermon.CongregationType
import com.manna.bible.domain.sermon.SermonAiEngine
import com.manna.bible.domain.sermon.SermonOutlineRequest
import com.manna.bible.domain.sermon.SermonOutlineResult
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named

/**
 * Cloud [SermonAiEngine] backed by Gemini Flash (F-04). Builds a structured sermon-outline
 * prompt — denomination-aware and shaped to the [CongregationType] — calls [GeminiApi]
 * with the injected key, and returns the model's text as an editable outline.
 *
 * Failure mapping mirrors [com.manna.bible.data.crisis.GeminiCrisisEngine]:
 * - [IOException] → [SermonOutlineResult.Offline] (no message; the editor stays usable).
 * - [HttpException] → `Unavailable("HTTP <code>")` — only the status code.
 * - empty / blank output → `Unavailable("Empty response.")`.
 * - [kotlinx.coroutines.CancellationException] is re-thrown so coroutine cancellation
 *   is never swallowed.
 * - any other [Exception] → `Unavailable(<simple class name>)` — never the prompt body.
 *
 * The key is injected (from `BuildConfig.GEMINI_API_KEY`) so the engine stays
 * unit-testable without Android, mirroring [com.manna.bible.data.explain.GeminiExplanationEngine].
 */
class GeminiSermonEngine @Inject constructor(
    private val api: GeminiApi,
    @Named("geminiApiKey") private val apiKey: String
) : SermonAiEngine {

    override val isConfigured: Boolean get() = apiKey.isNotBlank()

    override suspend fun generateOutline(request: SermonOutlineRequest): SermonOutlineResult {
        if (apiKey.isBlank()) return SermonOutlineResult.Unavailable("No API key configured.")
        return try {
            val response = api.generate(
                model = MODEL,
                key = apiKey,
                body = GeminiRequestDto(
                    contents = listOf(
                        GeminiContentDto(parts = listOf(GeminiPartDto(buildPrompt(request))))
                    ),
                    generationConfig = GeminiGenerationConfigDto(
                        temperature = 0.5f,
                        maxOutputTokens = 1024
                    )
                )
            )
            val text = response.candidates.firstOrNull()
                ?.content?.parts?.firstOrNull()?.text?.trim()
            if (text.isNullOrBlank()) {
                SermonOutlineResult.Unavailable("Empty response.")
            } else {
                SermonOutlineResult.Success(text)
            }
        } catch (e: IOException) {
            SermonOutlineResult.Offline
        } catch (e: HttpException) {
            SermonOutlineResult.Unavailable("HTTP ${e.code()}")
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            SermonOutlineResult.Unavailable(e.javaClass.simpleName)
        }
    }

    private fun buildPrompt(request: SermonOutlineRequest): String = buildString {
        appendLine(
            "You are a village pastor's sermon preparation assistant. Your task is to " +
                "produce a complete sermon outline — not prose, an outline — for the " +
                "following passage. The preacher may have no seminary training. Be practical, " +
                "concrete, and rooted in the daily life of an Indian village congregation."
        )
        appendLine()

        // Denomination
        val denNote = when (request.denomination) {
            Denomination.CATHOLIC ->
                "The congregation is Catholic. The homily should honour the sacramental " +
                    "reading of the text and may reference the liturgical season or the Mass."
            Denomination.ORTHODOX ->
                "The congregation is Orthodox. Reference patristic or liturgical tradition " +
                    "where relevant."
            Denomination.CSI ->
                "The congregation is CSI (Church of South India). South Indian Protestant-" +
                    "Anglican sensibility; social justice matters here."
            Denomination.MAR_THOMA ->
                "The congregation is Mar Thoma. Reformed, but shaped by ancient Syrian " +
                    "heritage. Kerala-rooted."
            else -> null
        }
        if (denNote != null) {
            appendLine(denNote)
            appendLine()
        }

        // Congregation type
        val congNote = when (request.congregationType) {
            CongregationType.GENERAL -> "Mixed Sunday congregation of all ages."
            CongregationType.YOUTH ->
                "Youth congregation (teens and young adults). Modern references welcome. " +
                    "Keep it real and direct."
            CongregationType.GRIEF ->
                "A funeral or memorial service. Tone must be gentle, comforting, and honest " +
                    "about grief. Do not be triumphalist."
        }
        appendLine("Congregation: $congNote")
        appendLine()
        appendLine("Passage: ${request.reference}")
        appendLine()
        appendLine(
            "Produce the outline in EXACTLY this structure:\n" +
                "**Introduction** — one sentence opening with a question or observation from " +
                "Indian daily life the congregation will immediately recognise.\n" +
                "**Point 1: [heading]** — 2 bullet supporting thoughts.\n" +
                "**Point 2: [heading]** — 2 bullet supporting thoughts.\n" +
                "**Point 3: [heading]** — 2 bullet supporting thoughts.\n" +
                "**Cross-references** — 2–3 related passages (book chapter:verse).\n" +
                "**Illustration** — one concrete story or image from Indian village, " +
                "farming, or family life.\n" +
                "**Conclusion** — one closing sentence."
        )
        appendLine()
        append("Write the entire outline in the language with ISO code '${request.languageCode}'.")
    }

    private companion object {
        /** Fast, low-cost model suited to a single-turn outline generation. */
        const val MODEL = "gemini-2.5-flash"
    }
}
