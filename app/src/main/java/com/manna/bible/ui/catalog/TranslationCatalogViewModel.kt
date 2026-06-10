package com.manna.bible.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manna.bible.data.ConnectivityChecker
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.canon.CanonEngine
import com.manna.bible.domain.download.DownloadManager
import com.manna.bible.domain.download.DownloadOutcome
import com.manna.bible.domain.model.Denomination
import com.manna.bible.domain.translation.TranslationFilter
import com.manna.bible.domain.usecase.SetActiveTranslationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A single row in the translation catalog (Req 4, 5, 6, 11.2).
 *
 * @property id Stable translation id.
 * @property name Human-readable edition name.
 * @property isDownloaded True when content is stored locally (bundled or downloaded).
 * @property isActive True when this is the active reading translation.
 */
data class CatalogItem(
    val id: String,
    val name: String,
    val isDownloaded: Boolean,
    val isActive: Boolean
)

/**
 * Immutable UI state for the `Translation_Catalog_Screen` (Req 4, 5, 6, 11).
 *
 * @property items In-language editions (downloaded ones first), each tagged with
 *   its downloaded/active state.
 * @property isLoading True until the first catalog snapshot is produced.
 * @property isOffline True when there is no connection (remote editions can't be
 *   fetched/downloaded, but stored ones remain usable).
 * @property downloadingId Id of the translation currently downloading, if any.
 * @property downloadProgress Determinate progress in 0f..1f for [downloadingId].
 * @property errorMessage Non-null when the last action failed.
 */
data class TranslationCatalogUiState(
    val items: List<CatalogItem> = emptyList(),
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val downloadingId: String? = null,
    val downloadProgress: Float? = null,
    val errorMessage: String? = null
)

/**
 * Drives the translation catalog (spec task 11.1).
 *
 * Immediately shows bundled + downloaded editions from the local catalog, refreshes
 * the remote catalog when online, and filters to the chosen Bible language (with the
 * same closest-in-language fallback as setup so a canon with no compatible edition is
 * never stranded). Downloads/cancels/deletes go through the [DownloadManager];
 * switching the active edition is persisted via [SetActiveTranslationUseCase].
 *
 * Uses only `androidx.lifecycle` + coroutines/flow — no Android framework types.
 */
@HiltViewModel
class TranslationCatalogViewModel @Inject constructor(
    private val translationRepository: com.manna.bible.domain.repository.TranslationRepository,
    private val downloadManager: DownloadManager,
    private val setActiveTranslationUseCase: SetActiveTranslationUseCase,
    private val preferencesStore: PreferencesStore,
    private val translationFilter: TranslationFilter,
    private val canonEngine: CanonEngine,
    private val connectivity: ConnectivityChecker
) : ViewModel() {

    private val _uiState = MutableStateFlow(TranslationCatalogUiState())
    val uiState: StateFlow<TranslationCatalogUiState> = _uiState.asStateFlow()

    init {
        refreshRemote()
        observeCatalog()
    }

    /** Pulls the live remote catalog when online; offline flips the offline flag. */
    fun refreshRemote() {
        viewModelScope.launch {
            val online = connectivity.isOnline()
            _uiState.update { it.copy(isOffline = !online) }
            if (online) runCatching { translationRepository.refreshCatalog() }
        }
    }

    /** Rebuilds the item list whenever the catalog, setup, or download progress changes. */
    private fun observeCatalog() {
        viewModelScope.launch {
            combine(
                translationRepository.catalog(),
                preferencesStore.setupState,
                downloadManager.progress()
            ) { catalog, setup, progress -> Triple(catalog, setup, progress) }
                .collect { (catalog, setup, progress) ->
                    val language = setup.bibleLanguage ?: setup.uiLanguage ?: ""
                    val denomination = setup.denomination ?: Denomination.SHOW_EVERYTHING
                    val profile = canonEngine.profileFor(denomination, language)
                    val compatible = translationFilter.filter(catalog, profile, language)
                    val inLanguage = compatible.ifEmpty {
                        catalog.filter { it.languageCode.equals(language, ignoreCase = true) }
                            .sortedBy { it.name }
                    }
                    val activeId = setup.bibleTranslationId
                    val items = inLanguage
                        .map { CatalogItem(it.id, it.name, it.isDownloaded, it.id == activeId) }
                        .sortedWith(
                            compareByDescending<CatalogItem> { it.isDownloaded }.thenBy { it.name }
                        )
                    val fraction = progress
                        ?.takeIf { it.totalChapters > 0 }
                        ?.let { it.completedChapters.toFloat() / it.totalChapters }
                    val downloadingId = progress?.takeIf { !it.done }?.translationId
                    _uiState.update {
                        it.copy(
                            items = items,
                            isLoading = false,
                            downloadingId = downloadingId,
                            downloadProgress = fraction
                        )
                    }
                }
        }
    }

    /** Downloads [id]; makes it active if no translation is active yet (Req 5, 6.1). */
    fun download(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(errorMessage = null) }
            val outcome = runCatching { downloadManager.download(id) }
                .getOrElse { DownloadOutcome.Failure(it.message ?: "download failed") }
            when (outcome) {
                DownloadOutcome.Success -> {
                    val active = preferencesStore.setupState.first().bibleTranslationId
                    if (active.isNullOrBlank()) setActiveTranslationUseCase(id)
                }
                DownloadOutcome.Offline ->
                    _uiState.update { it.copy(isOffline = true, errorMessage = OFFLINE) }
                is DownloadOutcome.Failure ->
                    _uiState.update { it.copy(errorMessage = outcome.reason) }
            }
        }
    }

    /** Cancels an in-flight download for [id], discarding any partial content (Req 5.4). */
    fun cancel(id: String) {
        viewModelScope.launch { downloadManager.cancel(id) }
    }

    /**
     * Deletes [id]'s stored content. If it was the active translation, falls back to
     * another downloaded edition so the reader is never left pointing at empty content
     * (Req 5.5).
     */
    fun delete(id: String) {
        viewModelScope.launch {
            downloadManager.delete(id)
            val active = preferencesStore.setupState.first().bibleTranslationId
            if (active == id) {
                val fallback = translationRepository.catalog().first()
                    .firstOrNull { it.isDownloaded && it.id != id }
                if (fallback != null) setActiveTranslationUseCase(fallback.id)
            }
        }
    }

    /** Switches the active reading translation to [id] (must already be downloaded). */
    fun setActive(id: String) {
        viewModelScope.launch { setActiveTranslationUseCase(id) }
    }

    private companion object {
        const val OFFLINE = "offline"
    }
}
