package com.manna.bible.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manna.bible.R
import com.manna.bible.ui.theme.MannaTheme

private val MinTouchTarget = 48.dp

/**
 * The translation catalog (Requirements 4, 5, 6, 11.2).
 *
 * Lists the editions available for the chosen Bible language — downloaded ones
 * first — and lets the reader download, cancel, delete, and switch the active
 * translation. An offline notice appears when remote editions can't be fetched;
 * stored editions remain fully usable.
 *
 * @param onBack returns to the reader. Pass null when the catalog is hosted as
 *   the Library tab and there is nothing to go back to.
 * @param onOpenCalendar when non-null, shows a "Jesus Events Calendar" tool entry.
 * @param onOpenPastorMode when non-null, shows a "Pastor Mode" tool entry.
 * @param onOpenAttribution when non-null, shows an "Attribution & about" entry
 *   below the catalog list (Library tab).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationCatalogScreen(
    modifier: Modifier = Modifier,
    viewModel: TranslationCatalogViewModel = hiltViewModel(),
    onBack: (() -> Unit)? = {},
    onOpenCalendar: (() -> Unit)? = null,
    onOpenPastorMode: (() -> Unit)? = null,
    onOpenAttribution: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val backDescription = stringResource(R.string.catalog_back)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.catalog_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(MinTouchTarget)
                                .semantics { contentDescription = backDescription }
                        ) {
                            Text(text = "‹", fontSize = 26.sp)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.isOffline) {
                OfflineBanner()
            }
            val error = state.errorMessage
            if (error != null && error != "offline") {
                Text(
                    text = stringResource(R.string.catalog_error),
                    color = MannaTheme.colors.red,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            when {
                state.isLoading -> CenteredBox { CircularProgressIndicator() }
                state.items.isEmpty() -> CenteredBox {
                    Text(
                        text = stringResource(R.string.catalog_empty),
                        textAlign = TextAlign.Center,
                        color = MannaTheme.colors.soft,
                        modifier = Modifier.padding(32.dp)
                    )
                }
                else -> LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items = state.items, key = { it.id }) { item ->
                        CatalogRow(
                            item = item,
                            isDownloading = state.downloadingId == item.id,
                            progress = state.downloadProgress.takeIf { state.downloadingId == item.id },
                            onDownload = { viewModel.download(item.id) },
                            onCancel = { viewModel.cancel(item.id) },
                            onDelete = { viewModel.delete(item.id) },
                            onSetActive = { viewModel.setActive(item.id) }
                        )
                    }
                }
            }
            if (onOpenCalendar != null) {
                ToolEntry(label = stringResource(R.string.calendar_tool_entry), onClick = onOpenCalendar)
            }
            if (onOpenPastorMode != null) {
                ToolEntry(label = stringResource(R.string.pastor_title), onClick = onOpenPastorMode)
            }
            if (onOpenAttribution != null) {
                ToolEntry(label = stringResource(R.string.reader_attribution), onClick = onOpenAttribution)
            }
        }
    }
}

@Composable
private fun ToolEntry(label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .defaultMinSize(minHeight = MinTouchTarget)
    ) { Text(label) }
}

@Composable
private fun OfflineBanner() {
    Surface(color = MannaTheme.colors.card, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.catalog_offline),
            color = MannaTheme.colors.soft,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun CatalogRow(
    item: CatalogItem,
    isDownloading: Boolean,
    progress: Float?,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onSetActive: () -> Unit
) {
    Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = item.name, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    if (item.isActive) {
                        Text(
                            text = stringResource(R.string.catalog_active),
                            color = MannaTheme.colors.gold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (item.isDownloaded) {
                        Text(
                            text = stringResource(R.string.catalog_downloaded),
                            color = MannaTheme.colors.sage,
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(Modifier.size(8.dp))
                CatalogActions(
                    item = item,
                    isDownloading = isDownloading,
                    onDownload = onDownload,
                    onCancel = onCancel,
                    onDelete = onDelete,
                    onSetActive = onSetActive
                )
            }
            if (isDownloading) {
                Spacer(Modifier.height(8.dp))
                if (progress != null) {
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun CatalogActions(
    item: CatalogItem,
    isDownloading: Boolean,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onSetActive: () -> Unit
) {
    when {
        isDownloading -> TextButton(
            onClick = onCancel,
            modifier = Modifier.defaultMinSize(minHeight = MinTouchTarget)
        ) { Text(stringResource(R.string.catalog_cancel)) }

        !item.isDownloaded -> Button(
            onClick = onDownload,
            modifier = Modifier.defaultMinSize(minHeight = MinTouchTarget)
        ) { Text(stringResource(R.string.catalog_download)) }

        else -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!item.isActive) {
                Button(
                    onClick = onSetActive,
                    modifier = Modifier.defaultMinSize(minHeight = MinTouchTarget)
                ) { Text(stringResource(R.string.catalog_use)) }
            }
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.defaultMinSize(minHeight = MinTouchTarget)
            ) { Text(stringResource(R.string.catalog_delete)) }
        }
    }
}

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) { content() }
}
