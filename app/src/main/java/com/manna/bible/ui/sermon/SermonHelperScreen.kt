package com.manna.bible.ui.sermon

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manna.bible.R
import com.manna.bible.domain.sermon.SermonNote
import com.manna.bible.ui.theme.MannaTheme

private val MinTouchTarget = 48.dp

/**
 * The Village Pastor Sermon Helper: an offline library of sermon notes. Shows the
 * saved sermons, and an editor (title · scripture reference · outline) for creating
 * or revising one. Everything is local — a preacher keeps their whole library on the
 * device with no connectivity.
 */
@Composable
fun SermonHelperScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SermonHelperViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val draft = state.draft

    // System back closes the editor first, then leaves the screen.
    BackHandler(enabled = draft != null) { viewModel.closeEditor() }

    if (draft == null) {
        SermonList(
            modifier = modifier,
            sermons = state.sermons,
            onBack = onBack,
            onNew = viewModel::newSermon,
            onOpen = viewModel::edit
        )
    } else {
        SermonEditor(
            modifier = modifier,
            draft = draft,
            onTitleChange = viewModel::updateTitle,
            onReferenceChange = viewModel::updateReference,
            onContentChange = viewModel::updateContent,
            onSave = viewModel::save,
            onDelete = viewModel::deleteCurrent,
            onClose = viewModel::closeEditor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SermonList(
    sermons: List<SermonNote>,
    onBack: () -> Unit,
    onNew: () -> Unit,
    onOpen: (SermonNote) -> Unit,
    modifier: Modifier = Modifier
) {
    val backDescription = stringResource(R.string.sermon_back)
    val newDescription = stringResource(R.string.sermon_new)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sermon_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(MinTouchTarget)
                            .semantics { contentDescription = backDescription }
                    ) { Text(text = "‹", fontSize = 26.sp) }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNew,
                containerColor = MannaTheme.colors.gold,
                modifier = Modifier.semantics { contentDescription = newDescription }
            ) { Text(text = "+", fontSize = 28.sp, color = MannaTheme.colors.bg) }
        }
    ) { padding ->
        if (sermons.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.sermon_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MannaTheme.colors.soft
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items = sermons, key = { it.id }) { sermon ->
                    SermonCard(sermon = sermon, onClick = { onOpen(sermon) })
                }
            }
        }
    }
}

@Composable
private fun SermonCard(sermon: SermonNote, onClick: () -> Unit) {
    Surface(
        color = MannaTheme.colors.card,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Text(
                text = sermon.title,
                style = MaterialTheme.typography.titleMedium,
                color = MannaTheme.colors.ink,
                fontWeight = FontWeight.SemiBold
            )
            if (sermon.reference.isNotBlank()) {
                Text(
                    text = sermon.reference,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MannaTheme.colors.gold
                )
            }
            if (sermon.content.isNotBlank()) {
                Text(
                    text = sermon.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MannaTheme.colors.soft,
                    maxLines = 2
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SermonEditor(
    draft: SermonDraft,
    onTitleChange: (String) -> Unit,
    onReferenceChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val closeDescription = stringResource(R.string.sermon_close)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    val titleRes = if (draft.id > 0L) R.string.sermon_edit else R.string.sermon_new
                    Text(stringResource(titleRes), fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(MinTouchTarget)
                            .semantics { contentDescription = closeDescription }
                    ) { Text(text = "✕", fontSize = 20.sp) }
                },
                actions = {
                    if (draft.id > 0L) {
                        TextButton(onClick = onDelete) {
                            Text(stringResource(R.string.sermon_delete), color = MannaTheme.colors.red)
                        }
                    }
                    TextButton(onClick = onSave, enabled = draft.canSave) {
                        Text(stringResource(R.string.sermon_save))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = draft.title,
                onValueChange = onTitleChange,
                label = { Text(stringResource(R.string.sermon_field_title)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.reference,
                onValueChange = onReferenceChange,
                label = { Text(stringResource(R.string.sermon_field_reference)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.content,
                onValueChange = onContentChange,
                label = { Text(stringResource(R.string.sermon_field_notes)) },
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
        }
    }
}
