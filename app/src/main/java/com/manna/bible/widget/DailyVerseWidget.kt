package com.manna.bible.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.manna.bible.MainActivity
import com.manna.bible.R
import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.daily.DailyVerseProvider
import com.manna.bible.domain.repository.BibleContentRepository
import com.manna.bible.domain.repository.TranslationRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/** Resolved widget content (already localized, with fallbacks applied). */
private data class WidgetVerse(val title: String, val body: String)

/**
 * Home-screen "Verse of the Day" widget (Jetpack Glance).
 *
 * Renders today's deterministic verse from the active translation and opens the app
 * on tap. Fully offline; dependencies are obtained via a Hilt [WidgetEntryPoint]
 * since `GlanceAppWidget` is instantiated by the framework. Resolution mirrors the
 * in-app daily verse so the widget and Home card always agree.
 */
class DailyVerseWidget : GlanceAppWidget() {

    /** Hilt accessor for the repositories the widget reads at refresh time. */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun dailyVerseProvider(): DailyVerseProvider
        fun bibleContentRepository(): BibleContentRepository
        fun translationRepository(): TranslationRepository
        fun preferencesStore(): PreferencesStore
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val verse = resolve(context)
        provideContent { Content(verse) }
    }

    private suspend fun resolve(context: Context): WidgetVerse {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        val fallbackTitle = context.getString(R.string.widget_title)
        val fallbackBody = context.getString(R.string.widget_tap_hint)

        val ref = entryPoint.dailyVerseProvider().verseForDate(LocalDate.now())
        val translationId = resolveTranslation(entryPoint)
            ?: return WidgetVerse(fallbackTitle, fallbackBody)

        val content = runCatching {
            entryPoint.bibleContentRepository().chapter(translationId, ref.osisId, ref.chapter)
        }.getOrNull()
        val text = content?.verses?.firstOrNull { it.verse == ref.verse }?.text
            ?: return WidgetVerse(fallbackTitle, fallbackBody)

        val bookName = runCatching {
            entryPoint.bibleContentRepository().books(translationId).first()
                .firstOrNull { it.osisId == ref.osisId }?.name
        }.getOrNull() ?: ref.osisId

        return WidgetVerse(title = "$bookName ${ref.chapter}:${ref.verse}", body = text)
    }

    private suspend fun resolveTranslation(entryPoint: WidgetEntryPoint): String? {
        val persistedId = entryPoint.preferencesStore().setupState.first().bibleTranslationId
        if (!persistedId.isNullOrBlank()) return persistedId
        val catalog = runCatching {
            entryPoint.translationRepository().catalog().first()
        }.getOrDefault(emptyList())
        return catalog.firstOrNull { it.isDownloaded }?.id ?: catalog.firstOrNull()?.id
    }

    @Composable
    private fun Content(verse: WidgetVerse) {
        val context = LocalContext.current
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(BackgroundColor))
                .padding(16.dp)
                .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = verse.title,
                style = TextStyle(
                    color = ColorProvider(GoldColor),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            )
            Text(
                text = verse.body,
                style = TextStyle(
                    color = ColorProvider(InkColor),
                    fontSize = 16.sp
                )
            )
        }
    }

    private companion object {
        // Light palette literals (Glance runs outside the app's MannaTheme).
        val BackgroundColor = Color(0xFFFAF7EF)
        val GoldColor = Color(0xFF8A671C)
        val InkColor = Color(0xFF1F2D3D)
    }
}
