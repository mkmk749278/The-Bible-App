package com.manna.bible.ui.card

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.manna.bible.R
import com.manna.bible.domain.FeatureFlags
import com.manna.bible.ui.theme.MannaTheme
import com.manna.bible.ui.util.rememberBibleLanguage
import com.manna.bible.ui.util.stringResourceIn

/**
 * A bottom sheet that turns a single verse into a shareable image card — the preview
 * shown *is* the image that gets shared (WhatsApp-ready, 1:1). Reuses
 * [ScriptureCardRenderer] for the bitmap and [CardSharer] for the share intent, so any
 * surface with a verse (the calendar, the reader, …) can offer the same card.
 *
 * @param verseText the verse text to render.
 * @param reference the human reference (e.g. "John 3:16").
 * @param onDismiss called when the sheet is dismissed.
 * @param onFindVerse optional entry point into the Context-Aware Verse Cards flow (F-05);
 *   when provided and the feature flag is on, a "Find a verse to share" action is shown.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerseCardSheet(
    verseText: String,
    reference: String,
    onDismiss: () -> Unit,
    onFindVerse: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val bibleLanguage = rememberBibleLanguage()
    var theme by remember { mutableStateOf(CardTheme.ALL.first()) }

    // Render the exact bitmap that will be shared, recomputed on theme change.
    val bitmap = remember(verseText, reference, theme) {
        ScriptureCardRenderer.render(verseText, reference, theme)
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MannaTheme.colors.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResourceIn(bibleLanguage, R.string.card_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MannaTheme.colors.ink
            )

            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = stringResourceIn(bibleLanguage, R.string.card_preview_description),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
            )

            val themeDescription = stringResourceIn(bibleLanguage, R.string.card_theme_description)
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
                            .semantics { contentDescription = themeDescription }
                    ) {}
                }
            }

            Button(
                onClick = { CardSharer.share(context, bitmap) },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResourceIn(bibleLanguage, R.string.card_share_image)) }

            TextButton(
                onClick = {
                    val message = "“$verseText”\n— $reference"
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, message)
                    }
                    context.startActivity(
                        Intent.createChooser(intent, context.getString(R.string.card_share_text))
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResourceIn(bibleLanguage, R.string.card_share_text)) }

            if (FeatureFlags.VERSE_RECOMMENDATION_AI && onFindVerse != null) {
                TextButton(
                    onClick = {
                        onDismiss()
                        onFindVerse()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResourceIn(bibleLanguage, R.string.verse_rec_entry)) }
            }
        }
    }
}
