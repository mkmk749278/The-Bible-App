package com.manna.bible.data.download

import com.manna.bible.data.ConnectivityChecker
import com.manna.bible.data.local.BibleContentDao
import com.manna.bible.data.local.BookEntity
import com.manna.bible.data.local.ChapterEntity
import com.manna.bible.data.local.TranslationDao
import com.manna.bible.data.local.VerseEntity
import com.manna.bible.data.remote.HelloAoRemoteDataSource
import com.manna.bible.data.remote.RemoteBook
import com.manna.bible.domain.download.DownloadManager
import com.manna.bible.domain.download.DownloadOutcome
import com.manna.bible.domain.download.DownloadProgress
import com.manna.bible.domain.repository.PendingDownloadRepository
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Default [DownloadManager] backed by the Free Use Bible API and Room.
 *
 * Design highlights (Req 5):
 *  - **Offline** ([ConnectivityChecker.isOnline] is false): the request is queued
 *    via [PendingDownloadRepository] and [DownloadOutcome.Offline] is returned;
 *    nothing is written and the translation is not marked downloaded (Req 5.6).
 *  - **Online success**: books and chapters are fetched from
 *    [HelloAoRemoteDataSource], stored **per book** (lower memory, resumable), and
 *    only after every chapter is committed is the translation marked downloaded
 *    with its content version and verse count (Req 5.1, 5.3, 15.4). The pending
 *    entry is then cleared (Req 5.6).
 *  - **Cancel / failure mid-way**: any partial content is deleted via
 *    [BibleContentDao.deleteTranslationContent] and the translation is left
 *    un-downloaded, so a partial translation is never presented as complete
 *    (Req 5.4, 5.7).
 *  - **Delete**: stored content is removed and the downloaded marker cleared; the
 *    active-translation fallback (Req 5.5) is handled by the catalog /
 *    active-translation layer, not here.
 *  - **Retry**: when online, every pending id is re-attempted (Req 5.6, 11.5).
 *
 * Progress ([progress]) is exposed as a single hot stream that is `null` when idle.
 */
@Singleton
class DefaultDownloadManager @Inject constructor(
    private val remote: HelloAoRemoteDataSource,
    private val bibleContentDao: BibleContentDao,
    private val translationDao: TranslationDao,
    private val pending: PendingDownloadRepository,
    private val connectivity: ConnectivityChecker
) : DownloadManager {

    private val progressState = MutableStateFlow<DownloadProgress?>(null)

    /** Translation ids whose in-flight download has been asked to cancel. */
    private val cancelled: MutableSet<String> = ConcurrentHashMap.newKeySet()

    override fun progress(): StateFlow<DownloadProgress?> = progressState.asStateFlow()

    override suspend fun download(translationId: String): DownloadOutcome {
        if (!connectivity.isOnline()) {
            pending.add(translationId)
            return DownloadOutcome.Offline
        }

        cancelled.remove(translationId)

        return try {
            val books = remote.books(translationId)
            val totalChapters = books.sumOf { it.chapterCount }
            progressState.value = DownloadProgress(translationId, 0, totalChapters)

            var completedChapters = 0
            var verseCount = 0
            var charCount = 0L

            for (book in books) {
                val chapterEntities = ArrayList<ChapterEntity>(book.chapterCount)
                val verseEntities = ArrayList<VerseEntity>()

                for (chapterNumber in 1..book.chapterCount) {
                    if (cancelled.remove(translationId)) {
                        bibleContentDao.deleteTranslationContent(translationId)
                        progressState.value = null
                        return DownloadOutcome.Failure("cancelled")
                    }

                    val remoteChapter = remote.chapter(translationId, book.osisId, chapterNumber)
                    chapterEntities += ChapterEntity(
                        translationId = translationId,
                        osisId = book.osisId,
                        chapter = chapterNumber,
                        verseCount = remoteChapter.verses.size
                    )
                    remoteChapter.verses.forEach { v ->
                        verseEntities += VerseEntity(
                            translationId = translationId,
                            osisId = book.osisId,
                            chapter = chapterNumber,
                            verse = v.verse,
                            text = v.text
                        )
                        verseCount++
                        charCount += v.text.length
                    }

                    completedChapters++
                    progressState.value =
                        DownloadProgress(translationId, completedChapters, totalChapters)
                }

                // Persist per book so progress is durable and memory stays bounded.
                bibleContentDao.insertContent(
                    books = listOf(book.toEntity(translationId)),
                    chapters = chapterEntities,
                    verses = verseEntities
                )
            }

            // Commit: mark downloaded only after everything is stored (Req 5.3, 15.4).
            val nextVersion = (translationDao.getById(translationId)?.contentVersion ?: 0) + 1
            translationDao.setDownloadedContent(
                id = translationId,
                sizeBytes = charCount,
                contentVersion = nextVersion,
                verseCount = verseCount
            )
            pending.remove(translationId)

            progressState.value =
                DownloadProgress(translationId, totalChapters, totalChapters, done = true)
            DownloadOutcome.Success
        } catch (e: Exception) {
            // Failure mid-way: remove partial content, leave un-downloaded (Req 5.7).
            bibleContentDao.deleteTranslationContent(translationId)
            progressState.value = null
            DownloadOutcome.Failure(e.message ?: "download failed")
        }
    }

    override suspend fun cancel(translationId: String) {
        cancelled.add(translationId)
        bibleContentDao.deleteTranslationContent(translationId)
        if (progressState.value?.translationId == translationId) {
            progressState.value = null
        }
    }

    override suspend fun delete(translationId: String) {
        bibleContentDao.deleteTranslationContent(translationId)
        translationDao.clearDownloaded(translationId)
    }

    override suspend fun retryPending() {
        if (!connectivity.isOnline()) return
        pending.all().forEach { id -> download(id) }
    }
}

/** Maps a normalized [RemoteBook] to its persistable [BookEntity] for [translationId]. */
private fun RemoteBook.toEntity(translationId: String): BookEntity = BookEntity(
    translationId = translationId,
    osisId = osisId,
    name = name,
    testament = testament ?: "",
    orderIndex = orderIndex,
    chapterCount = chapterCount
)
