package com.manna.bible.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalMannaPalette = staticCompositionLocalOf { LightMannaPalette }

/**
 * Accessor for the active [MannaPalette], mirroring `MaterialTheme`'s pattern.
 * Screens use `MannaTheme.colors.<token>` instead of hardcoded values.
 */
object MannaTheme {
    val colors: MannaPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalMannaPalette.current
}

private fun lightSchemeFrom(p: MannaPalette) = lightColorScheme(
    primary = p.gold,
    onPrimary = p.bg,
    background = p.bg,
    onBackground = p.ink,
    surface = p.surface,
    onSurface = p.ink,
    surfaceVariant = p.card,
    onSurfaceVariant = p.soft,
    secondary = p.sage,
    error = p.red,
    outline = p.border,
)

private fun darkSchemeFrom(p: MannaPalette) = darkColorScheme(
    primary = p.gold,
    onPrimary = p.bg,
    background = p.bg,
    onBackground = p.ink,
    surface = p.surface,
    onSurface = p.ink,
    surfaceVariant = p.card,
    onSurfaceVariant = p.soft,
    secondary = p.lavender,
    error = p.red,
    outline = p.border,
)

/**
 * App theme. Light by default per the UX directive ("morning sunlight entering a
 * church"); the dark palette is the dark-mode variant, following the system
 * setting.
 */
@Composable
fun MannaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) DarkMannaPalette else LightMannaPalette
    CompositionLocalProvider(LocalMannaPalette provides palette) {
        MaterialTheme(
            colorScheme = if (darkTheme) darkSchemeFrom(palette) else lightSchemeFrom(palette),
            typography = MannaTypography,
            content = content,
        )
    }
}
