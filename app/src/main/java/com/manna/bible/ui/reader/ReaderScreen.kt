package com.manna.bible.ui.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manna.bible.R
import com.manna.bible.domain.FeatureFlags
import com.manna.bible.domain.explain.ExplainDepth
import com.manna.bible.domain.explain.ExplanationUnavailableReason
import com.manna.bible.domain.reader.CanonBookOrdering
import com.manna.bible.ui.theme.MannaTheme
import com.manna.bible.ui.theme.ScriptureFontFamily

private val MinTouchTarget = 48.dp

/** Number of non-verse items at the top of the verse list (the search bar). */
private const val SEARCH_BAR_ITEMS = 1

/**
 * The offline `Reader_Screen` (Requirements 2, 3, 6, 8, 11.1).
 *
 * Renders the active chapter's verses, a canon-ordered book/chapter picker, and
 * previous/next navigation that respects canon boundaries. Verse taps open an
 * annotation sheet (highlight/bookmark/note). Loading, empty (seeding), and
 * chapter-unavailable states are handled explicitly. Audio (TTS), the translation
 * catalog, and the attribution screen are separate features; this screen exposes
 * callback hooks where they connect.
 *
 * @param onSwitchTranslation hook to the translation catalog (Task 11).
 * @param onOpenAttribution hook to the attribution/about surface (Task 12).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel = hiltViewModel(),
    onSwitchTranslation: () -> Unit = {},
    onOpenAttribution: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenDaily: () -> Unit = {},
    onOpenCrisis: (() -> Unit)? = null,
    pendingScrollRef: String? = null,
    onScrollRefConsumed: () -> Unit = {},
    autoPlayAudio: Boolean = false,
    onAutoPlayConsumed: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showPicker by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    // A search result (or other caller) handed us a canonical reference to open.
    androidx.compose.runtime.LaunchedEffect(pendingScrollRef) {
        val ref = com.manna.bible.domain.usecase.ReadingRef.parse(pendingScrollRef) ?: return@LaunchedEffect
        viewModel.openAt(ref.osisId, ref.chapter, ref.verse)
        onScrollRefConsumed()
    }

    // Continue Listening: start read-aloud once the chapter is on screen. [autoPlayAudio]
    // is a one-shot request (owned by the caller via [onAutoPlayConsumed]) so it fires
    // exactly once per request even though the Read tab is long-lived.
    androidx.compose.runtime.LaunchedEffect(autoPlayAudio, state.isLoading, state.verses) {
        if (autoPlayAudio && !state.isLoading && state.verses.isNotEmpty()) {
            onAutoPlayConsumed()
            if (!state.isAudioActive) viewModel.onAudioPlayPause()
        }
    }

    // Render the reader right-to-left for RTL Bible languages (Req 14.4).
    val layoutDirection = if (state.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ReaderTopBar(
                bookName = state.bookName,
                displayedChapterNumber = state.displayedChapterNumber,
                onOpenPicker = { showPicker = true },
                onOpenSearch = onOpenSearch,
                showMenu = showMenu,
                onMenuToggle = { showMenu = it },
                onSwitchTranslation = {
                    showMenu = false
                    onSwitchTranslation()
                },
                onOpenAttribution = {
                    showMenu = false
                    onOpenAttribution()
                },
                onOpenDaily = {
                    showMenu = false
                    onOpenDaily()
                },
                onOpenCrisis = onOpenCrisis?.let {
                    {
                        showMenu = false
                        it()
                    }
                }
            )
        },
        bottomBar = {
            ReaderBottomBar(
                state = state,
                onPrev = viewModel::previousChapter,
                onNext = viewModel::nextChapter,
                onAudioPlayPause = viewModel::onAudioPlayPause,
                onAudioStop = viewModel::stopAudio,
                onCycleSpeed = { viewModel.setAudioSpeed(nextSpeed(state.ttsSpeed)) },
                onToggleContinuous = { viewModel.setContinuousPlay(!state.continuousPlay) }
            )
        }
    ) { padding ->
        ReaderContent(
            state = state,
            contentPadding = padding,
            onVerseClick = viewModel::selectVerse,
            onScrollHandled = viewModel::onScrollHandled,
            onRetry = viewModel::refresh,
            onDownload = viewModel::downloadActiveTranslation,
            onSwitchTranslation = onSwitchTranslation,
            onOpenSearch = onOpenSearch
        )
    }

    if (showPicker) {
        val profile = state.profile
        BookChapterPicker(
            // Only books that are both in the active canon and present in the translation,
            // shown in canon order (Req 3.5, 3.7).
            books = remember(profile, state.books) {
                if (profile == null) {
                    emptyList()
                } else {
                    val byOsis = state.books.associateBy { it.osisId }
                    CanonBookOrdering.orderedBooks(profile)
                        .mapNotNull { canonBook -> byOsis[canonBook.osisId] }
                        .map { PickerBook(it.osisId, it.name, it.chapterCount) }
                }
            },
            onDismiss = { showPicker = false },
            onSelect = { osisId, chapter ->
                showPicker = false
                viewModel.openChapter(osisId, chapter)
            }
        )
    }

    val selected = state.selectedVerse
    if (selected != null) {
        val verse = state.verses.firstOrNull { it.verse == selected }
        AnnotationSheet(
            verseLabel = (verse?.displayNumber ?: selected).toString(),
            hasHighlight = verse?.hasHighlight == true,
            hasBookmark = verse?.hasBookmark == true,
            hasNote = verse?.hasNote == true,
            onToggleHighlight = { viewModel.toggleHighlight(selected) },
            onToggleBookmark = { viewModel.toggleBookmark(selected) },
            onAddNote = { content -> viewModel.addNote(selected, content) },
            onRemoveNote = { viewModel.removeNotes(selected) },
            onExplain = if (FeatureFlags.EXPLAIN_PASSAGE) {
                { viewModel.explainVerse(selected) }
            } else null,
            onDismiss = { viewModel.selectVerse(null) }
        )
    }

    state.explain?.let { explain ->
        ExplainSheet(
            explain = explain,
            onDepthChange = viewModel::setExplainDepth,
            onDismiss = viewModel::dismissExplain
        )
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderTopBar(
    bookName: String,
    displayedChapterNumber: Int,
    onOpenPicker: () -> Unit,
    onOpenSearch: () -> Unit,
    showMenu: Boolean,
    onMenuToggle: (Boolean) -> Unit,
    onSwitchTranslation: () -> Unit,
    onOpenAttribution: () -> Unit,
    onOpenDaily: () -> Unit,
    onOpenCrisis: (() -> Unit)? = null
) {
    val heading = if (bookName.isBlank()) {
        stringResource(R.string.app_name)
    } else {
        "$bookName $displayedChapterNumber"
    }
    val pickerDescription = stringResource(R.string.reader_open_picker)
    val searchDescription = stringResource(R.string.reader_search)
    val moreDescription = stringResource(R.string.reader_more_options)
    TopAppBar(
        title = {
            Text(
                text = heading,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .defaultMinSize(minHeight = MinTouchTarget)
                    .clickable(onClick = onOpenPicker)
                    .semantics { contentDescription = heading }
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onOpenPicker,
                modifier = Modifier
                    .size(MinTouchTarget)
                    .semantics { contentDescription = pickerDescription }
            ) {
                Text(text = "\u2630", fontSize = 20.sp)
            }
        },
        actions = {
            IconButton(
                onClick = onOpenSearch,
                modifier = Modifier
                    .size(MinTouchTarget)
                    .semantics { contentDescription = searchDescription }
            ) {
                Text(text = "\uD83D\uDD0D", fontSize = 18.sp)
            }
            IconButton(
                onClick = { onMenuToggle(true) },
                modifier = Modifier
                    .size(MinTouchTarget)
                    .semantics { contentDescription = moreDescription }
            ) {
                Text(text = "\u22EE", fontSize = 22.sp)
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { onMenuToggle(false) }) {
                if (onOpenCrisis != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.crisis_menu_entry)) },
                        onClick = onOpenCrisis
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reader_daily_verse)) },
                    onClick = onOpenDaily
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reader_switch_translation)) },
                    onClick = onSwitchTranslation
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reader_attribution)) },
                    onClick = onOpenAttribution
                )
            }
        }
    )
}

@Composable
private fun ReaderBottomBar(
    state: ReaderUiState,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onAudioPlayPause: () -> Unit,
    onAudioStop: () -> Unit,
    onCycleSpeed: () -> Unit,
    onToggleContinuous: () -> Unit
) {
    Surface(tonalElevation = 3.dp) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (state.audioVoiceUnavailable) {
                Text(
                    text = stringResource(R.string.reader_audio_voice_unavailable),
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = MannaTheme.colors.orange,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            AudioBar(
                isPlaying = state.isAudioPlaying,
                isActive = state.isAudioActive,
                speed = state.ttsSpeed,
                continuousPlay = state.continuousPlay,
                onPlayPause = onAudioPlayPause,
                onStop = onAudioStop,
                onCycleSpeed = onCycleSpeed,
                onToggleContinuous = onToggleContinuous
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val prevDescription = stringResource(R.string.reader_previous_chapter)
                val nextDescription = stringResource(R.string.reader_next_chapter)
                IconButton(
                    onClick = onPrev,
                    enabled = state.canPrev,
                    modifier = Modifier
                        .size(MinTouchTarget)
                        .semantics { contentDescription = prevDescription }
                ) {
                    Text(text = "\u2039", fontSize = 26.sp)
                }
                IconButton(
                    onClick = onNext,
                    enabled = state.canNext,
                    modifier = Modifier
                        .size(MinTouchTarget)
                        .semantics { contentDescription = nextDescription }
                ) {
                    Text(text = "\u203A", fontSize = 26.sp)
                }
            }
        }
    }
}

/** Speed steps cycled by the audio bar's speed control (Req 9.4). */
private val SpeedSteps = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

