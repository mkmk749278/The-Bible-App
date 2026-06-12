package com.manna.bible.data.repository

import com.manna.bible.data.local.PrayerDao
import com.manna.bible.data.local.PrayerEntryEntity
import com.manna.bible.data.local.toDomain
import com.manna.bible.domain.model.Prayer
import com.manna.bible.domain.model.PrayerStatus
import com.manna.bible.domain.repository.PrayerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed [PrayerRepository]. Maps entities to/from the pure-domain [Prayer] and
 * stamps timestamps with the system clock. Blank content is ignored on [add].
 */
@Singleton
class DefaultPrayerRepository @Inject constructor(
    private val prayerDao: PrayerDao
) : PrayerRepository {

    override fun observePrayers(): Flow<List<Prayer>> =
        prayerDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun add(content: String): Long {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return -1
        return prayerDao.insert(
            PrayerEntryEntity(
                content = trimmed,
                status = PrayerStatus.ACTIVE.name,
                createdAt = System.currentTimeMillis(),
                answeredAt = null
            )
        )
    }

    override suspend fun markAnswered(id: Long) {
        val existing = prayerDao.getById(id) ?: return
        prayerDao.update(
            existing.copy(status = PrayerStatus.ANSWERED.name, answeredAt = System.currentTimeMillis())
        )
    }

    override suspend fun reopen(id: Long) {
        val existing = prayerDao.getById(id) ?: return
        prayerDao.update(existing.copy(status = PrayerStatus.ACTIVE.name, answeredAt = null))
    }

    override suspend fun delete(id: Long) {
        prayerDao.deleteById(id)
    }
}
