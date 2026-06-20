package com.manna.bible.data.download

import com.manna.bible.data.ConnectivityChecker
import com.manna.bible.data.local.BibleContentDao
import com.manna.bible.data.local.BookEntity
import com.manna.bible.data.local.ChapterEntity
import com.manna.bible.data.local.TranslationDao
import com.manna.bible.data.local.TranslationEntity
import com.manna.bible.data.local.VerseEntity
import com.manna.bible.data.remote.HelloAoRemoteDataSource
import com.manna.bible.data.remote.RemoteBook
import com.manna.bible.data.remote.RemoteChapter
import com.manna.bible.data.remote.RemoteVerse
import com.manna.bible.domain.download.DownloadOutcome
import com.manna.bible.domain.repository.PendingDownloadRepository
import com.manna.bible.domain.translation.Translation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [DefaultDownloadManager] using pure in-memory fakes (no Room, no
 * mocks) so the logic runs on the JVM in GitHub Actions CI.
 *
 * Validates: Requirements 5.1, 5.3, 5.4, 5.5, 5.6, 5.7, 11.5, 15.4
 */
class DefaultDownloadManagerTest {

    // --- fakes ---------------------------------------------------------------

    /**
     * In-memory [HelloAoRemoteDataSource]. Serves a fixed book/verse structure and
     * can be told to throw when a particular (book, chapter) is fetched.
     */
    private class FakeRemote(
        private val books: List<RemoteBook>,
        private val versesPerChapter: Int = 2,
        private val failOn: Pair<String, Int>? = null
    ) : HelloAoRemoteDataSource {

        /** Records every (osisId, chapter) fetched, to assert resume skips stored books. */
        val chapterCalls = mutableListOf<Pair<String, Int>>()

        override suspend fun books(id: String): List<RemoteBook> = books

        override suspend fun chapter(id: String, osisId: String, chapter: Int): RemoteChapter {
            chapterCalls += osisId to chapter
            if (failOn == osisId to chapter) throw RuntimeException("boom")
            val verses = (1..versesPerChapter).map { v ->
                RemoteVerse(verse = v, text = "$osisId $chapter:$v")
            }
            return RemoteChapter(osisId = osisId, chapter = chapter, verses = verses)
        }

        override suspend fun fetchCatalog(): List<Translation> = emptyList()
        override suspend fun downloadTranslation(id: String): Long = 0L
    }

    /** In-memory [BibleContentDao]; only the abstract members are implemented. */
    private class FakeContentDao : BibleContentDao {
        val books = mutableListOf<BookEntity>()
        val chapters = mutableListOf<ChapterEntity>()
        val verses = mutableListOf<VerseEntity>()
        var deleteCount = 0

        override fun observeBooks(translationId: String): Flow<List<BookEntity>> =
            MutableStateFlow(books.filter { it.translationId == translationId })

        override suspend fun getBook(translationId: String, osisId: String): BookEntity? =
            books.firstOrNull { it.translationId == translationId && it.osisId == osisId }

        override suspend fun getChapter(t: String, b: String, c: Int): List<VerseEntity> =
            verses.filter { it.translationId == t && it.osisId == b && it.chapter == c }

        override suspend fun getChapterMeta(
            translationId: String,
            osisId: String,
            chapter: Int
        ): ChapterEntity? = chapters.firstOrNull {
            it.translationId == translationId && it.osisId == osisId && it.chapter == chapter
        }

        override suspend fun hasAnyContent(t: String): Boolean =
            verses.any { it.translationId == t }

        override suspend fun countVerses(translationId: String): Int =
            verses.count { it.translationId == translationId }

        override suspend fun sumTextLength(translationId: String): Long =
            verses.filter { it.translationId == translationId }.sumOf { it.text.length.toLong() }

        override suspend fun search(t: String, query: String, limit: Int): List<VerseEntity> =
            emptyList()

        override suspend fun searchInBooks(
            t: String,
            query: String,
            osisIds: List<String>,
            limit: Int
        ): List<VerseEntity> = emptyList()

        override suspend fun insertBooks(list: List<BookEntity>) {
            books += list
        }

        override suspend fun insertChapters(list: List<ChapterEntity>) {
            chapters += list
        }

