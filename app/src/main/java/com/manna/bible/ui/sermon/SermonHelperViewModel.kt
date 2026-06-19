package com.manna.bible.ui.sermon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manna.bible.domain.repository.SermonRepository
import com.manna.bible.domain.sermon.SermonNote
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
 */
data class SermonHelperUiState(
    val sermons: List<SermonNote> = emptyList(),
    val draft: SermonDraft? = null
)

/**
 * Backs the Village Pastor Sermon Helper: a local, offline library of sermon notes.
 * The list comes from the [SermonRepository]; the editor is local UI state that is
 * persisted on [save]. No network, no account — a preacher's notes live on-device.
 *
 * Uses only `androidx.lifecycle` + coroutines/flow — no Android framework types.
 */
@HiltViewModel
class SermonHelperViewModel @Inject constructor(
    private val repository: SermonRepository
) : ViewModel() {

    private val draft = MutableStateFlow<SermonDraft?>(null)

    val uiState: StateFlow<SermonHelperUiState> = combine(
        repository.observeSermons(),
        draft
    ) { sermons, currentDraft ->
        SermonHelperUiState(sermons = sermons, draft = currentDraft)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SermonHelperUiState()
    )

    /** Opens the editor on a blank new sermon. */
    fun newSermon() {
        draft.value = SermonDraft()
    }

    /** Opens the editor on an existing [note]. */
    fun edit(note: SermonNote) {
        draft.value = SermonDraft(
            id = note.id,
            title = note.title,
            reference = note.reference,
            content = note.content
        )
    }

    fun updateTitle(value: String) = updateDraft { it.copy(title = value) }
    fun updateReference(value: String) = updateDraft { it.copy(reference = value) }
    fun updateContent(value: String) = updateDraft { it.copy(content = value) }

    /** Persists the current draft (no-op when empty) and returns to the list. */
    fun save() {
        val current = draft.value ?: return
        if (current.canSave) {
            viewModelScope.launch {
                repository.save(current.id, current.title, current.reference, current.content)
            }
        }
        draft.value = null
    }

    /** Deletes the sermon being edited (if it was already saved) and returns to the list. */
    fun deleteCurrent() {
        val current = draft.value ?: return
        if (current.id > 0L) {
            viewModelScope.launch { repository.delete(current.id) }
        }
        draft.value = null
    }

    /** Closes the editor without saving. */
    fun closeEditor() {
        draft.value = null
    }

    private inline fun updateDraft(transform: (SermonDraft) -> SermonDraft) {
        draft.value = draft.value?.let(transform)
    }
}