/** Returns the next speed in [SpeedSteps], wrapping back to the slowest after the fastest. */
private fun nextSpeed(current: Float): Float {
    val index = SpeedSteps.indexOfFirst { kotlin.math.abs(it - current) < 0.01f }
    return SpeedSteps[(index + 1).mod(SpeedSteps.size)]
}

/** Formats a speed multiplier without a trailing `.0` (e.g. `1`, `0.75`, `1.5`). */
private fun formatSpeed(speed: Float): String =
    if (speed % 1f == 0f) speed.toInt().toString() else speed.toString()

/**
 * Offline read-aloud controls (Requirement 9.3, 9.4, 9.7): play/pause, stop (only
 * while active), a tappable speed multiplier that cycles 0.5x-2.0x, and a
 * continuous-play toggle. Each control meets the 48dp touch target and carries a
 * TalkBack description.
 */
@Composable
private fun AudioBar(
    isPlaying: Boolean,
    isActive: Boolean,
    speed: Float,
    continuousPlay: Boolean,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onCycleSpeed: () -> Unit,
    onToggleContinuous: () -> Unit
) {
    val playPauseLabel = if (isPlaying) {
        stringResource(R.string.reader_audio_pause)
    } else if (isActive) {
        stringResource(R.string.reader_audio_resume)
    } else {
        stringResource(R.string.reader_audio_play)
    }
    val stopLabel = stringResource(R.string.reader_audio_stop)
    val speedLabel = stringResource(R.string.reader_audio_speed_label, formatSpeed(speed))
    val continuousState =
        stringResource(if (continuousPlay) R.string.a11y_on else R.string.a11y_off)
    val continuousLabel = stringResource(R.string.reader_audio_continuous_toggle, continuousState)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPlayPause,
            modifier = Modifier
                .size(MinTouchTarget)
                .semantics { contentDescription = playPauseLabel }
        ) {
            Text(text = if (isPlaying) "⏸" else "▶", fontSize = 20.sp)
        }
        IconButton(
            onClick = onStop,
            enabled = isActive,
            modifier = Modifier
                .size(MinTouchTarget)
                .semantics { contentDescription = stopLabel }
        ) {
            Text(text = "■", fontSize = 18.sp)
        }
        TextButton(
            onClick = onCycleSpeed,
            modifier = Modifier
                .defaultMinSize(minHeight = MinTouchTarget)
                .semantics { contentDescription = speedLabel }
        ) {
            Text(
                text = stringResource(R.string.reader_audio_speed, formatSpeed(speed)),
                color = MannaTheme.colors.gold,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.weight(1f))
        TextButton(
            onClick = onToggleContinuous,
            modifier = Modifier
                .defaultMinSize(minHeight = MinTouchTarget)
                .semantics { contentDescription = continuousLabel }
        ) {
            Text(
                text = stringResource(R.string.reader_audio_continuous),
                color = if (continuousPlay) MannaTheme.colors.gold else MannaTheme.colors.soft,
                fontWeight = if (continuousPlay) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun ReaderContent(
    state: ReaderUiState,
    contentPadding: PaddingValues,
    onVerseClick: (Int) -> Unit,
    onScrollHandled: () -> Unit,
    onRetry: () -> Unit,
    onDownload: () -> Unit,
    onSwitchTranslation: () -> Unit,
    onOpenSearch: () -> Unit
) {
    when {
        state.isLoading && state.verses.isEmpty() -> CenteredState(contentPadding) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.reader_loading))
        }

        state.isDownloading -> DownloadingState(
            contentPadding = contentPadding,
            progress = state.downloadProgress
        )

        state.isEmptyContent -> CenteredMessage(
            contentPadding = contentPadding,
            title = stringResource(R.string.reader_empty_title),
            message = state.errorMessage.toEmptyStateMessage(),
            primaryLabel = stringResource(R.string.reader_download),
            onPrimary = onDownload,
            secondaryLabel = stringResource(R.string.reader_switch_translation),
            onSecondary = onSwitchTranslation
        )

        state.chapterUnavailable -> CenteredMessage(
            contentPadding = contentPadding,
            title = stringResource(R.string.reader_unavailable_title),
            message = stringResource(R.string.reader_unavailable_message),
            primaryLabel = stringResource(R.string.reader_switch_translation),
            onPrimary = onSwitchTranslation,
            secondaryLabel = stringResource(R.string.reader_retry),
            onSecondary = onRetry
        )

        else -> VerseList(
            state = state,
            contentPadding = contentPadding,
            onVerseClick = onVerseClick,
            onScrollHandled = onScrollHandled,
            onOpenSearch = onOpenSearch
        )
    }
}

