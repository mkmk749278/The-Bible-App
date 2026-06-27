package com.manna.bible.ui.crisis

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manna.bible.R
import com.manna.bible.domain.FeatureFlags
import com.manna.bible.domain.crisis.CrisisAiResult
import com.manna.bible.domain.crisis.PersecutionCategory
import com.manna.bible.ui.theme.MannaTheme
import com.manna.bible.ui.theme.ScriptureFontFamily
import com.manna.bible.ui.util.rememberBibleLanguage
import com.manna.bible.ui.util.stringResourceIn

private val MinTouchTarget = 48.dp

/**
 * 3AM / Crisis Mode — a calm, warm companion for the hardest moments (blueprint
 * P2-1). A gentle reassurance, an immediate "just listen" path, and curated
 * comforting scripture. No streaks, no plans, no guilt.
 *
 * @param onBack returns to the previous surface.
 * @param onListen opens the reader at a comforting passage with audio auto-playing.
 * @param onOpenVerse opens the reader at a canonical `OSIS.CHAPTER.VERSE`.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CrisisModeScreen(
    modifier: Modifier = Modifier,
    viewModel: CrisisModeViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onListen: (String) -> Unit = {},
    onOpenVerse: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val bibleLanguage = rememberBibleLanguage()
    val backDescription = stringResourceIn(bibleLanguage, R.string.crisis_back)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResourceIn(bibleLanguage, R.string.crisis_title), fontWeight = FontWeight.SemiBold) },
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
        if (state.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 24.dp,
                vertical = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResourceIn(bibleLanguage, R.string.crisis_heading),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MannaTheme.colors.ink,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResourceIn(bibleLanguage, R.string.crisis_reassurance),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MannaTheme.colors.soft
                    )
                }
            }

            state.listenRef?.let { ref ->
                item {
                    Button(
                        onClick = { onListen(ref) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                    ) {
                        Text(stringResourceIn(bibleLanguage, R.string.crisis_listen))
                    }
                }
            }

            if (FeatureFlags.CRISIS_AI_COMPANION) {
                item {
                    CrisisAiSection(
                        situationText = state.situationText,
                        aiResponse = state.aiResponse,
                        isAiLoading = state.isAiLoading,
                        aiConfigured = state.aiConfigured,
                        onSituationChange = viewModel::updateSituation,
                        onSubmit = viewModel::submitSituation,
                        onOpenVerse = onOpenVerse
                    )
                }
            }

            if (FeatureFlags.PERSECUTION_COMFORT) {
                item {
                    PersecutionSection(
                        selected = state.selectedPersecutionCategory,
                        onSelect = viewModel::selectPersecutionCategory
                    )
                }
                items(items = state.persecutionVerses, key = { "persecution_${it.osisRef}" }) { verse ->
                    ComfortCard(verse = verse, onClick = { onOpenVerse(verse.osisRef) })
                }
            }

            items(items = state.comfortVerses, key = { it.osisRef }) { verse ->
                ComfortCard(verse = verse, onClick = { onOpenVerse(verse.osisRef) })
            }

            item {
                Surface(
                    color = MannaTheme.colors.card,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResourceIn(bibleLanguage, R.string.crisis_prayer_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MannaTheme.colors.gold,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResourceIn(bibleLanguage, R.string.crisis_prayer_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MannaTheme.colors.soft
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CrisisAiSection(
    situationText: String,
    aiResponse: CrisisAiResult?,
    isAiLoading: Boolean,
    aiConfigured: Boolean,
    onSituationChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onOpenVerse: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val bibleLanguage = rememberBibleLanguage()
        if (aiConfigured) {
            val submitDescription = stringResourceIn(bibleLanguage, R.string.crisis_ai_submit)
            OutlinedTextField(
                value = situationText,
                onValueChange = onSituationChange,
                placeholder = { Text(stringResourceIn(bibleLanguage, R.string.crisis_ai_placeholder)) },
                trailingIcon = {
                    if (situationText.isNotBlank() && !isAiLoading) {
                        IconButton(
                            onClick = onSubmit,
                            modifier = Modifier.semantics { contentDescription = submitDescription }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null
                            )
                        }
                    }
                },
                enabled = !isAiLoading,
                modifier = Modifier.fillMaxWidth()
            )

            if (isAiLoading) {
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(
                        text = stringResourceIn(bibleLanguage, R.string.crisis_ai_thinking),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MannaTheme.colors.soft
                    )
                }
            }

            when (val response = aiResponse) {
                is CrisisAiResult.Success -> CrisisAiResponseCard(
                    response = response,
                    onClick = { onOpenVerse(response.osisRef) }
                )
                CrisisAiResult.Offline -> Text(
                    text = stringResourceIn(bibleLanguage, R.string.crisis_ai_offline_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MannaTheme.colors.muted
                )
                is CrisisAiResult.Unavailable -> Unit
                null -> Unit
            }
        } else {
            Text(
                text = stringResourceIn(bibleLanguage, R.string.crisis_ai_offline_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MannaTheme.colors.muted
            )
        }
    }
}

@Composable
private fun CrisisAiResponseCard(response: CrisisAiResult.Success, onClick: () -> Unit) {
    val description = stringResourceIn(
        rememberBibleLanguage(),
        R.string.a11y_open_in_reader,
        response.passageRef
    )
    Surface(
        color = MannaTheme.colors.card,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { contentDescription = description }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = response.explanation,
                fontFamily = ScriptureFontFamily,
                style = MaterialTheme.typography.titleMedium,
                color = MannaTheme.colors.ink,
                lineHeight = 28.sp
            )
            Text(
                text = response.passageRef,
                style = MaterialTheme.typography.labelLarge,
                color = MannaTheme.colors.gold,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PersecutionSection(
    selected: PersecutionCategory?,
    onSelect: (PersecutionCategory) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val bibleLanguage = rememberBibleLanguage()
        Text(
            text = stringResourceIn(bibleLanguage, R.string.crisis_persecution_heading),
            style = MaterialTheme.typography.titleMedium,
            color = MannaTheme.colors.ink,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = stringResourceIn(bibleLanguage, R.string.crisis_persecution_subheading),
            style = MaterialTheme.typography.bodyMedium,
            color = MannaTheme.colors.soft
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PersecutionCategory.entries.forEach { category ->
                val label = persecutionLabel(category)
                FilterChip(
                    selected = selected == category,
                    onClick = { onSelect(category) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(),
                    modifier = Modifier
                        .heightIn(min = MinTouchTarget)
                        .semantics { contentDescription = label }
                )
            }
        }
    }
}

@Composable
private fun persecutionLabel(category: PersecutionCategory): String = stringResourceIn(
    rememberBibleLanguage(),
    when (category) {
        PersecutionCategory.FAMILY_REJECTION -> R.string.crisis_persecution_family
        PersecutionCategory.JOB_LIVELIHOOD -> R.string.crisis_persecution_livelihood
        PersecutionCategory.PHYSICAL_DANGER -> R.string.crisis_persecution_danger
        PersecutionCategory.SOCIAL_EXCLUSION -> R.string.crisis_persecution_exclusion
        PersecutionCategory.FAITH_CRISIS -> R.string.crisis_persecution_faith
    }
)

@Composable
private fun ComfortCard(verse: ComfortVerse, onClick: () -> Unit) {
    val description = stringResourceIn(
        rememberBibleLanguage(),
        R.string.a11y_open_in_reader,
        verse.reference
    )
    Surface(
        color = MannaTheme.colors.card,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { contentDescription = description }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = verse.text,
                fontFamily = ScriptureFontFamily,
                style = MaterialTheme.typography.titleMedium,
                color = MannaTheme.colors.ink,
                lineHeight = 28.sp
            )
            Text(
                text = verse.reference,
                style = MaterialTheme.typography.labelLarge,
                color = MannaTheme.colors.gold,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
