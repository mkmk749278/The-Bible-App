package com.manna.bible.data.repository

import com.manna.bible.data.local.SermonDao
import com.manna.bible.data.local.SermonNoteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** Unit tests for [DefaultSermonRepository] using an in-memory [SermonDao] fake. */
class DefaultSermonRepositoryTest {

    @Test
    @DisplayName("save creates a sermon; an entirely empty note is ignored")
    fun saveCreates() = runTest {
        val repo = DefaultSermonRepository(FakeSermonDao())
        assertEquals(-1, repo.save(0, "  ", "  ", "  "))

        val id = repo.save(0, "  Grace  ", "  Eph 2:8  ", "  By grace through faith  ")
        assertTrue(id >= 1)
        val sermons = repo.observeSermons().first()
        assertEquals(1, sermons.size)
        with(sermons.first()) {
            assertEquals("Grace", title)
            assertEquals("Eph 2:8", reference)
            assertEquals("By grace through faith", content)
        }
    }

    @Test
    @DisplayName("a blank title falls back to the reference")
    fun titleFallsBackToReference() = runTest {
        val repo = DefaultSermonRepository(FakeSermonDao())
        val id = repo.save(0, "", "John 3:16", "God so loved the world")
        val saved = repo.get(id)!!
        assertEquals("John 3:16", saved.title)
    }

    @Test
    @DisplayName("save with an existing id updates in place and preserves createdAt")
    fun updateInPlace() = runTest {
        val repo = DefaultSermonRepository(FakeSermonDao())
        val id = repo.save(0, "Draft", "Rom 8", "First pass")
        val created = repo.get(id)!!.createdAt

        repo.save(id, "Final", "Romans 8", "Polished outline")
        val sermons = repo.observeSermons().first()
        assertEquals(1, sermons.size, "update must not create a second row")
        with(sermons.first()) {
            assertEquals("Final", title)
            assertEquals("Romans 8", reference)
            assertEquals("Polished outline", content)
            assertEquals(created, createdAt, "createdAt must be preserved on update")
        }
    }

    @Test
    @DisplayName("delete removes the sermon")
    fun delete() = runTest {
        val repo = DefaultSermonRepository(FakeSermonDao())
        val id = repo.save(0, "Temp", "", "")
        repo.delete(id)
        assertNull(repo.get(id))
        assertTrue(repo.observeSermons().first().isEmpty())
    }

    /** In-memory [SermonDao] fake backed by a StateFlow so observation works in tests. */
    private class FakeSermonDao : SermonDao {
        private val rows = MutableStateFlow<List<SermonNoteEntity>>(emptyList())
        private var nextId = 1L

        override fun observeAll(): Flow<List<SermonNoteEntity>> =
            rows.map { list -> list.sortedByDescending { it.updatedAt } }

        override suspend fun getById(id: Long): SermonNoteEntity? =
            rows.value.firstOrNull { it.id == id }

        override suspend fun insert(entity: SermonNoteEntity): Long {
            val id = nextId++
            rows.value = rows.value + entity.copy(id = id)
            return id
        }

        override suspend fun update(entity: SermonNoteEntity) {
            rows.value = rows.value.map { if (it.id == entity.id) entity else it }
        }

        override suspend fun deleteById(id: Long) {
            rows.value = rows.value.filterNot { it.id == id }
        }
    }
}
