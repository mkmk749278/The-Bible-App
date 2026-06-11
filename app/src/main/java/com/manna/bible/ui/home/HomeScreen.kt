package com.manna.bible.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manna.bible.R
import com.manna.bible.ui.daily.DailyVerseViewModel
import com.manna.bible.ui.theme.MannaTheme

private val MinTouchTarget = 48.dp

/**
 * The Home experience (UX directive): exactly three components — Continue
 * Reading, Today's Verse, Continue Listening. No feeds, no promotions.
 *
 * @param onContinueReading opens the reader at the last position.
 * @param onOpenVerse opens the reader at a canonical `OSIS.CHAPTER.VERSE`.
 * @param onContinueListening opens the reader at the last position with
 *   read-aloud starting automatically.
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    dailyViewModel: DailyVerseViewModel = hiltViewModel(),
    onContinueReading: () -> Unit = {},
    onOpenVerse: (String) -> Unit = {},
    onContinueListening: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val daily by dailyViewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            color = MannaTheme.colors.gold,
            fontWeight = FontWeight.SemiBold
        )

        HomeCard(
            title = stringResource(R.string.home_continue_reading),
            body = state.continueLabel ?: stringResource(R.string.home_start_reading),
            onClick = onContinueReading
        )

        if (daily.verseText != null) {
            HomeCard(
                title = stringResource(R.string.home_todays_verse),
                body = "“${daily.verseText}”",
                footer = daily.reference,
                onClick = { daily.osisRef?.let(onOpenVerse) }
            )
        }

        HomeCard(
            title = stringResource(R.string.home_continue_listening),
            body = state.continueLabel
                ?.let { stringResource(R.string.home_listen_from, it) }
                ?: stringResource(R.string.home_listen_start),
            onClick = onContinueListening
        )
    }
}

@Composable
private fun HomeCard(
    title: String,
    body: String,
    onClick: () -> Unit,
    footer: String? = null
) {
    Surface(
        color = MannaTheme.colors.card,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = MinTouchTarget)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MannaTheme.colors.soft,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = body,
                style = MaterialTheme.typography.titleLarge,
                color = MannaTheme.colors.ink,
                lineHeight = 30.sp
            )
            if (footer != null) {
                Text(
                    text = footer,
                    style = MaterialTheme.typography.labelLarge,
                    color = MannaTheme.colors.gold
                )
            }
        }
    }
}
