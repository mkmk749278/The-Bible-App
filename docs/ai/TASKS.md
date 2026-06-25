# Gemini AI Features — Task Breakdown

> **Status**: Active  
> **Prerequisite reading**: [ROADMAP.md](ROADMAP.md)  
> **Last updated**: 2026-06-25

Each task has a unique ID, its parent phase, a precise scope statement, file-level
acceptance criteria, and a testing requirement. Tasks within a phase are ordered by
dependency — complete them top-to-bottom unless they are marked `[PARALLEL]`.

---

## Task Status Key

| Symbol | Meaning |
|--------|---------|
| `[ ]` | Not started |
| `[~]` | In progress |
| `[x]` | Complete |
| `[!]` | Blocked |

---

## Phase A — Foundation

### A-01 · [x] Add `denomination` field to `ExplanationRequest` `[PARALLEL]`

**Files changed**:
- `app/src/main/java/com/manna/bible/domain/explain/Explanation.kt`

**Scope**: Add `denomination: Denomination? = null` to `ExplanationRequest`. No other
change. The default `null` keeps every existing call site compiling without modification.

**Acceptance criteria**:
- `ExplanationRequest` constructor has a `denomination` parameter with default `null`.
- All existing call sites compile without any change (confirmed by `./gradlew compileDebugKotlin`).
- `ExplanationUnavailableReason`, `ExplanationResult`, and `ExplanationEngine` are untouched.

**Tests**: No new test required for this mechanical change. Covered by A-03 and A-04.

---

### A-02 · [x] Update `DefaultExplanationRepository` cache key `[PARALLEL with A-01]`

**Files changed**:
- `app/src/main/java/com/manna/bible/data/explain/DefaultExplanationRepository.kt`

**Scope**: Change `cacheKey()` to append `|${denomination?.id ?: "any"}`.

**Before**:
```kotlin
"${request.osisRef}|${request.uiLanguageCode}|${request.depth.name}"
```

**After**:
```kotlin
"${request.osisRef}|${request.uiLanguageCode}|${request.depth.name}|${request.denomination?.id ?: "any"}"
```

**Acceptance criteria**:
- Two requests for the same verse with different denominations produce different cache keys.
- A request with `denomination = null` produces key ending in `|any`.
- Old cached rows in the `explanations` table are not corrupted — they simply miss the
  new-key lookup and are regenerated on next tap (safe stale-key behaviour).

**Tests**: Add to `DefaultExplanationRepositoryTest` (or create it):

```kotlin
@Test fun `cache key includes denomination`() {
    val catholic = ExplanationRequest("GEN.1.1", "Genesis 1:1", "In the beginning...",
        "te", ExplainDepth.PLAIN, Denomination.CATHOLIC)
    val protestant = catholic.copy(denomination = Denomination.PROTESTANT_OTHER)
    assertNotEquals(cacheKeyOf(catholic), cacheKeyOf(protestant))
}

@Test fun `null denomination uses 'any' suffix`() {
    val req = ExplanationRequest("GEN.1.1", "Genesis 1:1", "text", "en",
        ExplainDepth.PLAIN, null)
    assertTrue(cacheKeyOf(req).endsWith("|any"))
}
```

---

### A-03 · [x] Rewrite `ExplanationPrompt.build()` with cultural and denominational grounding

**Files changed**:
- `app/src/main/java/com/manna/bible/domain/explain/ExplanationPrompt.kt`

**Scope**: Implement the `build()` and `buildCulturalInstruction()` functions exactly
as specified in `IMPLEMENTATION.md` § F-01 Step 2. The function signature must remain:

```kotlin
fun build(request: ExplanationRequest): String
```

**Acceptance criteria**:
- For any denomination, the prompt contains "India" and a reference to Indian daily life.
- For `CATHOLIC`, the prompt contains "sacramental".
- For `ORTHODOX`, the prompt contains "patristic" or "liturgical".
- For `CSI`, the prompt contains "Church of South India".
- For `MAR_THOMA`, the prompt contains "Mar Thoma".
- For `PROTESTANT_OTHER` or `null`, no denomination-specific block is present.
- The phrase "do not invent facts" is present in all prompts.
- The `uiLanguageCode` ISO instruction is present in all prompts.

**Tests**: Create `app/src/test/java/com/manna/bible/domain/explain/ExplanationPromptTest.kt`

