package com.manna.bible.ui.prayers.paraloka

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manna.bible.R
import com.manna.bible.ui.theme.MannaTheme
import com.manna.bible.ui.theme.ScriptureFontFamily

private val MinTouchTarget = 48.dp

/** Maps a passage id to its localized theme. */
private fun passageThemeRes(id: String): Int = when (id) {
    "paraloka_01" -> R.string.paraloka_01_theme
    "paraloka_02" -> R.string.paraloka_02_theme
    "paraloka_03" -> R.string.paraloka_03_theme
    "paraloka_04" -> R.string.paraloka_04_theme
    "paraloka_05" -> R.string.paraloka_05_theme
    "paraloka_06" -> R.string.paraloka_06_theme
    "paraloka_07" -> R.string.paraloka_07_theme
    "paraloka_08" -> R.string.paraloka_08_theme
    "paraloka_09" -> R.string.paraloka_09_theme
    "paraloka_10" -> R.string.paraloka_10_theme
    "paraloka_11" -> R.string.paraloka_11_theme
    "paraloka_12" -> R.string.paraloka_12_theme
    "paraloka_13" -> R.string.paraloka_13_theme
    "paraloka_14" -> R.string.paraloka_14_theme
    else -> R.string.paraloka_15_theme
}

/** Maps a passage id to its localized reflection. */
private fun passageReflectionRes(id: String): Int = when (id) {
    "paraloka_01" -> R.string.paraloka_01_reflection
    "paraloka_02" -> R.string.paraloka_02_reflection
    "paraloka_03" -> R.string.paraloka_03_reflection
    "paraloka_04" -> R.string.paraloka_04_reflection
    "paraloka_05" -> R.string.paraloka_05_reflection
    "paraloka_06" -> R.string.paraloka_06_reflection
    "paraloka_07" -> R.string.paraloka_07_reflection
    "paraloka_08" -> R.string.paraloka_08_reflection
    "paraloka_09" -> R.string.paraloka_09_reflection
    "paraloka_10" -> R.string.paraloka_10_reflection
    "paraloka_11" -> R.string.paraloka_11_reflection
    "paraloka_12" -> R.string.paraloka_12_reflection
    "paraloka_13" -> R.string.paraloka_13_reflection
    "paraloka_14" -> R.string.paraloka_14_reflection
    else -> R.string.paraloka_15_reflection
}

/** Maps a prayer id to its localized title. */
private fun prayerTitleRes(id: String): Int = when (id) {
    "paraloka_prayer_commendation" -> R.string.paraloka_prayer_commendation_title
    "paraloka_prayer_rest" -> R.string.paraloka_prayer_rest_title
    "paraloka_prayer_comfort" -> R.string.paraloka_prayer_comfort_title
    "paraloka_prayer_hope" -> R.string.paraloka_prayer_hope_title
    else -> R.string.paraloka_prayer_light_title
}

/** Maps a prayer id to its localized text. */
private fun prayerTextRes(id: String): Int = when (id) {
    "paraloka_prayer_commendation" -> R.string.paraloka_prayer_commendation_text
    "paraloka_prayer_rest" -> R.string.paraloka_prayer_rest_text
    "paraloka_prayer_comfort" -> R.string.paraloka_prayer_comfort_text
    "paraloka_prayer_hope" -> R.string.paraloka_prayer_hope_text
    else -> R.string.paraloka_prayer_light_text
}

/**
 * Paraloka — eternal-life Scripture and prayers of Christian hope. Shows the curated
 * passages (each opening in the reader) and the prayers of commendation, rest, and
 * comfort drawn from the Indian churches' funeral traditions.
 *
 * @param onBack returns to the Prayers hub.
 * @param onOpenVerse opens the reader at a canonical `OSIS.CHAPTER.VERSE`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParalokaScreen(
    modifier: Modifier = Modifier,
    viewModel: ParalokaViewModel = hiltViewModel(),
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
                    Text(stringResource(R.string.paraloka_title), fontWeight = FontWeight.SemiBold)
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "intro") {
                Text(
                    text = stringResource(R.string.paraloka_intro),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MannaTheme.colors.soft,
                    lineHeight = 26.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            item(key = "scripture_heading") {
                SectionHeading(stringResource(R.string.paraloka_scripture_heading))
            }
            items(state.passages, key = { it.id }) { passage ->
                if (passage.verseText != null && passage.reference != null) {
                    PassageCard(
                        theme = stringResource(passageThemeRes(passage.id)),
                        text = passage.verseText,
                        reference = passage.reference,
                        reflection = stringResource(passageReflectionRes(passage.id)),
                        onClick = { passage.osisRef?.let(onOpenVerse) }
                    )
                }
            }
            item(key = "prayers_heading") {
                SectionHeading(stringResource(R.string.paraloka_prayers_heading))
            }
            items(state.prayers, key = { it.id }) { prayer ->
                PrayerCard(
                    title = stringResource(prayerTitleRes(prayer.id)),
                    text = stringResource(prayerTextRes(prayer.id)),
                    reference = prayer.reference,
                    onClick = { prayer.osisRef?.let(onOpenVerse) }
                )
            }
        }
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MannaTheme.colors.gold,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun PassageCard(
    theme: String,
    text: String,
    reference: String,
    reflection: String,
    onClick: () -> Unit
) {
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
                text = theme,
                style = MaterialTheme.typography.labelMedium,
                color = MannaTheme.colors.cyan,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = text,
                fontFamily = ScriptureFontFamily,
                style = MaterialTheme.typography.titleMedium,
                color = MannaTheme.colors.ink,
                lineHeight = 28.sp
            )
            Text(
                text = reflection,
                style = MaterialTheme.typography.bodyMedium,
                color = MannaTheme.colors.soft,
                lineHeight = 24.sp
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
private fun PrayerCard(
    title: String,
    text: String,
    reference: String?,
    onClick: () -> Unit
) {
    Surface(
        color = MannaTheme.colors.surface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MannaTheme.colors.ink,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MannaTheme.colors.soft,
                lineHeight = 26.sp
            )
            if (reference != null) {
                Text(
                    text = reference,
                    style = MaterialTheme.typography.labelMedium,
                    color = MannaTheme.colors.gold,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
