package com.manna.bible.data.share

import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.data.remote.GeminiApi
import com.manna.bible.data.remote.GeminiContentDto
import com.manna.bible.data.remote.GeminiGenerationConfigDto
import com.manna.bible.data.remote.GeminiPartDto
import com.manna.bible.data.remote.GeminiRequestDto
import com.manna.bible.domain.model.Denomination
import com.manna.bible.domain.repository.BibleContentRepository
import com.manna.bible.domain.repository.TranslationRepository
import com.manna.bible.domain.share.VerseRecommendation
import com.manna.bible.domain.share.VerseRecommendationEngine
import com.manna.bible.domain.share.VerseRecommendationRequest
import com.manna.bible.domain.usecase.ReadingRef
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named

/**
 * Cloud [VerseRecommendationEngine] backed by Gemini Flash (F-05). Builds a prompt asking
 * the model to choose the single passage best suited to share for a described situation,
 * parses the model's three-line reply, then resolves the verse text from the **active
 * local translation** — the model only supplies the reference; the canonical text always
 * comes from the user's downloaded Bible so the shared card matches what they read.
 *
 * Failure mapping mirrors [com.manna.bible.data.crisis.GeminiCrisisEngine]:
 * - [IOException] → [VerseRecommendation.Offline] (no message at all).
 * - [HttpException] → `Unavailable("HTTP <code>")` — only the status code.
 * - malformed output → `Unavailable("Malformed AI response …")` — never throws.
 * - [kotlinx.coroutines.CancellationException] is re-thrown so cancellation is never swallowed.
 * - generic [Exception] → `Unavailable(javaClass.simpleName)`.
 *
 * When the recommended verse is absent from the active translation the engine returns
 * `Unavailable("Verse not found in active translation. Try a different description.")` so
 * the user can rephrase rather than seeing an empty card.
 *
 * The active-translation resolution reuses the same offline-first pattern as
 * [com.manna.bible.ui.crisis.CrisisModeViewModel] and
 * [com.manna.bible.ui.card.ScriptureCardViewModel]: the persisted selection, else the
 * first downloaded/bundled translation from the catalog.
 */