```kotlin
package com.manna.bible.domain.explain

import com.manna.bible.domain.model.Denomination
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ExplanationPromptTest {

    private fun req(denomination: Denomination?, langCode: String = "te") =
        ExplanationRequest(
            osisRef = "JHN.3.16", reference = "John 3:16",
            passageText = "For God so loved the world...",
            uiLanguageCode = langCode, depth = ExplainDepth.PLAIN,
            denomination = denomination
        )

    @Test fun `all prompts contain India cultural grounding`() {
        Denomination.entries.forEach { den ->
            val prompt = ExplanationPrompt.build(req(den))
            assertTrue(prompt.contains("India", ignoreCase = true),
                "Expected 'India' in prompt for $den")
        }
    }

    @Test fun `null denomination contains cultural grounding but no denomination block`() {
        val prompt = ExplanationPrompt.build(req(null))
        assertTrue(prompt.contains("India", ignoreCase = true))
        assertFalse(prompt.contains("sacramental", ignoreCase = true))
        assertFalse(prompt.contains("patristic", ignoreCase = true))
    }

    @Test fun `Catholic denomination includes sacramental note`() {
        val prompt = ExplanationPrompt.build(req(Denomination.CATHOLIC))
        assertTrue(prompt.contains("sacramental", ignoreCase = true))
    }

    @Test fun `Orthodox denomination includes patristic or liturgical`() {
        val prompt = ExplanationPrompt.build(req(Denomination.ORTHODOX))
        val hasEither = prompt.contains("patristic", ignoreCase = true) ||
                        prompt.contains("liturgical", ignoreCase = true)
        assertTrue(hasEither)
    }

    @Test fun `CSI denomination names Church of South India`() {
        val prompt = ExplanationPrompt.build(req(Denomination.CSI))
        assertTrue(prompt.contains("Church of South India", ignoreCase = true))
    }

    @Test fun `Mar Thoma denomination names Mar Thoma`() {
        val prompt = ExplanationPrompt.build(req(Denomination.MAR_THOMA))
        assertTrue(prompt.contains("Mar Thoma", ignoreCase = true))
    }

    @Test fun `language code ISO instruction present`() {
        val prompt = ExplanationPrompt.build(req(null, "ml"))
        assertTrue(prompt.contains("'ml'"))
    }

    @Test fun `do not invent facts present`() {
        val prompt = ExplanationPrompt.build(req(null))
        assertTrue(prompt.contains("do not invent facts", ignoreCase = true))
    }
}
```

---

### A-04 · [x] Pass `denomination` from `ReaderViewModel` to `ExplanationRequest`

**Files changed**:
- `app/src/main/java/com/manna/bible/ui/reader/ReaderViewModel.kt`

**Scope**: In the coroutine that builds `ExplanationRequest`, add `denomination = setup.denomination`.
The `setup` variable is already in scope from the `preferencesStore.setupState.first()` call.

**Acceptance criteria**:
- `ExplanationRequest` constructed in `ReaderViewModel` has `denomination` set to the value
  from `SetupState.denomination`.
- No other ViewModel logic changes.

**Tests**: In `ReaderViewModelTest`, update any `ExplanationRequest` equality assertions to
account for the new field. Fake the store with `denomination = Denomination.CATHOLIC` and
assert the captured request contains that denomination.

---

### A-05 · [x] Define `PersecutionCategory` and `DefaultPersecutionCompanion` `[PARALLEL with A-01]`

**Files changed** (new):
- `app/src/main/java/com/manna/bible/domain/crisis/PersecutionCompanion.kt`

**Scope**: Implement `PersecutionCategory` enum and `DefaultPersecutionCompanion` exactly
as specified in `IMPLEMENTATION.md` § F-06 Step 2, with the full curated verse sets for
all five categories.

**Acceptance criteria**:
- All five `PersecutionCategory` values return a non-empty `List<ReadingRef>`.
- All `ReadingRef` values use OSIS IDs from the 66-book Protestant canon (no deuterocanon refs).
- `versesFor(FAMILY_REJECTION)` contains `ReadingRef("MAT", 10, 34)`.
- `versesFor(PHYSICAL_DANGER)` contains `ReadingRef("ISA", 43, 2)`.
- `versesFor(SOCIAL_EXCLUSION)` contains `ReadingRef("GAL", 3, 28)`.
- `versesFor(FAITH_CRISIS)` contains `ReadingRef("MRK", 9, 24)`.

