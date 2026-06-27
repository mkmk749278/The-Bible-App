package com.manna.bible.ui.church

import com.manna.bible.data.preferences.PreferencesStore
import com.manna.bible.domain.liturgy.Liturgy
import com.manna.bible.domain.liturgy.LiturgyPart
import com.manna.bible.domain.liturgy.LiturgyProvider
import com.manna.bible.domain.liturgy.LiturgyRole
import com.manna.bible.domain.liturgy.LiturgySection
import com.manna.bible.domain.liturgy.LocalizedText
import com.manna.bible.domain.model.CanonProfile
import com.manna.bible.domain.model.CanonType
import com.manna.bible.domain.model.Denomination
import com.manna.bible.domain.model.NumberingScheme
import com.manna.bible.domain.model.SetupState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Builds a minimal but complete [Liturgy] for ViewModel tests. */
internal fun testLiturgy(
    id: String,
    title: Map<String, String> = mapOf("en" to "Title $id"),
    tradition: String = "Tradition $id",
    denominations: List<Denomination> = emptyList()
): Liturgy = Liturgy(
    id = id,
    title = LocalizedText(title),
    tradition = tradition,
    sections = listOf(
        LiturgySection(
            LocalizedText(mapOf("en" to "Section")),
            listOf(LiturgyPart(role = LiturgyRole.PEOPLE, text = LocalizedText(mapOf("en" to "Amen."))))
        )
    ),
    sourceNote = LocalizedText(mapOf("en" to "Source $id")),
    denominations = denominations,
    languages = title.keys
)

/**
 * A fixed-content [LiturgyProvider] for ViewModel tests: [forDenomination] surfaces the
 * mapped ids first (mirroring [com.manna.bible.domain.liturgy.AssetLiturgyProvider]), then
 * the rest; [defaultFor] returns the first mapped, present liturgy or null.
 */
internal class FakeLiturgyProvider(
    private val liturgies: List<Liturgy>,
    private val mapping: Map<Denomination, List<String>> = emptyMap()
) : LiturgyProvider {
    override fun all(): List<Liturgy> = liturgies

    override fun defaultFor(denomination: Denomination): Liturgy? =
        mapping[denomination].orEmpty().firstNotNullOfOrNull { id -> liturgies.firstOrNull { it.id == id } }

    override fun forDenomination(denomination: Denomination): List<Liturgy> {
        if (denomination == Denomination.SHOW_EVERYTHING) return liturgies
        val mappedIds = mapping[denomination].orEmpty()
        val mapped = mappedIds.mapNotNull { id -> liturgies.firstOrNull { it.id == id } }
        val mappedSet = mapped.map { it.id }.toSet()
        return mapped + liturgies.filter { it.id !in mappedSet }
    }
}

/** A [PreferencesStore] whose setup state carries a fixed denomination + Bible language. */
internal class FakeLiturgyPreferencesStore(
    denomination: Denomination?,
    bibleLanguage: String = "en"
) : PreferencesStore {
    private val state = MutableStateFlow(
        SetupState(
            denomination = denomination,
            canonType = CanonType.PROTESTANT_66,
            uiLanguage = "en",
            bibleLanguage = bibleLanguage,
            numberingScheme = NumberingScheme.MASORETIC,
            namingConventionId = null,
            bibleTranslationId = "web",
            lectionaryId = null,
            showDeuterocanonical = false,
            setupCompleted = true
        )
    )
    override val setupState: Flow<SetupState> = state
    override val lastReadPosition: Flow<String?> = MutableStateFlow(null)
    override suspend fun saveSetup(state: SetupState) {}
    override suspend fun setSetupCompleted(value: Boolean) {}
    override suspend fun updateDenomination(profile: CanonProfile) {}
    override suspend fun setShowDeuterocanonical(value: Boolean) {}
    override suspend fun setActiveTranslation(translationId: String) {}
    override suspend fun setLastReadPosition(ref: String) {}
}
