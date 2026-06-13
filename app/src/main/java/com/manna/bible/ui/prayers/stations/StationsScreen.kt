package com.manna.bible.ui.prayers.stations

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

/** Per-station prose, indexed by `number - 1`. Keeps resource ids explicit (no reflection). */
private val StationTitles = listOf(
    R.string.station_01_title, R.string.station_02_title, R.string.station_03_title,
    R.string.station_04_title, R.string.station_05_title, R.string.station_06_title,
    R.string.station_07_title, R.string.station_08_title, R.string.station_09_title,
    R.string.station_10_title, R.string.station_11_title, R.string.station_12_title,
    R.string.station_13_title, R.string.station_14_title
)
private val StationReflections = listOf(
    R.string.station_01_reflection, R.string.station_02_reflection, R.string.station_03_reflection,
    R.string.station_04_reflection, R.string.station_05_reflection, R.string.station_06_reflection,
    R.string.station_07_reflection, R.string.station_08_reflection, R.string.station_09_reflection,
    R.string.station_10_reflection, R.string.station_11_reflection, R.string.station_12_reflection,
    R.string.station_13_reflection, R.string.station_14_reflection
)
private val StationPrayers = listOf(
    R.string.station_01_prayer, R.string.station_02_prayer, R.string.station_03_prayer,
    R.string.station_04_prayer, R.string.station_05_prayer, R.string.station_06_prayer,
    R.string.station_07_prayer, R.string.station_08_prayer, R.string.station_09_prayer,
    R.string.station_10_prayer, R.string.station_11_prayer, R.string.station_12_prayer,
    R.string.station_13_prayer, R.string.station_14_prayer
)

/**
 * The Stations of the Cross — a meditation on Christ's passion, one station at a time.
 * Each station shows the traditional versicle and response, the Gospel passage (which
 * opens in the reader), a reflection, and a prayer. Move with Previous / Next.
 *
 * @param onBack returns to the Prayers hub.
 * @param onOpenVerse opens the reader at a canonical `OSIS.CHAPTER.VERSE`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationsScreen(
    modifier: Modifier = Modifier,
    viewModel: StationsViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onOpenVerse: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val backDescription = stringResource(R.string.prayers_back)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.stations_title), fontWeight = FontWeight.SemiBold)
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
        val current = state.current
        when {
            state.isLoading -> Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) { CircularProgressIndicator() }

            current == null -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.stations_intro),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MannaTheme.colors.soft
                )
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.stations_indicator, current.number, state.total),
                    style = MaterialTheme.typography.labelLarge,
                    color = MannaTheme.colors.gold,
                    fontWeight = FontWeight.Bold
                )
                StationTitles.getOrNull(current.number - 1)?.let { titleRes ->
                    Text(
                        text = stringResource(titleRes),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MannaTheme.colors.ink,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Versicle & response.
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.stations_versicle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MannaTheme.colors.soft,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(R.string.stations_response),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MannaTheme.colors.gold
                    )
                }

                // Scripture (opens in reader).
                if (current.verseText != null && current.reference != null) {
                    ScriptureCard(
                        text = current.verseText,
                        reference = current.reference,
                        onClick = { current.osisRef?.let(onOpenVerse) }
                    )
                }

                StationReflections.getOrNull(current.number - 1)?.let { reflectionRes ->
                    LabelledProse(
                        label = stringResource(R.string.stations_reflection_label),
                        body = stringResource(reflectionRes)
                    )
                }
                StationPrayers.getOrNull(current.number - 1)?.let { prayerRes ->
                    LabelledProse(
                        label = stringResource(R.string.stations_prayer_label),
                        body = stringResource(prayerRes)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = viewModel::previous,
                        enabled = state.canGoPrevious,
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.stations_previous)) }
                    Button(
                        onClick = viewModel::next,
                        enabled = state.canGoNext,
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.stations_next)) }
                }
            }
        }
    }
}

@Composable
private fun ScriptureCard(text: String, reference: String, onClick: () -> Unit) {
    Surface(
        color = MannaTheme.colors.card,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = text,
                fontFamily = ScriptureFontFamily,
                style = MaterialTheme.typography.titleMedium,
                color = MannaTheme.colors.ink,
                lineHeight = 28.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = reference,
                    style = MaterialTheme.typography.labelLarge,
                    color = MannaTheme.colors.gold,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.prayers_open_in_reader),
                    style = MaterialTheme.typography.labelMedium,
                    color = MannaTheme.colors.soft
                )
            }
        }
    }
}

@Composable
private fun LabelledProse(label: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MannaTheme.colors.muted,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MannaTheme.colors.soft,
            lineHeight = 26.sp
        )
    }
}