**Tests**: Create `app/src/test/java/com/manna/bible/domain/crisis/PersecutionCompanionTest.kt`

```kotlin
package com.manna.bible.domain.crisis

import com.manna.bible.domain.usecase.ReadingRef
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PersecutionCompanionTest {

    private val companion = DefaultPersecutionCompanion()
    private val sixtyBookCanon = setOf(
        "GEN","EXO","LEV","NUM","DEU","JOS","JDG","RUT","1SA","2SA","1KI","2KI",
        "1CH","2CH","EZR","NEH","EST","JOB","PSA","PRO","ECC","SNG","ISA","JER",
        "LAM","EZK","DAN","HOS","JOL","AMO","OBA","JON","MIC","NAH","HAB","ZEP",
        "HAG","ZEC","MAL","MAT","MRK","LUK","JHN","ACT","ROM","1CO","2CO","GAL",
        "EPH","PHP","COL","1TH","2TH","1TI","2TI","TIT","PHM","HEB","JAS","1PE",
        "2PE","1JN","2JN","3JN","JUD","REV"
    )

    @Test fun `all categories return non-empty lists`() {
        PersecutionCategory.entries.forEach { cat ->
            assertTrue(companion.versesFor(cat).isNotEmpty(), "Empty list for $cat")
        }
    }

    @Test fun `all refs are within 66-book canon`() {
        PersecutionCategory.entries.forEach { cat ->
            companion.versesFor(cat).forEach { ref ->
                assertTrue(ref.osisId in sixtyBookCanon,
                    "${ref.osisId} in $cat is not in the 66-book canon")
            }
        }
    }

    @Test fun `family rejection contains Matthew 10 verse 34`() {
        assertTrue(companion.versesFor(PersecutionCategory.FAMILY_REJECTION)
            .any { it.osisId == "MAT" && it.chapter == 10 && it.verse == 34 })
    }

    @Test fun `physical danger contains Isaiah 43 verse 2`() {
        assertTrue(companion.versesFor(PersecutionCategory.PHYSICAL_DANGER)
            .any { it.osisId == "ISA" && it.chapter == 43 && it.verse == 2 })
    }

    @Test fun `social exclusion contains Galatians 3 verse 28`() {
        assertTrue(companion.versesFor(PersecutionCategory.SOCIAL_EXCLUSION)
            .any { it.osisId == "GAL" && it.chapter == 3 && it.verse == 28 })
    }

    @Test fun `faith crisis contains Mark 9 verse 24`() {
        assertTrue(companion.versesFor(PersecutionCategory.FAITH_CRISIS)
            .any { it.osisId == "MRK" && it.chapter == 9 && it.verse == 24 })
    }
}
```

---

### A-06 · [x] Bind `PersecutionCompanion` in DI; add `PERSECUTION_COMFORT` feature flag

**Files changed**:
- `app/src/main/java/com/manna/bible/domain/FeatureFlags.kt`
- `app/src/main/java/com/manna/bible/di/BindingsModule.kt`

**Scope**:
1. Add `const val PERSECUTION_COMFORT: Boolean = true` to `FeatureFlags`.
2. Add `@Binds abstract fun bindPersecutionCompanion(impl: DefaultPersecutionCompanion): PersecutionCompanion`
   to `BindingsModule`.

**Tests**: `./gradlew compileDebugKotlin` is sufficient — DI wiring is validated at
compile time via Hilt.

---

### A-07 · [x] Extend `CrisisModeViewModel` and `CrisisModeScreen` for persecution categories

**Files changed**:
- `app/src/main/java/com/manna/bible/ui/crisis/CrisisModeViewModel.kt`
- `app/src/main/java/com/manna/bible/ui/crisis/CrisisModeScreen.kt`

**Scope**: Inject `PersecutionCompanion`. Add `selectedPersecutionCategory`,
`persecutionVerses`, `isPersecutionLoading` to `CrisisUiState`. Add
`selectPersecutionCategory(category: PersecutionCategory)` to the ViewModel — this
function loads the verses for the category, resolves their text from the active
translation (using the same `bibleContentRepository.chapter()` pattern as the existing
`load()`), and emits the resolved verse list.

In `CrisisModeScreen`, under `FeatureFlags.PERSECUTION_COMFORT`, render five
horizontally scrollable `FilterChip` components (one per category). Tapping one calls
`vm.selectPersecutionCategory(it)`. The resolved verse list appears below the chips.

