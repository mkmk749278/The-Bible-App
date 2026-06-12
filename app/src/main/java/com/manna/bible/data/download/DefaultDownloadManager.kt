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
import com.manna.bible.di.DownloadScope
import com.manna.bible.domain.repository.PendingDownloadRepository
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
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
    private val connectivity: ConnectivityChecker,
    @DownloadScope private val scope: CoroutineScope
) : DownloadManager {

    private val progressState = MutableStateFlow<DownloadProgress?>(null)

    /** Translation ids whose in-flight download has been asked to cancel. */
    private val cancelled: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /** In-flight downloads, keyed by translation id, so callers join rather than restart. */
    private val inFlight = ConcurrentHashMap<String, Deferred<DownloadOutcome>>()

    override fun progress(): StateFlow<DownloadProgress?> = progressState.asStateFlow()

    override suspend fun download(translationId: String): DownloadOutcome {
        if (!connectivity.isOnline()) {
            pending.add(translationId)
            return DownloadOutcome.Offline
        }

        // Clear any stale cancel request from a previous attempt before starting.
        cancelled.remove(translationId)

        // Run on the app-lifetime [scope] so leaving the screen or backgrounding the
        // app does NOT cancel and discard the in-flight download. A second call for
        // the same id joins the existing download instead of restarting it — so
        // returning to the catalog mid-download resumes progress rather than starting
        // over.
        val deferred = inFlight.computeIfAbsent(translationId) {
            scope.async {
                try {
                    runDownload(translationId)
                } finally {
                    inFlight.remove(translationId)
                }
            }
        }
        return deferred.await()
    }

    private suspend fun runDownload(translationId: String): DownloadOutcome {
        return try {
            val books = remote.books(translationId)
            val totalChapters = books.sumOf { it.chapterCount }
            progressState.value = DownloadProgress(translationId, 0, totalChapters)

            var completedChapters = 0

            for (book in books) {
                if (isCancelled(translationId)) return onCancelled(translationId)

                // Resume: each book is stored atomically, so its presence means it is
                // fully downloaded — count it toward progress and skip refetching. This
                // lets a download interrupted by process death continue from where it
                // stopped instead of starting over.
                if (bibleContentDao.getBook(translationId, book.osisId) != null) {
                    completedChapters += book.chapterCount
                    progressState.value =
                        DownloadProgress(translationId, completedChapters, totalChapters)
                    continue
                }

                val chapterEntities = ArrayList<ChapterEntity>(book.chapterCount)
                val verseEntities = ArrayList<VerseEntity>()

                for (chapterNumber in 1..book.chapterCount) {
                    // Cancellation is cooperative: cancel() (running on another
                    // coroutine) raises the per-id flag; we observe it both before
                    // and after each chapter fetch so a cancel that arrives WHILE a
                    // chapter is in flight is still honored before more is written.
                    if (isCancelled(translationId)) return onCancelled(translationId)

                    val remoteChapter = remote.chapter(translationId, book.osisId, chapterNumber)

                    if (isCancelled(translationId)) return onCancelled(translationId)

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
                    }

                    completedChapters++
                    progressState.value =
                        DownloadProgress(translationId, completedChapters, totalChapters)
                }

                // Don't persist a book's content if a cancel landed during it.
                if (isCancelled(translationId)) return onCancelled(translationId)

                // Persist per book so progress is durable and memory stays bounded.
                bibleContentDao.insertContent(
                    books = listOf(book.toEntity(translationId)),
                    chapters = chapterEntities,
                    verses = verseEntities
                )
            }

            // Final guard: never mark a cancelled download as complete (Req 5.4).
            if (isCancelled(translationId)) return onCancelled(translationId)

            // Commit: mark downloaded only after everything is stored (Req 5.3, 15.4).
            // Totals are read back from storage so resumed (skipped) books are counted.
            val verseCount = bibleContentDao.countVerses(translationId)
            val sizeBytes = bibleContentDao.sumTextLength(translationId)
            val nextVersion = (translationDao.getById(translationId)?.contentVersion ?: 0) + 1
            translationDao.setDownloadedContent(
                id = translationId,
                sizeBytes = sizeBytes,
                contentVersion = nextVersion,
                verseCount = verseCount
            )
            pending.remove(translationId)

            progressState.value =
                DownloadProgress(translationId, totalChapters, totalChapters, done = true)
            DownloadOutcome.Success
        } catch (e: CancellationException) {
            // The download coroutine was cancelled (its scope torn down, or an
            // explicit cancel cancelled it). Do NOT delete partial content here —
            // completed books are kept so the download resumes next time. Explicit
            // cancel() deletes content separately (Req 5.4).
            if (progressState.value?.translationId == translationId) {
                progressState.value = null
            }
            throw e
        } catch (e: Exception) {
            // Failure mid-way: remove partial content, leave un-downloaded (Req 5.7).
            bibleContentDao.deleteTranslationContent(translationId)
            progressState.value = null
            DownloadOutcome.Failure(e.message ?: "download failed")
        }
    }

    override suspend fun cancel(translationId: String) {
        // Signal any in-flight download to stop at its next cooperative checkpoint,
        // cancel its coroutine, and remove whatever content has been committed so far.
        cancelled.add(translationId)
        inFlight[translationId]?.cancel()
        bibleContentDao.deleteTranslationContent(translationId)
        if (progressState.value?.translationId == translationId) {
            progressState.value = null
        }
    }

    /** True when a cancel has been requested for [translationId] (non-consuming). */
    private fun isCancelled(translationId: String): Boolean =
        cancelled.contains(translationId)

    /**
     * Cleanup for an observed cooperative cancellation: drop any partial content,
     * clear the cancel flag and progress, and report the download as failed/cancelled
     * so it is never presented as complete (Req 5.4, 5.7).
     */
    private suspend fun onCancelled(translationId: String): DownloadOutcome {
        bibleContentDao.deleteTranslationContent(translationId)
        cancelled.remove(translationId)
        if (progressState.value?.translationId == translationId) {
            progressState.value = null
        }
        return DownloadOutcome.Failure("cancelled")
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
