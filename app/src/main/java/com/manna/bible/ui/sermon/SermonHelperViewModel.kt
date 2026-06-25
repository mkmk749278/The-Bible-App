package com.manna.bible.ui.sermon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.model.CanonProfile
import com.manna.bible.domain.model.SetupState
import com.manna.bible.domain.repository.SermonRepository
import com.manna.bible.domain.sermon.CongregationType
import com.manna.bible.domain.sermon.SermonAiEngine
import com.manna.bible.domain.sermon.SermonNote
import com.manna.bible.domain.sermon.SermonOutlineRequest
import com.manna.bible.domain.sermon.SermonOutlineResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The sermon being edited: a draft for a new note ([id] == 0) or an existing one.
 */
data class SermonDraft(
    val id: Long = 0,
    val title: String = "",
    val reference: String = "",
    val content: String = ""
) {
    val canSave: Boolean
        get() = title.isNotBlank() || reference.isNotBlank() || content.isNotBlank()
}

/**
 * UI state for the Sermon Helper.
 *
 * @property sermons the saved sermon library, most recently updated first.
 * @property draft the sermon currently being edited, or null when showing the list.
 * @property congregationType the audience the AI outline is being shaped for (F-04).
 * @property isGeneratingOutline true while an AI outline request is in flight (F-04).
 * @property outlineError a non-null marker when the last outline request failed — either
 *   the literal [SermonHelperViewModel.OUTLINE_ERROR_OFFLINE] (no network) or an
 *   engine-supplied reason. The UI maps it to a Snackbar and the manual editor stays fully
 *   usable (F-04).
 * @property canGenerateOutline true when the AI engine is configured AND the draft's
 *   reference is non-blank — gates the "Build outline" button. The compile-time
 *   `FeatureFlags.SERMON_AI_BUILDER` gate is applied at the UI layer (mirroring how Phase
 *   B gates `CRISIS_AI_COMPANION` around `aiConfigured`).
 */
data class SermonHelperUiState(
    val sermons: List<SermonNote> = emptyList(),
    val draft: SermonDraft? = null,
    val congregationType: CongregationType = CongregationType.GENERAL,
    val isGeneratingOutline: Boolean = false,
    val outlineError: String? = null,
    val canGenerateOutline: Boolean = false
)

/**
 * The mutable, editor-side slice of the screen state. Held in a [MutableStateFlow] so it
 * can be evolved with `update { }`, then merged with the reactively-observed sermon list
 * to form [SermonHelperUiState]. Keeping the list out of this object preserves the
 * original pipeline's semantics: the list value is always read live (from the repository
 * flow) at merge time, so closing the editor and the list refreshing settle as one state.
 */
private data class SermonEditorState(
    val draft: SermonDraft? = null,
    val congregationType: CongregationType = CongregationType.GENERAL,
    val isGeneratingOutline: Boolean = false,
    val outlineError: String? = null,
    val canGenerateOutline: Boolean = false
)

/**
 * Backs the Village Pastor Sermon Helper: a local, offline library of sermon notes.
 * The list is observed reactively from the [SermonRepository] and merged with the
 * editor-side [MutableStateFlow]; the editor itself is local state persisted on [save].
 * No network, no account — a preacher's notes live on-device.
 *
 * The AI Sermon Builder layer (F-04) is an optional enrichment: given the active draft's
 * reference, the preacher's tradition (from [PreferencesStore]), and the chosen
 * [CongregationType], it asks [SermonAiEngine] for an editable outline and drops the
 * result straight into the draft's content. Outlines are never cached.
 *
 * Uses only `androidx.lifecycle` + coroutines/flow — no Android framework types.
 *
 * The [sermonAiEngine] and [preferencesStore] parameters carry inert defaults so the
 * list/editor surface can be constructed in isolation; in the app Hilt always injects the
 * real bindings (the defaults are ignored by the generated factory).
 */
