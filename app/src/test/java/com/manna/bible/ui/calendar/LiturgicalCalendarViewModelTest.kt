package com.manna.bible.ui.calendar

import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.calendar.DefaultJesusEventsProvider
import com.manna.bible.domain.calendar.DefaultLectionaryReadingsProvider
import com.manna.bible.domain.calendar.DefaultLiturgicalCalendarProvider
import com.manna.bible.domain.canon.CanonEngine
import com.manna.bible.domain.model.CanonProfile
import com.manna.bible.domain.model.CanonType
import com.manna.bible.domain.model.Denomination
import com.manna.bible.domain.model.NumberingScheme
import com.manna.bible.domain.model.SetupState
import com.manna.bible.domain.share.DefaultBookNameProvider
import com.manna.bible.domain.share.ShareReferenceFormatter
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

/** Unit tests for [LiturgicalCalendarViewModel] — grid layout, navigation, selection. */
class LiturgicalCalendarViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(denomination: Denomination = Denomination.CSI) =
        LiturgicalCalendarViewModel(
            provider = DefaultLiturgicalCalendarProvider(DefaultJesusEventsProvider()),
            jesusEvents = DefaultJesusEventsProvider(),
            lectionary = DefaultLectionaryReadingsProvider(DefaultJesusEventsProvider()),
            referenceFormatter = ShareReferenceFormatter(DefaultBookNameProvider()),
            canonEngine = FakeCanonEngine,
            preferencesStore = FakePreferencesStore(denomination)
        )

    private object FakeCanonEngine : CanonEngine {
        override fun canonTypeFor(denomination: Denomination) = CanonType.PROTESTANT_66
        override suspend fun profileFor(denomination: Denomination, bibleLanguage: String) =
            CanonProfile(
                denomination = denomination,
                canonType = CanonType.PROTESTANT_66,
                books = emptyList(),
                numberingScheme = NumberingScheme.MASORETIC,
                namingConventionId = null,
                suggestedTranslationId = null,
                lectionaryId = null
            )
    }

    @Test
    @DisplayName("Initial state shows the current month, a full grid, and selects today")
    fun initialState() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        val state = vm.uiState.value
        val now = YearMonth.now()

        assertFalse(state.isLoading)
        assertEquals(now.year, state.year)
        assertEquals(now.monthValue, state.month)
        assertEquals(now.lengthOfMonth(), state.days.size)
        assertEquals(1, state.days.first().date.dayOfMonth)
        // Sunday-first leading blanks.
        assertEquals(now.atDay(1).dayOfWeek.value % 7, state.leadingBlanks)
        assertEquals(LocalDate.now(), state.selected?.date)
        // Days are consecutive.
        assertTrue(state.days.zipWithNext().all { (a, b) -> a.date.plusDays(1) == b.date })
    }

    @Test
    @DisplayName("nextMonth advances the month and selects its first day")
    fun nextMonth() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.nextMonth()
        advanceUntilIdle()
        val expected = YearMonth.now().plusMonths(1)
        val state = vm.uiState.value
        assertEquals(expected.year, state.year)
        assertEquals(expected.monthValue, state.month)
        assertEquals(expected.atDay(1), state.selected?.date)
    }

    @Test
    @DisplayName("Selecting Christmas resolves its feast and proper readings")
    fun selectFeast() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        val christmas = LocalDate.of(LocalDate.now().year, 12, 25)
        vm.selectDate(christmas)
        advanceUntilIdle()
        val selected = vm.uiState.value.selected
        assertNotNull(selected)
        assertEquals(christmas, selected!!.date)
        assertEquals("nativity", selected.feastId)
        assertNotNull(selected.osisRef)
        // Christmas has its four proper readings, each with a non-blank reference.
        assertEquals(4, selected.readings.size)
        assertTrue(selected.readings.all { it.reference.isNotBlank() && it.osisRef.isNotBlank() })
        assertEquals("LUK.2.1", selected.readings.last().osisRef) // Gospel: Luke 2:1
    }

    @Test
    @DisplayName("An ordinary day has no proper readings")
    fun ordinaryDayNoReadings() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.selectDate(LocalDate.of(LocalDate.now().year, 7, 15))
        advanceUntilIdle()
        val selected = vm.uiState.value.selected
        assertNotNull(selected)
        assertTrue(selected!!.readings.isEmpty())
    }

    private class FakePreferencesStore(denomination: Denomination) : PreferencesStore {
        private val state = MutableStateFlow(
            SetupState(
                denomination = denomination,
                canonType = CanonType.PROTESTANT_66,
                uiLanguage = "en",
                bibleLanguage = "en",
                numberingScheme = NumberingScheme.MASORETIC,
                namingConventionId = null,
                bibleTranslationId = "web",
                lectionaryId = null,
                showDeuterocanonical = false,
                setupCompleted = true
            )
        )
        override val setupState: Flow<SetupState> = state
        override val lastReadPosition: Flow<String?> = MutableStateFlow(null)
        override suspend fun saveSetup(state: SetupState) {}
        override suspend fun setSetupCompleted(value: Boolean) {}
        override suspend fun updateDenomination(profile: CanonProfile) {}
        override suspend fun setShowDeuterocanonical(value: Boolean) {}
        override suspend fun setActiveTranslation(translationId: String) {}
        override suspend fun setLastReadPosition(ref: String) {}
    }
}
