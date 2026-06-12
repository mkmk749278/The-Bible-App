package com.manna.bible.data.repository

import com.manna.bible.data.local.PrayerDao
import com.manna.bible.data.local.PrayerEntryEntity
import com.manna.bible.domain.model.PrayerStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** Unit tests for [DefaultPrayerRepository] using an in-memory [PrayerDao] fake. */
class DefaultPrayerRepositoryTest {

    @Test
    @DisplayName("add records an active prayer; blank content is ignored")
    fun add() = runTest {
        val repo = DefaultPrayerRepository(FakePrayerDao())
        assertEquals(-1, repo.add("   "))

        val id = repo.add("  Healing for my mother  ")
        assertTrue(id >= 1)
        val prayers = repo.observePrayers().first()
        assertEquals(1, prayers.size)
        assertEquals("Healing for my mother", prayers.first().content)
        assertEquals(PrayerStatus.ACTIVE, prayers.first().status)
        assertNull(prayers.first().answeredAt)
    }

    @Test
    @DisplayName("markAnswered then reopen flips status and the answered timestamp")
    fun answerAndReopen() = runTest {
        val repo = DefaultPrayerRepository(FakePrayerDao())
        val id = repo.add("A new job")

        repo.markAnswered(id)
        repo.observePrayers().first().first().let {
            assertEquals(PrayerStatus.ANSWERED, it.status)
            assertNotNull(it.answeredAt)
        }

        repo.reopen(id)
        repo.observePrayers().first().first().let {
            assertEquals(PrayerStatus.ACTIVE, it.status)
            assertNull(it.answeredAt)
        }
    }

    @Test
    @DisplayName("delete removes the prayer")
    fun delete() = runTest {
        val repo = DefaultPrayerRepository(FakePrayerDao())
        val id = repo.add("Wisdom")
        repo.delete(id)
        assertTrue(repo.observePrayers().first().isEmpty())
    }

    private class FakePrayerDao : PrayerDao {
        private val rows = MutableStateFlow<List<PrayerEntryEntity>>(emptyList())
        private var nextId = 1L

        override fun observeAll(): Flow<List<PrayerEntryEntity>> =
            rows.map { list -> list.sortedByDescending { it.createdAt } }

        override suspend fun getById(id: Long): PrayerEntryEntity? =
            rows.value.firstOrNull { it.id == id }

        override suspend fun insert(entity: PrayerEntryEntity): Long {
            val id = nextId++
            rows.value = rows.value + entity.copy(id = id)
            return id
        }

        override suspend fun update(entity: PrayerEntryEntity) {
            rows.value = rows.value.map { if (it.id == entity.id) entity else it }
        }

        override suspend fun deleteById(id: Long) {
            rows.value = rows.value.filterNot { it.id == id }
        }
    }
}
