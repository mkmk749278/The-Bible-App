package com.manna.bible.domain.share

import com.manna.bible.domain.model.Denomination

/**
 * A request for a verse to share, tied to a free-text situation or occasion (F-05).
 *
 * @property situationText The occasion or situation the user typed, e.g. "failed exam",
 *   "wedding today", "my mother is in hospital".
 * @property languageCode ISO code of the Bible language the personal message must be
 *   written in (also the language whose active translation supplies the verse text).
 * @property denomination The user's tradition, used to frame the choice, or null.
 *
 * Pure Kotlin — no Android dependency — so it is fully JVM-testable.
 */
data class VerseRecommendationRequest(
    val situationText: String,
    val languageCode: String,
    val denomination: Denomination?
)

/**
 * The outcome of a single Context-Aware Verse Card recommendation (F-05).
 *
 * On [Success] the recommended passage's text is resolved from the active local
 * translation — the model only supplies the reference; the canonical text always comes
 * from the user's downloaded Bible so the shared card matches what they read. Like the
 * other AI contracts, [Offline] carries no message (the UI sources its wording from
 * string resources) and the type stays free of presentation concerns.
 */
sealed interface VerseRecommendation {

    /**
     * A recommended passage, resolved against the active translation, ready to render
     * onto a shareable card.
     *
     * @property osisRef Canonical `OSIS.CHAPTER.VERSE`, e.g. "EPH.5.25".
     * @property reference Display reference, e.g. "Ephesians 5:25".
     * @property verseText The verse text from the active translation.
     * @property personalMessage A two-sentence personal note in the Bible language.
     */
    data class Success(
        val osisRef: String,
        val reference: String,
        val verseText: String,
        val personalMessage: String
    ) : VerseRecommendation

    /** The request could not reach the network; the manual share flow remains available. */
    data object Offline : VerseRecommendation

    /** The engine is unavailable (not configured, malformed output, HTTP error, or the
     *  recommended verse is absent from the active translation). */
    data class Unavailable(val reason: String) : VerseRecommendation
}

/**
 * Recommends the single passage best suited to share for a described situation (F-05).
 * The engine chooses a reference via the model, then resolves the verse text from the
 * active local translation so the shared card always uses the user's own Bible.
 *
 * Pure Kotlin — no Android dependency — so it is fully JVM-testable.
 */
interface VerseRecommendationEngine {

    /** True when the engine has the credentials it needs to make a request. */
    val isConfigured: Boolean

    /** Chooses and resolves the passage most suited to [request]. */
    suspend fun recommend(request: VerseRecommendationRequest): VerseRecommendation
}
