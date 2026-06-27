package com.manna.bible.ui.church

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manna.bible.R
import com.manna.bible.ui.theme.MannaTheme
import com.manna.bible.ui.util.rememberBibleLanguage
import com.manna.bible.ui.util.rememberSimplifiedMode
import com.manna.bible.ui.util.stringResourceIn

/** Minimum interactive target; enlarged in Simplified / Elder Mode (Req 14.2/14.3). */
private val MinTouchTarget = 48.dp
private val SimplifiedTouchTarget = 56.dp

/**
 * Liturgy Library — a browsable, denomination-ordered list of every bundled order of
 * worship (Req 5). Tapping a row opens its expanded order ([onOpenLiturgy]). Fully offline.
 * The framing chrome resolves in the user's Bible language via [stringResourceIn].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiturgyLibraryScreen(
    onBack: () -> Unit,
    onOpenLiturgy: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LiturgyLibraryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val bibleLanguage = rememberBibleLanguage()
    val simplified = rememberSimplifiedMode()

    LiturgyLibraryContent(
        state = state,
        bibleLanguage = bibleLanguage,
        simplified = simplified,
        onBack = onBack,
        onOpenLiturgy = onOpenLiturgy,
        modifier = modifier
    )
}

/**
 * Stateless rendering of the Liturgy Library, so it can be driven directly (and rendered in
 * property/rendering tests) without Hilt or a ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiturgyLibraryContent(
    state: LiturgyLibraryUiState,
    bibleLanguage: String,
    simplified: Boolean,
    onBack: () -> Unit,
    onOpenLiturgy: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val backDescription = stringResourceIn(bibleLanguage, R.string.church_back)
    val rowMinHeight = if (simplified) SimplifiedTouchTarget else MinTouchTarget

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResourceIn(bibleLanguage, R.string.church_library_title),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(rowMinHeight)
                            .semantics { contentDescription = backDescription }
                    ) { Text(text = "\u2039", fontSize = 26.sp) }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) return@Scaffold
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!state.denominationHasMapping) {
                item(key = "preparing") {
                    Text(
                        text = stringResourceIn(bibleLanguage, R.string.church_library_preparing),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MannaTheme.colors.soft,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
            items(state.entries, key = { it.id }) { entry ->
                LiturgyLibraryRow(
                    entry = entry,
                    bibleLanguage = bibleLanguage,
                    minHeight = rowMinHeight,
                    onOpen = { onOpenLiturgy(entry.id) }
                )
            }
        }
    }
}

@Composable
private fun LiturgyLibraryRow(
    entry: LiturgyListItem,
    bibleLanguage: String,
    minHeight: androidx.compose.ui.unit.Dp,
    onOpen: () -> Unit
) {
    val openDescription = stringResourceIn(bibleLanguage, R.string.church_library_open)
        .format(entry.title)
    Surface(
        color = MannaTheme.colors.card,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = minHeight)
            .clickable(onClickLabel = openDescription) { onOpen() }
            .semantics { contentDescription = openDescription }
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MannaTheme.colors.ink
            )
            Text(
                text = entry.tradition,
                style = MaterialTheme.typography.bodyMedium,
                color = MannaTheme.colors.gold,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
