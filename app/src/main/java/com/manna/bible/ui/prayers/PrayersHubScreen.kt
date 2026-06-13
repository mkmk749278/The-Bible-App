package com.manna.bible.ui.prayers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manna.bible.R
import com.manna.bible.ui.theme.MannaTheme

/** A single devotional practice shown on the hub. */
private data class PrayerCategory(
    val title: String,
    val description: String,
    val glyph: String,
    val accent: Color,
    val onClick: () -> Unit
)

/**
 * The Prayers hub — the calm front door to the guided devotional practices: the
 * Stations of the Cross, the Rosary (Japamala), the Jesus Prayer, and Paraloka. Each
 * card opens a full practice; disabled features (null callbacks) simply disappear.
 *
 * Surfaces as a primary tab (Read · Calendar · Prayers · More). Pure presentation —
 * the categories are static, gated by the navigation layer's feature flags.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayersHubScreen(
    modifier: Modifier = Modifier,
    onOpenStations: (() -> Unit)? = null,
    onOpenRosary: (() -> Unit)? = null,
    onOpenJesusPrayer: (() -> Unit)? = null,
    onOpenParaloka: (() -> Unit)? = null,
    onOpenSramanikal: (() -> Unit)? = null
) {
    val categories = listOfNotNull(
        onOpenStations?.let {
            PrayerCategory(
                title = stringResource(R.string.prayers_category_stations_title),
                description = stringResource(R.string.prayers_category_stations_desc),
                glyph = "✝", // ✝ cross
                accent = MannaTheme.colors.gold,
                onClick = it
            )
        },
        onOpenRosary?.let {
            PrayerCategory(
                title = stringResource(R.string.prayers_category_rosary_title),
                description = stringResource(R.string.prayers_category_rosary_desc),
                glyph = "⚬", // ⚬ bead
                accent = MannaTheme.colors.lavender,
                onClick = it
            )
        },
        onOpenJesusPrayer?.let {
            PrayerCategory(
                title = stringResource(R.string.prayers_category_jesus_title),
                description = stringResource(R.string.prayers_category_jesus_desc),
                glyph = "♥", // ♥ heart
                accent = MannaTheme.colors.sage,
                onClick = it
            )
        },
        onOpenParaloka?.let {
            PrayerCategory(
                title = stringResource(R.string.prayers_category_paraloka_title),
                description = stringResource(R.string.prayers_category_paraloka_desc),
                glyph = "✴", // ✴ star
                accent = MannaTheme.colors.cyan,
                onClick = it
            )
        },
        onOpenSramanikal?.let {
            PrayerCategory(
                title = stringResource(R.string.prayers_category_sramanikal_title),
                description = stringResource(R.string.prayers_category_sramanikal_desc),
                glyph = "✦", // ✦ remembrance
                accent = MannaTheme.colors.lavender,
                onClick = it
            )
        }
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.prayers_hub_title),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "subtitle") {
                Text(
                    text = stringResource(R.string.prayers_hub_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MannaTheme.colors.soft,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            categories.forEach { category ->
                item(key = category.title) {
                    CategoryCard(category)
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(category: PrayerCategory) {
    Surface(
        color = MannaTheme.colors.card,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = category.onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(category.accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = category.glyph, fontSize = 24.sp, color = category.accent)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MannaTheme.colors.ink,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = category.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MannaTheme.colors.soft,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
