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
 * Default [AttributionProvider]. Public-domain status is asserted from the *text*,
 * not from whether it is bundled: Manna ships the public-domain World English Bible
 * **and** the Indian Revised Version family (Tamil/Hindi/Telugu/Malayalam), and the
 * IRV editions are licensed (CC BY-SA), not public domain. So only translations the
 * app positively recognizes as public domain (the WEB) are labeled
 * [TranslationLicense.PUBLIC_DOMAIN] (Req 12.2); every other edition — bundled or
 * downloaded — is reported as provided through the Free Use Bible API under its
 * source license (Req 12.4), which keeps the attribution honest for share-alike texts.
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
        name.contains(WORLD_ENGLISH_BIBLE, ignoreCase = true)

    private companion object {
        const val WORLD_ENGLISH_BIBLE = "World English Bible"
    }
}