**Acceptance criteria**:
- Tapping `FAMILY_REJECTION` chip shows Matthew 10:34 text from the active translation.
- Category is deselected (and list cleared) by tapping the selected chip again.
- The existing curated comfort list is always visible regardless of category selection.
- With airplane mode on: all five categories work, chips are responsive.

**Tests**: Add to `CrisisModeViewModelTest`:
```kotlin
@Test fun `selectPersecutionCategory resolves verse texts`()
@Test fun `selecting same category again clears selection`()
```

---

## Phase B — Crisis AI + Oral AI

### B-01 · [x] Define `CrisisAiEngine` interface and `CrisisAiResult` sealed class

**Files changed** (new):
- `app/src/main/java/com/manna/bible/domain/crisis/CrisisAiEngine.kt`

**Scope**: Pure Kotlin domain contract as specified in `IMPLEMENTATION.md` § F-03 Step 2.
No Android imports. No Hilt annotations.

**Tests**: None required — interface-only file. Covered by B-03.

---

### B-02 · [x] Add `CRISIS_AI_COMPANION` feature flag

**Files changed**:
- `app/src/main/java/com/manna/bible/domain/FeatureFlags.kt`

**Scope**: Add `const val CRISIS_AI_COMPANION: Boolean = false`.

---

### B-03 · [x] Implement `GeminiCrisisEngine`

**Files changed** (new):
- `app/src/main/java/com/manna/bible/data/crisis/GeminiCrisisEngine.kt`

**Scope**: Exact implementation from `IMPLEMENTATION.md` § F-03 Step 3. Key invariants:
- `situation` text must not appear in any logged string or exception message.
- Night prompt prefix applied when `isNight = true`.
- `parseResponse()` must return `CrisisAiResult.Unavailable` — never throw — on malformed output.
- `IOException` maps to `CrisisAiResult.Offline`.
- `CancellationException` is re-thrown (never swallowed).

**Tests**: Create `app/src/test/java/com/manna/bible/data/crisis/GeminiCrisisEngineTest.kt`

```kotlin
class GeminiCrisisEngineTest {

    @Test fun `unconfigured engine returns Unavailable`()
    @Test fun `valid response parsed into Success`()
    @Test fun `missing REF line returns Unavailable with parse message`()
    @Test fun `IOException returns Offline`()
    @Test fun `night=true prompt contains night window prefix`()
    @Test fun `denomination included in prompt for Catholic`()
    @Test fun `situation text not present in Unavailable reason on parse failure`()
}
```

Use a `FakeGeminiApi` that returns canned `GeminiResponseDto` objects. Do not use Mockito.

---

### B-04 · [x] Bind `GeminiCrisisEngine` in DI

**Files changed**:
- `app/src/main/java/com/manna/bible/di/BindingsModule.kt`

**Scope**: Add `@Binds abstract fun bindCrisisAiEngine(impl: GeminiCrisisEngine): CrisisAiEngine`.

---

### B-05 · [x] Extend `CrisisModeViewModel` for AI companion

**Files changed**:
- `app/src/main/java/com/manna/bible/ui/crisis/CrisisModeViewModel.kt`

**Scope**: Add `situationText`, `aiResponse`, `isAiLoading`, `aiConfigured` to
`CrisisUiState`. Inject `CrisisAiEngine` and `PreferencesStore`. Implement
`updateSituation()`, `submitSituation()`, `clearAiResponse()` per `IMPLEMENTATION.md`
§ F-03 Step 5.

**Privacy invariant**: After `crisisAiEngine.respond()` returns, the `situation`
variable must be out of scope. Do not copy it to any field on the ViewModel or state.

**Tests**: Create `app/src/test/java/com/manna/bible/ui/crisis/CrisisModeViewModelTest.kt`

```kotlin
class CrisisModeViewModelTest {

    @Test fun `submitSituation emits loading then success`()
    @Test fun `submitSituation when not configured is no-op`()
    @Test fun `clearAiResponse resets situationText and aiResponse`()
    @Test fun `offline result emits Offline state`()
    @Test fun `situationText is not stored in state after submit`()
        // Assert uiState.situationText == "" after successful submit
}
```

---

### B-06 · [x] Update `CrisisModeScreen` for AI companion UI