@Composable
private fun VerseList(
    state: ReaderUiState,
    contentPadding: PaddingValues,
    onVerseClick: (Int) -> Unit,
    onScrollHandled: () -> Unit,
    onOpenSearch: () -> Unit
) {
    val listState = rememberLazyListState()

    androidx.compose.runtime.LaunchedEffect(state.scrollToVerse, state.verses) {
        val target = state.scrollToVerse ?: return@LaunchedEffect
        val index = state.verses.indexOfFirst { it.verse == target }
        if (index >= 0) {
            // +1 for the leading search-bar item.
            listState.animateScrollToItem(index + SEARCH_BAR_ITEMS)
        }
        onScrollHandled()
    }

    // Keep the verse being read aloud on screen as playback advances (Req 9.2).
    androidx.compose.runtime.LaunchedEffect(state.audioVerse) {
        val target = state.audioVerse ?: return@LaunchedEffect
        val index = state.verses.indexOfFirst { it.verse == target }
        if (index >= 0) {
            // +1 for the leading search-bar item.
            listState.animateScrollToItem(index + SEARCH_BAR_ITEMS)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "__reader_search_bar") {
            ReaderSearchBar(onClick = onOpenSearch)
        }
        items(items = state.verses, key = { it.verse }) { verse ->
            VerseRow(
                verse = verse,
                isSpoken = verse.verse == state.audioVerse,
                enlarged = state.simplifiedMode,
                onClick = { onVerseClick(verse.verse) }
            )
        }
    }
}

