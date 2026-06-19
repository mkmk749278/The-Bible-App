package com.manna.bible.ui.prayers.rosary

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manna.bible.R
import com.manna.bible.domain.devotion.MysterySet
import com.manna.bible.ui.theme.MannaTheme
import com.manna.bible.ui.theme.ScriptureFontFamily

private val MinTouchTarget = 48.dp

/** Maps a mystery set to its localized name and traditional days. */
private fun setNameRes(set: MysterySet): Int = when (set) {
    MysterySet.JOYFUL -> R.string.rosary_set_joyful
    MysterySet.SORROWFUL -> R.string.rosary_set_sorrowful
    MysterySet.GLORIOUS -> R.string.rosary_set_glorious
    MysterySet.LUMINOUS -> R.string.rosary_set_luminous
}

private fun setDaysRes(set: MysterySet): Int = when (set) {
    MysterySet.JOYFUL -> R.string.rosary_set_joyful_days
    MysterySet.SORROWFUL -> R.string.rosary_set_sorrowful_days
    MysterySet.GLORIOUS -> R.string.rosary_set_glorious_days
    MysterySet.LUMINOUS -> R.string.rosary_set_luminous_days
}

/** Maps a mystery id to its localized title (explicit — no reflection). */
private fun mysteryTitleRes(id: String): Int = when (id) {
    "joyful_1" -> R.string.joyful_1_title
    "joyful_2" -> R.string.joyful_2_title
    "joyful_3" -> R.string.joyful_3_title
    "joyful_4" -> R.string.joyful_4_title
    "joyful_5" -> R.string.joyful_5_title
    "sorrowful_1" -> R.string.sorrowful_1_title
    "sorrowful_2" -> R.string.sorrowful_2_title
    "sorrowful_3" -> R.string.sorrowful_3_title
    "sorrowful_4" -> R.string.sorrowful_4_title
    "sorrowful_5" -> R.string.sorrowful_5_title
    "glorious_1" -> R.string.glorious_1_title
    "glorious_2" -> R.string.glorious_2_title
    "glorious_3" -> R.string.glorious_3_title
    "glorious_4" -> R.string.glorious_4_title
    "glorious_5" -> R.string.glorious_5_title
    "luminous_1" -> R.string.luminous_1_title
    "luminous_2" -> R.string.luminous_2_title
    "luminous_3" -> R.string.luminous_3_title
    "luminous_4" -> R.string.luminous_4_title
    else -> R.string.luminous_5_title
}

/** Maps a mystery id to its localized fruit. */
private fun mysteryFruitRes(id: String): Int = when (id) {
    "joyful_1" -> R.string.joyful_1_fruit
    "joyful_2" -> R.string.joyful_2_fruit
    "joyful_3" -> R.string.joyful_3_fruit
    "joyful_4" -> R.string.joyful_4_fruit
    "joyful_5" -> R.string.joyful_5_fruit
    "sorrowful_1" -> R.string.sorrowful_1_fruit
    "sorrowful_2" -> R.string.sorrowful_2_fruit
    "sorrowful_3" -> R.string.sorrowful_3_fruit
    "sorrowful_4" -> R.string.sorrowful_4_fruit
    "sorrowful_5" -> R.string.sorrowful_5_fruit
    "glorious_1" -> R.string.glorious_1_fruit
    "glorious_2" -> R.string.glorious_2_fruit
    "glorious_3" -> R.string.glorious_3_fruit
    "glorious_4" -> R.string.glorious_4_fruit
    "glorious_5" -> R.string.glorious_5_fruit
    "luminous_1" -> R.string.luminous_1_fruit
    "luminous_2" -> R.string.luminous_2_fruit
    "luminous_3" -> R.string.luminous_3_fruit
    "luminous_4" -> R.string.luminous_4_fruit
    else -> R.string.luminous_5_fruit
}