        override suspend fun insertVerses(list: List<VerseEntity>) {
            verses += list
        }

        override suspend fun deleteBooks(t: String) {
            books.removeAll { it.translationId == t }
        }

        override suspend fun deleteChapters(t: String) {
            chapters.removeAll { it.translationId == t }
        }

        override suspend fun deleteVerses(t: String) {
            deleteCount++
            verses.removeAll { it.translationId == t }
        }
    }

    /** In-memory [TranslationDao]; records download state per id. */
    private class FakeTranslationDao(seed: List<TranslationEntity> = emptyList()) : TranslationDao {
        val rows = seed.associateBy { it.id }.toMutableMap()

        override fun observeAll(): Flow<List<TranslationEntity>> =
            MutableStateFlow(rows.values.toList())

        override suspend fun upsertAll(translations: List<TranslationEntity>) {
            translations.forEach { rows[it.id] = it }
        }

        override suspend fun setDownloaded(id: String, sizeBytes: Long) {
            rows[id]?.let { rows[id] = it.copy(isDownloaded = true, sizeBytes = sizeBytes) }
        }

        override suspend fun setDownloadedContent(
            id: String,
            sizeBytes: Long,
            contentVersion: Int,
            verseCount: Int
        ) {
            val existing = rows[id] ?: TranslationEntity(
                id = id,
                name = id,
                languageCode = "en",
                canonType = "protestant_66",
                hasDeuterocanon = false,
                isDownloaded = false,
                sizeBytes = 0,
                isDefaultForCanon = false
            )
            rows[id] = existing.copy(
                isDownloaded = true,
                sizeBytes = sizeBytes,
                contentVersion = contentVersion,
                verseCount = verseCount
            )
        }

        override suspend fun clearDownloaded(id: String) {
            rows[id]?.let {
                rows[id] = it.copy(
                    isDownloaded = false,
                    contentVersion = 0,
                    verseCount = 0,
                    sizeBytes = 0
                )
            }
        }

        override suspend fun getById(id: String): TranslationEntity? = rows[id]
    }

    private class FakePending : PendingDownloadRepository {
        val ids = MutableStateFlow<List<String>>(emptyList())
        override fun pending(): Flow<List<String>> = ids
        override suspend fun add(id: String) {
            if (id !in ids.value) ids.value = ids.value + id
        }

        override suspend fun remove(id: String) {
            ids.value = ids.value - id
        }

        override suspend fun all(): List<String> = ids.value
    }

    private class FakeConnectivity(var online: Boolean) : ConnectivityChecker {
        override fun isOnline(): Boolean = online
    }

    // --- helpers -------------------------------------------------------------

    private fun book(osisId: String, chapters: Int, order: Int) = RemoteBook(
        osisId = osisId,
        name = osisId,
        testament = "OLD",
        orderIndex = order,
        chapterCount = chapters
    )

    private fun entity(id: String) = TranslationEntity(
        id = id,
        name = id,
        languageCode = "en",
        canonType = "protestant_66",
        hasDeuterocanon = false,
        isDownloaded = false,
        sizeBytes = 0,
        isDefaultForCanon = false
    )

    private fun manager(
        scope: CoroutineScope,
        remote: FakeRemote,
        content: FakeContentDao = FakeContentDao(),
        translations: FakeTranslationDao = FakeTranslationDao(),
        pending: FakePending = FakePending(),
        connectivity: FakeConnectivity = FakeConnectivity(online = true)
    ) = DefaultDownloadManager(remote, content, translations, pending, connectivity, NoopForeground, scope)

    /** Foreground promotion is an Android concern; the JVM tests don't exercise it. */
    private object NoopForeground : com.manna.bible.domain.download.DownloadForegroundController {
        override fun ensureRunning() {}
    }

    // --- offline (Req 5.6) ---------------------------------------------------

