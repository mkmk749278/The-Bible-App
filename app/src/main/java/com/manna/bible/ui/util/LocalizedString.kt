package com.manna.bible.ui.util

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Resolving sacred text in a specific language, independent of the app/system UI locale.
 *
 * Manna's UI language and Bible language are independent (e.g. an English interface with
 * a Telugu Bible). Recited prayer text — the Rosary prayers, the mysteries, etc. — is
 * scripture-side and should follow the **Bible language**, not the device/UI locale that
 * a plain `stringResource` resolves against. These helpers resolve a string resource in
 * an explicit language via a configuration-overridden [Context], falling back to the
 * default (English) resource when a translation is missing.
 */

/** A [Context] whose resources resolve in [languageTag] (BCP-47, e.g. "te", "hi"). */
@Composable
fun rememberLocalizedContext(languageTag: String): Context {
    val base = LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(languageTag, configuration) {
        if (languageTag.isBlank()) {
            base
        } else {
            val localized = Configuration(configuration).apply {
                setLocale(Locale.forLanguageTag(languageTag))
            }
            base.createConfigurationContext(localized)
        }
    }
}

/** Resolves [id] in [languageTag] (the Bible language), not the app/system UI locale. */
@Composable
fun stringResourceIn(languageTag: String, @StringRes id: Int): String =
    rememberLocalizedContext(languageTag).getString(id)

/**
 * Resolves a formatted string [id] in [languageTag] (the Bible language), substituting
 * [formatArgs] into the resource's positional placeholders (e.g. `%1$s`). Mirrors
 * `stringResource(id, vararg)` but against the Bible-language context rather than the UI
 * locale, so AI-feature surfaces can render templated copy (e.g. "Open %1$s") in the
 * user's chosen Bible language while preserving the placeholder substitution.
 */
@Composable
fun stringResourceIn(languageTag: String, @StringRes id: Int, vararg formatArgs: Any): String =
    rememberLocalizedContext(languageTag).getString(id, *formatArgs)

/** Resolves a string-array [id] in [languageTag] (the Bible language). */
@Composable
fun stringArrayResourceIn(languageTag: String, @ArrayRes id: Int): Array<String> =
    rememberLocalizedContext(languageTag).resources.getStringArray(id)
