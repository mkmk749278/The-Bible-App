package com.manna.bible.domain.usecase

import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.repository.BibleContentRepository
import com.manna.bible.domain.repository.TranslationRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * A [ReadingRef] resolved to displayable text against the active translation.
 *
 * @property ref The reference that was resolved.
 * @property reference Display reference, e.g. "Psalm 23:4" (book name + chapter:verse).
 * @property text The verse text in the active translation.
 * @property osisRef Canonical `OSIS.CHAPTER.VERSE` string for "read in context".
 */
data class ResolvedVerse(
    val ref: ReadingRef,
    val reference: String,
    val text: String,
    val osisRef: String
)

/**
 * Resolves [ReadingRef]s to verse text against the active translation, fully offline.
 *
 * The active translation is the user's persisted selection, falling back to the first
 * downloaded (else first catalogued) translation. Several Prayers-hub surfaces
 * (Stations, the Rosary, the Jesus Prayer, Paraloka) each need the same "ref → text"
 * resolution that the Grief, Crisis, and Fasting companions wrote inline; this use
 * case is the single shared implementation.
 *
 * Verses not present in the active translation resolve to null (single) or are
 * dropped (batch), so a partially-downloaded Bible never crashes a devotion.
 * Chapter loads are de-duplicated within a batch so a screen that lists many verses
 * from the same chapter reads it once.
 *
 * Uses only repositories + coroutines — no Android framework types.
 */
class ResolveVerseTextUseCase @Inject constructor(
    private val preferencesStore: PreferencesStore,
    private val translationRepository: TranslationRepository,
    private val bibleContentRepository: BibleContentRepository
) {

    /** Resolves a single [ref], or null when no translation/verse is available. */
    suspend operator fun invoke(ref: ReadingRef): ResolvedVerse? =
        invoke(listOf(ref)).firstOrNull()

    /**
     * Resolves [refs] in order, dropping any that can't be found. Returns an empty
     * list when no translation is available at all.
     */
    suspend operator fun invoke(refs: List<ReadingRef>): List<ResolvedVerse> {
        if (refs.isEmpty()) return emptyList()
        val translationId = resolveActiveTranslation() ?: return emptyList()

        val bookNames = runCatching {
            bibleContentRepository.books(translationId).first().associate { it.osisId to it.name }
        }.getOrDefault(emptyMap())

        // Load each distinct (book, chapter) once, then index verses for fast lookup.
        val chapterCache = mutableMapOf<Pair<String, Int>, Map<Int, String>>()
        return refs.mapNotNull { ref ->
            val key = ref.osisId to ref.chapter
            val verses = chapterCache.getOrPut(key) {
                runCatching {
                    bibleContentRepository.chapter(translationId, ref.osisId, ref.chapter)
                }.getOrNull()?.verses?.associate { it.verse to it.text } ?: emptyMap()
            }
            val text = verses[ref.verse] ?: return@mapNotNull null
            val name = bookNames[ref.osisId] ?: ref.osisId
            ResolvedVerse(
                ref = ref,
                reference = "$name ${ref.chapter}:${ref.verse}",
                text = text,
                osisRef = ref.format()
            )
        }
    }

    /** Picks the persisted translation, else the first downloaded/bundled one available. */
    private suspend fun resolveActiveTranslation(): String? {
        val persistedId = preferencesStore.setupState.first().bibleTranslationId
        if (!persistedId.isNullOrBlank()) return persistedId
        val catalog = runCatching { translationRepository.catalog().first() }.getOrDefault(emptyList())
        return catalog.firstOrNull { it.isDownloaded }?.id ?: catalog.firstOrNull()?.id
    }
}
