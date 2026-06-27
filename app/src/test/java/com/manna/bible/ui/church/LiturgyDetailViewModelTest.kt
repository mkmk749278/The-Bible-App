package com.manna.bible.ui.church

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.manna.bible.domain.model.Denomination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Unit tests for [LiturgyDetailViewModel] (Task 8). */
@OptIn(ExperimentalCoroutinesApi::class)
class LiturgyDetailViewModelTest {

    @BeforeEach fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterEach fun tearDown() = Dispatchers.resetMain()

    private val mass = testLiturgy("roman_catholic_mass", tradition = "Roman Catholic")
    private val communion = testLiturgy("csi_holy_communion", tradition = "Church of South India")

    private fun viewModel(id: String?, bibleLanguage: String = "en") = LiturgyDetailViewModel(
        liturgyProvider = FakeLiturgyProvider(listOf(mass, communion)),
        preferencesStore = FakeLiturgyPreferencesStore(Denomination.CATHOLIC, bibleLanguage),
        savedStateHandle = SavedStateHandle(mapOf(LITURGY_ID_ARG to id))
    )

    @Test
    fun `loads the liturgy named by the nav argument and exposes the Bible language`() = runTest {
        viewModel("csi_holy_communion", bibleLanguage = "te").uiState.test {
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertEquals("csi_holy_communion", state.liturgy?.id)
            assertEquals("te", state.bibleLanguage)
        }
    }

    @Test
    fun `an unknown id yields a null liturgy without loading`() = runTest {
        viewModel("does_not_exist").uiState.test {
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertNull(state.liturgy)
        }
    }
}
