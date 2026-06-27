package com.manna.bible.ui.church

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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Unit tests for [LiturgyLibraryViewModel] (Task 7.5). */
@OptIn(ExperimentalCoroutinesApi::class)
class LiturgyLibraryViewModelTest {

    @BeforeEach fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterEach fun tearDown() = Dispatchers.resetMain()

    private val catholicMass = testLiturgy(
        "roman_catholic_mass",
        title = mapOf("en" to "The Holy Mass"),
        tradition = "Roman Catholic"
    )
    private val csiCommunion = testLiturgy(
        "csi_holy_communion",
        title = mapOf("en" to "The Holy Communion"),
        tradition = "Church of South India"
    )
    private val mapping = mapOf(
        Denomination.CATHOLIC to listOf("roman_catholic_mass"),
        Denomination.CSI to listOf("csi_holy_communion")
    )

    private fun viewModel(denomination: Denomination?, bibleLanguage: String = "en") =
        LiturgyLibraryViewModel(
            liturgyProvider = FakeLiturgyProvider(listOf(catholicMass, csiCommunion), mapping),
            preferencesStore = FakeLiturgyPreferencesStore(denomination, bibleLanguage)
        )

    @Test
    fun `entries surface the mapped liturgy first and list the full library`() = runTest {
        viewModel(Denomination.CSI).uiState.test {
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertEquals(listOf("csi_holy_communion", "roman_catholic_mass"), state.entries.map { it.id })
            assertTrue(state.denominationHasMapping)
            assertEquals("The Holy Communion", state.entries.first().title)
            assertEquals("Church of South India", state.entries.first().tradition)
        }
    }

    @Test
    fun `an unmapped denomination still lists the full library with the prepared note flagged`() = runTest {
        viewModel(Denomination.ORTHODOX).uiState.test {
            val state = expectMostRecentItem()
            assertFalse(state.denominationHasMapping)
            assertEquals(setOf("roman_catholic_mass", "csi_holy_communion"), state.entries.map { it.id }.toSet())
        }
    }

    @Test
    fun `titles resolve in the Bible language with English fallback`() = runTest {
        val mass = testLiturgy(
            "roman_catholic_mass",
            title = mapOf("en" to "The Holy Mass", "ta" to "\u0BA4\u0BBF\u0BB0\u0BC1\u0BAA\u0BCD\u0BAA\u0BB2\u0BBF"),
            tradition = "Roman Catholic"
        )
        val vm = LiturgyLibraryViewModel(
            FakeLiturgyProvider(listOf(mass), mapOf(Denomination.CATHOLIC to listOf("roman_catholic_mass"))),
            FakeLiturgyPreferencesStore(Denomination.CATHOLIC, bibleLanguage = "ta")
        )
        vm.uiState.test {
            val state = expectMostRecentItem()
            assertEquals("\u0BA4\u0BBF\u0BB0\u0BC1\u0BAA\u0BCD\u0BAA\u0BB2\u0BBF", state.entries.first().title)
        }
    }
}
