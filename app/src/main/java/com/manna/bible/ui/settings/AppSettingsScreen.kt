package com.manna.bible.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manna.bible.R
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.FeatureFlags
import com.manna.bible.domain.audio.TtsReader
import com.manna.bible.ui.theme.MannaTheme
import kotlin.math.roundToInt

private val MIN_TOUCH_TARGET = 48.dp

/**
 * The consolidated Settings screen — the single, calm home for the controls that used
 * to be scattered or unreachable: theme (light / dark / system), global reading text
 * size, read-aloud speed, Simplified (Elder) Mode, and links to the Denomination &
 * Languages settings and Stealth (Persecution) Mode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    onBack: () -> Unit,
    onOpenDenomination: () -> Unit,
    onOpenStealth: (() -> Unit)?,
    modifier: Modifier = Modifier,
    viewModel: AppSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.settings_title), fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // --- Appearance ---------------------------------------------------
            SectionHeader(R.string.settings_section_appearance)
            ThemeChoice.entries.forEach { choice ->
                SelectableRow(
                    selected = state.theme == choice,
                    label = stringResource(choice.labelRes()),
                    onClick = { viewModel.setTheme(choice) },
                )
            }

            // --- Reading text size --------------------------------------------
            SectionHeader(R.string.settings_section_text_size)
            SliderRow(
                value = state.textScale,
                valueRange = PreferencesStore.MIN_TEXT_SCALE..PreferencesStore.MAX_TEXT_SCALE,
                valueLabel = "${(state.textScale * 100).roundToInt()}%",
                onValueChange = viewModel::setTextScale,
            )
            // Live preview: the whole app (this screen included) is wrapped in
            // ReadingTextScale, so this sample grows and shrinks as the slider moves.
            Text(
                text = stringResource(R.string.settings_text_size_preview),
                style = MaterialTheme.typography.bodyLarge,
                color = MannaTheme.colors.ink,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )

            // --- Audio --------------------------------------------------------
            SectionHeader(R.string.settings_section_audio)
            SliderRow(
                value = state.ttsSpeed,
                valueRange = TtsReader.MIN_SPEED..TtsReader.MAX_SPEED,
                valueLabel = "${formatSpeed(state.ttsSpeed)}×",
                onValueChange = viewModel::setTtsSpeed,
            )

            // --- Accessibility ------------------------------------------------
            SectionHeader(R.string.settings_section_accessibility)
            ToggleRow(
                title = stringResource(R.string.more_simplified_mode),
                description = stringResource(R.string.more_simplified_mode_desc),
                checked = state.simplifiedMode,
                onToggle = viewModel::setSimplifiedMode,
            )

            // --- Reading & language -------------------------------------------
            SectionHeader(R.string.settings_section_reading)
            LinkRow(
                label = stringResource(R.string.settings_denomination_entry),
                onClick = onOpenDenomination,
            )

            // --- Privacy ------------------------------------------------------
            if (onOpenStealth != null && FeatureFlags.STEALTH_MODE) {
                SectionHeader(R.string.settings_section_privacy)
                LinkRow(
                    label = stringResource(R.string.stealth_title),
                    onClick = onOpenStealth,
                )
            }

            Column(Modifier.padding(bottom = 24.dp)) {}
        }
    }
}

@StringRes
private fun ThemeChoice.labelRes(): Int = when (this) {
    ThemeChoice.SYSTEM -> R.string.settings_theme_system
    ThemeChoice.LIGHT -> R.string.settings_theme_light
    ThemeChoice.DARK -> R.string.settings_theme_dark
}

/** Renders a speed like 1.0, 1.5 with at most one decimal, dropping a trailing .0. */
private fun formatSpeed(speed: Float): String {
    val rounded = (speed * 10).roundToInt() / 10f
    return if (rounded % 1f == 0f) rounded.toInt().toString() else rounded.toString()
}

@Composable
private fun SectionHeader(@StringRes titleRes: Int) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleSmall,
        color = MannaTheme.colors.gold,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp, bottom = 2.dp),
    )
}

@Composable
private fun SelectableRow(selected: Boolean, label: String, onClick: () -> Unit) {
    Surface(
        color = MannaTheme.colors.card,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = MIN_TOUCH_TARGET)
                .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RadioButton(selected = selected, onClick = null)
            Text(text = label, style = MaterialTheme.typography.bodyLarge, color = MannaTheme.colors.ink)
        }
    }
}

@Composable
private fun SliderRow(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onValueChange: (Float) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MannaTheme.colors.soft,
            )
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange)
    }
}

@Composable
private fun ToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Surface(
        color = MannaTheme.colors.card,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clickable { onToggle(!checked) },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MannaTheme.colors.ink,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MannaTheme.colors.soft,
                )
            }
            Switch(checked = checked, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun LinkRow(label: String, onClick: () -> Unit) {
    Surface(
        color = MannaTheme.colors.card,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = MIN_TOUCH_TARGET)
            .clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MannaTheme.colors.ink,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
        )
    }
}
