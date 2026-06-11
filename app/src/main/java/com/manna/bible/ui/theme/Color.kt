package com.manna.bible.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Role-based color palette for the Manna design system. Screens never use
 * hardcoded hex values — they read the active palette via `MannaTheme.colors`.
 *
 * Two instances exist: [LightMannaPalette] (the default — "morning sunlight
 * entering a church": warm white, soft cream, deep navy, muted gold, sage) and
 * [DarkMannaPalette] (the dark-mode variant). Tokens are roles, not literal hues:
 *
 * @property bg App background.
 * @property surface Elevated surfaces (top bars, sheets).
 * @property card Cards and the spoken-verse highlight; must read against [bg].
 * @property border Hairline dividers and outlines.
 * @property gold Primary sacred accent; meets 4.5:1 contrast on [bg] as text.
 * @property goldDim Subdued decorative accent.
 * @property ink Primary text color.
 * @property muted Tertiary/disabled text.
 * @property soft Secondary text; meets 4.5:1 contrast on [bg] and [card].
 * @property lavender Notes/secondary semantic accent.
 * @property sage Success / downloaded state.
 * @property red Errors and destructive actions.
 * @property cyan Informational accent.
 * @property orange Warnings / in-progress state.
 */
@Immutable
data class MannaPalette(
    val bg: Color,
    val surface: Color,
    val card: Color,
    val border: Color,
    val gold: Color,
    val goldDim: Color,
    val ink: Color,
    val muted: Color,
    val soft: Color,
    val lavender: Color,
    val sage: Color,
    val red: Color,
    val cyan: Color,
    val orange: Color
)

/** Default palette — warm, light, calm (UX directive: light-first). */
val LightMannaPalette = MannaPalette(
    bg = Color(0xFFFAF7EF),
    surface = Color(0xFFF5EFE2),
    card = Color(0xFFF0E8D5),
    border = Color(0xFFE0D6BF),
    gold = Color(0xFF8A671C),
    goldDim = Color(0xFFC9A84C),
    ink = Color(0xFF1F2D3D),
    muted = Color(0xFF8595A3),
    soft = Color(0xFF5B6B7A),
    lavender = Color(0xFF6F5FA8),
    sage = Color(0xFF3F7A5C),
    red = Color(0xFFB03A30),
    cyan = Color(0xFF1E7D8C),
    orange = Color(0xFFA85F28)
)

/** Dark-mode variant of the palette. */
val DarkMannaPalette = MannaPalette(
    bg = Color(0xFF080C14),
    surface = Color(0xFF0D1420),
    card = Color(0xFF111C2C),
    border = Color(0xFF1A2A40),
    gold = Color(0xFFC9952A),
    goldDim = Color(0xFF7A5810),
    ink = Color(0xFFEDE3C8),
    muted = Color(0xFF4A6A80),
    soft = Color(0xFF7A9AB0),
    lavender = Color(0xFF8B7EC8),
    sage = Color(0xFF4A8A6A),
    red = Color(0xFFC0453A),
    cyan = Color(0xFF2A9AB0),
    orange = Color(0xFFC07030)
)