    @Test
    fun `offline download queues the id and stores nothing`() = runTest {
        val content = FakeContentDao()
        val translations = FakeTranslationDao(listOf(entity("en_web")))
        val pending = FakePending()
        val mgr = manager(
            scope = backgroundScope,
            remote = FakeRemote(listOf(book("GEN", 1, 0))),
            content = content,
            translations = translations,
            pending = pending,
            connectivity = FakeConnectivity(online = false)
        )

        val outcome = mgr.download("en_web")

        assertEquals(DownloadOutcome.Offline, outcome)
        assertTrue(pending.all().contains("en_web"), "offline request must be queued")
        assertFalse(content.hasAnyContent("en_web"), "nothing must be stored offline")
        assertFalse(translations.getById("en_web")!!.isDownloaded, "must not be marked downloaded")
    }

    // --- online success (Req 5.1, 5.3, 15.4) ---------------------------------

    @Test
    fun `online success stores content marks downloaded clears pending and finishes progress`() =
        runTest {
            val content = FakeContentDao()
            val translations = FakeTranslationDao(listOf(entity("en_web")))
            val pending = FakePending().apply { add("en_web") }
            val mgr = manager(
            scope = backgroundScope,
                remote = FakeRemote(
                    books = listOf(book("GEN", 2, 0), book("EXO", 1, 1)),
                    versesPerChapter = 2
                ),
                content = content,
                translations = translations,
                pending = pending
            )

            val outcome = mgr.download("en_web")

            assertEquals(DownloadOutcome.Success, outcome)
            // 3 chapters * 2 verses = 6 verses stored across 2 books.
            assertEquals(6, content.countVerses("en_web"))
            assertEquals(2, content.books.count { it.translationId == "en_web" })

            val row = translations.getById("en_web")!!
            assertTrue(row.isDownloaded, "must be marked downloaded on success")
            assertEquals(6, row.verseCount)
            assertEquals(1, row.contentVersion, "first download commits content version 1")

            assertFalse(pending.all().contains("en_web"), "pending entry must be cleared")

            val progress = mgr.progress().value
            assertNotNull(progress)
            assertTrue(progress!!.done, "progress must reach done")
            assertEquals(3, progress.totalChapters)
            assertEquals(3, progress.completedChapters)
        }

    // --- resume (Req 5: resumable downloads) ---------------------------------

    @Test
    fun `resume skips already-stored books and completes from where it stopped`() = runTest {
        val content = FakeContentDao()
        val translations = FakeTranslationDao(listOf(entity("en_web")))
        // Simulate GEN already stored by a prior, interrupted download attempt.
        content.books += BookEntity("en_web", "GEN", "Genesis", "OT", 0, 1)
        content.chapters += ChapterEntity("en_web", "GEN", 1, 1)
        content.verses += VerseEntity("en_web", "GEN", 1, 1, "in the beginning")

        val remote = FakeRemote(books = listOf(book("GEN", 1, 0), book("EXO", 1, 1)))
        val mgr = manager(
            scope = backgroundScope,
            remote = remote,
            content = content,
            translations = translations
        )

        val outcome = mgr.download("en_web")

        assertEquals(DownloadOutcome.Success, outcome)
        assertFalse(
            remote.chapterCalls.contains("GEN" to 1),
            "already-stored GEN must not be refetched on resume"
        )
        assertTrue(remote.chapterCalls.contains("EXO" to 1), "missing EXO must be fetched")
        assertTrue(translations.getById("en_web")!!.isDownloaded, "resumed download completes")
    }

    // --- online failure (Req 5.7) --------------------------------------------

    @Test
    fun `online failure deletes partial content and does not mark downloaded`() = runTest {
        val content = FakeContentDao()
        val translations = FakeTranslationDao(listOf(entity("en_web")))
        val pending = FakePending()
        // Two books; fail on the second book's first chapter after the first book commits.
        val mgr = manager(
            scope = backgroundScope,
            remote = FakeRemote(
                books = listOf(book("GEN", 1, 0), book("EXO", 1, 1)),
                failOn = "EXO" to 1
            ),
            content = content,
            translations = translations,
            pending = pending
        )

        val outcome = mgr.download("en_web")

        assertTrue(outcome is DownloadOutcome.Failure)
        assertEquals("boom", (outcome as DownloadOutcome.Failure).reason)
        assertFalse(content.hasAnyContent("en_web"), "partial content must be removed")
        assertTrue(content.deleteCount > 0, "deleteTranslationContent must run on failure")
        assertFalse(translations.getById("en_web")!!.isDownloaded, "must not mark downloaded")
        assertNull(mgr.progress().value)
    }

