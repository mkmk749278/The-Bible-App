package com.manna.bible.ui.fasting

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.manna.bible.domain.fasting.FastPlan
import com.manna.bible.ui.theme.MannaTheme
import com.manna.bible.ui.theme.ScriptureFontFamily
import kotlinx.coroutines.delay

private val MinTouchTarget = 48.dp

@StringRes
private fun planLabel(id: String): Int = when (id) {
    "partial" -> R.string.fasting_plan_partial
    "sunrise_sunset" -> R.string.fasting_plan_sunrise_sunset
    "full_day" -> R.string.fasting_plan_full_day
    "three_day" -> R.string.fasting_plan_three_day
    else -> R.string.fasting_plan_partial
}

/**
 * Fasting Companion — choose a time-boxed fast, watch its progress, and turn hunger
 * toward God with focus Scripture. Fully offline.
 *
 * @param onBack returns to the previous surface.
 * @param onOpenVerse opens the reader at a canonical `OSIS.CHAPTER.VERSE`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FastingScreen(
    modifier: Modifier = Modifier,
    viewModel: FastingViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onOpenVerse: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val backDescription = stringResource(R.string.fasting_back)

    // Tick the countdown while a fast is active (kept in the UI so the ViewModel has
    // no infinite loop).
    LaunchedEffect(state.active) {
        while (state.active) {
            delay(1000)
            viewModel.refresh()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.fasting_title), fontWeight = FontWeight.SemiBold) },
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
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) { CircularProgressIndicator() }

            state.active -> ActiveFast(
                modifier = Modifier.padding(padding),
                state = state,
                onEnd = viewModel::end,
                onOpenVerse = onOpenVerse
            )

            else -> PlanChooser(
                modifier = Modifier.padding(padding),
                plans = state.plans,
                onStart = viewModel::start
            )
        }
    }
}

@Composable
private fun PlanChooser(modifier: Modifier, plans: List<FastPlan>, onStart: (String) -> Unit) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.fasting_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = MannaTheme.colors.soft,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        items(items = plans, key = { it.id }) { plan ->
            Surface(
                color = MannaTheme.colors.card,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().clickable { onStart(plan.id) }
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(planLabel(plan.id)),
                        style = MaterialTheme.typography.titleMedium,
                        color = MannaTheme.colors.ink,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.fasting_hours, plan.hours),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MannaTheme.colors.gold
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveFast(
    modifier: Modifier,
    state: FastingUiState,
    onEnd: () -> Unit,
    onOpenVerse: (String) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = stringResource(
                if (state.isComplete) R.string.fasting_complete else R.string.fasting_active_title
            ),
            style = MaterialTheme.typography.headlineSmall,
            color = MannaTheme.colors.ink,
            fontWeight = FontWeight.SemiBold
        )
        if (!state.isComplete) {
            Text(
                text = stringResource(R.string.fasting_remaining, formatRemaining(state.remainingMillis)),
                style = MaterialTheme.typography.titleMedium,
                color = MannaTheme.colors.gold,
                fontWeight = FontWeight.Bold
            )
            LinearProgressIndicator(
                progress = { state.fractionComplete },
                modifier = Modifier.fillMaxWidth()
            )
        }

        state.focusText?.let { text ->
            Surface(
                color = MannaTheme.colors.card,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().clickable { state.focusOsisRef?.let(onOpenVerse) }
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.fasting_focus_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MannaTheme.colors.soft
                    )
                    Text(
                        text = text,
                        fontFamily = ScriptureFontFamily,
                        style = MaterialTheme.typography.titleMedium,
                        color = MannaTheme.colors.ink,
                        lineHeight = 28.sp
                    )
                    Text(
                        text = state.focusReference,
                        style = MaterialTheme.typography.labelLarge,
                        color = MannaTheme.colors.gold,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        OutlinedButton(onClick = onEnd, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.fasting_end))
        }
    }
}

/** Formats remaining millis as `H:MM:SS` (or `M:SS` under an hour). */
private fun formatRemaining(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