@Composable
private fun VerseRow(
    verse: ReaderVerse,
    isSpoken: Boolean,
    enlarged: Boolean,
    onClick: () -> Unit
) {
    val verseDescription = buildString {
        append(stringResource(R.string.a11y_verse, verse.displayNumber ?: verse.verse))
        append(". ")
        append(verse.text)
        if (verse.hasHighlight) append(", ${stringResource(R.string.a11y_has_highlight)}")
        if (verse.hasBookmark) append(", ${stringResource(R.string.a11y_has_bookmark)}")
        if (verse.hasNote) append(", ${stringResource(R.string.a11y_has_note)}")
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = MinTouchTarget)
            .clickable(onClick = onClick)
            .then(if (isSpoken) Modifier.background(MannaTheme.colors.card) else Modifier)
            .clearAndSetSemantics { contentDescription = verseDescription }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = (verse.displayNumber ?: verse.verse).toString(),
            color = MannaTheme.colors.gold,
            fontWeight = FontWeight.Bold,
            fontSize = if (enlarged) 16.sp else 13.sp,
            modifier = Modifier
                .width(if (enlarged) 36.dp else 28.dp)
                .padding(top = 3.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = verse.text,
                fontFamily = ScriptureFontFamily,
                color = MannaTheme.colors.ink,
                fontSize = if (enlarged) 24.sp else 18.sp,
                lineHeight = if (enlarged) 36.sp else 28.sp
            )
            if (verse.hasHighlight || verse.hasBookmark || verse.hasNote) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (verse.hasHighlight) {
                        IndicatorDot(MannaTheme.colors.gold)
                    }
                    if (verse.hasBookmark) {
                        IndicatorDot(MannaTheme.colors.lavender)
                    }
                    if (verse.hasNote) {
                        IndicatorDot(MannaTheme.colors.sage)
                    }
                }
            }
        }
    }
}

