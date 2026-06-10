package com.manna.bible.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Minimal placeholder for the post-setup main reading experience (Req 1.2).
 *
 * The full reader is out of scope for the first-launch gate task; this simply confirms that the
 * app routed past setup. It will be replaced by the real reader in a later task.
 */
@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "Manna", style = MaterialTheme.typography.headlineLarge)
        Text(text = "Your Bible", style = MaterialTheme.typography.bodyLarge)
    }
}
