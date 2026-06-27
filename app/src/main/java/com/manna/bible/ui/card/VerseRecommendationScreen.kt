package com.manna.bible.ui.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manna.bible.R
import com.manna.bible.domain.share.VerseRecommendation
import com.manna.bible.ui.theme.MannaTheme
import com.manna.bible.ui.theme.ScriptureFontFamily
import com.manna.bible.ui.util.rememberBibleLanguage
import com.manna.bible.ui.util.stringResourceIn

private val MinTouchTarget = 48.dp

/**
 * Context-Aware Verse Cards (F-05) — the user describes an occasion or situation and the
 * engine recommends a single passage (text resolved from the active local translation)
 * with a short personal note. Sharing reuses the existing [VerseCardSheet] so the produced
 * card is visually identical to a manually created one (FR-05.3).
 *
 * When the engine is not configured (offline / no key) the input is replaced with a hint
 * and the manual share flow remains available elsewhere (FR-05.5).
 *
 * @param onBack returns to the previous surface.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerseRecommendationScreen(
    modifier: Modifier = Modifier,
    viewModel: VerseRecommendationViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val bibleLanguage = rememberBibleLanguage()
    val backDescription = stringResourceIn(bibleLanguage, R.string.verse_rec_back)

    var shareTarget by remember { mutableStateOf<VerseRecommendation.Success?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResourceIn(bibleLanguage, R.string.verse_rec_title), fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(MinTouchTarget)
                            .semantics { contentDescription = backDescription }
                    ) { Text(text = "‹", fontSize = 26.sp) }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!state.engineConfigured) {
                Text(
                    text = stringResourceIn(bibleLanguage, R.string.verse_rec_offline_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MannaTheme.colors.muted
                )
                return@Column
            }

            Text(
                text = stringResourceIn(bibleLanguage, R.string.verse_rec_prompt),
                style = MaterialTheme.typography.titleMedium,
                color = MannaTheme.colors.ink,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = state.situationText,
                onValueChange = viewModel::onSituationChange,
                placeholder = { Text(stringResourceIn(bibleLanguage, R.string.verse_rec_placeholder)) },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = viewModel::recommend,
                enabled = state.situationText.isNotBlank() && !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
            ) { Text(stringResourceIn(bibleLanguage, R.string.verse_rec_recommend)) }

            if (state.isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(
                        text = stringResourceIn(bibleLanguage, R.string.verse_rec_thinking),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MannaTheme.colors.soft
                    )
                }
            }

            when (val result = state.result) {
                is VerseRecommendation.Success -> RecommendationCard(
                    result = result,
                    onShare = { shareTarget = result }
                )
                VerseRecommendation.Offline -> Text(
                    text = stringResourceIn(bibleLanguage, R.string.verse_rec_offline_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MannaTheme.colors.muted
                )
                is VerseRecommendation.Unavailable -> Text(
                    text = stringResourceIn(bibleLanguage, R.string.verse_rec_try_again),
                    style = MaterialTheme.typography.bodySmall,
                    color = MannaTheme.colors.muted
                )
                null -> Unit
            }
        }
    }

    // Sharing reuses the existing card pipeline exactly (render + share intents).
    shareTarget?.let { target ->
        VerseCardSheet(
            verseText = target.verseText,
            reference = target.reference,
            onDismiss = { shareTarget = null }
        )
    }
}

@Composable
private fun RecommendationCard(
    result: VerseRecommendation.Success,
    onShare: () -> Unit
) {
    val bibleLanguage = rememberBibleLanguage()
    Surface(
        color = MannaTheme.colors.card,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = result.verseText,
                fontFamily = ScriptureFontFamily,
                style = MaterialTheme.typography.titleMedium,
                color = MannaTheme.colors.ink,
                lineHeight = 28.sp
            )
            Text(
                text = result.reference,
                style = MaterialTheme.typography.labelLarge,
                color = MannaTheme.colors.gold,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResourceIn(bibleLanguage, R.string.verse_rec_message_label),
                style = MaterialTheme.typography.labelMedium,
                color = MannaTheme.colors.soft,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = result.personalMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MannaTheme.colors.ink
            )
            Button(
                onClick = onShare,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
            ) { Text(stringResourceIn(bibleLanguage, R.string.verse_rec_share)) }
        }
    }
}
