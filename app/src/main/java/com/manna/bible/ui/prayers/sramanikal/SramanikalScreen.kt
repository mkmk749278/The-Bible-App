package com.manna.bible.ui.prayers.sramanikal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manna.bible.R
import com.manna.bible.ui.theme.MannaTheme
import com.manna.bible.ui.theme.ScriptureFontFamily

private val MinTouchTarget = 48.dp

/**
 * Sramanikal — the 40-day memorial observance. Until begun, it invites the family to
 * name the one they are remembering. Then it shows one day at a time: that day's
 * Scripture (which opens in the reader), a short reflection, and the prayer of
 * commendation, with the freedom to revisit earlier days.
 *
 * @param onBack returns to the previous surface.
 * @param onOpenVerse opens the reader at a canonical `OSIS.CHAPTER.VERSE`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SramanikalScreen(
    modifier: Modifier = Modifier,
    viewModel: SramanikalViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onOpenVerse: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val backDescription = stringResource(R.string.prayers_back)
    val reflections = stringArrayResource(R.array.sramanikal_reflections)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.sramanikal_title), fontWeight = FontWeight.SemiBold)
                },
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
        when {
            state.isLoading -> Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) { CircularProgressIndicator() }

            !state.started -> NotStarted(
                modifier = Modifier.padding(padding),
                onBegin = viewModel::begin
            )

            else -> ActiveDay(
                modifier = Modifier.padding(padding),
                state = state,
                reflection = reflections.getOrNull(state.viewedDay - 1),
                onPrevious = viewModel::previous,
                onNext = viewModel::next,
                onOpenVerse = onOpenVerse,
                onEnd = viewModel::end
            )
        }
    }
}

@Composable
private fun NotStarted(modifier: Modifier, onBegin: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = stringResource(R.string.sramanikal_intro_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MannaTheme.colors.ink,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = stringResource(R.string.sramanikal_intro_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MannaTheme.colors.soft,
            lineHeight = 26.sp
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.sramanikal_name_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { onBegin(name) },
            enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.sramanikal_begin)) }
    }
}

@Composable
private fun ActiveDay(
    modifier: Modifier,
    state: SramanikalUiState,
    reflection: String?,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenVerse: (String) -> Unit,
    onEnd: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.sramanikal_day_indicator, state.viewedDay, state.totalDays),
            style = MaterialTheme.typography.labelLarge,
            color = MannaTheme.colors.gold,
            fontWeight = FontWeight.Bold
        )
        if (state.name.isNotBlank()) {
            Text(
                text = stringResource(R.string.sramanikal_remembering, state.name),
                style = MaterialTheme.typography.titleMedium,
                color = MannaTheme.colors.ink,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (state.verseText != null) {
            Surface(
                color = MannaTheme.colors.card,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
                    .clickable { state.osisRef?.let(onOpenVerse) }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = state.verseText,
                        fontFamily = ScriptureFontFamily,
                        style = MaterialTheme.typography.titleMedium,
                        color = MannaTheme.colors.ink,
                        lineHeight = 28.sp
                    )
                    Text(
                        text = state.reference,
                        style = MaterialTheme.typography.labelLarge,
                        color = MannaTheme.colors.gold,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (reflection != null) {
            Text(
                text = reflection,
                style = MaterialTheme.typography.bodyLarge,
                color = MannaTheme.colors.soft,
                lineHeight = 26.sp
            )
        }

        // The prayer of commendation, prayed each day for the departed.
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.sramanikal_prayer_label),
                style = MaterialTheme.typography.labelMedium,
                color = MannaTheme.colors.muted,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.paraloka_prayer_commendation_text),
                style = MaterialTheme.typography.bodyLarge,
                color = MannaTheme.colors.soft,
                lineHeight = 26.sp
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onPrevious,
                enabled = state.canGoPrevious,
                modifier = Modifier.weight(1f)
            ) { Text(stringResource(R.string.sramanikal_previous)) }
            Button(
                onClick = onNext,
                enabled = state.canGoNext,
                modifier = Modifier.weight(1f)
            ) { Text(stringResource(R.string.sramanikal_next)) }
        }

        TextButton(
            onClick = onEnd,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(stringResource(R.string.sramanikal_end), color = MannaTheme.colors.muted)
        }
    }
}
