package com.manna.bible.ui.setup

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manna.bible.R
import com.manna.bible.domain.lectionary.DefaultLectionaryProvider
import com.manna.bible.domain.model.Denomination
import com.manna.bible.domain.translation.Translation

/**
 * A selectable language option used by the UI / Bible language steps.
 */
private data class LanguageOption(val code: String, @StringRes val labelRes: Int)

private val SUPPORTED_LANGUAGES = listOf(
    LanguageOption("en", R.string.language_en),
    LanguageOption("ta", R.string.language_ta),
    LanguageOption("hi", R.string.language_hi),
    LanguageOption("te", R.string.language_te),
    LanguageOption("ml", R.string.language_ml),
)

/** All denominations shown on the denomination step, including the "show everything" escape hatch. */
private val DENOMINATION_OPTIONS = Denomination.entries.toList()

@StringRes
private fun Denomination.labelRes(): Int = when (this) {
    Denomination.CATHOLIC -> R.string.denomination_catholic
    Denomination.CSI -> R.string.denomination_csi
    Denomination.PROTESTANT_OTHER -> R.string.denomination_protestant_other
    Denomination.ORTHODOX -> R.string.denomination_orthodox
    Denomination.MAR_THOMA -> R.string.denomination_mar_thoma
    Denomination.SHOW_EVERYTHING -> R.string.denomination_show_everything
}

@StringRes
private fun Denomination.descriptionRes(): Int = when (this) {
    Denomination.CATHOLIC -> R.string.denomination_catholic_desc
    Denomination.CSI -> R.string.denomination_csi_desc
    Denomination.PROTESTANT_OTHER -> R.string.denomination_protestant_other_desc
    Denomination.ORTHODOX -> R.string.denomination_orthodox_desc
    Denomination.MAR_THOMA -> R.string.denomination_mar_thoma_desc
    Denomination.SHOW_EVERYTHING -> R.string.denomination_show_everything_desc
}

@StringRes
private fun languageLabelRes(code: String?): Int? =
    SUPPORTED_LANGUAGES.firstOrNull { it.code == code }?.labelRes

/**
 * Hosts the denomination-aware first-launch setup flow as a single-screen step
 * container driven by [SetupViewModel] (Requirements 1, 2, 3, 4, 5, 9, 16).
 *
 * Renders the current [SetupStep] inside a [Scaffold] with a persistent bottom bar
 * offering Back, Skip (Requirement 2), and Continue/Finish. Invokes [onSetupComplete]
 * once the view model reports the flow as completed.
 */
@Composable
fun SetupHost(
    onSetupComplete: () -> Unit,
    simplifiedMode: Boolean = false,
    viewModel: SetupViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.completed) {
        if (state.completed) {
            onSetupComplete()
        }
    }

    Scaffold(
        bottomBar = {
            SetupBottomBar(
                state = state,
                simplifiedMode = simplifiedMode,
                onBack = viewModel::back,
                onSkip = viewModel::skip,
                onContinue = viewModel::next,
                onFinish = viewModel::complete,
            )
        },
    ) { innerPadding ->
        // Layout relies on start/end-aware horizontal padding and Arrangement, so the
        // framework mirrors the flow automatically for RTL UI languages (Requirement 14.4).
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            state.errorMessage?.let { message ->
                ErrorBanner(message)
                Spacer(Modifier.size(12.dp))
            }

            when (state.step) {
                SetupStep.WELCOME -> WelcomeStep()
                SetupStep.DENOMINATION -> DenominationStep(
                    selected = state.denomination,
                    onSelect = viewModel::selectDenomination,
                    simplifiedMode = simplifiedMode,
                )
                SetupStep.UI_LANGUAGE -> UiLanguageStep(
                    selected = state.uiLanguage,
                    onSelect = viewModel::selectUiLanguage,
                    simplifiedMode = simplifiedMode,
                )
                SetupStep.BIBLE_LANGUAGE -> BibleLanguageStep(
                    selected = state.bibleLanguage,
                    onSelect = viewModel::selectBibleLanguage,
                    simplifiedMode = simplifiedMode,
                )
                SetupStep.TRANSLATION -> TranslationStep(
                    translations = state.availableTranslations,
                    selectedId = state.bibleTranslationId,
                    onSelect = viewModel::selectTranslation,
                    simplifiedMode = simplifiedMode,
                )
                SetupStep.LECTIONARY -> LectionaryStep(lectionaryId = state.lectionaryId)
                SetupStep.SUMMARY -> SummaryStep(state = state)
            }

            Spacer(Modifier.size(24.dp))
        }
    }
}

