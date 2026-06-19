package com.manna.bible.ui.church

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manna.bible.R
import com.manna.bible.domain.liturgy.LiturgyPart
import com.manna.bible.domain.liturgy.LiturgyRole
import com.manna.bible.domain.liturgy.LiturgySection
import com.manna.bible.ui.theme.MannaTheme

private val MinTouchTarget = 48.dp

/**
 * Church Mode — the order of worship for the user's tradition, followed step by step.
 * Each part shows who speaks (presider / people / all / reader) or a rubric (an
 * instruction), the public-domain / response text, and — where a presidential prayer
 * is proper to the day — a note to follow the parish book. Scripture references open
 * in the reader.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChurchModeScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChurchModeViewModel = hiltViewModel(),
    onOpenVerse: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val backDescription = stringResource(R.string.church_back)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.church_title), fontWeight = FontWeight.SemiBold) },
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
        if (state.isLoading) return@Scaffold
        val selected = state.selected
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Tradition switcher (and a note when nothing matched the user's tradition).
            if (state.available.size > 1 || !state.matchedTradition) {
                item(key = "switcher") {
                    Column {
                        if (!state.matchedTradition) {
                            Text(
                                text = stringResource(R.string.church_unavailable),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MannaTheme.colors.soft,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.available.forEach { option ->
                                FilterChip(
                                    selected = option.id == selected?.id,
                                    onClick = { viewModel.selectLiturgy(option.id) },
                                    label = { Text(option.tradition) }
                                )
                            }
                        }
                    }
                }
            }

            if (selected != null) {
                item(key = "header") {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = selected.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MannaTheme.colors.ink
                        )
                        Text(
                            text = selected.tradition,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MannaTheme.colors.gold
                        )
                    }
                }

                selected.sections.forEachIndexed { sIndex, section ->
                    item(key = "section_$sIndex") { SectionHeader(section) }
                    items(
                        count = section.parts.size,
                        key = { pIndex -> "part_${sIndex}_$pIndex" }
                    ) { pIndex ->
                        PartRow(part = section.parts[pIndex], onOpenVerse = onOpenVerse)
                    }
                }

                item(key = "source") {
                    Text(
                        text = "${stringResource(R.string.church_source)}: ${selected.sourceNote}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MannaTheme.colors.muted,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(section: LiturgySection) {
    Text(
        text = section.title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MannaTheme.colors.gold,
        modifier = Modifier.padding(top = 16.dp, bottom = 2.dp)
    )
}

@Composable
private fun PartRow(part: LiturgyPart, onOpenVerse: (String) -> Unit) {
    // A bare rubric reads as a quiet stage-direction; spoken parts get a role label.
    if (part.role == LiturgyRole.RUBRIC && part.title == null) {
        Text(
            text = part.rubric.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = MannaTheme.colors.muted,
            fontStyle = FontStyle.Italic,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        return
    }

    Surface(
        color = MannaTheme.colors.card,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().then(
            if (part.osisRef != null) Modifier.clickable { onOpenVerse(part.osisRef) } else Modifier
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                roleLabelRes(part.role)?.let { res ->
                    Text(
                        text = stringResource(res),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = roleColor(part.role)
                    )
                }
                if (part.title != null) {
                    if (roleLabelRes(part.role) != null) {
                        Text(
                            text = "  ·  ",
                            style = MaterialTheme.typography.labelMedium,
                            color = MannaTheme.colors.muted
                        )
                    }
                    Text(
                        text = part.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MannaTheme.colors.soft
                    )
                }
            }
            if (part.text != null) {
                Text(
                    text = part.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MannaTheme.colors.ink,
                    lineHeight = 26.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (part.rubric != null) {
                Text(
                    text = part.rubric,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MannaTheme.colors.muted,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(top = if (part.text != null) 6.dp else 4.dp)
                )
            }
            if (part.needsOfficialText) {
                Text(
                    text = stringResource(R.string.church_needs_official),
                    style = MaterialTheme.typography.labelSmall,
                    color = MannaTheme.colors.orange,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            if (part.osisRef != null) {
                Text(
                    text = stringResource(R.string.church_open_reading),
                    style = MaterialTheme.typography.labelMedium,
                    color = MannaTheme.colors.gold,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun roleColor(role: LiturgyRole) = when (role) {
    LiturgyRole.PRESIDER -> MannaTheme.colors.gold
    LiturgyRole.PEOPLE -> MannaTheme.colors.sage
    LiturgyRole.ALL -> MannaTheme.colors.cyan
    LiturgyRole.READER -> MannaTheme.colors.lavender
    LiturgyRole.RUBRIC -> MannaTheme.colors.muted
}

private fun roleLabelRes(role: LiturgyRole): Int? = when (role) {
    LiturgyRole.PRESIDER -> R.string.church_role_presider
    LiturgyRole.PEOPLE -> R.string.church_role_people
    LiturgyRole.ALL -> R.string.church_role_all
    LiturgyRole.READER -> R.string.church_role_reader
    LiturgyRole.RUBRIC -> null
}
