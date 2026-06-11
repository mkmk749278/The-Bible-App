package com.manna.bible.domain.attribution

import com.manna.bible.domain.translation.Translation
import javax.inject.Inject

/**
 * Derives the [Attribution] for the active translation (Requirement 12.1, 12.2,
 * 12.4). Pure Kotlin — no Android dependencies.
 */
interface AttributionProvider {
    /** Builds attribution for [translation], or an empty notice when none is active. */
    fun attributionFor(translation: Translation?): Attribution
}

/**
 * Default [AttributionProvider]. A translation is treated as public domain when it
 * is the bundled edition or its name identifies the World English Bible — the
 * public-domain text Manna ships (Req 12.2). Everything else is reported as
 * provided through the Free Use Bible API under its source license (Req 12.4).
 */
class DefaultAttributionProvider @Inject constructor() : AttributionProvider {

    override fun attributionFor(translation: Translation?): Attribution {
        if (translation == null) return Attribution(translationName = null, license = null)
        val license = if (translation.isPublicDomain()) {
            TranslationLicense.PUBLIC_DOMAIN
        } else {
            TranslationLicense.SOURCE_PROVIDED
        }
        return Attribution(translationName = translation.name, license = license)
    }

    private fun Translation.isPublicDomain(): Boolean =
        isBundled || name.contains(WORLD_ENGLISH_BIBLE, ignoreCase = true)

    private companion object {
        const val WORLD_ENGLISH_BIBLE = "World English Bible"
    }
}
