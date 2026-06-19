package com.manna.bible.ui.reminder

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manna.bible.R
import com.manna.bible.ui.theme.MannaTheme

private val MinTouchTarget = 48.dp

/**
 * Settings for the daily verse reminder: an on/off switch and a time picker.
 *
 * Enabling the reminder requests the notification permission on Android 13+; the
 * scheduling itself is handled by `ReminderCoordinator` observing the saved prefs.
 *
 * @param onBack returns to the previous surface.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: ReminderSettingsViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val backDescription = stringResource(R.string.reminder_back)

    // On Android 13+ the reminder is only useful with the POST_NOTIFICATIONS grant.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result handled by the OS; the toggle stays on regardless */ }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.reminder_title), fontWeight = FontWeight.SemiBold)
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.reminder_enable_label),
                    style = MaterialTheme.typography.titleMedium,
                    color = MannaTheme.colors.ink
                )
                Switch(
                    checked = state.enabled,
                    onCheckedChange = { checked ->
                        if (checked &&
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                            PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        viewModel.setEnabled(checked)
                    }
                )
            }

            Text(
                text = stringResource(R.string.reminder_times_label),
                style = MaterialTheme.typography.titleMedium,
                color = MannaTheme.colors.ink
            )

            val removeDescription = stringResource(R.string.reminder_remove_time)
            state.times.forEach { time ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = time.format(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (state.enabled) MannaTheme.colors.gold else MannaTheme.colors.muted,
                        fontWeight = FontWeight.Bold
                    )
                    if (state.times.size > 1) {
                        IconButton(
                            onClick = { viewModel.removeTime(time) },
                            modifier = Modifier
                                .size(MinTouchTarget)
                                .semantics { contentDescription = removeDescription }
                        ) {
                            Text(text = "✕", fontSize = 18.sp, color = MannaTheme.colors.soft)
                        }
                    }
                }
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, hour, minute -> viewModel.addTime(hour, minute) },
                        7,
                        0,
                        false
                    ).show()
                }
            ) {
                Text(stringResource(R.string.reminder_add_time))
            }

            Text(
                text = stringResource(R.string.reminder_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MannaTheme.colors.soft
            )
        }
    }
}