@HiltViewModel
class SermonHelperViewModel @Inject constructor(
    private val repository: SermonRepository,
    private val sermonAiEngine: SermonAiEngine = UnconfiguredSermonAiEngine,
    private val preferencesStore: PreferencesStore = EmptyPreferencesStore
) : ViewModel() {

    private val editor = MutableStateFlow(SermonEditorState())

    /** Captured once: the engine's credential state never changes within a session. */
    private val engineConfigured: Boolean = sermonAiEngine.isConfigured

    val uiState: StateFlow<SermonHelperUiState> = combine(
        repository.observeSermons(),
        editor
    ) { sermons, current ->
        SermonHelperUiState(
            sermons = sermons,
            draft = current.draft,
            congregationType = current.congregationType,
            isGeneratingOutline = current.isGeneratingOutline,
            outlineError = current.outlineError,
            canGenerateOutline = current.canGenerateOutline
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SermonHelperUiState()
    )

    /** Opens the editor on a blank new sermon. */
    fun newSermon() {
        openDraft(SermonDraft())
    }

    /** Opens the editor on an existing [note]. */
    fun edit(note: SermonNote) {
        openDraft(
            SermonDraft(
                id = note.id,
                title = note.title,
                reference = note.reference,
                content = note.content
            )
        )
    }

    fun updateTitle(value: String) = updateDraft { it.copy(title = value) }
    fun updateReference(value: String) = updateDraft { it.copy(reference = value) }
    fun updateContent(value: String) = updateDraft { it.copy(content = value) }

    /** Persists the current draft (no-op when empty) and returns to the list. */
    fun save() {
        val current = editor.value.draft ?: return
        if (current.canSave) {
            viewModelScope.launch {
                repository.save(current.id, current.title, current.reference, current.content)
            }
        }
        closeDraft()
    }

    /** Deletes the sermon being edited (if it was already saved) and returns to the list. */
    fun deleteCurrent() {
        val current = editor.value.draft ?: return
        if (current.id > 0L) {
            viewModelScope.launch { repository.delete(current.id) }
        }
        closeDraft()
    }

    /** Closes the editor without saving. */
    fun closeEditor() {
        closeDraft()
    }

    /** Selects the congregation type the next AI outline will be shaped for (F-04). */
    fun selectCongregationType(type: CongregationType) {
        editor.update { it.copy(congregationType = type) }
    }

    /** Clears the last outline error once the UI has surfaced it (e.g. after a Snackbar). */
    fun clearOutlineError() {
        editor.update { it.copy(outlineError = null) }
    }

    /**
     * Generates an AI sermon outline for the active draft's reference and inserts the
     * result into the draft's content (F-04). No-op when the AI gate is closed
     * ([SermonHelperUiState.canGenerateOutline] is false — i.e. the engine is unconfigured
     * or the reference is blank). The denomination and Bible language are read from
     * [PreferencesStore.setupState] at call time (FR-04.7); the congregation type comes
     * from current editor state. Outlines are never cached.
     *
     * The compile-time `FeatureFlags.SERMON_AI_BUILDER` gate is applied at the UI layer
     * (the "Build outline" affordance only appears when the flag is on), mirroring how
     * Phase B gates `CRISIS_AI_COMPANION` around the runtime `aiConfigured` signal. This
     * keeps the generation logic unit-testable without flipping a `const`.
     */
    fun generateOutline() {
        if (!editor.value.canGenerateOutline) return
        val reference = editor.value.draft?.reference ?: return
        viewModelScope.launch {
            editor.update { it.copy(isGeneratingOutline = true, outlineError = null) }
            val setup = preferencesStore.setupState.first()
            val result = sermonAiEngine.generateOutline(
                SermonOutlineRequest(
                    reference = reference,
                    denomination = setup.denomination,
                    languageCode = setup.bibleLanguage ?: DEFAULT_LANGUAGE,
                    congregationType = editor.value.congregationType
                )
            )
            when (result) {
                is SermonOutlineResult.Success -> editor.update { state ->
                    val newDraft = state.draft?.copy(content = result.outlineText)
                    state.copy(
                        draft = newDraft,
                        isGeneratingOutline = false,
                        canGenerateOutline = canGenerate(newDraft)
                    )
                }

                SermonOutlineResult.Offline -> editor.update {
                    it.copy(isGeneratingOutline = false, outlineError = OUTLINE_ERROR_OFFLINE)
                }

                is SermonOutlineResult.Unavailable -> editor.update {
                    it.copy(isGeneratingOutline = false, outlineError = result.reason)
                }
            }
        }
    }

    /** Opens [draft] in the editor, recomputing the AI gate and clearing any prior error. */
    private fun openDraft(draft: SermonDraft) {
        editor.update {
            it.copy(
                draft = draft,
                canGenerateOutline = canGenerate(draft),
                outlineError = null
            )
        }
    }

    /** Closes the editor and resets the per-draft AI flags. */
    private fun closeDraft() {
        editor.update {
            it.copy(draft = null, canGenerateOutline = false, outlineError = null)
        }
    }

    private fun updateDraft(transform: (SermonDraft) -> SermonDraft) {
        val current = editor.value.draft ?: return
        val updated = transform(current)
        editor.update {
            it.copy(draft = updated, canGenerateOutline = canGenerate(updated))
        }
    }

    /** The AI gate: engine configured AND the draft carries a non-blank reference. */
    private fun canGenerate(draft: SermonDraft?): Boolean =
        engineConfigured && (draft?.reference?.isNotBlank() == true)

    internal companion object {
        /** Fallback Bible-language ISO code when setup has not recorded one. */
        const val DEFAULT_LANGUAGE = "en"

        /** Sentinel [SermonHelperUiState.outlineError] value for a no-network failure. */
        const val OUTLINE_ERROR_OFFLINE = "offline"
    }
}

/**
 * Inert [SermonAiEngine] used only as a constructor default so [SermonHelperViewModel]
 * can be built without the AI layer (e.g. previews / focused list-editor tests). Reports
 * itself unconfigured, so `canGenerateOutline` stays false. Hilt never uses this — it
 * injects [com.manna.bible.data.sermon.GeminiSermonEngine].
 */
private object UnconfiguredSermonAiEngine : SermonAiEngine {
    override val isConfigured: Boolean = false
    override suspend fun generateOutline(request: SermonOutlineRequest): SermonOutlineResult =
        SermonOutlineResult.Unavailable("AI sermon builder not available.")
}

/**
 * Inert [PreferencesStore] used only as a constructor default for [SermonHelperViewModel]
 * (see [UnconfiguredSermonAiEngine]). Every value is a benign empty/disabled default; the
 * mutating operations are no-ops. Hilt always injects the real DataStore-backed store.
 */
private object EmptyPreferencesStore : PreferencesStore {
    override val setupState: Flow<SetupState> = flowOf(
        SetupState(
            denomination = null,
            canonType = null,
            uiLanguage = null,
            bibleLanguage = null,
            numberingScheme = null,
            namingConventionId = null,
            bibleTranslationId = null,
            lectionaryId = null
        )
    )
    override val lastReadPosition: Flow<String?> = flowOf(null)
    override suspend fun saveSetup(state: SetupState) {}
    override suspend fun setSetupCompleted(value: Boolean) {}
    override suspend fun updateDenomination(profile: CanonProfile) {}
    override suspend fun setShowDeuterocanonical(value: Boolean) {}
    override suspend fun setActiveTranslation(translationId: String) {}
    override suspend fun setLastReadPosition(ref: String) {}
}
