package com.manna.bible.data.local

import com.manna.bible.data.TranslationLocalDataSource
import com.manna.bible.domain.translation.Translation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Room-backed [TranslationLocalDataSource] that maps between [TranslationEntity]
 * and the pure-domain [Translation].
 */
class RoomTranslationLocalDataSource @Inject constructor(
    private val dao: TranslationDao
) : TranslationLocalDataSource {

    override fun catalog(): Flow<List<Translation>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun upsertAll(translations: List<Translation>) {
        dao.upsertAll(translations.map { it.toEntity() })
    }

    override suspend fun setDownloaded(id: String, sizeBytes: Long) {
        dao.setDownloaded(id, sizeBytes)
    }
}