    // --- delete (Req 5.5) ----------------------------------------------------

    @Test
    fun `delete clears content and downloaded marker`() = runTest {
        val content = FakeContentDao()
        val translations = FakeTranslationDao(listOf(entity("en_web")))
        val mgr = manager(
            scope = backgroundScope,
            remote = FakeRemote(listOf(book("GEN", 1, 0))),
            content = content,
            translations = translations
        )

        // First download so there is content to delete.
        mgr.download("en_web")
        assertTrue(content.hasAnyContent("en_web"))
        assertTrue(translations.getById("en_web")!!.isDownloaded)

        mgr.delete("en_web")

        assertFalse(content.hasAnyContent("en_web"), "content must be cleared on delete")
        assertFalse(translations.getById("en_web")!!.isDownloaded, "marker must be cleared")
    }

    // --- cancel mid-download (Req 5.4, 5.7) ----------------------------------

    /**
     * Remote that serves a fixed structure but blocks on a specific (book, chapter)
     * until [gate] completes, letting a test deterministically cancel a download
     * while a chapter fetch is in flight.
     */
    private class GatedRemote(
        private val books: List<RemoteBook>,
        private val blockOn: Pair<String, Int>,
        private val gate: kotlinx.coroutines.CompletableDeferred<Unit>
    ) : HelloAoRemoteDataSource {
        override suspend fun books(id: String): List<RemoteBook> = books
        override suspend fun chapter(id: String, osisId: String, chapter: Int): RemoteChapter {
            if (blockOn == osisId to chapter) gate.await()
            return RemoteChapter(
                osisId = osisId,
                chapter = chapter,
                verses = listOf(RemoteVerse(verse = 1, text = "$osisId $chapter:1"))
            )
        }

        override suspend fun fetchCatalog(): List<Translation> = emptyList()
        override suspend fun downloadTranslation(id: String): Long = 0L
    }

    @Test
    fun `cancel mid-download deletes content and does not mark downloaded`() = runTest {
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        val content = FakeContentDao()
        val translations = FakeTranslationDao(listOf(entity("en_web")))
        // GEN (1 chapter) commits first; EXO's 2nd chapter blocks so we can cancel
        // after some content is already persisted but before the download finishes.
        val mgr = DefaultDownloadManager(
            remote = GatedRemote(
                books = listOf(book("GEN", 1, 0), book("EXO", 2, 1)),
                blockOn = "EXO" to 2,
                gate = gate
            ),
            bibleContentDao = content,
            translationDao = translations,
            pending = FakePending(),
            connectivity = FakeConnectivity(online = true),
            foregroundController = NoopForeground,
            scope = backgroundScope
        )

        val job = launch { mgr.download("en_web") }
        // Run until the download suspends awaiting the gate on EXO chapter 2.
        runCurrent()
        // GEN was committed before the block, proving there is content to delete.
        assertTrue(content.hasAnyContent("en_web"), "GEN content should be committed by now")

        mgr.cancel("en_web")
        gate.complete(Unit)
        job.join()

        assertFalse(content.hasAnyContent("en_web"), "cancel must delete partial content")
        assertFalse(
            translations.getById("en_web")!!.isDownloaded,
            "cancelled download must not be marked downloaded"
        )
    }

    // --- retry pending (Req 5.6, 11.5) ---------------------------------------

    @Test
    fun `retryPending online attempts each pending id`() = runTest {
        val content = FakeContentDao()
        val translations = FakeTranslationDao(listOf(entity("a"), entity("b")))
        val pending = FakePending().apply {
            add("a")
            add("b")
        }
        val mgr = manager(
            scope = backgroundScope,
            remote = FakeRemote(listOf(book("GEN", 1, 0))),
            content = content,
            translations = translations,
            pending = pending
        )

        mgr.retryPending()

        assertTrue(translations.getById("a")!!.isDownloaded, "pending id a must be downloaded")
        assertTrue(translations.getById("b")!!.isDownloaded, "pending id b must be downloaded")
        assertTrue(pending.all().isEmpty(), "successful retries clear the queue")
    }
}
