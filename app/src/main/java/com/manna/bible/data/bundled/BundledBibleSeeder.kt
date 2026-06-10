package com.manna.bible.data.bundled

import com.manna.bible.data.local.BibleContentDao
import com.manna.bible.data.local.BookEntity
import com.manna.bible.data.local.ChapterEntity
import com.manna.bible.data.local.TranslationDao
import com.manna.bible.data.local.TranslationEntity
import com.manna.bible.domain.model.CanonType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeds bundled (shipped-in-app) translations into Room on startup (Task 5.1).
 *
 * The seeder is:
 *  - **Tolerant**: when no bundled assets are present (the
 *    [BundledBibleAssetReader] returns a `null` manifest), [seed] is a no-op, so
 *    builds without generated assets run unaffected.
 *  - **Idempotent**: a translation is re-seeded only when its stored
 *    [TranslationEntity.contentVersion] is older than the manifest's
 *    `bundledContentVersion`, so repeated startups do not duplicate content.
 *  - **Off the main thread**: all parsing and inserts run on [Dispatchers.IO].
 *
 * Content is inserted transactionally per translation via
 * [BibleContentDao.insertContent], then the catalog row is registered/updated as a
 * downloaded bundled translation so the reader can open it immediately offline.
 */
@Singleton
class BundledBibleSeeder @Inject constructor(
    private val assetReader: BundledBibleAssetReader,
    private val bibleContentDao: BibleContentDao,
    private val translationDao: TranslationDao
) {

    /** Seeds every bundled translation whose content is missing or out of date. */
    suspend fun seed() = withContext(Dispatchers.IO) {
        val manifest = assetReader.manifest() ?: return@withContext
        for (entry in manifest.translations) {
            val existing = translationDao.getById(entry.id)
            val alreadySeeded = existing != null &&
                existing.verseCount > 0 &&
                existing.contentVersion >= entry.bundledContentVersion
            if (alreadySeeded) continue
            seedTranslation(entry)
        }
    }

    private suspend fun seedTranslation(entry: BundledTranslationManifest) {
        val bible = assetReader.read(entry.assetFile)

        val books = bible.books.map { book ->
            BookEntity(
                translationId = entry.id,
                osisId = book.osisId,
                name = book.name,
                testament = book.testament,
                orderIndex = book.orderIndex,
                chapterCount = book.chapterCount
            )
        }

        val chapters = bible.verses
            .groupBy { it.osisId to it.chapter }
            .map { (key, verses) ->
                ChapterEntity(
                    translationId = entry.id,
                    osisId = key.first,
                    chapter = key.second,
                    verseCount = verses.size
                )
            }

        val verses = bible.verses.map { verse ->
            com.manna.bible.data.local.VerseEntity(
                translationId = entry.id,
                osisId = verse.osisId,
                chapter = verse.chapter,
                verse = verse.verse,
                text = verse.text
            )
        }

        // Replace any stale partial content, then write the fresh content atomically.
        bibleContentDao.deleteTranslationContent(entry.id)
        bibleContentDao.insertContent(books = books, chapters = chapters, verses = verses)

        translationDao.upsertAll(
            listOf(
                TranslationEntity(
                    id = entry.id,
                    name = entry.name,
                    languageCode = entry.languageCode,
                    canonType = (CanonType.fromId(entry.canonType) ?: CanonType.PROTESTANT_66).id,
                    hasDeuterocanon = entry.isDeuterocanon,
                    isDownloaded = true,
                    sizeBytes = verses.sumOf { it.text.length.toLong() },
                    isDefaultForCanon = false,
                    isBundled = true,
                    contentVersion = entry.bundledContentVersion,
                    verseCount = entry.verseCount
                )
            )
        )
    }
}
