package com.manna.bible.ui.prayers.jesus

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manna.bible.R
import com.manna.bible.domain.devotion.PrayerDepth
import com.manna.bible.ui.theme.MannaTheme
import com.manna.bible.ui.theme.ScriptureFontFamily
import com.manna.bible.ui.util.rememberBibleLanguage
import com.manna.bible.ui.util.stringResourceIn

private val MinTouchTarget = 48.dp

private fun depthTitleRes(depth: PrayerDepth): Int = when (depth) {
    PrayerDepth.VOCAL -> R.string.jesus_prayer_vocal_title
    PrayerDepth.MENTAL -> R.string.jesus_prayer_mental_title
    PrayerDepth.HEART -> R.string.jesus_prayer_heart_title
}

private fun depthGuidanceRes(depth: PrayerDepth): Int = when (depth) {
    PrayerDepth.VOCAL -> R.string.jesus_prayer_vocal_guidance
    PrayerDepth.MENTAL -> R.string.jesus_prayer_mental_guidance
    PrayerDepth.HEART -> R.string.jesus_prayer_heart_guidance
}

/**
 * The Jesus Prayer — the prayer of the heart. Shows the prayer, an optional breathing
 * guide that paces it to the breath, and the three traditional depths (vocal, mental,
 * heart), each grounded in Scripture that opens in the reader.
 *
 * @param onBack returns to the Prayers hub.
 * @param onOpenVerse opens the reader at a canonical `OSIS.CHAPTER.VERSE`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JesusPrayerScreen(
    modifier: Modifier = Modifier,
    viewModel: JesusPrayerViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onOpenVerse: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val backDescription = stringResource(R.string.prayers_back)
    val bibleLanguage = rememberBibleLanguage()
    var breathing by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.jesus_prayer_title), fontWeight = FontWeight.SemiBold)
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
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = stringResourceIn(bibleLanguage, R.string.jesus_prayer_text),
                fontFamily = ScriptureFontFamily,
                style = MaterialTheme.typography.headlineSmall,
                color = MannaTheme.colors.ink,
                lineHeight = 36.sp
            )
            Text(
                text = stringResource(R.string.jesus_prayer_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = MannaTheme.colors.soft,
                lineHeight = 26.sp
            )

            if (breathing) {
                BreathingGuide(bibleLanguage = bibleLanguage)
                TextButton(
                    onClick = { breathing = false },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) { Text(stringResource(R.string.jesus_prayer_stop), color = MannaTheme.colors.soft) }
            } else {
                Surface(
                    color = MannaTheme.colors.gold.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { breathing = true }
                ) {
                    Text(
                        text = stringResource(R.string.jesus_prayer_begin),
                        style = MaterialTheme.typography.titleMedium,
                        color = MannaTheme.colors.gold,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp)
                    )
                }
            }

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                state.stages.forEach { stage ->
                    DepthSection(stage = stage, onOpenVerse = onOpenVerse)
                }
            }
        }
    }
}

/**
 * A calm breathing circle that expands on the inhale phrase and contracts on the
 * exhale phrase, pacing the prayer to the breath over an 8-second cycle.
 */
@Composable
private fun BreathingGuide(bibleLanguage: String) {
    val transition = rememberInfiniteTransition(label = "breath")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath-progress"
    )
    val inhaling = progress >= 0.5f
    val scale = 0.7f + progress * 0.6f // 0.7..1.3

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(220.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(MannaTheme.colors.gold.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MannaTheme.colors.gold.copy(alpha = 0.28f))
                )
            }
        }
        Text(
            text = stringResource(
                if (inhaling) R.string.jesus_prayer_breath_in_label
                else R.string.jesus_prayer_breath_out_label
            ),
            style = MaterialTheme.typography.labelLarge,
            color = MannaTheme.colors.muted,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResourceIn(
                bibleLanguage,
                if (inhaling) R.string.jesus_prayer_breath_in
                else R.string.jesus_prayer_breath_out
            ),
            fontFamily = ScriptureFontFamily,
            style = MaterialTheme.typography.titleMedium,
            color = MannaTheme.colors.ink,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DepthSection(stage: PrayerDepthUi, onOpenVerse: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(depthTitleRes(stage.depth)),
            style = MaterialTheme.typography.titleMedium,
            color = MannaTheme.colors.gold,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = stringResource(depthGuidanceRes(stage.depth)),
            style = MaterialTheme.typography.bodyLarge,
            color = MannaTheme.colors.soft,
            lineHeight = 26.sp
        )
        if (stage.verseText != null && stage.reference != null) {
            Surface(
                color = MannaTheme.colors.card,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().clickable { stage.osisRef?.let(onOpenVerse) }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stage.verseText,
                        fontFamily = ScriptureFontFamily,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MannaTheme.colors.ink,
                        lineHeight = 26.sp
                    )
                    Text(
                        text = stage.reference,
                        style = MaterialTheme.typography.labelMedium,
                        color = MannaTheme.colors.gold,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
