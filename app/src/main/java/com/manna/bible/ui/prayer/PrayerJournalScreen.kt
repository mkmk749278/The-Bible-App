package com.manna.bible.ui.prayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.manna.bible.domain.model.Prayer
import com.manna.bible.ui.theme.MannaTheme
import java.text.DateFormat
import java.util.Date

private val MinTouchTarget = 48.dp

/**
 * Prayer Journal + Faith Timeline. Record what you're praying about, mark prayers
 * answered (building a timeline of God's faithfulness), and remove entries. Private
 * and local-first.
 *
 * @param onBack returns to the previous surface.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerJournalScreen(
    modifier: Modifier = Modifier,
    viewModel: PrayerJournalViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val backDescription = stringResource(R.string.prayer_back)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.prayer_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(MinTouchTarget)
                            .semantics { contentDescription = backDescription }
                    ) { Text(text = "‹", fontSize = 26.sp) }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 20.dp,
                vertical = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = state.draft,
                    onValueChange = viewModel::onDraftChange,
                    label = { Text(stringResource(R.string.prayer_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Button(
                    onClick = viewModel::add,
                    enabled = state.draft.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.prayer_add)) }
            }

            if (state.active.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.prayer_active_section)) }
                items(items = state.active, key = { it.id }) { prayer ->
                    PrayerCard(
                        prayer = prayer,
                        primaryLabel = stringResource(R.string.prayer_mark_answered),
                        onPrimary = { viewModel.markAnswered(prayer.id) },
                        onDelete = { viewModel.delete(prayer.id) }
                    )
                }
            }

            if (state.answered.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.prayer_answered_section)) }
                items(items = state.answered, key = { it.id }) { prayer ->
                    PrayerCard(
                        prayer = prayer,
                        primaryLabel = stringResource(R.string.prayer_reopen),
                        onPrimary = { viewModel.reopen(prayer.id) },
                        onDelete = { viewModel.delete(prayer.id) },
                        answered = true
                    )
                }
            }

            if (state.active.isEmpty() && state.answered.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.prayer_empty),
                        color = MannaTheme.colors.soft,
                        modifier = Modifier.padding(top = 24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MannaTheme.colors.gold,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun PrayerCard(
    prayer: Prayer,
    primaryLabel: String,
    onPrimary: () -> Unit,
    onDelete: () -> Unit,
    answered: Boolean = false
) {
    Surface(
        color = MannaTheme.colors.card,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = prayer.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MannaTheme.colors.ink
            )
            if (answered && prayer.answeredAt != null) {
                Text(
                    text = stringResource(
                        R.string.prayer_answered_on,
                        DateFormat.getDateInstance().format(Date(prayer.answeredAt!!))
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MannaTheme.colors.sage
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onPrimary,
                    modifier = Modifier.defaultMinSizeHeight()
                ) { Text(primaryLabel) }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.defaultMinSizeHeight()
                ) { Text(stringResource(R.string.prayer_delete)) }
            }
        }
    }
}

private fun Modifier.defaultMinSizeHeight(): Modifier =
    this.then(androidx.compose.foundation.layout.defaultMinSize(minHeight = MinTouchTarget))
