package com.manna.bible.ui.pastor

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import com.manna.bible.domain.pastor.SermonStep
import com.manna.bible.ui.theme.MannaTheme

private val MinTouchTarget = 48.dp

/** Step-title string resource for each [SermonStep]. */
@StringRes
private fun SermonStep.titleRes(): Int = when (this) {
    SermonStep.PASSAGE -> R.string.pastor_step_passage_title
    SermonStep.OBSERVE -> R.string.pastor_step_observe_title
    SermonStep.OUTLINE -> R.string.pastor_step_outline_title
    SermonStep.ILLUSTRATE -> R.string.pastor_step_illustrate_title
    SermonStep.APPLY -> R.string.pastor_step_apply_title
}

/** Per-step helper prompt / text-field placeholder for each [SermonStep]. */
@StringRes
private fun SermonStep.promptRes(): Int = when (this) {
    SermonStep.PASSAGE -> R.string.pastor_step_passage_prompt
    SermonStep.OBSERVE -> R.string.pastor_step_observe_prompt
    SermonStep.OUTLINE -> R.string.pastor_step_outline_prompt
    SermonStep.ILLUSTRATE -> R.string.pastor_step_illustrate_prompt
    SermonStep.APPLY -> R.string.pastor_step_apply_prompt
}

/**
 * Pastor Mode — a calm, 5-step guided sermon-preparation flow. The pastor moves
 * through PASSAGE → OBSERVE → OUTLINE → ILLUSTRATE → APPLY, jotting notes at each
 * step. Notes are held in memory by the ViewModel for the duration of the flow.
 *
 * @param onBack returns to the previous surface; also invoked when the pastor taps
 *   "Done" on the final step.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastorModeScreen(
    modifier: Modifier = Modifier,
    viewModel: PastorModeViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val backDescription = stringResource(R.string.pastor_back)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.pastor_title), fontWeight = FontWeight.SemiBold)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Step indicator: "Step 2 of 5" + the step title.
            Text(
                text = stringResource(
                    R.string.pastor_step_indicator,
                    state.stepNumber,
                    state.totalSteps
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MannaTheme.colors.gold,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(state.currentStep.titleRes()),
                style = MaterialTheme.typography.headlineSmall,
                color = MannaTheme.colors.ink,
                fontWeight = FontWeight.SemiBold
            )

            // Per-step note field with a guiding prompt as placeholder.
            OutlinedTextField(
                value = state.currentNote,
                onValueChange = { viewModel.updateNote(state.currentStep, it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .heightIn(min = 160.dp),
                placeholder = {
                    Text(
                        text = stringResource(state.currentStep.promptRes()),
                        color = MannaTheme.colors.soft
                    )
                }
            )

            Spacer(modifier = Modifier.size(4.dp))

            // Previous / Next (Next becomes "Done" on the final step).
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.previous() },
                    enabled = !state.isFirstStep,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = MinTouchTarget)
                ) {
                    Text(stringResource(R.string.pastor_previous))
                }
                Button(
                    onClick = { if (state.isLastStep) onBack() else viewModel.next() },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = MinTouchTarget)
                ) {
                    Text(
                        stringResource(
                            if (state.isLastStep) R.string.pastor_done else R.string.pastor_next
                        )
                    )
                }
            }
        }
    }
}
