package com.manna.bible.ui.stealth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * The disguised Stealth (Persecution) Mode lock. To anyone glancing at the device it
 * is an ordinary calculator; only the person who set the PIN knows that entering it
 * and pressing "=" reveals Manna. A wrong entry just resets the display, divulging
 * nothing — there is no error, no "wrong PIN", no hint that scripture is hidden here.
 *
 * The PIN is the digits typed before "=". It is checked offline against the stored
 * PBKDF2 credential via [StealthLockViewModel].
 *
 * @param onUnlock invoked once the correct PIN is confirmed.
 */
@Composable
fun StealthLockScreen(
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StealthLockViewModel = hiltViewModel(),
) {
    StealthCalculator(verify = viewModel::check, onUnlock = onUnlock, modifier = modifier)
}

/**
 * The stateless calculator-disguise keypad. Split out from [StealthLockScreen] so it
 * can be driven directly in UI tests (it takes a plain [verify] predicate instead of a
 * ViewModel). Entering digits and pressing "=" calls [verify]; a true result unlocks.
 */
@Composable
fun StealthCalculator(
    verify: (String) -> Boolean,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var display by remember { mutableStateOf("0") }

    fun append(digit: String) {
        display = if (display == "0") digit else (display + digit).take(MAX_DIGITS)
    }

    fun backspace() {
        display = display.dropLast(1).ifEmpty { "0" }
    }

    fun clear() {
        display = "0"
    }

    fun equals() {
        if (verify(display)) {
            onUnlock()
        } else {
            // Behave like a calculator that simply finished: reset, reveal nothing.
            display = "0"
        }
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            // Display
            Text(
                text = display,
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 24.dp),
            )

            val rows = listOf(
                listOf("7", "8", "9"),
                listOf("4", "5", "6"),
                listOf("1", "2", "3"),
            )
            rows.forEach { row ->
                CalcRow {
                    row.forEach { d -> CalcKey(d) { append(d) } }
                }
            }
            CalcRow {
                CalcKey("C") { clear() }
                CalcKey("0") { append("0") }
                CalcKey("=") { equals() }
            }
            CalcRow {
                CalcKey("⌫") { backspace() }
            }
        }
    }
}

private const val MAX_DIGITS = 12

@Composable
private fun CalcRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        content()
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.CalcKey(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = label, style = MaterialTheme.typography.headlineSmall)
        }
    }
}
