package com.manna.bible.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manna.bible.R
import com.manna.bible.ui.theme.MannaTheme

/** A single tappable entry within a section. */
private data class MoreEntry(val label: String, val onClick: () -> Unit)

/** A titled group of entries. */
private data class MoreSection(val title: String, val entries: List<MoreEntry>)

/**
 * The "More" hub — a calm, grouped home for everything that isn't reading or the
 * calendar: the translation library, daily rhythms, care/prayer tools, practices,
 * sharing, and settings. Replaces the old flat "Library" dump with clear sections.
 *
 * Each destination is a nullable callback; null entries (feature off) are omitted,
 * and empty sections disappear.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    modifier: Modifier = Modifier,
    onOpenLibrary: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    onOpenTranslations: () -> Unit = {},
    onOpenDaily: (() -> Unit)? = null,
    onOpenCalendar: (() -> Unit)? = null,
    onOpenReminder: (() -> Unit)? = null,
    onOpenPrayer: (() -> Unit)? = null,
    onOpenCrisis: (() -> Unit)? = null,
    onOpenGrief: (() -> Unit)? = null,
    onOpenFasting: (() -> Unit)? = null,
    onOpenCard: (() -> Unit)? = null,
    onOpenSermon: (() -> Unit)? = null,
    onOpenChurch: (() -> Unit)? = null,
    onOpenAttribution: () -> Unit = {},
    viewModel: MoreViewModel = hiltViewModel()
) {
    val settings by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_more), fontWeight = FontWeight.SemiBold) }
            )
        }
    ) { padding ->
        val sections = listOf(
            MoreSection(
                stringResource(R.string.more_section_library),
                listOfNotNull(
                    onOpenLibrary?.let { MoreEntry(stringResource(R.string.more_library_entry), it) },
                )
            ),
            MoreSection(
                stringResource(R.string.more_section_bible),
                listOfNotNull(
                    MoreEntry(stringResource(R.string.more_translations), onOpenTranslations),
                    onOpenDaily?.let { MoreEntry(stringResource(R.string.reader_daily_verse), it) },
                    onOpenCalendar?.let { MoreEntry(stringResource(R.string.calendar_tool_entry), it) },
                    onOpenReminder?.let { MoreEntry(stringResource(R.string.reminder_tool_entry), it) }
                )
            ),
            MoreSection(
                stringResource(R.string.more_section_pray),
                listOfNotNull(
                    onOpenChurch?.let { MoreEntry(stringResource(R.string.church_entry), it) },
                    onOpenPrayer?.let { MoreEntry(stringResource(R.string.prayer_tool_entry), it) },
                    onOpenCrisis?.let { MoreEntry(stringResource(R.string.crisis_tool_entry), it) },
                    onOpenGrief?.let { MoreEntry(stringResource(R.string.grief_tool_entry), it) }
                )
            ),
            MoreSection(
                stringResource(R.string.more_section_practice),
                listOfNotNull(
                    onOpenFasting?.let { MoreEntry(stringResource(R.string.fasting_tool_entry), it) },
                    onOpenSermon?.let { MoreEntry(stringResource(R.string.sermon_tool_entry), it) }
                )
            ),
            MoreSection(
                stringResource(R.string.more_section_create),
                listOfNotNull(
                    onOpenCard?.let { MoreEntry(stringResource(R.string.card_tool_entry), it) }
                )
            ),
            MoreSection(
                stringResource(R.string.more_section_about),
                listOf(MoreEntry(stringResource(R.string.reader_attribution), onOpenAttribution))
            )
        ).filter { it.entries.isNotEmpty() }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sections.forEach { section ->
                item(key = "header_${section.title}") {
                    SectionHeader(section.title)
                }
                items(
                    items = section.entries,
                    key = { "${section.title}_${it.label}" }
                ) { entry ->
                    EntryCard(label = entry.label, onClick = entry.onClick)
                }
            }

            // --- Settings (Simplified / Elder Mode) ------------------------------
            item(key = "header_settings") {
                SectionHeader(stringResource(R.string.more_section_settings))
            }
            if (onOpenSettings != null) {
                item(key = "entry_all_settings") {
                    EntryCard(
                        label = stringResource(R.string.more_settings_entry),
                        onClick = onOpenSettings
                    )
                }
            }
            item(key = "toggle_simplified_mode") {
                SimplifiedModeToggle(
                    enabled = settings.simplifiedMode,
                    onToggle = viewModel::setSimplifiedMode
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MannaTheme.colors.gold,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
    )
}

/**
 * The Simplified (Elder / Oral Bible) Mode switch: enlarges scripture text and
 * controls and turns on continuous read-aloud. A 56dp minimum height keeps the
 * row comfortably tappable for the elderly users it serves (Req 14.5, a11y).
 */
@Composable
private fun SimplifiedModeToggle(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    val title = stringResource(R.string.more_simplified_mode)
    val description = stringResource(R.string.more_simplified_mode_desc)
    Surface(
        color = MannaTheme.colors.card,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 56.dp)
            .clickable { onToggle(!enabled) }
            .semantics(mergeDescendants = true) {}
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MannaTheme.colors.ink,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MannaTheme.colors.soft
                )
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun EntryCard(label: String, onClick: () -> Unit) {
    Surface(
        color = MannaTheme.colors.card,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MannaTheme.colors.ink,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)
        )
    }
}