/**
 * Minimum interactive touch target. Requirement 14.1 mandates 48dp; Requirement 14.2
 * raises this to 56dp while Simplified Mode (audio-first / elder mode) is active.
 */
private fun minTouchTarget(simplifiedMode: Boolean) = if (simplifiedMode) 56.dp else 48.dp

@Composable
private fun SetupBottomBar(
    state: SetupUiState,
    simplifiedMode: Boolean,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onContinue: () -> Unit,
    onFinish: () -> Unit,
) {
    val target = minTouchTarget(simplifiedMode)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (state.step != SetupStep.WELCOME) {
            TextButton(
                onClick = onBack,
                enabled = !state.isSaving,
                modifier = Modifier.defaultMinSize(minHeight = target),
            ) {
                Text(stringResource(R.string.setup_back))
            }
        }

        // Skip is available on every step (Requirement 2).
        TextButton(
            onClick = onSkip,
            enabled = !state.isSaving,
            modifier = Modifier.defaultMinSize(minHeight = target),
        ) {
            Text(stringResource(R.string.setup_skip))
        }

        Spacer(Modifier.weight(1f))

        if (state.isSaving) {
            val savingDescription = stringResource(R.string.a11y_saving)
            CircularProgressIndicator(
                modifier = Modifier
                    .size(24.dp)
                    .semantics { contentDescription = savingDescription },
            )
        } else if (state.isLastStep) {
            Button(
                onClick = onFinish,
                modifier = Modifier.defaultMinSize(minHeight = target),
            ) {
                Text(stringResource(R.string.setup_finish))
            }
        } else {
            Button(
                onClick = onContinue,
                enabled = state.canContinue,
                modifier = Modifier.defaultMinSize(minHeight = target),
            ) {
                Text(stringResource(R.string.setup_continue))
            }
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun StepHeader(@StringRes titleRes: Int, @StringRes subtitleRes: Int? = null) {
    Spacer(Modifier.size(24.dp))
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
    )
    if (subtitleRes != null) {
        Spacer(Modifier.size(8.dp))
        Text(
            text = stringResource(subtitleRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Spacer(Modifier.size(16.dp))
}

@Composable
private fun WelcomeStep() {
    StepHeader(
        titleRes = R.string.setup_welcome_title,
        subtitleRes = R.string.setup_welcome_subtitle,
    )
}

@Composable
private fun DenominationStep(
    selected: Denomination?,
    onSelect: (Denomination) -> Unit,
    simplifiedMode: Boolean,
) {
    StepHeader(
        titleRes = R.string.setup_denomination_title,
        subtitleRes = R.string.setup_denomination_subtitle,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DENOMINATION_OPTIONS.forEach { denomination ->
            SelectableRow(
                selected = selected == denomination,
                onClick = { onSelect(denomination) },
                title = stringResource(denomination.labelRes()),
                description = stringResource(denomination.descriptionRes()),
                simplifiedMode = simplifiedMode,
            )
        }
    }
}

@Composable
private fun UiLanguageStep(
    selected: String?,
    onSelect: (String) -> Unit,
    simplifiedMode: Boolean,
) {
    StepHeader(
        titleRes = R.string.setup_ui_language_title,
        subtitleRes = R.string.setup_ui_language_subtitle,
    )
    LanguageList(selected = selected, onSelect = onSelect, simplifiedMode = simplifiedMode)
}

@Composable
private fun BibleLanguageStep(
    selected: String?,
    onSelect: (String) -> Unit,
    simplifiedMode: Boolean,
) {
    StepHeader(
        titleRes = R.string.setup_bible_language_title,
        subtitleRes = R.string.setup_bible_language_subtitle,
    )
    LanguageList(selected = selected, onSelect = onSelect, simplifiedMode = simplifiedMode)
}

@Composable
private fun LanguageList(
    selected: String?,
    onSelect: (String) -> Unit,
    simplifiedMode: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SUPPORTED_LANGUAGES.forEach { option ->
            SelectableRow(
                selected = selected == option.code,
                onClick = { onSelect(option.code) },
                title = stringResource(option.labelRes),
                simplifiedMode = simplifiedMode,
            )
        }
    }
}

@Composable
private fun TranslationStep(
    translations: List<Translation>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    simplifiedMode: Boolean,
) {
    StepHeader(
        titleRes = R.string.setup_translation_title,
        subtitleRes = R.string.setup_translation_subtitle,
    )
    if (translations.isEmpty()) {
        Text(
            text = stringResource(R.string.setup_translation_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            translations.forEach { translation ->
                SelectableRow(
                    selected = selectedId == translation.id,
                    onClick = { onSelect(translation.id) },
                    title = translation.name,
                    description = translation.languageCode,
                    simplifiedMode = simplifiedMode,
                )
            }
        }
    }
}

@Composable
private fun LectionaryStep(lectionaryId: String?) {
    StepHeader(
        titleRes = R.string.setup_lectionary_title,
        subtitleRes = R.string.setup_lectionary_subtitle,
    )
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(lectionaryLabelRes(lectionaryId)),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(16.dp),
        )
    }
}

/** Maps a lectionary id to its human display name (never the raw id). */
@androidx.annotation.StringRes
private fun lectionaryLabelRes(id: String?): Int = when (id) {
    DefaultLectionaryProvider.CSI_ALMANAC -> R.string.lectionary_csi
    DefaultLectionaryProvider.RC_CALENDAR -> R.string.lectionary_rc
    DefaultLectionaryProvider.ORTHODOX_CALENDAR -> R.string.lectionary_orthodox
    DefaultLectionaryProvider.GENERAL_LECTIONARY -> R.string.lectionary_general
    else -> R.string.setup_lectionary_none
}

@Composable
private fun SummaryStep(state: SetupUiState) {
    StepHeader(
        titleRes = R.string.setup_summary_title,
        subtitleRes = R.string.setup_summary_subtitle,
    )

    val notSelected = stringResource(R.string.setup_summary_none_selected)

    val denominationLabel = state.denomination?.let { stringResource(it.labelRes()) } ?: notSelected
    val uiLanguageLabel = languageLabelRes(state.uiLanguage)?.let { stringResource(it) }
        ?: state.uiLanguage ?: notSelected
    val bibleLanguageLabel = languageLabelRes(state.bibleLanguage)?.let { stringResource(it) }
        ?: state.bibleLanguage ?: notSelected
    val translationLabel = state.availableTranslations
        .firstOrNull { it.id == state.bibleTranslationId }?.name
        ?: state.bibleTranslationId ?: notSelected
    val lectionaryLabel = stringResource(lectionaryLabelRes(state.lectionaryId))

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SummaryRow(stringResource(R.string.setup_summary_denomination), denominationLabel)
        SummaryRow(stringResource(R.string.setup_summary_ui_language), uiLanguageLabel)
        SummaryRow(stringResource(R.string.setup_summary_bible_language), bibleLanguageLabel)
        SummaryRow(stringResource(R.string.setup_summary_translation), translationLabel)
        SummaryRow(stringResource(R.string.setup_summary_lectionary), lectionaryLabel)
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun SelectableRow(
    selected: Boolean,
    onClick: () -> Unit,
    title: String,
    description: String? = null,
    simplifiedMode: Boolean = false,
) {
    val target = minTouchTarget(simplifiedMode)
    val stateDescriptionText = stringResource(
        if (selected) R.string.a11y_selected else R.string.a11y_not_selected,
    )
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = target)
                .selectable(
                    selected = selected,
                    onClick = onClick,
                    role = Role.RadioButton,
                )
                // Merge the row + texts into a single TalkBack node and override its
                // announcement with the title and selection state (Requirement 14.3).
                .semantics(mergeDescendants = true) {
                    contentDescription = title
                    stateDescription = stateDescriptionText
                    this.selected = selected
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Decorative: selection semantics live on the row, so the radio takes no click.
            RadioButton(selected = selected, onClick = null)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
