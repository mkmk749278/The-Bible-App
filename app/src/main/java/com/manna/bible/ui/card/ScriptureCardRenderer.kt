package com.manna.bible.ui.card

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint

/**
 * A colour scheme for a shareable verse card.
 *
 * @property id Stable id (also used as the selection key).
 * @property background ARGB background colour.
 * @property ink ARGB verse-text colour.
 * @property accent ARGB reference / accent colour.
 */
data class CardTheme(
    val id: String,
    val background: Int,
    val ink: Int,
    val accent: Int
) {
    companion object {
        /** Built-in card palettes (drawn from the app's light/dark design tokens). */
        val ALL: List<CardTheme> = listOf(
            CardTheme("sunrise", background = 0xFFFAF7EF.toInt(), ink = 0xFF1F2D3D.toInt(), accent = 0xFF8A671C.toInt()),
            CardTheme("night", background = 0xFF080C14.toInt(), ink = 0xFFEDE3C8.toInt(), accent = 0xFFC9952A.toInt()),
            CardTheme("sage", background = 0xFF2F4A3C.toInt(), ink = 0xFFF0E8D5.toInt(), accent = 0xFFC9A84C.toInt()),
            CardTheme("navy", background = 0xFF1F2D3D.toInt(), ink = 0xFFFAF7EF.toInt(), accent = 0xFFC9952A.toInt())
        )
    }
}

/**
 * Renders a verse + reference onto a square [Bitmap] for sharing (Scripture Card
 * Generator, Phase 2). Uses Android 2D drawing directly so the preview shown in the
 * app is exactly the image that gets shared.
 */
object ScriptureCardRenderer {

    /** Renders [verseText] / [reference] with [theme] to a [size]×[size] bitmap. */
    fun render(verseText: String, reference: String, theme: CardTheme, size: Int = 1080): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(theme.background)

        val margin = size * 0.12f
        val contentWidth = (size - margin * 2).toInt()

        val versePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.ink
            textSize = size * 0.058f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        }
        val verseLayout = StaticLayout.Builder
            .obtain(verseText, 0, verseText.length, versePaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(size * 0.012f, 1f)
            .build()

        val refPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.accent
            textSize = size * 0.040f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val refLayout = StaticLayout.Builder
            .obtain(reference, 0, reference.length, refPaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .build()

        val gap = size * 0.05f
        val blockHeight = verseLayout.height + gap + refLayout.height
        var top = (size - blockHeight) / 2f

        canvas.save()
        canvas.translate(margin, top)
        verseLayout.draw(canvas)
        canvas.restore()

        top += verseLayout.height + gap
        canvas.save()
        canvas.translate(margin, top)
        refLayout.draw(canvas)
        canvas.restore()

        // A small accent rule between verse and reference.
        val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = theme.accent }
        val ruleY = top - gap / 2f
        canvas.drawRect(size / 2f - size * 0.04f, ruleY, size / 2f + size * 0.04f, ruleY + size * 0.004f, rulePaint)

        // Subtle wordmark.
        val markPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.accent
            textSize = size * 0.028f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            alpha = 160
        }
        canvas.drawText("Manna", margin, size - margin * 0.5f, markPaint)

        return bitmap
    }
}
