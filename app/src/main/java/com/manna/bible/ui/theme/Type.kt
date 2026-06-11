package com.manna.bible.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily

/**
 * App typography.
 *
 * UI chrome uses the platform sans-serif default (clean, excellent Unicode/Indic
 * coverage). Scripture is rendered in a serif family for a premium, book-like
 * reading rhythm (UX directive: "Typography is the primary design element").
 *
 * We deliberately use the system serif ([ScriptureFontFamily]) rather than a
 * bundled Inter/Noto Serif: it is fully offline, adds no APK weight, requires no
 * Google Play Services (downloadable fonts would), and on most devices the system
 * serif already resolves to Noto Serif with full Indic-script support. Bundled
 * brand fonts can replace this later without touching call sites.
 */
val MannaTypography = Typography()

/** Serif family used for immersive scripture text (reader verses, daily verse). */
val ScriptureFontFamily: FontFamily = FontFamily.Serif