@Composable
private fun CenteredState(
    contentPadding: PaddingValues,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        content()
    }
}

/** Determinate/indeterminate download progress shown in place of the empty state. */
@Composable
private fun DownloadingState(contentPadding: PaddingValues, progress: Float?) {
    CenteredState(contentPadding) {
        if (progress != null) {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(
                    R.string.reader_download_percent,
                    (progress.coerceIn(0f, 1f) * 100).toInt()
                ),
                color = MannaTheme.colors.gold,
                fontWeight = FontWeight.Bold
            )
        } else {
            CircularProgressIndicator()
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.reader_downloading_title),
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.reader_downloading_message),
            textAlign = TextAlign.Center,
            color = MannaTheme.colors.soft
        )
    }
}

/** Maps a reader error (if any) to the message shown above the download button. */
@Composable
private fun String?.toEmptyStateMessage(): String = when (this) {
    null -> stringResource(R.string.reader_empty_message)
    "offline" -> stringResource(R.string.reader_offline_message)
    else -> stringResource(R.string.reader_download_failed)
}

@Composable
private fun CenteredMessage(
    contentPadding: PaddingValues,
    title: String,
    message: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null
) {
    CenteredState(contentPadding) {
        Text(
            text = title,
            style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(text = message, textAlign = TextAlign.Center, color = MannaTheme.colors.soft)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onPrimary,
            modifier = Modifier.defaultMinSize(minHeight = MinTouchTarget)
        ) {
            Text(primaryLabel)
        }
        if (secondaryLabel != null && onSecondary != null) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onSecondary,
                modifier = Modifier.defaultMinSize(minHeight = MinTouchTarget)
            ) {
                Text(secondaryLabel)
            }
        }
    }
}

