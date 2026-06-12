package com.manna.bible.ui.prayer

import app.cash.turbine.test
import com.manna.bible.domain.model.Prayer
import com.manna.bible.domain.model.PrayerStatus
import com.manna.bible.domain.repository.PrayerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** Unit tests for [PrayerJournalViewModel] with a fake [PrayerRepository]. */
class PrayerJournalViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeEach fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterEach fun tearDown() { Dispatchers.resetMain() }

    @Test
    @DisplayName("adds prayers and splits them into active and answered")
    fun addAndSplit() = runTest {
        val repo = FakeRepo()
        val vm = PrayerJournalViewModel(repo)
        vm.uiState.test {
            advanceUntilIdle()

            vm.onDraftChange("Pray for peace")
            vm.add()
            advanceUntilIdle()
            var state = expectMostRecentItem()
            assertEquals(1, state.active.size)
            assertEquals("", state.draft, "draft clears after add")

            val id = repo.prayers.value.first().id
            vm.markAnswered(id)
            advanceUntilIdle()
            state = expectMostRecentItem()
            assertTrue(state.active.isEmpty())
            assertEquals(1, state.answered.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private class FakeRepo : PrayerRepository {
        val prayers = MutableStateFlow<List<Prayer>>(emptyList())
        private var nextId = 1L

        override fun observePrayers(): Flow<List<Prayer>> = prayers

        override suspend fun add(content: String): Long {
            val trimmed = content.trim()
            if (trimmed.isEmpty()) return -1
            val id = nextId++
            prayers.value = prayers.value + Prayer(
                id = id, content = trimmed, status = PrayerStatus.ACTIVE, createdAt = id
            )
            return id
        }

        override suspend fun markAnswered(id: Long) {
            prayers.value = prayers.value.map {
                if (it.id == id) it.copy(status = PrayerStatus.ANSWERED, answeredAt = 100L) else it
            }
        }

        override suspend fun reopen(id: Long) {
            prayers.value = prayers.value.map {
                if (it.id == id) it.copy(status = PrayerStatus.ACTIVE, answeredAt = null) else it
            }
        }

        override suspend fun delete(id: Long) {
            prayers.value = prayers.value.filterNot { it.id == id }
        }
    }
}
