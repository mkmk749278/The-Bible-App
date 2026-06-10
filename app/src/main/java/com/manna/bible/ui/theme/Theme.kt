package com.manna.bible.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val MannaDarkColors = darkColorScheme(
    primary = MannaColors.gold,
    onPrimary = MannaColors.bg,
    background = MannaColors.bg,
    onBackground = MannaColors.cream,
    surface = MannaColors.surface,
    onSurface = MannaColors.cream,
    secondary = MannaColors.lavender,
    error = MannaColors.red,
)

private val MannaLightColors = lightColorScheme(
    primary = MannaColors.goldDim,
    background = MannaColors.cream,
    surface = MannaColors.cream,
    error = MannaColors.red,
)

/**
 * App theme. Dark by default per the design system; light mode available.
 */
@Composable
fun MannaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) MannaDarkColors else MannaLightColors,
        typography = MannaTypography,
        content = content,
    )
}
