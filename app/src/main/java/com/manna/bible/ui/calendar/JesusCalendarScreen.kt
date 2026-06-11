package com.manna.bible.ui.calendar

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.remember
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
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MinTouchTarget = 48.dp

/**
 * The Jesus Events Calendar: a calm chronological list of the key events of Jesus'
 * life for the current year, with the next upcoming event highlighted. Tapping an
 * event opens its primary passage in the reader. Fully offline.
 *
 * @param onBack returns to the previous surface.
 * @param onOpenVerse opens the reader at a canonical `OSIS.CHAPTER.VERSE`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JesusCalendarScreen(
    modifier: Modifier = Modifier,
    viewModel: JesusCalendarViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onOpenVerse: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val backDescription = stringResource(R.string.calendar_back)
    val formatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.calendar_title, state.year),
                        fontWeight = FontWeight.SemiBold
                    )
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 20.dp,
                vertical = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items = state.rows, key = { it.id }) { row ->
                CalendarEventCard(
                    name = stringResource(eventNameRes(row.id)),
                    description = stringResource(eventDescriptionRes(row.id)),
                    dateLabel = row.date.format(formatter),
                    isNext = row.isNext,
                    onClick = { row.osisRef?.let(onOpenVerse) }
                )
            }
        }
    }
}

@Composable
private fun CalendarEventCard(
    name: String,
    description: String,
    dateLabel: String,
    isNext: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = MannaTheme.colors.card,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MannaTheme.colors.gold,
                    fontWeight = FontWeight.Bold
                )
                if (isNext) {
                    Surface(
                        color = MannaTheme.colors.sage,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.calendar_next_badge),
                            style = MaterialTheme.typography.labelSmall,
                            color = MannaTheme.colors.bg,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = MannaTheme.colors.ink,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MannaTheme.colors.soft
            )
        }
    }
}

@StringRes
private fun eventNameRes(id: String): Int = when (id) {
    "holy_name" -> R.string.calendar_event_holy_name_name
    "epiphany" -> R.string.calendar_event_epiphany_name
    "presentation" -> R.string.calendar_event_presentation_name
    "annunciation" -> R.string.calendar_event_annunciation_name
    "transfiguration" -> R.string.calendar_event_transfiguration_name
    "nativity" -> R.string.calendar_event_nativity_name
    "holy_innocents" -> R.string.calendar_event_holy_innocents_name
    "ash_wednesday" -> R.string.calendar_event_ash_wednesday_name
    "palm_sunday" -> R.string.calendar_event_palm_sunday_name
    "maundy_thursday" -> R.string.calendar_event_maundy_thursday_name
    "good_friday" -> R.string.calendar_event_good_friday_name
    "easter" -> R.string.calendar_event_easter_name
    "ascension" -> R.string.calendar_event_ascension_name
    "pentecost" -> R.string.calendar_event_pentecost_name
    else -> R.string.calendar_event_easter_name
}

@StringRes
private fun eventDescriptionRes(id: String): Int = when (id) {
    "holy_name" -> R.string.calendar_event_holy_name_desc
    "epiphany" -> R.string.calendar_event_epiphany_desc
    "presentation" -> R.string.calendar_event_presentation_desc
    "annunciation" -> R.string.calendar_event_annunciation_desc
    "transfiguration" -> R.string.calendar_event_transfiguration_desc
    "nativity" -> R.string.calendar_event_nativity_desc
    "holy_innocents" -> R.string.calendar_event_holy_innocents_desc
    "ash_wednesday" -> R.string.calendar_event_ash_wednesday_desc
    "palm_sunday" -> R.string.calendar_event_palm_sunday_desc
    "maundy_thursday" -> R.string.calendar_event_maundy_thursday_desc
    "good_friday" -> R.string.calendar_event_good_friday_desc
    "easter" -> R.string.calendar_event_easter_desc
    "ascension" -> R.string.calendar_event_ascension_desc
    "pentecost" -> R.string.calendar_event_pentecost_desc
    else -> R.string.calendar_event_easter_desc
}