/**
 * The Rosary (Japamala) — choose the mysteries, then pray each decade with an
 * interactive bead counter. The Gospel passage for each mystery opens in the reader.
 *
 * @param onBack returns to the Prayers hub.
 * @param onOpenVerse opens the reader at a canonical `OSIS.CHAPTER.VERSE`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RosaryScreen(
    modifier: Modifier = Modifier,
    viewModel: RosaryViewModel = hiltViewModel(),
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
                    Text(stringResource(R.string.rosary_title), fontWeight = FontWeight.SemiBold)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mystery-set selector.
            Text(
                text = stringResource(R.string.rosary_choose_mysteries),
                style = MaterialTheme.typography.labelMedium,
                color = MannaTheme.colors.muted,
                fontWeight = FontWeight.Bold
            )
            MysterySetSelector(
                selected = state.set,
                todaysSet = state.todaysSet,
                onSelect = viewModel::selectSet
            )

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val current = state.current
                if (current != null) {
                    Text(
                        text = stringResource(R.string.rosary_mystery_indicator, current.number, state.total),
                        style = MaterialTheme.typography.labelLarge,
                        color = MannaTheme.colors.gold,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(mysteryTitleRes(current.id)),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MannaTheme.colors.ink,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.rosary_fruit_label) + ":",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MannaTheme.colors.muted
                        )
                        Text(
                            text = stringResource(mysteryFruitRes(current.id)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MannaTheme.colors.lavender,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (current.verseText != null && current.reference != null) {
                        ScriptureCard(
                            text = current.verseText,
                            reference = current.reference,
                            onClick = { current.osisRef?.let(onOpenVerse) }
                        )
                    }

                    Text(
                        text = stringResource(R.string.rosary_decade_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MannaTheme.colors.soft
                    )

                    BeadCounter(
                        count = state.beadCount,
                        onTapBead = viewModel::tapBead,
                        onReset = viewModel::resetBeads
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = viewModel::previous,
                            enabled = state.canGoPrevious,
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(R.string.rosary_previous)) }
                        Button(
                            onClick = viewModel::next,
                            enabled = state.canGoNext,
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(R.string.rosary_next)) }
                    }
                }
            }

            // The prayers themselves — tap a title to read the words, in the app's
            // language where available (falling back to English otherwise).
            PrayersSection()
        }
    }
}

/** The ordered prayers of the Rosary, each (title, body) as string resources. */
private val RosaryPrayers: List<Pair<Int, Int>> = listOf(
    R.string.rosary_prayer_sign_title to R.string.rosary_prayer_sign,
    R.string.rosary_prayer_creed_title to R.string.rosary_prayer_creed,
    R.string.rosary_prayer_our_father_title to R.string.rosary_prayer_our_father,
    R.string.rosary_prayer_hail_mary_title to R.string.rosary_prayer_hail_mary,
    R.string.rosary_prayer_glory_be_title to R.string.rosary_prayer_glory_be,
    R.string.rosary_prayer_fatima_title to R.string.rosary_prayer_fatima,
    R.string.rosary_prayer_hail_holy_queen_title to R.string.rosary_prayer_hail_holy_queen
)

@Composable
private fun PrayersSection() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.rosary_prayers_title),
            style = MaterialTheme.typography.labelMedium,
            color = MannaTheme.colors.muted,
            fontWeight = FontWeight.Bold
        )
        RosaryPrayers.forEach { (titleRes, textRes) ->
            PrayerItem(titleRes = titleRes, textRes = textRes)
        }
    }
}

@Composable
private fun PrayerItem(titleRes: Int, textRes: Int) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        color = MannaTheme.colors.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(titleRes),
                    style = MaterialTheme.typography.titleSmall,
                    color = MannaTheme.colors.ink,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (expanded) "–" else "+",
                    style = MaterialTheme.typography.titleMedium,
                    color = MannaTheme.colors.gold,
                    fontWeight = FontWeight.Bold
                )
            }
            if (expanded) {
                Text(
                    text = stringResource(textRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MannaTheme.colors.soft,
                    lineHeight = 24.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MysterySetSelector(
    selected: MysterySet,
    todaysSet: MysterySet,
    onSelect: (MysterySet) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MysterySet.entries.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { set ->
                    val isToday = set == todaysSet
                    FilterChip(
                        selected = set == selected,
                        onClick = { onSelect(set) },
                        modifier = Modifier.weight(1f),
                        label = {
                            Column {
                                Text(stringResource(setNameRes(set)))
                                Text(
                                    text = if (isToday) {
                                        stringResource(R.string.rosary_today_label)
                                    } else {
                                        stringResource(setDaysRes(set))
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isToday) MannaTheme.colors.gold else MannaTheme.colors.muted
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MannaTheme.colors.gold.copy(alpha = 0.18f)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun BeadCounter(
    count: Int,
    onTapBead: () -> Unit,
    onReset: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Surface(
        color = MannaTheme.colors.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.rosary_bead_count, count),
                style = MaterialTheme.typography.titleMedium,
                color = MannaTheme.colors.gold,
                fontWeight = FontWeight.Bold
            )
            // Two rows of five beads.
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                (0 until 2).forEach { rowIndex ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        (0 until 5).forEach { col ->
                            val beadIndex = rowIndex * 5 + col
                            Bead(
                                filled = beadIndex < count,
                                isNext = beadIndex == count,
                                onTap = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onTapBead()
                                }
                            )
                        }
                    }
                }
            }
            Text(
                text = stringResource(R.string.rosary_bead_tap_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MannaTheme.colors.muted
            )
            TextButton(onClick = onReset) {
                Text(stringResource(R.string.rosary_bead_reset), color = MannaTheme.colors.soft)
            }
        }
    }
}

@Composable
private fun Bead(filled: Boolean, isNext: Boolean, onTap: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (filled) 1f else if (isNext) 1.08f else 0.92f,
        animationSpec = tween(durationMillis = 200),
        label = "bead-scale"
    )
    val color = when {
        filled -> MannaTheme.colors.gold
        isNext -> MannaTheme.colors.goldDim
        else -> MannaTheme.colors.border
    }
    Box(
        modifier = Modifier
            .size(MinTouchTarget)
            .scale(scale)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onTap)
    )
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