**Files changed**:
- `app/src/main/java/com/manna/bible/ui/crisis/CrisisModeScreen.kt`
- `app/src/main/res/values/strings.xml`
- (All translated `values-*/strings.xml` where the crisis screen strings exist)

**Scope**: Under `FeatureFlags.CRISIS_AI_COMPANION`, add the text field, AI response
card, and offline hint as specified in `IMPLEMENTATION.md` § F-03 Step 6.

**String resources to add**:
- `crisis_ai_placeholder` — "Tell me what's happening…"
- `crisis_ai_offline_hint` — "AI response needs internet. The verses below work offline."

---

### B-07 · [x] Add `isLanguageAvailable()` to `SpeechEngine` and `AndroidSpeechEngine`

**Files changed**:
- `app/src/main/java/com/manna/bible/domain/audio/SpeechEngine.kt`
- `app/src/main/java/com/manna/bible/audio/AndroidSpeechEngine.kt`

**Scope**: Add `fun isLanguageAvailable(languageTag: String): Boolean` to the
`SpeechEngine` interface. Implement in `AndroidSpeechEngine` using
`TextToSpeech.isLanguageAvailable()`. Return `false` if TTS is not yet initialised.

**Acceptance criteria**:
- Calling with `"te"` returns `false` on a device with no Telugu TTS voice installed.
- Calling with `"en"` returns `true` on any standard Android device.

**Tests**: This requires an instrumented test or a manual verification. Document the
manual test: install app on a device without Telugu TTS; confirm speak button greyed.

---

### B-08 · [x] Add `ORAL_AI_EXPLANATION` flag + speak/stop to `ReaderViewModel`

**Files changed**:
- `app/src/main/java/com/manna/bible/domain/FeatureFlags.kt`
- `app/src/main/java/com/manna/bible/ui/reader/ReaderViewModel.kt`

**Scope**:
1. Add `const val ORAL_AI_EXPLANATION: Boolean = false` to `FeatureFlags`.
2. Inject `SpeechEngine` into `ReaderViewModel`.
3. Add `explanationSpeaking: Boolean = false` and `canSpeakExplanation: Boolean = false`
   to `ReaderUiState`.
4. Implement `speakExplanation()` and `stopExplanation()` per `IMPLEMENTATION.md` § F-02
   Step 3.
5. Resolve `canSpeakExplanation` during the setup observation coroutine.

**Acceptance criteria**:
- `speakExplanation()` calls `speechEngine.speak()` with the Bible language tag.
- `explanationSpeaking` flips `true` on start and `false` on completion or stop.
- `stopExplanation()` calls `speechEngine.stop()`.

**Tests**: Add to `ReaderViewModelTest` using a `FakeSpeechEngine`:
```kotlin
@Test fun `speakExplanation sets explanationSpeaking true then false`()
@Test fun `stopExplanation calls speechEngine stop`()
@Test fun `speakExplanation is no-op when flag off`()
```

---

### B-09 · [x] Add speak button to explanation sheet in `ReaderScreen`

**Files changed**:
- `app/src/main/java/com/manna/bible/ui/reader/ReaderScreen.kt`

**Scope**: Under `FeatureFlags.ORAL_AI_EXPLANATION`, add the `IconButton` and
language-unavailable hint text as specified in `IMPLEMENTATION.md` § F-02 Step 5.

---

## Phase C — AI Sermon Builder

### C-01 · [x] Define sermon AI domain contracts

**Files changed** (new):
- `app/src/main/java/com/manna/bible/domain/sermon/SermonAiEngine.kt`

**Scope**: `CongregationType`, `SermonOutlineRequest`, `SermonOutlineResult`,
`SermonAiEngine` interface. Pure Kotlin. No Android dependency.

---

### C-02 · [x] Add `SERMON_AI_BUILDER` flag

**Files changed**:
- `app/src/main/java/com/manna/bible/domain/FeatureFlags.kt`

Add `const val SERMON_AI_BUILDER: Boolean = false`.

---

### C-03 · [x] Implement `GeminiSermonEngine`

**Files changed** (new):
- `app/src/main/java/com/manna/bible/data/sermon/GeminiSermonEngine.kt`

**Scope**: Exact implementation from `IMPLEMENTATION.md` § F-04 Step 3.

**Tests**: Create `app/src/test/java/com/manna/bible/data/sermon/GeminiSermonEngineTest.kt`

