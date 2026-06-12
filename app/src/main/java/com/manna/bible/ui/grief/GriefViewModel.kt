package com.manna.bible.ui.grief

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.grief.GriefJourney
import com.manna.bible.domain.repository.BibleContentRepository
import com.manna.bible.domain.repository.TranslationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * UI state for the 30-day Grief Companion.
 *
 * @property isLoading True until the journey state resolves.
 * @property started True once the journey has been begun.
 * @property currentDay The day unlocked today (1..total), 0 before starting.
 * @property viewedDay The day currently being shown (1..currentDay).
 * @property totalDays Total days in the journey.
 * @property reference Display reference for the viewed day's passage.
 * @property verseText Passage text, or null when unavailable.
 * @property osisRef Canonical reference for "read in context".
 */
data class GriefUiState(
    val isLoading: Boolean = true,
    val started: Boolean = false,
    val currentDay: Int = 0,
    val viewedDay: Int = 0,
    val totalDays: Int = 30,
    val reference: String = "",
    val verseText: String? = null,
    val osisRef: String? = null
) {
    val canGoPrevious: Boolean get() = viewedDay > 1
    val canGoNext: Boolean get() = viewedDay < currentDay
}

/**
 * Drives the Grief Companion: a date-paced 30-day journey. The unlocked day is
 * derived from the persisted start date and today, so the journey gently reveals one
 * day at a time; the reader can revisit any earlier day. Verse text comes from the
 * active translation (offline). Reflection prose is supplied by the screen as string
 * resources indexed by day.
 *
 * Uses only `androidx.lifecycle` + coroutines/flow — no Android framework types.
 */
@HiltViewModel
class GriefViewModel @Inject constructor(
    private val griefJourney: GriefJourney,
    private val preferencesStore: PreferencesStore,
    private val translationRepository: TranslationRepository,
    private val bibleContentRepository: BibleContentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GriefUiState(totalDays = griefJourney.dayCount))
    val uiState: StateFlow<GriefUiState> = _uiState.asStateFlow()

    private var currentDay = 0
    private var viewedDay = 0

    init {
        refresh()
    }

    /** Re-reads the journey state and loads the active day. */
    fun refresh() {
        viewModelScope.launch {
            val start = preferencesStore.griefStartEpochDay.first()
            if (start < 0) {
                _uiState.value = GriefUiState(
                    isLoading = false,
                    started = false,
                    totalDays = griefJourney.dayCount
                )
                return@launch
            }
            currentDay = ((LocalDate.now().toEpochDay() - start) + 1)
                .coerceIn(1L, griefJourney.dayCount.toLong())
                .toInt()
            viewedDay = currentDay
            loadDay()
        }
    }

    /** Begins the journey today. */
    fun begin() {
        viewModelScope.launch {
            preferencesStore.setGriefStartEpochDay(LocalDate.now().toEpochDay())
            refresh()
        }
    }

    /** Moves to the next unlocked day, if any. */
    fun next() {
        if (viewedDay < currentDay) {
            viewedDay++
            viewModelScope.launch { loadDay() }
        }
    }

    /** Moves to the previous day, if any. */
    fun previous() {
        if (viewedDay > 1) {
            viewedDay--
            viewModelScope.launch { loadDay() }
        }
    }

    private suspend fun loadDay() {
        val ref = griefJourney.verseFor(viewedDay)
        val translationId = resolveActiveTranslation()
        if (ref == null || translationId == null) {
            _uiState.value = _uiState.value.copy(
                isLoading = false, started = true,
                currentDay = currentDay, viewedDay = viewedDay,
                verseText = null, reference = "", osisRef = null
            )
            return
        }
        val content = runCatching {
            bibleContentRepository.chapter(translationId, ref.osisId, ref.chapter)
        }.getOrNull()
        val text = content?.verses?.firstOrNull { it.verse == ref.verse }?.text
        val bookName = runCatching {
            bibleContentRepository.books(translationId).first()
                .firstOrNull { it.osisId == ref.osisId }?.name
        }.getOrNull() ?: ref.osisId
        _uiState.value = GriefUiState(
            isLoading = false,
            started = true,
            currentDay = currentDay,
            viewedDay = viewedDay,
            totalDays = griefJourney.dayCount,
            reference = "$bookName ${ref.chapter}:${ref.verse}",
            verseText = text,
            osisRef = ref.format()
        )
    }

    private suspend fun resolveActiveTranslation(): String? {
        val persistedId = preferencesStore.setupState.first().bibleTranslationId
        if (!persistedId.isNullOrBlank()) return persistedId
        val catalog = runCatching { translationRepository.catalog().first() }.getOrDefault(emptyList())
        return catalog.firstOrNull { it.isDownloaded }?.id ?: catalog.firstOrNull()?.id
    }
}
