package com.manna.bible.ui.sermon

import app.cash.turbine.test
import com.manna.bible.domain.repository.SermonRepository
import com.manna.bible.domain.sermon.SermonNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** Unit tests for [SermonHelperViewModel] — list + editor over a fake repository. */
class SermonHelperViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    @DisplayName("new → edit fields → save persists and closes the editor")
    fun newSaveFlow() = runTest {
        val repo = FakeSermonRepository()
        val vm = SermonHelperViewModel(repo)

        vm.uiState.test {
            assertNull(awaitItem().draft) // list view initially

            vm.newSermon()
            assertNotNull(awaitItem().draft)

            vm.updateTitle("Light of the world")
            assertEquals("Light of the world", awaitItem().draft?.title)

            vm.updateReference("John 8:12")
            awaitItem()
            vm.updateContent("Walk in the light")
            awaitItem()

            vm.save()
            advanceUntilIdle()
            // Editor closes and the saved sermon shows up in the list.
            val saved = awaitItem()
            assertNull(saved.draft)
            assertEquals(1, saved.sermons.size)
            assertEquals("Light of the world", saved.sermons.first().title)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("saving an empty draft persists nothing")
    fun emptyDraftNotSaved() = runTest {
        val repo = FakeSermonRepository()
        val vm = SermonHelperViewModel(repo)

        vm.newSermon()
        vm.save()
        advanceUntilIdle()

        assertTrue(repo.current().isEmpty())
    }

    @Test
    @DisplayName("deleteCurrent removes an existing sermon")
    fun deleteExisting() = runTest {
        val repo = FakeSermonRepository()
        repo.seed(SermonNote(1, "Old", "Ps 23", "Shepherd", 1L, 1L))
        val vm = SermonHelperViewModel(repo)
        advanceUntilIdle()

        vm.edit(repo.current().first())
        vm.deleteCurrent()
        advanceUntilIdle()

        assertTrue(repo.current().isEmpty())
    }

    private class FakeSermonRepository : SermonRepository {
        private val rows = MutableStateFlow<List<SermonNote>>(emptyList())
        private var nextId = 1L

        fun current() = rows.value
        fun seed(note: SermonNote) {
            rows.value = rows.value + note
            nextId = maxOf(nextId, note.id + 1)
        }

        override fun observeSermons(): Flow<List<SermonNote>> =
            rows.map { list -> list.sortedByDescending { it.updatedAt } }

        override suspend fun get(id: Long): SermonNote? = rows.value.firstOrNull { it.id == id }

        override suspend fun save(id: Long, title: String, reference: String, content: String): Long {
            val resolvedTitle = title.trim().ifEmpty { reference.trim().ifEmpty { "Untitled sermon" } }
            if (title.isBlank() && reference.isBlank() && content.isBlank()) return -1
            return if (id <= 0L) {
                val newId = nextId++
                rows.value = rows.value + SermonNote(newId, resolvedTitle, reference.trim(), content.trim(), 1L, 1L)
                newId
            } else {
                rows.value = rows.value.map {
                    if (it.id == id) it.copy(title = resolvedTitle, reference = reference.trim(), content = content.trim()) else it
                }
                id
            }
        }

        override suspend fun delete(id: Long) {
            rows.value = rows.value.filterNot { it.id == id }
        }
    }
}
