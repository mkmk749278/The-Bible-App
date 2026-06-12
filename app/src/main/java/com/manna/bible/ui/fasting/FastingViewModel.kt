package com.manna.bible.ui.fasting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.fasting.FastPlan
import com.manna.bible.domain.fasting.FastProgress
import com.manna.bible.domain.fasting.FastingPlans
import com.manna.bible.domain.repository.BibleContentRepository
import com.manna.bible.domain.repository.TranslationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the Fasting Companion.
 *
 * @property isLoading True until the initial state resolves.
 * @property active True while a fast is in progress.
 * @property plans Available fasts (shown when not fasting).
 * @property activePlanId Id of the active plan, or null.
 * @property fractionComplete Progress through the active fast, 0f..1f.
 * @property remainingMillis Time left in the active fast.
 * @property isComplete True once the fast's duration has elapsed.
 * @property focusReference Display reference of the current focus verse.
 * @property focusText Focus verse text, or null when unavailable.
 * @property focusOsisRef Canonical reference for "read in context".
 */
data class FastingUiState(
    val isLoading: Boolean = true,
    val active: Boolean = false,
    val plans: List<FastPlan> = emptyList(),
    val activePlanId: String? = null,
    val fractionComplete: Float = 0f,
    val remainingMillis: Long = 0L,
    val isComplete: Boolean = false,
    val focusReference: String = "",
    val focusText: String? = null,
    val focusOsisRef: String? = null
)

/**
 * Drives the Fasting Companion: choose a time-boxed fast, track its progress, and
 * dwell on Scripture while you fast. Progress is recomputed on each [refresh] (the
 * screen ticks it once a second); the maths lives in the pure [FastProgress].
 *
 * Uses only `androidx.lifecycle` + coroutines/flow — no Android framework types.
 */
@HiltViewModel
class FastingViewModel @Inject constructor(
    private val fastingPlans: FastingPlans,
    private val preferencesStore: PreferencesStore,
    private val translationRepository: TranslationRepository,
    private val bibleContentRepository: BibleContentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FastingUiState())
    val uiState: StateFlow<FastingUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    /** Recomputes the fasting state and current progress. */
    fun refresh() {
        viewModelScope.launch {
            val start = preferencesStore.fastStartMillis.first()
            val planId = preferencesStore.fastPlanId.first()
            val plan = if (start >= 0 && planId.isNotBlank()) fastingPlans.planById(planId) else null
            if (plan == null) {
                _uiState.value = FastingUiState(
                    isLoading = false,
                    active = false,
                    plans = fastingPlans.plans()
                )
                return@launch
            }
            val now = System.currentTimeMillis()
            val focus = focusFor(start, now)
            _uiState.value = FastingUiState(
                isLoading = false,
                active = true,
                plans = fastingPlans.plans(),
                activePlanId = plan.id,
                fractionComplete = FastProgress.fraction(start, plan.hours, now),
                remainingMillis = FastProgress.remainingMillis(start, plan.hours, now),
                isComplete = FastProgress.isComplete(start, plan.hours, now),
                focusReference = focus?.reference.orEmpty(),
                focusText = focus?.text,
                focusOsisRef = focus?.osisRef
            )
        }
    }

    /** Begins [planId] now. */
    fun start(planId: String) {
        viewModelScope.launch {
            preferencesStore.setActiveFast(System.currentTimeMillis(), planId)
            refresh()
        }
    }

    /** Ends the active fast. */
    fun end() {
        viewModelScope.launch {
            preferencesStore.setActiveFast(-1L, "")
            refresh()
        }
    }

    private data class Focus(val reference: String, val text: String?, val osisRef: String)

    /** Picks a focus verse by elapsed hours and resolves its text from the active translation. */
    private suspend fun focusFor(start: Long, now: Long): Focus? {
        val verses = fastingPlans.focusVerses()
        if (verses.isEmpty()) return null
        val elapsedHours = (FastProgress.elapsedMillis(start, now) / 3_600_000L).toInt()
        val ref = verses[elapsedHours % verses.size]
        val translationId = resolveActiveTranslation()
            ?: return Focus(ref.osisId + " " + ref.chapter + ":" + ref.verse, null, ref.format())
        val content = runCatching {
            bibleContentRepository.chapter(translationId, ref.osisId, ref.chapter)
        }.getOrNull()
        val text = content?.verses?.firstOrNull { it.verse == ref.verse }?.text
        val bookName = runCatching {
            bibleContentRepository.books(translationId).first().firstOrNull { it.osisId == ref.osisId }?.name
        }.getOrNull() ?: ref.osisId
        return Focus("$bookName ${ref.chapter}:${ref.verse}", text, ref.format())
    }

    private suspend fun resolveActiveTranslation(): String? {
        val persistedId = preferencesStore.setupState.first().bibleTranslationId
        if (!persistedId.isNullOrBlank()) return persistedId
        val catalog = runCatching { translationRepository.catalog().first() }.getOrDefault(emptyList())
        return catalog.firstOrNull { it.isDownloaded }?.id ?: catalog.firstOrNull()?.id
    }
}