```kotlin
class GeminiSermonEngineTest {

    @Test fun `unconfigured engine returns Unavailable`()
    @Test fun `success response returned as Success`()
    @Test fun `IOException returns Offline`()
    @Test fun `Catholic denomination included in prompt`()
    @Test fun `GRIEF congregation type included in prompt`()
    @Test fun `outline format instruction present in prompt`()
}
```

---

### C-04 · [x] Bind `GeminiSermonEngine` in DI

**Files changed**:
- `app/src/main/java/com/manna/bible/di/BindingsModule.kt`

Add `@Binds abstract fun bindSermonAiEngine(impl: GeminiSermonEngine): SermonAiEngine`.

---

### C-05 · [x] Refactor `SermonHelperViewModel` to MutableStateFlow pattern

**Files changed**:
- `app/src/main/java/com/manna/bible/ui/sermon/SermonHelperViewModel.kt`

**Scope**: Convert the `stateIn(combine(...))` pattern to a `MutableStateFlow` pattern
to support `_uiState.update { }` calls. The sermon list must still be observed reactively
from the repository. Maintain all existing public functions with identical behaviour.

**Tests**: All existing `SermonHelperViewModelTest` tests must pass unchanged after this
refactor. Do not delete or modify the existing tests — use them as a regression harness.

---

### C-06 · [x] Add `generateOutline()` and congregation type selector to `SermonHelperViewModel`

**Files changed**:
- `app/src/main/java/com/manna/bible/ui/sermon/SermonHelperViewModel.kt`

**Scope**: Inject `SermonAiEngine` and `PreferencesStore`. Add `congregationType`,
`isGeneratingOutline`, `outlineError`, `canGenerateOutline` to `SermonHelperUiState`.
Implement `selectCongregationType()` and `generateOutline()` per `IMPLEMENTATION.md`
§ F-04 Step 5.

**Tests**: Add to `SermonHelperViewModelTest`:
```kotlin
@Test fun `generateOutline inserts outline into draft content`()
@Test fun `generateOutline sets isGeneratingOutline true during call`()
@Test fun `generateOutline when offline sets outlineError`()
@Test fun `generateOutline is no-op when flag off`()
@Test fun `canGenerateOutline false when reference blank`()
```

---

### C-07 · [x] Update `SermonHelperScreen` with congregation selector and Build outline button

**Files changed**:
- `app/src/main/java/com/manna/bible/ui/sermon/SermonHelperScreen.kt`

**Scope**: Under `FeatureFlags.SERMON_AI_BUILDER`, add the congregation type segmented
control below the reference field and the "Build outline" button (visible only when
`uiState.canGenerateOutline`). Show an inline progress indicator while `isGeneratingOutline`.
Show `outlineError` as a `Snackbar`.

---

## Phase D — Context-Aware Verse Cards

### D-01 · [x] Define `VerseRecommendationEngine` interface

**Files changed** (new):
- `app/src/main/java/com/manna/bible/domain/share/VerseRecommendationEngine.kt`

**Scope**: `VerseRecommendationRequest`, `VerseRecommendation`, `VerseRecommendationEngine`
interface. Pure Kotlin.

---

### D-02 · [x] Add `VERSE_RECOMMENDATION_AI` flag

**Files changed**:
- `app/src/main/java/com/manna/bible/domain/FeatureFlags.kt`

Add `const val VERSE_RECOMMENDATION_AI: Boolean = false`.

---

### D-03 · [x] Implement `GeminiVerseRecommendationEngine`

**Files changed** (new):
- `app/src/main/java/com/manna/bible/data/share/GeminiVerseRecommendationEngine.kt`

**Scope**: Inject `GeminiApi`, `@Named("geminiApiKey") String`,
`BibleContentRepository`, `TranslationRepository`, `PreferencesStore`.

Prompt instructs Gemini to return:
```
REF: <OSIS_ID>.<chapter>.<verse>
DISPLAY: <Book> <chapter>:<verse>
MESSAGE: <2-sentence personal note>
```

After parsing, call `bibleContentRepository.chapter(translationId, osisId, chapter)` to
resolve the verse text. If the verse is not found in the active translation, return
`VerseRecommendation.Unavailable("Verse not found in active translation. Try a different description.")`.

**Tests**: Create `app/src/test/java/com/manna/bible/data/share/GeminiVerseRecommendationEngineTest.kt`

