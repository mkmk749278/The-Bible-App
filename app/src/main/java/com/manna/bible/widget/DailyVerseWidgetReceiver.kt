package com.manna.bible.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * Broadcast receiver that hosts the [DailyVerseWidget]. Registered in the manifest
 * with the app-widget provider metadata; the framework instantiates it to bind and
 * update the widget.
 */
class DailyVerseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DailyVerseWidget()
}
