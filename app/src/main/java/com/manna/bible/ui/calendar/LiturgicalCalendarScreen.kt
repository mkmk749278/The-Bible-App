package com.manna.bible.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manna.bible.R
import com.manna.bible.domain.calendar.LiturgicalColor
import com.manna.bible.ui.theme.MannaTheme
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Maps a liturgical (vestment) colour to a palette token. */
@Composable
private fun liturgicalColor(color: LiturgicalColor): Color = when (color) {
    LiturgicalColor.VIOLET -> MannaTheme.colors.lavender
    LiturgicalColor.WHITE -> MannaTheme.colors.gold
    LiturgicalColor.GREEN -> MannaTheme.colors.sage
    LiturgicalColor.RED -> MannaTheme.colors.red
}

/**
 * The Calendar tab — a real month grid of the liturgical year for the chosen tradition.
 * Days are tinted by their season's colour; feasts and fast days are marked. Tapping a
 * day shows its detail (season, feast + readings, fast); a feast can be opened in the
 * reader. "Today" jumps back to the current month.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiturgicalCalendarScreen(
    modifier: Modifier = Modifier,
    viewModel: LiturgicalCalendarViewModel = hiltViewModel(),
    onOpenVerse: (String) -> Unit = {},
    onFindVerse: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.nav_calendar), fontWeight = FontWeight.SemiBold)
                },
                actions = {
                    TextButton(onClick = viewModel::goToToday) {
                        Text(stringResource(R.string.calendar_today))
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // Month navigation.
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = viewModel::previousMonth) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.calendar_previous_month)
                    )
                }
                Text(
                    text = YearMonth.of(state.year, state.month).format(monthFormatter),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MannaTheme.colors.ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = viewModel::nextMonth) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.calendar_next_month)
                    )
                }
            }

            // Weekday header.
            val weekdays = stringArrayResource(R.array.calendar_weekdays)
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                weekdays.forEach { letter ->
                    Text(
                        text = letter,
                        style = MaterialTheme.typography.labelMedium,
                        color = MannaTheme.colors.muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).clearAndSetSemantics {}
                    )
                }
            }

            // The month grid, Sunday-first, chunked into weeks.
            val cells: List<CalendarDayCell?> =
                List(state.leadingBlanks) { null } + state.days
            val padded = cells + List((7 - cells.size % 7) % 7) { null }
            padded.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { cell ->
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            if (cell != null) {
                                DayCell(
                                    cell = cell,
                                    selected = cell.date == state.selected?.date,
                                    onClick = { viewModel.selectDate(cell.date) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            state.selected?.let { selected ->
                SelectedDayCard(selected = selected, onOpenVerse = onOpenVerse, onFindVerse = onFindVerse)
            }

            Spacer(Modifier.height(20.dp))

            // The whole month's feasts at a glance — tap one to jump to its day.
            MonthEventsList(
                events = state.monthEvents,
                selectedDate = state.selected?.date,
                onSelect = viewModel::selectDate
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * The shown month's feasts as a tappable overview, so the user sees every event at
 * once rather than discovering them by tapping individual cells. Selecting one jumps
 * the detail card (and the grid selection) to that day.
 */
@Composable
private fun MonthEventsList(
    events: List<MonthEvent>,
    selectedDate: java.time.LocalDate?,
    onSelect: (java.time.LocalDate) -> Unit
) {
    Text(
        text = stringResource(R.string.calendar_events_header),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MannaTheme.colors.gold,
        modifier = Modifier.padding(bottom = 6.dp)
    )
    if (events.isEmpty()) {
        Text(
            text = stringResource(R.string.calendar_events_none),
            style = MaterialTheme.typography.bodyMedium,
            color = MannaTheme.colors.muted
        )
        return
    }
    val dayFormatter = remember { DateTimeFormatter.ofPattern("EEE d", Locale.getDefault()) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        events.forEach { event ->
            val isSelected = event.date == selectedDate
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .then(
                        if (isSelected) {
                            Modifier.background(MannaTheme.colors.gold.copy(alpha = 0.12f))
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onSelect(event.date) }
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Dot(MannaTheme.colors.gold)
                Spacer(Modifier.size(10.dp))
                Text(
                    text = event.date.format(dayFormatter),
                    style = MaterialTheme.typography.labelMedium,
                    color = MannaTheme.colors.soft,
                    modifier = Modifier.width(56.dp)
                )
                Text(
                    text = stringResource(eventNameRes(event.feastId)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MannaTheme.colors.ink
                )
            }
        }
    }
}

@Composable
private fun DayCell(cell: CalendarDayCell, selected: Boolean, onClick: () -> Unit) {
    val tint = liturgicalColor(cell.color)
    val feastLabel = stringResource(R.string.calendar_feast_label)
    val fastLabel = stringResource(R.string.calendar_fast_label)
    val description = buildString {
        append(cell.date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())))
        if (cell.isFeast) append(", ").append(feastLabel)
        if (cell.isFast) append(", ").append(fastLabel)
    }
    Box(
        modifier = Modifier
            .padding(2.dp)
            .heightIn(min = 44.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = 0.16f))
            .then(
                if (selected) Modifier.border(2.dp, MannaTheme.colors.gold, RoundedCornerShape(10.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 6.dp)
        ) {
            Text(
                text = cell.dayOfMonth.toString(),
                fontSize = 15.sp,
                fontWeight = if (cell.isToday || cell.isFeast) FontWeight.Bold else FontWeight.Normal,
                color = if (cell.isToday) MannaTheme.colors.gold else MannaTheme.colors.ink
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                if (cell.isFeast) Dot(MannaTheme.colors.gold)
                if (cell.isFast) Dot(MannaTheme.colors.lavender)
            }
        }
    }
}

