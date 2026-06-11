package com.manna.bible.ui.search

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manna.bible.R
import com.manna.bible.domain.usecase.ReadingRef
import com.manna.bible.domain.usecase.SearchResult
import com.manna.bible.ui.theme.MannaColors

private val MinTouchTarget = 48.dp

/**
 * Full-text search over the active translation (Requirement 10).
 *
 * Shows a query field and the canon-visible matches with their display reference
 * and a text snippet. Selecting a result hands a canonical reference back to the
 * reader, which opens that chapter at the matching verse (Req 10.4).
 *
 * @param onBack returns to the reader without jumping.
 * @param onResultSelected receives the canonical `OSIS.CHAPTER.VERSE` reference of
 *   the chosen result.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onResultSelected: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val backDescription = stringResource(R.string.search_back)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(MinTouchTarget)
                            .semantics { contentDescription = backDescription }
                    ) { Text(text = "‹", fontSize = 26.sp) }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                label = { Text(stringResource(R.string.search_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )

            when {
                state.isSearching -> Centered { CircularProgressIndicator() }

                !state.contentAvailable -> Centered {
                    Message(stringResource(R.string.search_no_content))
                }

                state.isNoResults -> Centered {
                    Message(stringResource(R.string.search_no_results, state.query))
                }

                state.results.isNotEmpty() -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(items = state.results, key = { "${it.osisId}.${it.chapter}.${it.verse}" }) { result ->
                        ResultRow(
                            result = result,
                            onClick = {
                                onResultSelected(
                                    ReadingRef(result.osisId, result.chapter, result.verse).format()
                                )
                            }
                        )
                        HorizontalDivider(color = MannaColors.border)
                    }
                }

                else -> Centered {
                    Message(stringResource(R.string.search_prompt))
                }
            }
        }
    }
}

@Composable
private fun ResultRow(result: SearchResult, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = MinTouchTarget)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = result.reference,
            color = MannaColors.gold,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Text(
            text = result.snippet,
            fontSize = 15.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) { content() }
}

@Composable
private fun Message(text: String) {
    Text(text = text, textAlign = TextAlign.Center, color = MannaColors.soft)
}
