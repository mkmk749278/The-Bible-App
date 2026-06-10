package com.manna.bible.data.repository

import com.manna.bible.data.ConnectivityChecker
import com.manna.bible.data.TranslationLocalDataSource
import com.manna.bible.data.TranslationRemoteDataSource
import com.manna.bible.domain.model.CanonType
import com.manna.bible.domain.repository.DownloadResult
import com.manna.bible.domain.repository.PendingDownloadRepository
import com.manna.bible.domain.translation.Translation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [DefaultTranslationRepository] offline-first download behavior.
 *
 * Uses pure in-memory fakes (no mocks, no Room) so the logic runs on the JVM in
 * GitHub Actions CI.
 *
 * Validates: Requirements 13
 */
class DefaultTranslationRepositoryTest {

    // --- fakes ---------------------------------------------------------------

    /** In-memory catalog cache; records whether setDownloaded was called. */
    private class FakeLocal : TranslationLocalDataSource {
        val state = MutableStateFlow<List<Translation>>(emptyList())
        val downloaded = mutableMapOf<String, Long>()

        override fun catalog(): Flow<List<Translation>> = state
        override suspend fun upsertAll(translations: List<Translation>) {
            state.value = translations
        }

        override suspend fun setDownloaded(id: String, sizeBytes: Long) {
            downloaded[id] = sizeBytes
            state.value = state.value.map {
                if (it.id == id) it.copy(isDownloaded = true) else it
            }
        }
    }

    /** Remote that returns a fixed catalog and either succeeds or throws on download. */
    private class FakeRemote(
        private val catalog: List<Translation> = emptyList(),
        private val downloadSize: Long = 1_024L,
        private val failWith: Exception? = null
    ) : TranslationRemoteDataSource {
        var downloadAttempts = mutableListOf<String>()

        override suspend fun fetchCatalog(): List<Translation> = catalog
        override suspend fun downloadTranslation(id: String): Long {
            downloadAttempts.add(id)
            failWith?.let { throw it }
            return downloadSize
        }
    }

    private class FakeConnectivity(var online: Boolean) : ConnectivityChecker {
        override fun isOnline(): Boolean = online
    }

    /** Simple in-memory pending queue backed by a set. */
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

    private fun translation(id: String, downloaded: Boolean = false) = Translation(
        id = id,
        name = id,
        languageCode = "en",
        canonType = CanonType.PROTESTANT_66,
        hasDeuterocanon = false,
        isDownloaded = downloaded
    )

    private fun repository(
        local: FakeLocal = FakeLocal(),
        pending: FakePending = FakePending(),
        remote: FakeRemote = FakeRemote(),
        connectivity: FakeConnectivity = FakeConnectivity(online = true)
    ) = DefaultTranslationRepository(local, pending, remote, connectivity)

    // --- offline download (Req 13.1) ----------------------------------------

    @Test
    fun `offline download returns Offline and queues the id without downloading`() = runTest {
        val local = FakeLocal()
        val pending = FakePending()
        val remote = FakeRemote()
        val repo = repository(
            local = local,
            pending = pending,
            remote = remote,
            connectivity = FakeConnectivity(online = false)
        )

        val result = repo.download("ml_cath")

        assertEquals(DownloadResult.Offline, result)
        assertTrue(pending.all().contains("ml_cath"), "id must be queued while offline")
        assertFalse(local.downloaded.containsKey("ml_cath"), "must not mark downloaded offline")
        assertTrue(remote.downloadAttempts.isEmpty(), "must not hit the network offline")
    }

    // --- online success (Req 13.2, 13.3) -------------------------------------

    @Test
    fun `online success returns Success marks downloaded and clears pending`() = runTest {
        val local = FakeLocal()
        val pending = FakePending().apply { add("en_kjv") }
        val remote = FakeRemote(downloadSize = 2_048L)
        val repo = repository(
            local = local,
            pending = pending,
            remote = remote,
            connectivity = FakeConnectivity(online = true)
        )

        val result = repo.download("en_kjv")

        assertEquals(DownloadResult.Success, result)
        assertEquals(2_048L, local.downloaded["en_kjv"], "setDownloaded must record size")
        assertFalse(pending.all().contains("en_kjv"), "pending entry must be removed on success")
    }

    // --- online failure (Req 13.4) -------------------------------------------

    @Test
    fun `online remote failure returns Failure and does not mark downloaded`() = runTest {
        val local = FakeLocal()
        val remote = FakeRemote(failWith = RuntimeException("boom"))
        val repo = repository(
            local = local,
            remote = remote,
            connectivity = FakeConnectivity(online = true)
        )

        val result = repo.download("en_kjv")

        assertTrue(result is DownloadResult.Failure)
        assertEquals("boom", (result as DownloadResult.Failure).reason)
        assertFalse(local.downloaded.containsKey("en_kjv"), "failed download must not mark downloaded")
    }

    @Test
    fun `online failure with null message falls back to default reason`() = runTest {
        val remote = FakeRemote(failWith = RuntimeException())
        val repo = repository(remote = remote, connectivity = FakeConnectivity(online = true))

        val result = repo.download("en_kjv")

        assertTrue(result is DownloadResult.Failure)
        assertEquals("download failed", (result as DownloadResult.Failure).reason)
    }

    // --- markPendingDownload (Req 13.1) --------------------------------------

    @Test
    fun `markPendingDownload queues the id`() = runTest {
        val pending = FakePending()
        val repo = repository(pending = pending)

        repo.markPendingDownload("te_cath")

        assertTrue(pending.all().contains("te_cath"))
    }

    // --- retryPendingDownloads (Req 13.4) ------------------------------------

    @Test
    fun `retryPendingDownloads online attempts each pending id`() = runTest {
        val pending = FakePending().apply {
            add("a")
            add("b")
        }
        val remote = FakeRemote(downloadSize = 10L)
        val repo = repository(
            pending = pending,
            remote = remote,
            connectivity = FakeConnectivity(online = true)
        )

        repo.retryPendingDownloads()

        assertEquals(setOf("a", "b"), remote.downloadAttempts.toSet())
        assertTrue(pending.all().isEmpty(), "successful retries clear the queue")
    }

    @Test
    fun `retryPendingDownloads offline attempts nothing`() = runTest {
        val pending = FakePending().apply { add("a") }
        val remote = FakeRemote()
        val repo = repository(
            pending = pending,
            remote = remote,
            connectivity = FakeConnectivity(online = false)
        )

        repo.retryPendingDownloads()

        assertTrue(remote.downloadAttempts.isEmpty(), "must not retry while offline")
        assertTrue(pending.all().contains("a"), "pending entries remain while offline")
    }

    // --- refreshCatalog (Req 13) ---------------------------------------------

    @Test
    fun `refreshCatalog online populates local cache`() = runTest {
        val local = FakeLocal()
        val remote = FakeRemote(catalog = listOf(translation("en_kjv")))
        val repo = repository(
            local = local,
            remote = remote,
            connectivity = FakeConnectivity(online = true)
        )

        repo.refreshCatalog()

        assertEquals(listOf("en_kjv"), local.state.value.map { it.id })
    }

    @Test
    fun `refreshCatalog offline leaves cache untouched`() = runTest {
        val local = FakeLocal().apply { state.value = listOf(translation("cached")) }
        val remote = FakeRemote(catalog = listOf(translation("en_kjv")))
        val repo = repository(
            local = local,
            remote = remote,
            connectivity = FakeConnectivity(online = false)
        )

        repo.refreshCatalog()

        assertEquals(listOf("cached"), local.state.value.map { it.id })
    }
}