@Composable
private fun Dot(color: Color) {
    Box(
        modifier = Modifier
            .size(5.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun SelectedDayCard(selected: SelectedDay, onOpenVerse: (String) -> Unit, onFindVerse: (() -> Unit)? = null) {
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())
    }
    Surface(
        color = MannaTheme.colors.card,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = selected.date.format(dateFormatter),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MannaTheme.colors.ink
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Dot(liturgicalColor(selected.color))
                Spacer(Modifier.size(6.dp))
                Text(
                    text = stringResource(seasonNameRes(selected.season)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MannaTheme.colors.soft
                )
                if (selected.isFast) {
                    Spacer(Modifier.size(10.dp))
                    Text(
                        text = stringResource(R.string.calendar_fast_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MannaTheme.colors.lavender,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (selected.feastId != null) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(eventNameRes(selected.feastId)),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MannaTheme.colors.gold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(eventDescriptionRes(selected.feastId)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MannaTheme.colors.soft
                )
            }

            if (selected.readings.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.calendar_readings_header),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MannaTheme.colors.muted
                )
                Spacer(Modifier.height(4.dp))
                selected.readings.forEach { reading ->
                    ReadingRowItem(reading = reading, onOpenVerse = onOpenVerse)
                }
            } else if (selected.feastId != null && selected.osisRef != null) {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { onOpenVerse(selected.osisRef) },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Text(stringResource(R.string.calendar_read_passage))
                }
            }

            selected.dailyVerse?.let { verse ->
                Spacer(Modifier.height(18.dp))
                VerseOfDaySection(verse = verse, onOpenVerse = onOpenVerse, onFindVerse = onFindVerse)
            }
        }
    }
}

/**
 * The verse of the day for the selected date, displayed as a share-ready card —
 * the same visual style as the "Share a verse" screen in the More tab.
 */
@Composable
private fun VerseOfDaySection(verse: CalendarDailyVerse, onOpenVerse: (String) -> Unit, onFindVerse: (() -> Unit)? = null) {
    var showCard by remember { mutableStateOf(false) }

    Text(
        text = stringResource(R.string.calendar_verse_header),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MannaTheme.colors.gold,
        modifier = Modifier.padding(bottom = 10.dp)
    )

    // Verse card — same look as the "Share a verse" preview card.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MannaTheme.colors.card)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = verse.text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MannaTheme.colors.ink,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp
            )
            // Gold underline divider
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(2.dp)
                    .background(MannaTheme.colors.gold, RoundedCornerShape(1.dp))
            )
            Text(
                text = verse.reference,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MannaTheme.colors.gold,
                textAlign = TextAlign.Center
            )
        }
    }

    Spacer(Modifier.height(12.dp))

    // Action row
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        TextButton(
            onClick = { onOpenVerse(verse.osisRef) },
            modifier = Modifier.weight(1f)
        ) {
            Text(stringResource(R.string.calendar_verse_open))
        }
        Button(
            onClick = { showCard = true },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MannaTheme.colors.gold,
                contentColor = MannaTheme.colors.bg
            )
        ) {
            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.calendar_verse_share))
        }
    }

    if (showCard) {
        com.manna.bible.ui.card.VerseCardSheet(
            verseText = verse.text,
            reference = verse.reference,
            onDismiss = { showCard = false },
            onFindVerse = onFindVerse
        )
    }
}

@Composable
private fun ReadingRowItem(reading: ReadingRow, onOpenVerse: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onOpenVerse(reading.osisRef) }
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = stringResource(readingKindRes(reading.kind)),
            style = MaterialTheme.typography.labelMedium,
            color = MannaTheme.colors.gold,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = reading.reference,
            style = MaterialTheme.typography.bodyMedium,
            color = MannaTheme.colors.ink,
            modifier = Modifier.weight(0.6f)
        )
    }
}
