package com.manna.bible.ui.church

import com.manna.bible.domain.FeatureFlags
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Feature-flag gating test for the Liturgy surface (Task 9.2, Req 13.1/13.3/13.4).
 *
 * Exercises the real production gating predicate ([LiturgyNavigation.isEntryVisible]) that
 * the navigation graph uses, asserting the entry point / routes are hidden when
 * `CHURCH_MODE` is off and exposed when it is on — gated through [FeatureFlags].
 */
class LiturgyNavigationGatingTest {

    @Test
    fun `the liturgy entry point is hidden when CHURCH_MODE is disabled`() {
        assertFalse(
            LiturgyNavigation.isEntryVisible(churchModeEnabled = false),
            "liturgy entry must be hidden when CHURCH_MODE is off"
        )
    }

    @Test
    fun `the liturgy entry point is exposed when CHURCH_MODE is enabled`() {
        assertTrue(
            LiturgyNavigation.isEntryVisible(churchModeEnabled = true),
            "liturgy entry must be exposed when CHURCH_MODE is on"
        )
    }

    @Test
    fun `gating defaults to the live FeatureFlags CHURCH_MODE value`() {
        assertEquals(
            FeatureFlags.CHURCH_MODE,
            LiturgyNavigation.isEntryVisible(),
            "the default gating must follow FeatureFlags.CHURCH_MODE"
        )
    }

    @Test
    fun `liturgy routes are defined and the detail route carries the id argument`() {
        assertTrue(LiturgyNavigation.LIBRARY_ROUTE.isNotBlank())
        assertTrue(
            LiturgyNavigation.DETAIL_ROUTE.contains("{$LITURGY_ID_ARG}"),
            "detail route must carry the liturgy id nav argument"
        )
        assertEquals("liturgy_detail/roman_catholic_mass", LiturgyNavigation.detailRoute("roman_catholic_mass"))
    }
}
