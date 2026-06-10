package com.manna.bible.data.repository

import com.manna.bible.data.ConnectivityChecker
import com.manna.bible.data.TranslationLocalDataSource
import com.manna.bible.data.TranslationRemoteDataSource
import com.manna.bible.domain.repository.DownloadResult
import com.manna.bible.domain.repository.PendingDownloadRepository
import com.manna.bible.domain.repository.TranslationRepository
import com.manna.bible.domain.translation.Translation
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Offline-first [TranslationRepository] (Req 13).
 *
 * Reads the catalog from the local cache, refreshes from the network only when
 * online, and queues downloads requested while offline so they can be retried once
 * a connection is available.
 */
class DefaultTranslationRepository @Inject constructor(
    private val local: TranslationLocalDataSource,
    private val pending: PendingDownloadRepository,
    private val remote: TranslationRemoteDataSource,
    private val connectivity: ConnectivityChecker
) : TranslationRepository {

    override fun catalog(): Flow<List<Translation>> = local.catalog()

    override suspend fun refreshCatalog() {
        if (connectivity.isOnline()) {
            local.upsertAll(remote.fetchCatalog())
        }
    }

    override suspend fun download(id: String): DownloadResult {
        if (!connectivity.isOnline()) {
            markPendingDownload(id)
            return DownloadResult.Offline
        }
        return try {
            val size = remote.downloadTranslation(id)
            local.setDownloaded(id, size)
            pending.remove(id)
            DownloadResult.Success
        } catch (e: Exception) {
            DownloadResult.Failure(e.message ?: "download failed")
        }
    }

    override suspend fun markPendingDownload(id: String) {
        pending.add(id)
    }

    override suspend fun retryPendingDownloads() {
        if (connectivity.isOnline()) {
            pending.all().forEach { download(it) }
        }
    }
}
