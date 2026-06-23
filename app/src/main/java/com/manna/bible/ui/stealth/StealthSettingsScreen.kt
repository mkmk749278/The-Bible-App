package com.manna.bible.ui.stealth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manna.bible.R

private val MIN_TOUCH_TARGET = 48.dp

/**
 * Settings for Stealth (Persecution) Mode. When armed, the app opens to a disguised
 * calculator lock and only reveals scripture once the PIN is entered — protecting
 * believers whose devices may be inspected.
 *
 * The PIN never leaves the device in plaintext; only its PBKDF2 hash is stored.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StealthSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StealthSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.stealth_title),
                        fontWeight = FontWeight.SemiBold,
                    )
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
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.stealth_explainer),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )

            state.message?.let { message ->
                Text(
                    text = stringResource(message.labelRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isError()) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (state.enabled) {
                EnabledControls(
                    onChangePin = viewModel::changePin,
                    onDisable = viewModel::disable,
                )
            } else {
                DisabledControls(onEnable = viewModel::enable)
            }
        }
    }
}

@Composable
private fun DisabledControls(onEnable: (String, String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    SectionTitle(R.string.stealth_set_up)
    PinField(value = pin, onValueChange = { pin = it }, labelRes = R.string.stealth_pin)
    PinField(value = confirm, onValueChange = { confirm = it }, labelRes = R.string.stealth_confirm_pin)
    Button(
        onClick = {
            onEnable(pin, confirm)
            pin = ""
            confirm = ""
        },
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = MIN_TOUCH_TARGET),
    ) {
        Text(stringResource(R.string.stealth_turn_on))
    }
}

@Composable
private fun EnabledControls(
    onChangePin: (String, String, String) -> Unit,
    onDisable: (String) -> Unit,
) {
    var current by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var disablePin by remember { mutableStateOf("") }

    SectionTitle(R.string.stealth_change_pin)
    PinField(value = current, onValueChange = { current = it }, labelRes = R.string.stealth_current_pin)
    PinField(value = newPin, onValueChange = { newPin = it }, labelRes = R.string.stealth_new_pin)
    PinField(value = confirm, onValueChange = { confirm = it }, labelRes = R.string.stealth_confirm_pin)
    OutlinedButton(
        onClick = {
            onChangePin(current, newPin, confirm)
            current = ""
            newPin = ""
            confirm = ""
        },
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = MIN_TOUCH_TARGET),
    ) {
        Text(stringResource(R.string.stealth_change_pin))
    }

    SectionTitle(R.string.stealth_turn_off)
    PinField(value = disablePin, onValueChange = { disablePin = it }, labelRes = R.string.stealth_current_pin)
    OutlinedButton(
        onClick = {
            onDisable(disablePin)
            disablePin = ""
        },
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = MIN_TOUCH_TARGET),
    ) {
        Text(stringResource(R.string.stealth_turn_off))
    }
}

@Composable
private fun SectionTitle(titleRes: Int) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun PinField(value: String, onValueChange: (String) -> Unit, labelRes: Int) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> onValueChange(input.filter(Char::isDigit).take(12)) },
        label = { Text(stringResource(labelRes)) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun StealthMessage.isError(): Boolean = when (this) {
    StealthMessage.PIN_TOO_SHORT,
    StealthMessage.PIN_MISMATCH,
    StealthMessage.WRONG_PIN -> true
    StealthMessage.ENABLED,
    StealthMessage.DISABLED,
    StealthMessage.PIN_CHANGED -> false
}

private fun StealthMessage.labelRes(): Int = when (this) {
    StealthMessage.ENABLED -> R.string.stealth_msg_enabled
    StealthMessage.DISABLED -> R.string.stealth_msg_disabled
    StealthMessage.PIN_CHANGED -> R.string.stealth_msg_pin_changed
    StealthMessage.PIN_TOO_SHORT -> R.string.stealth_msg_pin_too_short
    StealthMessage.PIN_MISMATCH -> R.string.stealth_msg_pin_mismatch
    StealthMessage.WRONG_PIN -> R.string.stealth_msg_wrong_pin
}