/** A book entry for the picker: stable id, display name, and chapter count. */
private data class PickerBook(
    val osisId: String,
    val name: String,
    val chapterCount: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookChapterPicker(
    books: List<PickerBook>,
    onDismiss: () -> Unit,
    onSelect: (osisId: String, chapter: Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedBook by remember { mutableStateOf<PickerBook?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        val book = selectedBook
        if (book == null) {
            Text(
                text = stringResource(R.string.reader_book_picker_title),
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
            ) {
                items(items = books, key = { it.osisId }) { entry ->
                    Text(
                        text = entry.name,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = MinTouchTarget)
                            .clickable { selectedBook = entry }
                            .padding(horizontal = 24.dp, vertical = 14.dp)
                    )
                }
            }
        } else {
            Text(
                text = stringResource(R.string.reader_chapter_picker_title, book.name),
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 56.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                gridItems((1..book.chapterCount).toList()) { chapter ->
                    Surface(
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .size(MinTouchTarget)
                            .clickable { onSelect(book.osisId, chapter) }
                            .semantics {
                                contentDescription = "Chapter $chapter"
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = chapter.toString(), fontSize = 16.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnnotationSheet(
    verseLabel: String,
    hasHighlight: Boolean,
    hasBookmark: Boolean,
    hasNote: Boolean,
    onToggleHighlight: () -> Unit,
    onToggleBookmark: () -> Unit,
    onAddNote: (String) -> Unit,
    onRemoveNote: () -> Unit,
    onExplain: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var noteText by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.reader_annotate_title, verseLabel),
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            if (onExplain != null) {
                AnnotationAction(
                    label = stringResource(R.string.explain_action),
                    onClick = onExplain
                )
            }

            AnnotationAction(
                label = if (hasHighlight) {
                    stringResource(R.string.annotation_remove_highlight)
                } else {
                    stringResource(R.string.annotation_highlight)
                },
                onClick = onToggleHighlight
            )
            AnnotationAction(
                label = if (hasBookmark) {
                    stringResource(R.string.annotation_remove_bookmark)
                } else {
                    stringResource(R.string.annotation_bookmark)
                },
                onClick = onToggleBookmark
            )

            if (hasNote) {
                AnnotationAction(
                    label = stringResource(R.string.annotation_remove_note),
                    onClick = onRemoveNote
                )
            }

            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text(stringResource(R.string.annotation_note_hint)) },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    onAddNote(noteText)
                    noteText = ""
                },
                enabled = noteText.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = MinTouchTarget)
            ) {
                Text(stringResource(R.string.annotation_save_note))
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = MinTouchTarget)
            ) {
                Text(stringResource(R.string.annotation_close))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExplainSheet(
    explain: ExplainSheetState,
    onDepthChange: (ExplainDepth) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.explain_title, explain.reference),
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 8.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = explain.depth == ExplainDepth.PLAIN,
                    onClick = { onDepthChange(ExplainDepth.PLAIN) },
                    label = { Text(stringResource(R.string.explain_depth_plain)) }
                )
                FilterChip(
                    selected = explain.depth == ExplainDepth.PREACHING,
                    onClick = { onDepthChange(ExplainDepth.PREACHING) },
                    label = { Text(stringResource(R.string.explain_depth_preaching)) }
                )
            }

            when (val status = explain.status) {
                ExplainStatus.Loading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(vertical = 16.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        Text(stringResource(R.string.explain_loading))
                    }
                }
                is ExplainStatus.Ready -> {
                    Text(
                        text = status.text,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
                    )
                }
                is ExplainStatus.Unavailable -> {
                    val message = when (status.reason) {
                        ExplanationUnavailableReason.NOT_CONFIGURED ->
                            stringResource(R.string.explain_unavailable_key)
                        ExplanationUnavailableReason.OFFLINE ->
                            stringResource(R.string.explain_unavailable_offline)
                        ExplanationUnavailableReason.ERROR ->
                            stringResource(R.string.explain_unavailable_error)
                    }
                    Text(
                        text = message,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                    )
                }
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = MinTouchTarget)
            ) {
                Text(stringResource(R.string.annotation_close))
            }
        }
    }
}

@Composable
private fun ReaderSearchBar(onClick: () -> Unit) {
    Surface(
        color = MannaTheme.colors.card,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = MannaTheme.colors.muted
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.reader_search_hint),
                color = MannaTheme.colors.muted
            )
        }
    }
}

@Composable
private fun AnnotationAction(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = MinTouchTarget)
    ) {
        Text(label)
    }
}

@Composable
private fun IndicatorDot(color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .background(color = color, shape = CircleShape)
    )
}
