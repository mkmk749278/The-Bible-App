package com.manna.bible.ui.church

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manna.bible.R
import com.manna.bible.domain.liturgy.Liturgy
import com.manna.bible.domain.liturgy.LiturgyPart
import com.manna.bible.domain.liturgy.LiturgyRole
import com.manna.bible.domain.liturgy.LiturgySection
import com.manna.bible.ui.theme.MannaTheme
import com.manna.bible.ui.util.rememberBibleLanguage
import com.manna.bible.ui.util.rememberSimplifiedMode
import com.manna.bible.ui.util.stringResourceIn
import java.util.Locale

private val MinTouchTarget = 48.dp
private val SimplifiedTouchTarget = 56.dp

/**
 * Liturgy Detail — the selected order of worship expanded into its sections and parts in
 * celebrated sequence (Req 6). Each part shows who speaks (presider / people / all /
 * reader) or a rubric (a muted instruction), the public-domain / response text resolved in
 * the Bible language (English fallback), and — where a presidential prayer is proper to the
 * day — a notice to follow the parish book (never a fabricated text). Scripture references
 * open in the reader. The static framing always resolves in the Bible language.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiturgyDetailScreen(
    onBack: () -> Unit,
    onOpenVerse: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LiturgyDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val fallbackLanguage = rememberBibleLanguage()
    val simplified = rememberSimplifiedMode()

    LiturgyDetailContent(
        liturgy = state.liturgy,
        bibleLanguage = if (state.isLoading) fallbackLanguage else state.bibleLanguage,
        isLoading = state.isLoading,
        simplified = simplified,
        onBack = onBack,
        onOpenVerse = onOpenVerse,
        modifier = modifier
    )
}

/**
 * Stateless rendering of a single [Liturgy], so the surface can be driven directly (and
 * rendered in property/rendering tests) without Hilt or a ViewModel. When the Bible
 * language is an RTL script the whole layout mirrors via [LocalLayoutDirection],
 * independent of the individual content language (Req 14.5).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiturgyDetailContent(
    liturgy: Liturgy?,
    bibleLanguage: String,
    onBack: () -> Unit,
    onOpenVerse: (String) -> Unit,
    isLoading: Boolean = false,
    simplified: Boolean = false,
    modifier: Modifier = Modifier
) {
    val backDescription = stringResourceIn(bibleLanguage, R.string.church_back)
    val rowMinHeight = if (simplified) SimplifiedTouchTarget else MinTouchTarget

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirectionFor(bibleLanguage)) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = liturgy?.title?.resolve(bibleLanguage)
                                ?: stringResourceIn(bibleLanguage, R.string.church_title),
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
            if (isLoading || liturgy == null) return@Scaffold
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "header") {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = liturgy.title.resolve(bibleLanguage),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MannaTheme.colors.ink
                        )
                        Text(
                            text = liturgy.tradition,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MannaTheme.colors.gold
                        )
                    }
                }

                liturgy.sections.forEachIndexed { sIndex, section ->
                    item(key = "section_$sIndex") { SectionHeader(section, bibleLanguage) }
                    section.parts.forEachIndexed { pIndex, part ->
                        item(key = "part_${sIndex}_$pIndex") {
                            PartView(
                                part = part,
                                bibleLanguage = bibleLanguage,
                                minHeight = rowMinHeight,
                                onOpenVerse = onOpenVerse
                            )
                        }
                    }
                }

                item(key = "source") {
                    Text(
                        text = "${stringResourceIn(bibleLanguage, R.string.church_source)}: " +
                            liturgy.sourceNote.resolve(bibleLanguage),
                        style = MaterialTheme.typography.labelSmall,
                        color = MannaTheme.colors.muted,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(section: LiturgySection, bibleLanguage: String) {
    Text(
        text = section.title.resolve(bibleLanguage),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MannaTheme.colors.gold,
        modifier = Modifier.padding(top = 16.dp, bottom = 2.dp)
    )
}

@Composable
private fun PartView(
    part: LiturgyPart,
    bibleLanguage: String,
    minHeight: androidx.compose.ui.unit.Dp,
    onOpenVerse: (String) -> Unit
) {
    // A bare rubric reads as a quiet stage-direction with NO speaker label, even when its
    // text itself references a role (Req 7.2). Its scripture link, if any, is still offered.
    // A rubric that also carries spoken text / a title — or that is an official-text part —
    // falls through to the labelled card layout below (which still assigns it no speaker
    // label, as RUBRIC has none) so its official-text notice is never dropped (Req 7.4, 12.2).
    if (part.role == LiturgyRole.RUBRIC && part.title == null && part.text == null &&
        !part.needsOfficialText
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(
                text = part.rubric?.resolve(bibleLanguage).orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MannaTheme.colors.muted,
                fontStyle = FontStyle.Italic
            )
            ReadInContextAction(part, bibleLanguage, onOpenVerse)
        }
        return
    }

    Surface(
        color = MannaTheme.colors.card,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = minHeight)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                liturgyRoleLabelRes(part.role)?.let { res ->
                    Text(
                        text = stringResourceIn(bibleLanguage, res),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = liturgyRoleColor(part.role, MannaTheme.colors)
                    )
                }
                if (part.title != null) {
                    if (liturgyRoleLabelRes(part.role) != null) {
                        Text(
                            text = "  \u00B7  ",
                            style = MaterialTheme.typography.labelMedium,
                            color = MannaTheme.colors.muted
                        )
                    }
                    Text(
                        text = part.title.resolve(bibleLanguage),
                        style = MaterialTheme.typography.labelMedium,
                        color = MannaTheme.colors.soft
                    )
                }
            }
            if (part.text != null) {
                Text(
                    text = part.text.resolve(bibleLanguage),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MannaTheme.colors.ink,
                    lineHeight = 26.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (part.rubric != null) {
                Text(
                    text = part.rubric.resolve(bibleLanguage),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MannaTheme.colors.muted,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(top = if (part.text != null) 6.dp else 4.dp)
                )
            }
            // An official-text part points to the parish book; its proper text is never
            // fabricated here (Req 7.4, 12.2).
            if (part.needsOfficialText) {
                Text(
                    text = stringResourceIn(bibleLanguage, R.string.church_needs_official),
                    style = MaterialTheme.typography.labelSmall,
                    color = MannaTheme.colors.orange,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            ReadInContextAction(part, bibleLanguage, onOpenVerse)
        }
    }
}

/** The "read in context" affordance, shown only for a part with a scripture reference (Req 7.5). */
@Composable
private fun ReadInContextAction(
    part: LiturgyPart,
    bibleLanguage: String,
    onOpenVerse: (String) -> Unit
) {
    val osisRef = part.osisRef ?: return
    val label = stringResourceIn(bibleLanguage, R.string.church_open_reading)
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MannaTheme.colors.gold,
        modifier = Modifier
            .padding(top = 6.dp)
            .clickable(onClickLabel = label) { onOpenVerse(osisRef) }
            .semantics { contentDescription = label }
    )
}

/**
 * The layout direction implied by a Bible-language tag: RTL for right-to-left scripts,
 * LTR otherwise. None of Manna's current Bible languages (en/ta/te/hi/ml) are RTL, but the
 * surface mirrors automatically should an RTL language ship (Req 14.5).
 */
private fun layoutDirectionFor(languageTag: String): LayoutDirection {
    if (languageTag.isBlank()) return LayoutDirection.Ltr
    val locale = Locale.forLanguageTag(languageTag)
    val dir = android.text.TextUtils.getLayoutDirectionFromLocale(locale)
    return if (dir == android.view.View.LAYOUT_DIRECTION_RTL) LayoutDirection.Rtl else LayoutDirection.Ltr
}
