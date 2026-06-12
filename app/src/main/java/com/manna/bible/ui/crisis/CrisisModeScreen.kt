package com.manna.bible.ui.crisis

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.manna.bible.ui.theme.MannaTheme
import com.manna.bible.ui.theme.ScriptureFontFamily

private val MinTouchTarget = 48.dp

/**
 * 3AM / Crisis Mode — a calm, warm companion for the hardest moments (blueprint
 * P2-1). A gentle reassurance, an immediate "just listen" path, and curated
 * comforting scripture. No streaks, no plans, no guilt.
 *
 * @param onBack returns to the previous surface.
 * @param onListen opens the reader at a comforting passage with audio auto-playing.
 * @param onOpenVerse opens the reader at a canonical `OSIS.CHAPTER.VERSE`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrisisModeScreen(
    modifier: Modifier = Modifier,
    viewModel: CrisisModeViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onListen: (String) -> Unit = {},
    onOpenVerse: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val backDescription = stringResource(R.string.crisis_back)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.crisis_title), fontWeight = FontWeight.SemiBold) },
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
        if (state.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 24.dp,
                vertical = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.crisis_heading),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MannaTheme.colors.ink,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.crisis_reassurance),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MannaTheme.colors.soft
                    )
                }
            }

            state.listenRef?.let { ref ->
                item {
                    Button(
                        onClick = { onListen(ref) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                    ) {
                        Text(stringResource(R.string.crisis_listen))
                    }
                }
            }

            items(items = state.comfortVerses, key = { it.osisRef }) { verse ->
                ComfortCard(verse = verse, onClick = { onOpenVerse(verse.osisRef) })
            }

            item {
                Surface(
                    color = MannaTheme.colors.card,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.crisis_prayer_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MannaTheme.colors.gold,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.crisis_prayer_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MannaTheme.colors.soft
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComfortCard(verse: ComfortVerse, onClick: () -> Unit) {
    Surface(
        color = MannaTheme.colors.card,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = verse.text,
                fontFamily = ScriptureFontFamily,
                style = MaterialTheme.typography.titleMedium,
                color = MannaTheme.colors.ink,
                lineHeight = 28.sp
            )
            Text(
                text = verse.reference,
                style = MaterialTheme.typography.labelLarge,
                color = MannaTheme.colors.gold,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