```kotlin
@Test fun `valid response resolves verse text from repository`()
@Test fun `verse not found in translation returns Unavailable`()
@Test fun `IOException returns Offline`()
@Test fun `malformed response returns Unavailable`()
```

Use `FakeGeminiApi` and `FakeBibleContentRepository`.

---

### D-04 · [x] Bind `GeminiVerseRecommendationEngine` in DI

**Files changed**:
- `app/src/main/java/com/manna/bible/di/BindingsModule.kt`

---

### D-05 · [x] Implement `VerseRecommendationViewModel`

**Files changed** (new):
- `app/src/main/java/com/manna/bible/ui/card/VerseRecommendationViewModel.kt`

**Scope**: `VerseRecommendationUiState` with `situationText`, `result`, `isLoading`,
`engineConfigured`. Functions: `onSituationChange(text)`, `recommend()`, `clear()`.

**Tests**: Create `app/src/test/java/com/manna/bible/ui/card/VerseRecommendationViewModelTest.kt`

---

### D-06 · [x] Implement `VerseRecommendationScreen` and navigation entry point

**Files changed** (new):
- `app/src/main/java/com/manna/bible/ui/card/VerseRecommendationScreen.kt`

**Files changed**:
- `app/src/main/java/com/manna/bible/ui/MannaApp.kt` (new route)
- `app/src/main/java/com/manna/bible/ui/card/VerseCardSheet.kt` (entry point button)

**Scope**: Under `FeatureFlags.VERSE_RECOMMENDATION_AI`, add a "Find a verse to share"
button in `VerseCardSheet`. The screen itself: text field → Recommend button →
result card → share button using `ScriptureCardRenderer`.

---

## Phase E — Flag Promotion

### E-01 · [x] Promote `ORAL_AI_EXPLANATION` to `true`

**Prerequisite**: Phase B QA sign-off documented in the PR description.

---

### E-02 · [x] Promote `CRISIS_AI_COMPANION` to `true`

**Prerequisite**: Phase B QA sign-off. Privacy audit of `GeminiCrisisEngine` completed
(confirm no situation text in logs, Room, or DataStore).

---

### E-03 · [x] Promote `SERMON_AI_BUILDER` to `true`

**Prerequisite**: Phase C QA sign-off.

---

### E-04 · [x] Promote `VERSE_RECOMMENDATION_AI` to `true`

**Prerequisite**: Phase D QA sign-off.

---

## Dependency Graph

```
A-01 ──► A-03
A-01 ──► A-04
A-02 ──► (no downstream; cache key is independent)
A-03 ──► A-04
A-05 ──► A-06 ──► A-07
A-04 ──► B-08

B-01 ──► B-03 ──► B-04 ──► B-05 ──► B-06
B-02 ──► B-05
B-07 ──► B-08 ──► B-09

C-01 ──► C-03 ──► C-04 ──► C-06 ──► C-07
C-02 ──► C-06
C-05 ──► C-06   ← refactor must precede new functions

D-01 ──► D-03 ──► D-04 ──► D-05 ──► D-06
D-02 ──► D-05

B-08 ──► E-01
B-05 ──► E-02
C-06 ──► E-03
D-05 ──► E-04
```

Tasks with no incoming arrows (A-01, A-02, A-05, B-01, B-02, B-07, C-01, C-02, D-01,
D-02) can be started in parallel as long as they are in the same phase window.

---

## Completed Baseline (before these tasks)

| Task | Description | Commit |
|------|-------------|--------|
| ~~BASE-01~~ | `GeminiExplanationEngine` (`gemini-2.5-flash`) | Pre-existing |
| ~~BASE-02~~ | `GeminiNanoExplanationEngine` + `HybridExplanationEngine` | Pre-existing |
| ~~BASE-03~~ | `DefaultExplanationRepository` with Room cache | Pre-existing |
| ~~BASE-04~~ | `ExplanationPrompt` (base — without cultural lens) | Pre-existing |
| ~~BASE-05~~ | `ReaderViewModel` passes `bibleLanguageTag` as `uiLanguageCode` | PR #78 |
| ~~BASE-06~~ | `bundle { language { enableSplit = false } }` in build.gradle | PR #78 |
| ~~BASE-07~~ | Telugu prayer translations complete | PR #78 |
| ~~BASE-08~~ | UI language selection removed; English hardcoded | PR #78 |
