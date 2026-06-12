package com.manna.bible.ui.card

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
 * Scripture Card Generator — turns the day's verse into a shareable image
 * (WhatsApp-ready). The preview shown here *is* the image that gets shared.
 *
 * @param onBack returns to the previous surface.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptureCardScreen(
    modifier: Modifier = Modifier,
    viewModel: ScriptureCardViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val backDescription = stringResource(R.string.card_back)

    var theme by remember { mutableStateOf(CardTheme.ALL.first()) }

    // Render the exact bitmap that will be shared, recomputed on text/theme change.
    val verseText = state.verseText
    val bitmap = remember(verseText, state.reference, theme) {
        if (verseText.isNullOrBlank()) null
        else ScriptureCardRenderer.render(verseText, state.reference, theme)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.card_title), fontWeight = FontWeight.SemiBold) },
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
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                state.isLoading -> CircularProgressIndicator()

                bitmap == null -> Text(
                    text = stringResource(R.string.card_unavailable),
                    color = MannaTheme.colors.soft
                )

                else -> {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.card_preview_description),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CardTheme.ALL.forEach { option ->
                            val selected = option.id == theme.id
                            Column(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(option.background))
                                    .border(
                                        width = if (selected) 3.dp else 1.dp,
                                        color = if (selected) MannaTheme.colors.gold else MannaTheme.colors.border,
                                        shape = CircleShape
                                    )
                                    .clickable { theme = option }
                            ) {}
                        }
                    }

                    Button(
                        onClick = { CardSharer.share(context, bitmap) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.card_share)) }
                }
            }
        }
    }
}
