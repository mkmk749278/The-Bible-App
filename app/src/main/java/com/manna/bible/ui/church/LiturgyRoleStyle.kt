package com.manna.bible.ui.church

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.manna.bible.R
import com.manna.bible.domain.liturgy.LiturgyPart
import com.manna.bible.domain.liturgy.LiturgyRole
import com.manna.bible.ui.theme.MannaPalette

/**
 * The string resource for a speaker role's label, or null when the role carries **no**
 * speaker label — i.e. [LiturgyRole.RUBRIC], which renders as a plain instruction
 * (Req 7.1, 7.2). Pure (non-`@Composable`) so the role→label totality is unit/property
 * testable on the JVM.
 */
@StringRes
fun liturgyRoleLabelRes(role: LiturgyRole): Int? = when (role) {
    LiturgyRole.PRESIDER -> R.string.church_role_presider
    LiturgyRole.PEOPLE -> R.string.church_role_people
    LiturgyRole.ALL -> R.string.church_role_all
    LiturgyRole.READER -> R.string.church_role_reader
    LiturgyRole.RUBRIC -> null
}

/**
 * The distinct accent color for each speaker role, drawn from the active [palette]
 * (Req 7.3). The four speaking roles — presider, people, all, reader — map to four
 * different palette tokens so they are visually distinguishable; rubrics use the muted
 * token. Pure (takes the palette explicitly) so role-color distinctness is JVM-testable.
 */
fun liturgyRoleColor(role: LiturgyRole, palette: MannaPalette): Color = when (role) {
    LiturgyRole.PRESIDER -> palette.gold
    LiturgyRole.PEOPLE -> palette.sage
    LiturgyRole.ALL -> palette.cyan
    LiturgyRole.READER -> palette.lavender
    LiturgyRole.RUBRIC -> palette.muted
}

/**
 * Whether [Liturgy_Detail][LiturgyDetailScreen] must show the "follow your parish book /
 * Missal" official-text notice for [part]: exactly the parts whose presidential prayer is
 * proper to the day and must never be fabricated by the app (Req 7.4, 12.2). The decision is
 * `part.needsOfficialText` for **every** part — including a bare RUBRIC whose only content
 * is an instruction — so the notice is never dropped on the rubric fast-path (the bug this
 * guards against). Pure (non-`@Composable`) so the notice-presence decision is property-
 * testable on the JVM, and [PartView] consumes this same function rather than re-deciding.
 */
fun shouldShowOfficialNotice(part: LiturgyPart): Boolean = part.needsOfficialText

/**
 * The scripture reference a part's "read in context" action opens, or `null` when the part
 * carries no scripture link (Req 7.5). It is exactly `part.osisRef`: invoking the action
 * calls `onOpenVerse` with this value. Pure (non-`@Composable`) so the osisRef→open-action
 * wiring is property-testable on the JVM, with [ReadInContextAction] consuming the same
 * function so the UI and the test agree by construction.
 */
fun openVerseRefFor(part: LiturgyPart): String? = part.osisRef
