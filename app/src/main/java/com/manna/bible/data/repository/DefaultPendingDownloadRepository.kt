package com.manna.bible.data.repository

import com.manna.bible.data.local.PendingDownloadDao
import com.manna.bible.data.local.PendingDownloadEntity
import com.manna.bible.domain.repository.PendingDownloadRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Room-backed [PendingDownloadRepository] mapping between [PendingDownloadEntity]
 * rows and bare translation ids.
 */
class DefaultPendingDownloadRepository @Inject constructor(
    private val dao: PendingDownloadDao
) : PendingDownloadRepository {

    override fun pending(): Flow<List<String>> =
        dao.observeAll().map { rows -> rows.map { it.translationId } }

    override suspend fun add(id: String) {
        dao.insert(
            PendingDownloadEntity(
                translationId = id,
                requestedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun remove(id: String) {
        dao.deleteById(id)
    }

    override suspend fun all(): List<String> =
        dao.getAll().map { it.translationId }
}
