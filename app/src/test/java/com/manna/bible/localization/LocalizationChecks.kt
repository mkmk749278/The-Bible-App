package com.manna.bible.localization

/**
 * Pure, test-side logic for the Concern 1 localization inventory / quality checks
 * (Requirements 3 & 4). Kept free of Android dependencies so it can be exercised both by
 * the data-driven [LocalizationInventoryTest] and by the property tests for Property 16
 * (placeholder preservation with review override) and Property 17 (gap-check correctness).
 *
 * The functions here intentionally encode *only* the decision logic; reading the resource
 * XML and the provenance manifest lives in the inventory test.
 */
object LocalizationChecks {

    /**
     * A printf-style format specifier: a positional argument (`%1$s`), a plain conversion
     * (`%d`, `%s`), or an escaped percent (`%%`). These are the tokens whose loss or
     * corruption would break `String.format` / `Context.getString(id, args)`.
     */
    private val FORMAT_TOKEN = Regex("""%%|%\d+\$[a-zA-Z]|%[a-zA-Z]""")

    /**
     * An Android string-resource formatting escape that affects layout: newline (`\n`) or
     * tab (`\t`). After XML parsing these survive as a literal backslash followed by the
     * escaped character, so we match them on the raw resource text. Escaped quotes (`\'`,
     * `\"`) and literal backslashes are content-level XML escaping — they carry no
     * formatting/placeholder semantics and legitimately differ across scripts (a Tamil
     * sentence has no apostrophe), so they are intentionally excluded.
     */
    private val ESCAPE_TOKEN = Regex("""\\[nt]""")

    /**
     * The multiset (token -> occurrence count) of placeholder/format tokens and escape
     * sequences in [value]. Two values "preserve placeholders" exactly when their multisets
     * are equal (Req 4.3). Literal text and XML entities (e.g. `&amp;` -> `&`) are content,
     * not format tokens, and are deliberately excluded.
     */
    fun placeholderTokens(value: String): Map<String, Int> {
        val tokens = ArrayList<String>()
        FORMAT_TOKEN.findAll(value).forEach { tokens += it.value }
        ESCAPE_TOKEN.findAll(value).forEach { tokens += it.value }
        return tokens.groupingBy { it }.eachCount()
    }

    /**
     * Property 16 decision: a translated value is **accepted without a placeholder flag**
     * exactly when its token multiset equals the English Fallback's, OR it is marked
     * human-reviewed. Otherwise (tokens differ and not reviewed) it is flagged invalid.
     */
    fun placeholderAccepted(english: String, translation: String, reviewed: Boolean): Boolean =
        reviewed || placeholderTokens(english) == placeholderTokens(translation)

    /**
     * Property 17 decision: [key] is a localization **gap** for a target locale if and only
     * if it exists in the default `values/` resources, has no value in that locale, and is
     * not recorded as a deferred item for that locale.
     */
    fun isGap(
        key: String,
        defaultKeys: Set<String>,
        localeKeys: Set<String>,
        deferred: Set<String>
    ): Boolean = key in defaultKeys && key !in localeKeys && key !in deferred
}
