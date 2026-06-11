package com.manna.bible.ui.daily

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manna.bible.R
import com.manna.bible.ui.theme.MannaColors

private val MinTouchTarget = 48.dp

/**
 * Verse of the Day surface. Shows today's deterministic verse and offers to open it
 * in the reader at the right position. Fully offline; when no content is available
 * it shows a hint to download a translation.
 *
 * @param onBack returns to the reader.
 * @param onOpenVerse opens the reader at the given canonical `OSIS.CHAPTER.VERSE`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyVerseScreen(
    modifier: Modifier = Modifier,
    viewModel: DailyVerseViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onOpenVerse: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val backDescription = stringResource(R.string.daily_back)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.daily_title), fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(MinTouchTarget)
                            .semantics { contentDescription = backDescription }
                    ) {
                        Text(text = "‹", fontSize = 26.sp)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(R.string.daily_loading),
                        modifier = Modifier.padding(top = 16.dp),
                        color = MannaColors.soft
                    )
                }

                state.unavailable || state.verseText == null -> {
                    Text(
                        text = stringResource(R.string.daily_unavailable),
                        textAlign = TextAlign.Center,
                        color = MannaColors.soft,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                else -> DailyVerseCard(
                    reference = state.reference,
                    verseText = state.verseText.orEmpty(),
                    onReadInContext = { state.osisRef?.let(onOpenVerse) }
                )
            }
        }
    }
}

@Composable
private fun DailyVerseCard(
    reference: String,
    verseText: String,
    onReadInContext: () -> Unit
) {
    Surface(color = MannaColors.card, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = reference,
                style = MaterialTheme.typography.titleMedium,
                color = MannaColors.gold,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = verseText,
                style = MaterialTheme.typography.headlineSmall,
                color = MannaColors.cream,
                textAlign = TextAlign.Center,
                lineHeight = 34.sp
            )
            Button(
                onClick = onReadInContext,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text(stringResource(R.string.daily_read_in_context))
            }
        }
    }
}
