package com.manna.bible.ui.grief

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
 * Grief Companion — a gentle, date-paced 30-day journey through loss. Shows a "begin"
 * invitation until started, then one day at a time (theme, passage, reflection) with
 * the freedom to revisit earlier days.
 *
 * @param onBack returns to the previous surface.
 * @param onOpenVerse opens the reader at a canonical `OSIS.CHAPTER.VERSE`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GriefScreen(
    modifier: Modifier = Modifier,
    viewModel: GriefViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onOpenVerse: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val backDescription = stringResource(R.string.grief_back)
    val themes = stringArrayResource(R.array.grief_themes)
    val reflections = stringArrayResource(R.array.grief_reflections)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.grief_title), fontWeight = FontWeight.SemiBold) },
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
            state.isLoading -> CenteredBox(padding) { CircularProgressIndicator() }

            !state.started -> NotStarted(
                modifier = Modifier.padding(padding),
                onBegin = viewModel::begin
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.grief_day_indicator, state.viewedDay, state.totalDays),
                    style = MaterialTheme.typography.labelLarge,
                    color = MannaTheme.colors.gold,
                    fontWeight = FontWeight.Bold
                )
                themes.getOrNull(state.viewedDay - 1)?.let { theme ->
                    Text(
                        text = theme,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MannaTheme.colors.ink,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                state.verseText?.let { verseText ->
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
                                text = verseText,
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

                reflections.getOrNull(state.viewedDay - 1)?.let { reflection ->
                    Text(
                        text = reflection,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MannaTheme.colors.soft,
                        lineHeight = 26.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = viewModel::previous,
                        enabled = state.canGoPrevious,
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.grief_previous)) }
                    Button(
                        onClick = viewModel::next,
                        enabled = state.canGoNext,
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.grief_next)) }
                }
            }
        }
    }
}

@Composable
private fun CenteredBox(padding: androidx.compose.foundation.layout.PaddingValues, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) { content() }
}

@Composable
private fun NotStarted(modifier: Modifier, onBegin: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = stringResource(R.string.grief_intro_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MannaTheme.colors.ink,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = stringResource(R.string.grief_intro_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MannaTheme.colors.soft,
            lineHeight = 26.sp
        )
        Button(
            onClick = onBegin,
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.grief_begin)) }
    }
}