class GeminiVerseRecommendationEngine @Inject constructor(
    private val api: GeminiApi,
    @Named("geminiApiKey") private val apiKey: String,
    private val bibleContentRepository: BibleContentRepository,
    private val translationRepository: TranslationRepository,
    private val preferencesStore: PreferencesStore
) : VerseRecommendationEngine {

    override val isConfigured: Boolean get() = apiKey.isNotBlank()

    override suspend fun recommend(request: VerseRecommendationRequest): VerseRecommendation {
        if (apiKey.isBlank()) return VerseRecommendation.Unavailable("No API key configured.")
        return try {
            val prompt = buildPrompt(request)
            val response = api.generate(
                model = MODEL,
                key = apiKey,
                body = GeminiRequestDto(
                    contents = listOf(
                        GeminiContentDto(parts = listOf(GeminiPartDto(prompt)))
                    ),
                    generationConfig = GeminiGenerationConfigDto(
                        temperature = 0.4f,
                        maxOutputTokens = 256
                    )
                )
            )
            val raw = response.candidates.firstOrNull()
                ?.content?.parts?.firstOrNull()?.text?.trim()
            if (raw.isNullOrBlank()) {
                VerseRecommendation.Unavailable("Empty response from AI.")
            } else {
                resolve(raw)
            }
        } catch (e: IOException) {
            VerseRecommendation.Offline
        } catch (e: HttpException) {
            VerseRecommendation.Unavailable("HTTP ${e.code()}")
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            VerseRecommendation.Unavailable(e.javaClass.simpleName)
        }
    }

    private fun buildPrompt(request: VerseRecommendationRequest): String = buildString {
        appendLine(
            "You are a wise, warm Christian friend helping someone pick the single best " +
                "Bible verse to send to a person for a specific occasion or situation. Choose " +
                "one passage (one verse) that is most fitting to share in that moment, then " +
                "write a short, two-sentence personal note they can send alongside it."
        )
        val denomination = request.denomination
        if (denomination != null && denomination != Denomination.SHOW_EVERYTHING) {
            appendLine()
            appendLine("The person's tradition: ${denomination.id.replace('_', ' ')}.")
        }
        appendLine()
        appendLine("Occasion or situation: ${request.situationText}")
        appendLine()
        appendLine(
            "Respond ONLY in this exact format (no preamble, no greeting, just these three lines):\n" +
                "REF: <OSIS_ID>.<chapter>.<verse>\n" +
                "DISPLAY: <Book Name> <chapter>:<verse>\n" +
                "MESSAGE: <your two-sentence personal note>\n" +
                "\nThe OSIS_ID must be a standard three-letter book code (e.g. EPH, PSA, ROM). " +
                "Write the MESSAGE in the language with ISO code '${request.languageCode}'."
        )
    }

    /**
     * Parses the model's three-line reply and resolves the verse text from the active
     * translation. Returns [VerseRecommendation.Unavailable] — never throws — on a
     * malformed reply, when no active translation is available, or when the verse is not
     * present in the active translation.
     */
    private suspend fun resolve(raw: String): VerseRecommendation {
        val lines = raw.lines().associate { line ->
            val colon = line.indexOf(':')
            if (colon < 0) return@associate "" to ""
            line.substring(0, colon).trim() to line.substring(colon + 1).trim()
        }
        val osisRefRaw = lines["REF"]?.takeIf { it.isNotBlank() }
            ?: return VerseRecommendation.Unavailable("Malformed AI response (missing REF).")
        val display = lines["DISPLAY"]?.takeIf { it.isNotBlank() }
            ?: return VerseRecommendation.Unavailable("Malformed AI response (missing DISPLAY).")
        val message = lines["MESSAGE"]?.takeIf { it.isNotBlank() }
            ?: return VerseRecommendation.Unavailable("Malformed AI response (missing MESSAGE).")

        val ref = ReadingRef.parse(osisRefRaw)
            ?: return VerseRecommendation.Unavailable("Malformed AI response (unparseable REF).")

        val translationId = resolveActiveTranslation()
            ?: return VerseRecommendation.Unavailable(VERSE_NOT_FOUND)

        val content = runCatching {
            bibleContentRepository.chapter(translationId, ref.osisId, ref.chapter)
        }.getOrNull()
        val verseText = content?.verses?.firstOrNull { it.verse == ref.verse }?.text
            ?: return VerseRecommendation.Unavailable(VERSE_NOT_FOUND)

        // Prefer a reference built from the active translation's book name; fall back to
        // the model's DISPLAY line when the book name is not available locally.
        val bookName = runCatching {
            bibleContentRepository.books(translationId).first()
                .firstOrNull { it.osisId == ref.osisId }?.name
        }.getOrNull()
        val reference = if (bookName != null) "$bookName ${ref.chapter}:${ref.verse}" else display

        return VerseRecommendation.Success(
            osisRef = ref.format(),
            reference = reference,
            verseText = verseText,
            personalMessage = message
        )
    }

    /** Picks the persisted translation, else the first downloaded/bundled one available. */
    private suspend fun resolveActiveTranslation(): String? {
        val persistedId = preferencesStore.setupState.first().bibleTranslationId
        if (!persistedId.isNullOrBlank()) return persistedId
        val catalog = runCatching { translationRepository.catalog().first() }.getOrDefault(emptyList())
        return catalog.firstOrNull { it.isDownloaded }?.id ?: catalog.firstOrNull()?.id
    }

    private companion object {
        /** Fast, low-cost model suited to a single-turn recommendation. */
        const val MODEL = "gemini-2.5-flash"

        /** Returned when the recommended verse is absent from the active translation. */
        const val VERSE_NOT_FOUND =
            "Verse not found in active translation. Try a different description."
    }
}
