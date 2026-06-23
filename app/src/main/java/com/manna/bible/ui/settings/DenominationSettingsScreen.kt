package com.manna.bible.ui.settings

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manna.bible.R
import com.manna.bible.domain.model.Denomination

/** A selectable language option used by the UI / Bible language lists. */
private data class LanguageOption(val code: String, @StringRes val labelRes: Int)

private val SUPPORTED_LANGUAGES = listOf(
    LanguageOption("en", R.string.language_en),
    LanguageOption("ta", R.string.language_ta),
    LanguageOption("hi", R.string.language_hi),
    LanguageOption("te", R.string.language_te),
    LanguageOption("ml", R.string.language_ml),
)

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

/**
 * Settings section that lets the user change their denomination, UI/Bible language, and the
 * Protestant deuterocanonical toggle after initial setup, and re-run the full setup flow
 * (Requirements 11, 12, 13, 15, 16).
 *
 * Selecting a different denomination routes through the view model, which may surface a
 * canon-switch warning dialog (Req 12) before applying the change. Annotations are never
 * deleted; books outside the active canon are simply hidden.
 *
 * @param onReRunSetup invoked when the user chooses to re-run the setup flow (Req 11.1).
 * @param onBack invoked when the user dismisses the screen; null hides the back affordance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DenominationSettingsScreen(
    viewModel: DenominationSettingsViewModel = hiltViewModel(),
    onReRunSetup: () -> Unit = {},
    onBack: (() -> Unit)? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_denomination_section_title)) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Spacer(Modifier.size(16.dp))

        state.errorMessage?.let { message ->
            ErrorBanner(message)
        }

        // Denomination selection (Req 11.2, 16.3).
        SubHeader(R.string.settings_denomination_heading)
        DENOMINATION_OPTIONS.forEach { denomination ->
            SelectableRow(
                selected = state.denomination == denomination,
                onClick = { viewModel.requestDenominationChange(denomination) },
                title = stringResource(denomination.labelRes()),
                description = stringResource(denomination.descriptionRes()),
            )
        }

        // UI language (Req 13).
        SubHeader(R.string.settings_ui_language_heading)
        SUPPORTED_LANGUAGES.forEach { option ->
            SelectableRow(
                selected = state.uiLanguage == option.code,
                onClick = { viewModel.changeUiLanguage(option.code) },
                title = stringResource(option.labelRes),
            )
        }

        // Bible language (Req 13).
        SubHeader(R.string.settings_bible_language_heading)
        SUPPORTED_LANGUAGES.forEach { option ->
            SelectableRow(
                selected = state.bibleLanguage == option.code,
                onClick = { viewModel.changeBibleLanguage(option.code) },
                title = stringResource(option.labelRes),
            )
        }

        // Deuterocanonical toggle, Protestant canon only (Req 15.1).
        if (state.showDeuterocanonicalToggle) {
            SubHeader(R.string.settings_deuterocanonical_heading)
            DeuterocanonicalToggle(
                checked = state.showDeuterocanonical,
                onCheckedChange = viewModel::setShowDeuterocanonical,
            )
        }

        Spacer(Modifier.size(16.dp))
        // Re-run setup (Req 11.1).
        OutlinedButton(
            onClick = onReRunSetup,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = MIN_TOUCH_TARGET),
        ) {
            Text(stringResource(R.string.settings_rerun_setup))
        }
        Spacer(Modifier.size(24.dp))
    }

    if (state.showCanonSwitchDialog) {
        CanonSwitchWarningDialog(
            excludedBookIds = state.pendingImpact?.excludedBookIds.orEmpty(),
            onConfirm = viewModel::confirmDenominationChange,
            onCancel = viewModel::cancelDenominationChange,
        )
    }
    }
}

private val MIN_TOUCH_TARGET = 48.dp

@Composable
private fun SubHeader(@StringRes titleRes: Int) {
    Spacer(Modifier.size(8.dp))
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
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

/**
 * Warns that switching canon will hide (never delete) annotations on books excluded by the
 * new canon, listing the affected books (Req 12.1).
 */
@Composable
private fun CanonSwitchWarningDialog(
    excludedBookIds: Set<String>,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val bookList = excludedBookIds.sorted().joinToString(", ")
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.settings_canon_switch_title)) },
        text = {
            Text(stringResource(R.string.settings_canon_switch_message, bookList))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.settings_canon_switch_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.settings_canon_switch_cancel))
            }
        },
    )
}

@Composable
private fun DeuterocanonicalToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val title = stringResource(R.string.settings_deuterocanonical_toggle)
    val stateDescriptionText = stringResource(
        if (checked) R.string.a11y_on else R.string.a11y_off,
    )
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = MIN_TOUCH_TARGET)
                .selectable(
                    selected = checked,
                    onClick = { onCheckedChange(!checked) },
                    role = Role.Switch,
                )
                .semantics(mergeDescendants = true) {
                    contentDescription = title
                    stateDescription = stateDescriptionText
                    selected = checked
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            // Decorative: the toggle semantics live on the row, so the switch takes no click.
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

@Composable
private fun SelectableRow(
    selected: Boolean,
    onClick: () -> Unit,
    title: String,
    description: String? = null,
) {
    val stateDescriptionText = stringResource(
        if (selected) R.string.a11y_selected else R.string.a11y_not_selected,
    )
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = MIN_TOUCH_TARGET)
                .selectable(
                    selected = selected,
                    onClick = onClick,
                    role = Role.RadioButton,
                )
                .semantics(mergeDescendants = true) {
                    contentDescription = title
                    stateDescription = stateDescriptionText
                    this.selected = selected
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
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
