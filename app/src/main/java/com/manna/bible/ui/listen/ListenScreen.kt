package com.manna.bible.ui.listen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manna.bible.R
import com.manna.bible.ui.home.HomeViewModel
import com.manna.bible.ui.theme.MannaTheme

private val MinTouchTarget = 48.dp

/**
 * The Listen destination (UX directive): a calm, audio-first entry point that
 * resumes read-aloud from the last reading position. Playback itself happens in
 * the reader, where the spoken verse is highlighted; this surface just starts it.
 *
 * @param onContinueListening opens the reader at the last position with
 *   read-aloud starting automatically.
 */
@Composable
fun ListenScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    onContinueListening: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = stringResource(R.string.nav_listen),
            style = MaterialTheme.typography.headlineMedium,
            color = MannaTheme.colors.gold,
            fontWeight = FontWeight.SemiBold
        )

        Surface(
            color = MannaTheme.colors.card,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = MinTouchTarget)
                .clickable(onClick = onContinueListening)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_continue_listening),
                    style = MaterialTheme.typography.titleSmall,
                    color = MannaTheme.colors.soft,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = state.continueLabel
                        ?.let { stringResource(R.string.home_listen_from, it) }
                        ?: stringResource(R.string.home_listen_start),
                    style = MaterialTheme.typography.titleLarge,
                    color = MannaTheme.colors.ink
                )
            }
        }

        Text(
            text = stringResource(R.string.listen_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MannaTheme.colors.soft
        )
    }
}
