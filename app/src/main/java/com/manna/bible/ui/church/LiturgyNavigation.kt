package com.manna.bible.ui.church

import com.manna.bible.domain.FeatureFlags

/**
 * Navigation contract for the Liturgy Library / Detail surfaces, shared between the
 * navigation graph ([com.manna.bible.ui.MannaApp]) and its gating test so the gating is
 * exercised against the real production decision rather than a duplicated copy.
 *
 * The whole surface — its entry point in the More tab and both routes — is gated behind
 * [FeatureFlags.CHURCH_MODE] (Req 13.1, 13.3, 13.4): hidden when the flag is off, exposed
 * when it is on.
 */
object LiturgyNavigation {

    /** The browse (library) route. */
    const val LIBRARY_ROUTE = "liturgy_library"

    /** The detail route, carrying the selected liturgy's id as a nav argument. */
    const val DETAIL_ROUTE = "liturgy_detail/{$LITURGY_ID_ARG}"

    /** Builds the concrete detail route for a given liturgy [id]. */
    fun detailRoute(id: String): String = "liturgy_detail/$id"

    /**
     * Whether the liturgy entry point and routes are exposed, gated through
     * [FeatureFlags.CHURCH_MODE]. Parameterised for testability; defaults to the live flag.
     */
    fun isEntryVisible(churchModeEnabled: Boolean = FeatureFlags.CHURCH_MODE): Boolean =
        churchModeEnabled
}
