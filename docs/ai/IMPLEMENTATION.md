# Gemini AI Features — Implementation Guide

> **Status**: Active  
> **Prerequisite reading**: [GEMINI_FEATURES.md](GEMINI_FEATURES.md)  
> **Last updated**: 2026-06-25

This document is the step-by-step engineering guide for implementing every feature in
GEMINI_FEATURES.md. Every new file is listed with its exact package path, every modified
file with its exact change, and every schema change with its migration SQL.

---

## Architecture Principles (must not be violated)

1. **ViewModels never import Android framework classes** (except `ViewModel` itself).
   Domain engines and use cases are pure Kotlin.
2. **All Gemini calls go through `GeminiApi`** — the single Retrofit interface already
   wired in `AppModule`. Never create a second HTTP client for AI features.
3. **New AI engines implement a domain interface** in `domain/` and live in `data/`.
   The ViewModel depends on the interface, not the implementation.
4. **Successful AI responses that are expensive to regenerate are cached in Room**.
   Responses involving personal or crisis text are never cached (see privacy requirements).
5. **Feature flags gate all new surfaces**. Add each new flag to `FeatureFlags.kt`
   before writing any navigation or UI code.
6. **Database schema changes require a Room migration**. The current version is v6.
   F-01 requires v7; assign subsequent features v8+.

---

## F-01 — Indian Cultural Lens

**Dependencies**: None (this is a prompt change + one new field).  
**Database change**: None — cache key format changes; old cache entries become stale
naturally (different denomination now in key; no migration needed).  
**Estimated complexity**: S (small — 5 files changed).

### Step 1: Add denomination to `ExplanationRequest`

**File**: `app/src/main/java/com/manna/bible/domain/explain/Explanation.kt`

Add the `denomination` field to `ExplanationRequest`:

```kotlin
data class ExplanationRequest(
    val osisRef: String,
    val reference: String,
    val passageText: String,
    val uiLanguageCode: String,
    val depth: ExplainDepth,
    val denomination: Denomination? = null   // ADD — null = no constraint applied
)
```

### Step 2: Update `ExplanationPrompt`

**File**: `app/src/main/java/com/manna/bible/domain/explain/ExplanationPrompt.kt`

Replace the `build()` function:

```kotlin
fun build(request: ExplanationRequest): String = buildString {
    appendLine(
        "You are a faithful, pastoral Bible teacher helping an ordinary believer " +
            "understand Scripture. Be warm, clear, and concise. Avoid jargon; when a " +
            "term is unavoidable, explain it in plain words."
    )
    appendLine()
    // Cultural and denominational grounding
    val culturalInstruction = buildCulturalInstruction(request)
    if (culturalInstruction.isNotBlank()) {
        appendLine(culturalInstruction)
        appendLine()
    }
    appendLine("Passage: ${request.reference}")
    appendLine("Text: ${request.passageText}")
    appendLine()
    when (request.depth) {
        ExplainDepth.PLAIN -> appendLine(
            "Explain this passage in three short parts: (1) the context, (2) what it " +
                "means, and (3) how to apply it today."
        )
        ExplainDepth.PREACHING -> appendLine(
            "Explain this passage for someone preparing to preach or teach it: " +
                "(1) the context, (2) what it means, (3) how to apply it today, " +
                "(4) a short three-point outline, (5) two or three cross-references, " +
                "and (6) one illustration idea."
        )
    }
    appendLine()
    append(
        "Write the entire response in the language identified by the ISO code " +
            "'${request.uiLanguageCode}'. Keep it faithful to the historic Christian " +
            "reading of the text and do not invent facts."
    )
}

private fun buildCulturalInstruction(request: ExplanationRequest): String {
    val parts = mutableListOf<String>()

    // Denominational framing
    val denominationNote = when (request.denomination) {
        Denomination.CATHOLIC ->
            "The reader belongs to the Catholic tradition. Where relevant, acknowledge " +
                "the sacramental, Marian, or magisterial dimensions of the text as a " +
                "Catholic would understand them."
        Denomination.ORTHODOX ->
            "The reader belongs to the Orthodox tradition. Where relevant, draw on the " +
                "patristic and liturgical reading of the text as practised in the Orthodox Church."
        Denomination.CSI ->
            "The reader belongs to the Church of South India (CSI), a South Indian " +
                "Protestant-Anglican tradition. Where relevant, reference the liturgical " +
                "calendar and social-justice heritage of the CSI."
        Denomination.MAR_THOMA ->
            "The reader belongs to the Mar Thoma Church, a reformed Oriental tradition " +
                "rooted in the Kerala church of St Thomas. Honour both its reformed " +
                "theology and its ancient Syrian heritage in how you apply the text."
        Denomination.SHOW_EVERYTHING, Denomination.PROTESTANT_OTHER, null -> ""
    }
    if (denominationNote.isNotBlank()) parts.add(denominationNote)

    // Cultural grounding — applied to all Indian language readers
    parts.add(
        "The reader lives in India. In your application section, use imagery, analogies, " +
            "and illustrations that are natural to Indian daily life — rice fields, monsoons, " +
            "family honour, joint families, village community, chai — rather than Western " +
            "equivalents. Where the original text uses agrarian or social imagery (harvest, " +
            "debt, servants, bread), name its Indian equivalent. Where the text speaks to " +
            "social equality, acknowledge the caste dimension directly and honestly, then " +
            "explain what the Gospel says about it. Never override the historic Christian " +
            "reading — add the Indian lens to the application, do not replace the meaning."
    )

    return parts.joinToString("\n\n")
}
```

### Step 3: Update cache key in `DefaultExplanationRepository`

**File**: `app/src/main/java/com/manna/bible/data/explain/DefaultExplanationRepository.kt`

```kotlin
private fun cacheKey(request: ExplanationRequest): String =
    "${request.osisRef}|${request.uiLanguageCode}|${request.depth.name}|${request.denomination?.id ?: "any"}"
```

### Step 4: Pass denomination from `ReaderViewModel`

**File**: `app/src/main/java/com/manna/bible/ui/reader/ReaderViewModel.kt`

In the `ExplanationRequest` constructor call (the block that builds the request):

```kotlin
ExplanationRequest(
    osisRef      = osisRef,
    reference    = reference,
    passageText  = verseLine.text,
    uiLanguageCode = bibleLanguageTag ?: DEFAULT_UI_LANGUAGE,
    depth        = depth,
    denomination = setup.denomination   // ADD
)
```

The `setup` object is already in scope from `preferencesStore.setupState.first()`.

### Step 5: Update unit tests

**File**: `app/src/test/java/com/manna/bible/ui/reader/ReaderViewModelTest.kt`

Update `ExplanationRequest` construction in all test helpers to include `denomination = null`.

**File**: (new) `app/src/test/java/com/manna/bible/domain/explain/ExplanationPromptTest.kt`

```kotlin
class ExplanationPromptTest {

    @Test
    fun `plain prompt for Catholic user contains sacramental note`() {
        val request = ExplanationRequest(
            osisRef = "JHN.3.16", reference = "John 3:16",
            passageText = "For God so loved the world...",
            uiLanguageCode = "ml", depth = ExplainDepth.PLAIN,
            denomination = Denomination.CATHOLIC
        )
        val prompt = ExplanationPrompt.build(request)
        assertTrue(prompt.contains("sacramental", ignoreCase = true))
        assertTrue(prompt.contains("India", ignoreCase = true))
    }

    @Test
    fun `prompt without denomination still contains cultural grounding`() {
        val request = ExplanationRequest(
            osisRef = "PSA.23.1", reference = "Psalm 23:1",
            passageText = "The LORD is my shepherd...",
            uiLanguageCode = "te", depth = ExplainDepth.PLAIN,
            denomination = null
        )
        val prompt = ExplanationPrompt.build(request)
        assertTrue(prompt.contains("India", ignoreCase = true))
        assertFalse(prompt.contains("sacramental", ignoreCase = true))
    }
}
```

---

## F-02 — Oral Bible AI (Spoken Explanations)

**Dependencies**: F-01 (denomination field), existing `SpeechEngine`, `DefaultTtsReader`.  
**Database change**: None.  
**Estimated complexity**: M (medium — 4 files changed, ViewModel logic added).

### Step 1: Add feature flag

**File**: `app/src/main/java/com/manna/bible/domain/FeatureFlags.kt`

```kotlin
const val ORAL_AI_EXPLANATION: Boolean = false
```

### Step 2: Extend `ReaderUiState`

**File**: `app/src/main/java/com/manna/bible/ui/reader/ReaderViewModel.kt`

Within `ReaderUiState` (or the existing state data class — locate the definition):

```kotlin
val explanationSpeaking: Boolean = false,
val canSpeakExplanation: Boolean = false
```

### Step 3: Add speak/stop functions to `ReaderViewModel`

In `ReaderViewModel`, inject `SpeechEngine` (it is already bound via `BindingsModule`):

```kotlin
@HiltViewModel
class ReaderViewModel @Inject constructor(
    // … existing …
    private val speechEngine: SpeechEngine   // ADD injection
) : ViewModel() {
```

Add functions:

```kotlin
fun speakExplanation() {
    if (!FeatureFlags.ORAL_AI_EXPLANATION) return
    val text = _uiState.value.explanation
        ?.let { (it as? ExplanationResult.Success)?.text } ?: return
    val langTag = _uiState.value.bibleLanguageTag ?: DEFAULT_UI_LANGUAGE
    viewModelScope.launch {
        // Pause narrated audio before starting explanation TTS.
        narratedAudioPlayer.pause()
        _uiState.update { it.copy(explanationSpeaking = true) }
        speechEngine.speak(text, langTag)
        _uiState.update { it.copy(explanationSpeaking = false) }
    }
}

fun stopExplanation() {
    speechEngine.stop()
    _uiState.update { it.copy(explanationSpeaking = false) }
}
```

Note: `SpeechEngine.speak()` is a suspending call in `DefaultTtsReader`; confirm the
existing `AndroidSpeechEngine` implementation's `speak` signature matches. If it is a
fire-and-forget `fun` (not `suspend`), wrap it in `withContext(Dispatchers.Main)`.

### Step 4: Resolve `canSpeakExplanation` during setup observation

In the coroutine that already observes `setupState`, add:

```kotlin
val langTag = setup.bibleLanguage ?: DEFAULT_UI_LANGUAGE
val canSpeak = FeatureFlags.ORAL_AI_EXPLANATION &&
    speechEngine.isLanguageAvailable(langTag)
_uiState.update { it.copy(canSpeakExplanation = canSpeak) }
```

`isLanguageAvailable(languageTag: String): Boolean` must be added to the `SpeechEngine`
interface and implemented in `AndroidSpeechEngine` using `TextToSpeech.isLanguageAvailable()`.

**File**: `app/src/main/java/com/manna/bible/domain/audio/SpeechEngine.kt`

```kotlin
interface SpeechEngine {
    // … existing …
    fun isLanguageAvailable(languageTag: String): Boolean
}
```

**File**: `app/src/main/java/com/manna/bible/audio/AndroidSpeechEngine.kt`

```kotlin
override fun isLanguageAvailable(languageTag: String): Boolean {
    val locale = java.util.Locale.forLanguageTag(languageTag)
    val result = tts?.isLanguageAvailable(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
    return result != TextToSpeech.LANG_NOT_SUPPORTED &&
           result != TextToSpeech.LANG_MISSING_DATA
}
```

### Step 5: Add speak button to the explanation bottom sheet

**File**: `app/src/main/java/com/manna/bible/ui/reader/ReaderScreen.kt`

Within the composable that renders the explanation result (look for the `ExplanationResult.Success`
branch in the explanation sheet), add:

```kotlin
if (FeatureFlags.ORAL_AI_EXPLANATION) {
    val speaking by remember { derivedStateOf { uiState.explanationSpeaking } }
    IconButton(
        onClick = { if (speaking) vm.stopExplanation() else vm.speakExplanation() },
        enabled = uiState.canSpeakExplanation || speaking
    ) {
        Icon(
            imageVector = if (speaking) Icons.Default.StopCircle else Icons.Default.VolumeUp,
            contentDescription = if (speaking) "Stop speaking explanation"
                                 else "Speak explanation"
        )
    }
    if (!uiState.canSpeakExplanation && !speaking) {
        Text(
            text = "Install ${uiState.bibleLanguageName} TTS voice in Android Settings to listen",
            style = MaterialTheme.typography.bodySmall,
            color = MannaTheme.colors.muted
        )
    }
}
```

---

## F-03 — Crisis AI Companion

**Dependencies**: Existing `CrisisCompanion`, `GeminiApi`, `PreferencesStore`.  
**Database change**: None (crisis text is never persisted).  
**Estimated complexity**: M (medium — 5 new files, 2 modified).

### Step 1: Add feature flag

**File**: `app/src/main/java/com/manna/bible/domain/FeatureFlags.kt`

```kotlin
const val CRISIS_AI_COMPANION: Boolean = false
```

### Step 2: Define domain contracts

**File** (new): `app/src/main/java/com/manna/bible/domain/crisis/CrisisAiEngine.kt`

```kotlin
package com.manna.bible.domain.crisis

sealed interface CrisisAiResult {
    data class Success(
        val passageRef: String,
        val osisRef: String,
        val explanation: String
    ) : CrisisAiResult
    data object Offline : CrisisAiResult
    data class Unavailable(val reason: String) : CrisisAiResult
}

interface CrisisAiEngine {
    val isConfigured: Boolean
    suspend fun respond(
        situation: String,
        languageCode: String,
        isNight: Boolean,
        denomination: Denomination?
    ): CrisisAiResult
}
```

### Step 3: Implement `GeminiCrisisEngine`

**File** (new): `app/src/main/java/com/manna/bible/data/crisis/GeminiCrisisEngine.kt`

```kotlin
package com.manna.bible.data.crisis

import com.manna.bible.data.remote.GeminiApi
import com.manna.bible.data.remote.GeminiContentDto
import com.manna.bible.data.remote.GeminiGenerationConfigDto
import com.manna.bible.data.remote.GeminiPartDto
import com.manna.bible.data.remote.GeminiRequestDto
import com.manna.bible.domain.crisis.CrisisAiEngine
import com.manna.bible.domain.crisis.CrisisAiResult
import com.manna.bible.domain.model.Denomination
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named

class GeminiCrisisEngine @Inject constructor(
    private val api: GeminiApi,
    @Named("geminiApiKey") private val apiKey: String
) : CrisisAiEngine {

    override val isConfigured: Boolean get() = apiKey.isNotBlank()

    override suspend fun respond(
        situation: String,
        languageCode: String,
        isNight: Boolean,
        denomination: Denomination?
    ): CrisisAiResult {
        if (apiKey.isBlank()) return CrisisAiResult.Unavailable("No API key configured.")
        return try {
            val prompt = buildPrompt(situation, languageCode, isNight, denomination)
            val response = api.generate(
                model = MODEL,
                key = apiKey,
                body = GeminiRequestDto(
                    contents = listOf(
                        GeminiContentDto(parts = listOf(GeminiPartDto(prompt)))
                    ),
                    generationConfig = GeminiGenerationConfigDto(
                        temperature = 0.3f,
                        maxOutputTokens = 256
                    )
                )
            )
            val raw = response.candidates.firstOrNull()
                ?.content?.parts?.firstOrNull()?.text?.trim()
            if (raw.isNullOrBlank()) {
                CrisisAiResult.Unavailable("Empty response from AI.")
            } else {
                parseResponse(raw)
            }
        } catch (e: IOException) {
            CrisisAiResult.Offline
        } catch (e: HttpException) {
            CrisisAiResult.Unavailable("HTTP ${e.code()}")
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            CrisisAiResult.Unavailable(e.message ?: "Unknown error")
        }
    }

    private fun buildPrompt(
        situation: String,
        languageCode: String,
        isNight: Boolean,
        denomination: Denomination?
    ): String = buildString {
        if (isNight) {
            appendLine(
                "It is the middle of the night and this person is alone. Begin with quiet " +
                    "presence. Do not open with energy or an action item. Sit with them first."
            )
            appendLine()
        }
        appendLine(
            "You are a compassionate Christian pastor. Someone has come to you in crisis. " +
                "Your task: choose the single Bible passage most directly relevant to their " +
                "situation, then explain in 2–3 sentences why it speaks to them right now. " +
                "Lead with compassion, not theology. Do not minimise what they are experiencing."
        )
        if (denomination != null && denomination != Denomination.SHOW_EVERYTHING) {
            appendLine()
            appendLine("The person's tradition: ${denomination.id.replace('_', ' ')}.")
        }
        appendLine()
        appendLine("Their situation (treat this as a private confession — never repeat it back): $situation")
        appendLine()
        appendLine(
            "Respond ONLY in this exact format (no preamble, no greeting, just these three lines):\n" +
                "REF: <OSIS_ID>.<chapter>.<verse>\n" +
                "DISPLAY: <Book Name> <chapter>:<verse>\n" +
                "MESSAGE: <your 2–3 sentence compassionate explanation>\n" +
                "\nWrite the MESSAGE in the language with ISO code '$languageCode'."
        )
    }

    private fun parseResponse(raw: String): CrisisAiResult {
        val lines = raw.lines().associate { line ->
            val colon = line.indexOf(':')
            if (colon < 0) return@associate "" to ""
            line.substring(0, colon).trim() to line.substring(colon + 1).trim()
        }
        val osisRef = lines["REF"] ?: return CrisisAiResult.Unavailable("Malformed AI response (missing REF).")
        val display = lines["DISPLAY"] ?: return CrisisAiResult.Unavailable("Malformed AI response (missing DISPLAY).")
        val message = lines["MESSAGE"] ?: return CrisisAiResult.Unavailable("Malformed AI response (missing MESSAGE).")
        return CrisisAiResult.Success(
            passageRef = display,
            osisRef = osisRef,
            explanation = message
        )
    }

    private companion object {
        const val MODEL = "gemini-2.5-flash"
    }
}
```

### Step 4: Wire into DI

**File**: `app/src/main/java/com/manna/bible/di/BindingsModule.kt`

```kotlin
@Binds
abstract fun bindCrisisAiEngine(impl: GeminiCrisisEngine): CrisisAiEngine
```

### Step 5: Extend `CrisisModeViewModel`

**File**: `app/src/main/java/com/manna/bible/ui/crisis/CrisisModeViewModel.kt`

Inject `CrisisAiEngine`, `PreferencesStore` (already present):

```kotlin
// Add to constructor
private val crisisAiEngine: CrisisAiEngine,
```

Add to `CrisisUiState`:

```kotlin
val situationText: String = "",
val aiResponse: CrisisAiResult? = null,
val isAiLoading: Boolean = false,
val aiConfigured: Boolean = false
```

Add to ViewModel:

```kotlin
init {
    load()
    _uiState.update { it.copy(aiConfigured = FeatureFlags.CRISIS_AI_COMPANION && crisisAiEngine.isConfigured) }
}

fun updateSituation(text: String) {
    _uiState.update { it.copy(situationText = text, aiResponse = null) }
}

fun submitSituation() {
    val text = _uiState.value.situationText.trim()
    if (text.isBlank() || !FeatureFlags.CRISIS_AI_COMPANION) return
    viewModelScope.launch {
        _uiState.update { it.copy(isAiLoading = true, aiResponse = null) }
        val setup = preferencesStore.setupState.first()
        val langCode = setup.bibleLanguage ?: "en"
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val result = crisisAiEngine.respond(
            situation = text,
            languageCode = langCode,
            isNight = NightWindow.isNight(hour),
            denomination = setup.denomination
        )
        // Never store the situation text — discard immediately after call.
        _uiState.update { it.copy(isAiLoading = false, aiResponse = result) }
    }
}

fun clearAiResponse() {
    _uiState.update { it.copy(aiResponse = null, situationText = "") }
}
```

### Step 6: Update `CrisisModeScreen`

**File**: `app/src/main/java/com/manna/bible/ui/crisis/CrisisModeScreen.kt`

Above the existing comfort verse list, when `FeatureFlags.CRISIS_AI_COMPANION`:

```kotlin
if (uiState.aiConfigured) {
    OutlinedTextField(
        value = uiState.situationText,
        onValueChange = vm::updateSituation,
        placeholder = { Text(stringResource(R.string.crisis_ai_placeholder)) },
        trailingIcon = {
            if (uiState.situationText.isNotBlank()) {
                IconButton(onClick = vm::submitSituation) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Submit")
                }
            }
        },
        enabled = !uiState.isAiLoading,
        modifier = Modifier.fillMaxWidth()
    )
} else if (FeatureFlags.CRISIS_AI_COMPANION) {
    Text(
        text = stringResource(R.string.crisis_ai_offline_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MannaTheme.colors.muted
    )
}
```

Add strings to `res/values/strings.xml` (and all translated variants):
- `crisis_ai_placeholder`: "Tell me what's happening…"
- `crisis_ai_offline_hint`: "AI response needs internet. The verses below work offline."

---

## F-04 — AI Sermon Builder

**Dependencies**: Existing `SermonRepository`, `GeminiApi`, `PreferencesStore`.  
**Database change**: None (outlines are not cached).  
**Estimated complexity**: M (medium — 4 new files, 2 modified).

### Step 1: Add feature flag

**File**: `app/src/main/java/com/manna/bible/domain/FeatureFlags.kt`

```kotlin
const val SERMON_AI_BUILDER: Boolean = false
```

### Step 2: Define domain contracts

**File** (new): `app/src/main/java/com/manna/bible/domain/sermon/SermonAiEngine.kt`

```kotlin
package com.manna.bible.domain.sermon

import com.manna.bible.domain.model.Denomination

enum class CongregationType(val id: String) {
    GENERAL("general"),
    YOUTH("youth"),
    GRIEF("grief")
}

data class SermonOutlineRequest(
    val reference: String,
    val denomination: Denomination?,
    val languageCode: String,
    val congregationType: CongregationType
)

sealed interface SermonOutlineResult {
    data class Success(val outlineText: String) : SermonOutlineResult
    data object Offline : SermonOutlineResult
    data class Unavailable(val reason: String) : SermonOutlineResult
}

interface SermonAiEngine {
    val isConfigured: Boolean
    suspend fun generateOutline(request: SermonOutlineRequest): SermonOutlineResult
}
```

### Step 3: Implement `GeminiSermonEngine`

**File** (new): `app/src/main/java/com/manna/bible/data/sermon/GeminiSermonEngine.kt`

```kotlin
package com.manna.bible.data.sermon

import com.manna.bible.data.remote.GeminiApi
import com.manna.bible.data.remote.GeminiContentDto
import com.manna.bible.data.remote.GeminiGenerationConfigDto
import com.manna.bible.data.remote.GeminiPartDto
import com.manna.bible.data.remote.GeminiRequestDto
import com.manna.bible.domain.model.Denomination
import com.manna.bible.domain.sermon.CongregationType
import com.manna.bible.domain.sermon.SermonAiEngine
import com.manna.bible.domain.sermon.SermonOutlineRequest
import com.manna.bible.domain.sermon.SermonOutlineResult
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named

class GeminiSermonEngine @Inject constructor(
    private val api: GeminiApi,
    @Named("geminiApiKey") private val apiKey: String
) : SermonAiEngine {

    override val isConfigured: Boolean get() = apiKey.isNotBlank()

    override suspend fun generateOutline(request: SermonOutlineRequest): SermonOutlineResult {
        if (apiKey.isBlank()) return SermonOutlineResult.Unavailable("No API key configured.")
        return try {
            val response = api.generate(
                model = MODEL,
                key = apiKey,
                body = GeminiRequestDto(
                    contents = listOf(
                        GeminiContentDto(parts = listOf(GeminiPartDto(buildPrompt(request))))
                    ),
                    generationConfig = GeminiGenerationConfigDto(
                        temperature = 0.5f,
                        maxOutputTokens = 1024
                    )
                )
            )
            val text = response.candidates.firstOrNull()
                ?.content?.parts?.firstOrNull()?.text?.trim()
            if (text.isNullOrBlank()) SermonOutlineResult.Unavailable("Empty response.")
            else SermonOutlineResult.Success(text)
        } catch (e: IOException) {
            SermonOutlineResult.Offline
        } catch (e: HttpException) {
            SermonOutlineResult.Unavailable("HTTP ${e.code()}")
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            SermonOutlineResult.Unavailable(e.message ?: "Unknown error")
        }
    }

    private fun buildPrompt(request: SermonOutlineRequest): String = buildString {
        appendLine(
            "You are a village pastor's sermon preparation assistant. Your task is to " +
                "produce a complete sermon outline — not prose, an outline — for the " +
                "following passage. The preacher may have no seminary training. Be practical, " +
                "concrete, and rooted in the daily life of an Indian village congregation."
        )
        appendLine()

        // Denomination
        val denNote = when (request.denomination) {
            Denomination.CATHOLIC ->
                "The congregation is Catholic. The homily should honour the sacramental " +
                    "reading of the text and may reference the liturgical season or the Mass."
            Denomination.ORTHODOX ->
                "The congregation is Orthodox. Reference patristic or liturgical tradition " +
                    "where relevant."
            Denomination.CSI ->
                "The congregation is CSI (Church of South India). South Indian Protestant-" +
                    "Anglican sensibility; social justice matters here."
            Denomination.MAR_THOMA ->
                "The congregation is Mar Thoma. Reformed, but shaped by ancient Syrian " +
                    "heritage. Kerala-rooted."
            else -> null
        }
        if (denNote != null) { appendLine(denNote); appendLine() }

        // Congregation type
        val congNote = when (request.congregationType) {
            CongregationType.GENERAL -> "Mixed Sunday congregation of all ages."
            CongregationType.YOUTH ->
                "Youth congregation (teens and young adults). Modern references welcome. " +
                    "Keep it real and direct."
            CongregationType.GRIEF ->
                "A funeral or memorial service. Tone must be gentle, comforting, and honest " +
                    "about grief. Do not be triumphalist."
        }
        appendLine("Congregation: $congNote")
        appendLine()
        appendLine("Passage: ${request.reference}")
        appendLine()
        appendLine(
            "Produce the outline in EXACTLY this structure:\n" +
                "**Introduction** — one sentence opening with a question or observation from " +
                    "Indian daily life the congregation will immediately recognise.\n" +
                "**Point 1: [heading]** — 2 bullet supporting thoughts.\n" +
                "**Point 2: [heading]** — 2 bullet supporting thoughts.\n" +
                "**Point 3: [heading]** — 2 bullet supporting thoughts.\n" +
                "**Cross-references** — 2–3 related passages (book chapter:verse).\n" +
                "**Illustration** — one concrete story or image from Indian village, " +
                    "farming, or family life.\n" +
                "**Conclusion** — one closing sentence."
        )
        appendLine()
        append("Write the entire outline in the language with ISO code '${request.languageCode}'.")
    }

    private companion object {
        const val MODEL = "gemini-2.5-flash"
    }
}
```

### Step 4: Wire into DI

**File**: `app/src/main/java/com/manna/bible/di/BindingsModule.kt`

```kotlin
@Binds
abstract fun bindSermonAiEngine(impl: GeminiSermonEngine): SermonAiEngine
```

### Step 5: Extend `SermonHelperViewModel`

**File**: `app/src/main/java/com/manna/bible/ui/sermon/SermonHelperViewModel.kt`

Add to constructor:

```kotlin
private val sermonAiEngine: SermonAiEngine,
private val preferencesStore: PreferencesStore
```

Add to `SermonHelperUiState`:

```kotlin
val congregationType: CongregationType = CongregationType.GENERAL,
val isGeneratingOutline: Boolean = false,
val outlineError: String? = null,
val canGenerateOutline: Boolean = false
```

The `canGenerateOutline` is derived: `FeatureFlags.SERMON_AI_BUILDER && sermonAiEngine.isConfigured`.
Expose as a `val` in init or as a computed property.

Add functions:

```kotlin
fun selectCongregationType(type: CongregationType) {
    _uiState.update { it.copy(congregationType = type) }   // needs converting to MutableStateFlow
}

fun generateOutline() {
    if (!FeatureFlags.SERMON_AI_BUILDER) return
    val ref = uiState.value.draft?.reference ?: return
    if (ref.isBlank()) return
    viewModelScope.launch {
        _uiState.update { it.copy(isGeneratingOutline = true, outlineError = null) }
        val setup = preferencesStore.setupState.first()
        val result = sermonAiEngine.generateOutline(
            SermonOutlineRequest(
                reference = ref,
                denomination = setup.denomination,
                languageCode = setup.bibleLanguage ?: "en",
                congregationType = uiState.value.congregationType
            )
        )
        when (result) {
            is SermonOutlineResult.Success -> {
                updateDraft { it.copy(content = result.outlineText) }
                _uiState.update { it.copy(isGeneratingOutline = false) }
            }
            is SermonOutlineResult.Offline ->
                _uiState.update { it.copy(isGeneratingOutline = false, outlineError = "offline") }
            is SermonOutlineResult.Unavailable ->
                _uiState.update { it.copy(isGeneratingOutline = false, outlineError = result.reason) }
        }
    }
}
```

Note: `SermonHelperUiState` currently uses `stateIn` from a `combine`. Shift the sermons
combine to a `private val _uiState = MutableStateFlow(SermonHelperUiState())` pattern to
support `update`, and merge the sermon list via a separate observation coroutine.

---

## F-05 — Context-Aware Verse Cards

**Dependencies**: F-01, existing `ScriptureCardRenderer`, `GeminiApi`.  
**Database change**: None.  
**Estimated complexity**: M (medium — 3 new files, 1 new screen, 1 new ViewModel).

### Step 1: Add feature flag

```kotlin
const val VERSE_RECOMMENDATION_AI: Boolean = false
```

### Step 2: Define domain contracts

**File** (new): `app/src/main/java/com/manna/bible/domain/share/VerseRecommendationEngine.kt`

```kotlin
package com.manna.bible.domain.share

import com.manna.bible.domain.model.Denomination

data class VerseRecommendationRequest(
    val situationText: String,
    val languageCode: String,
    val denomination: Denomination?
)

sealed interface VerseRecommendation {
    data class Success(
        val osisRef: String,
        val reference: String,
        val verseText: String,
        val personalMessage: String
    ) : VerseRecommendation
    data object Offline : VerseRecommendation
    data class Unavailable(val reason: String) : VerseRecommendation
}

interface VerseRecommendationEngine {
    val isConfigured: Boolean
    suspend fun recommend(request: VerseRecommendationRequest): VerseRecommendation
}
```

### Step 3: Implement `GeminiVerseRecommendationEngine`

**File** (new): `app/src/main/java/com/manna/bible/data/share/GeminiVerseRecommendationEngine.kt`

The prompt instructs Gemini to return a structured response:

```
REF: <OSIS_ID>.<chapter>.<verse>
DISPLAY: <Book> <chapter>:<verse>
MESSAGE: <2-sentence personal message>
```

The implementation pattern mirrors `GeminiCrisisEngine` — build prompt, call `api.generate()`,
parse the structured response. Extract `osisRef` from `REF:`, fetch verse text from
`BibleContentRepository.chapter()`, populate `VerseRecommendation.Success`.

Since the verse text requires a local DB lookup, `GeminiVerseRecommendationEngine` must
inject `BibleContentRepository` and `TranslationRepository` as well.

### Step 4: New `VerseRecommendationViewModel` + screen

**File** (new): `app/src/main/java/com/manna/bible/ui/card/VerseRecommendationViewModel.kt`

State:

```kotlin
data class VerseRecommendationUiState(
    val situationText: String = "",
    val result: VerseRecommendation? = null,
    val isLoading: Boolean = false,
    val engineConfigured: Boolean = false
)
```

Functions: `onSituationChange(text)`, `recommend()`, `clear()`.

**File** (new): `app/src/main/java/com/manna/bible/ui/card/VerseRecommendationScreen.kt`

A simple screen with: situation text field → Recommend button → result card (using the
existing `VerseCardSheet`) → share button that opens the system share sheet via
`ScriptureCardRenderer`.

### Step 5: Wire navigation

**File**: `app/src/main/java/com/manna/bible/ui/MannaApp.kt`

Add the new route under `FeatureFlags.VERSE_RECOMMENDATION_AI`. Entry point: a "Find a
verse to share" button in `VerseCardSheet` or `ScriptureCardScreen`.

---

## F-06 — Persecution-Aware Comfort

**Dependencies**: Existing `CrisisCompanion`, `BibleContentRepository`.  
**Database change**: None.  
**Estimated complexity**: S (small — 2 new files, 1 modified screen).

### Step 1: Add feature flag

```kotlin
const val PERSECUTION_COMFORT: Boolean = true
```

### Step 2: Define `PersecutionCompanion`

**File** (new): `app/src/main/java/com/manna/bible/domain/crisis/PersecutionCompanion.kt`

```kotlin
package com.manna.bible.domain.crisis

import com.manna.bible.domain.model.Denomination
import com.manna.bible.domain.usecase.ReadingRef

enum class PersecutionCategory(val id: String) {
    FAMILY_REJECTION("family_rejection"),
    JOB_LIVELIHOOD("job_livelihood"),
    PHYSICAL_DANGER("physical_danger"),
    SOCIAL_EXCLUSION("social_exclusion"),
    FAITH_CRISIS("faith_crisis")
}

interface PersecutionCompanion {
    fun versesFor(category: PersecutionCategory): List<ReadingRef>
}

class DefaultPersecutionCompanion : PersecutionCompanion {

    override fun versesFor(category: PersecutionCategory): List<ReadingRef> =
        VERSES[category] ?: emptyList()

    private companion object {
        val VERSES: Map<PersecutionCategory, List<ReadingRef>> = mapOf(
            PersecutionCategory.FAMILY_REJECTION to listOf(
                ReadingRef("MAT", 10, 34), // "I did not come to bring peace but a sword"
                ReadingRef("MAT", 10, 37), // "Anyone who loves father or mother more than me"
                ReadingRef("LUK", 14, 26), // "If anyone comes to me and does not hate his family"
                ReadingRef("GEN", 12, 1),  // "Leave your country, your people and your father's household"
                ReadingRef("PSA", 27, 10), // "Though my father and mother forsake me, the Lord will receive me"
                ReadingRef("JHN", 15, 18), // "If the world hates you, keep in mind that it hated me first"
                ReadingRef("ROM", 8, 35),  // "Who shall separate us from the love of Christ?"
                ReadingRef("HEB", 11, 8)   // "By faith Abraham obeyed and went"
            ),
            PersecutionCategory.JOB_LIVELIHOOD to listOf(
                ReadingRef("PHP", 4, 19),  // "God will meet all your needs"
                ReadingRef("MAT", 6, 33),  // "Seek first the kingdom of God"
                ReadingRef("HEB", 11, 25), // "He chose to be mistreated along with the people of God"
                ReadingRef("HEB", 11, 26), // "rather than to enjoy the fleeting pleasures of sin"
                ReadingRef("PSA", 37, 25), // "I have never seen the righteous forsaken"
                ReadingRef("LUK", 12, 22), // "Do not worry about your life, what you will eat"
                ReadingRef("ISA", 41, 10), // "Fear not, for I am with you"
                ReadingRef("PRO", 3, 5)    // "Trust in the Lord with all your heart"
            ),
            PersecutionCategory.PHYSICAL_DANGER to listOf(
                ReadingRef("PSA", 27, 1),  // "The Lord is my light and my salvation — whom shall I fear?"
                ReadingRef("PSA", 27, 3),  // "Though an army besiege me, my heart will not fear"
                ReadingRef("ISA", 43, 2),  // "When you pass through the waters, I will be with you"
                ReadingRef("ACT", 5, 41),  // "rejoicing because they had been counted worthy of suffering"
                ReadingRef("MAT", 10, 28), // "Do not be afraid of those who kill the body"
                ReadingRef("ROM", 8, 38),  // "neither death nor life… shall separate us from the love of God"
                ReadingRef("REV", 2, 10),  // "Be faithful, even to the point of death"
                ReadingRef("PSA", 91, 11)  // "He will command his angels concerning you"
            ),
            PersecutionCategory.SOCIAL_EXCLUSION to listOf(
                ReadingRef("GAL", 3, 28),  // "Neither Jew nor Greek, slave nor free"
                ReadingRef("JHN", 15, 18), // "If the world hates you, it hated me first"
                ReadingRef("JHN", 15, 19), // "You do not belong to the world"
                ReadingRef("1PE", 4, 14),  // "If you are insulted because of the name of Christ, you are blessed"
                ReadingRef("JAS", 2, 1),   // "Do not show favoritism"
                ReadingRef("ROM", 12, 16), // "Do not be proud, but be willing to associate with people of low position"
                ReadingRef("ACT", 17, 26), // "From one man he made all the nations"
                ReadingRef("EPH", 2, 19)   // "You are no longer foreigners and strangers, but fellow citizens"
            ),
            PersecutionCategory.FAITH_CRISIS to listOf(
                ReadingRef("MRK", 9, 24),  // "I believe; help me overcome my unbelief!"
                ReadingRef("HEB", 12, 1),  // "Let us run with perseverance the race marked out for us"
                ReadingRef("HEB", 12, 2),  // "fixing our eyes on Jesus, the pioneer and perfecter of faith"
                ReadingRef("PSA", 22, 1),  // "My God, my God, why have you forsaken me?"
                ReadingRef("PSA", 22, 24), // "he has not hidden his face from him but has listened to his cry"
                ReadingRef("ROM", 8, 26),  // "the Spirit helps us in our weakness"
                ReadingRef("JHN", 20, 27), // "Stop doubting and believe" — Thomas
                ReadingRef("1KI", 19, 4)   // Elijah under the broom tree — burnout and despair
            )
        )
    }
}
```

### Step 3: Bind in DI

**File**: `app/src/main/java/com/manna/bible/di/BindingsModule.kt`

```kotlin
@Binds
abstract fun bindPersecutionCompanion(impl: DefaultPersecutionCompanion): PersecutionCompanion
```

### Step 4: Extend `CrisisModeScreen`

Under `FeatureFlags.PERSECUTION_COMFORT`, add a horizontally scrolling row of five
`FilterChip` components, one per `PersecutionCategory`. Tapping one calls a new
`CrisisModeViewModel.selectPersecutionCategory(category)` function that loads the
verses for that category via `PersecutionCompanion.versesFor()`, resolves their text
from the active translation, and emits them as a separate list in `CrisisUiState`.

```kotlin
data class CrisisUiState(
    // … existing …
    val selectedPersecutionCategory: PersecutionCategory? = null,
    val persecutionVerses: List<ComfortVerse> = emptyList(),
    val isPersecutionLoading: Boolean = false
)
```

The existing curated `comfortVerses` list remains unchanged and always visible. The
persecution set is additive, shown below the category chips when a category is selected.

---

## Database Migration Reference

All previous migrations are in `app/src/main/java/com/manna/bible/data/local/Migrations.kt`.

| Version | Change | Feature |
|---------|--------|---------|
| v2 | Annotation tables (highlights, bookmarks, notes) | Core |
| v3 | Offline content tables (books, chapters, verses, FTS) | Core |
| v4 | `prayers` table | Prayer Journal |
| v5 | `explanations` cache table | Explain this Passage |
| v6 | `sermon_notes` table | Sermon Helper |
| **v7** | No change required | F-01 (cache key change is safe) |

F-02 through F-06 require no schema changes. All AI responses that need persistence
(explanations from F-01) already use the v5 `explanations` table. Crisis text (F-03),
sermon outlines (F-04), and verse recommendations (F-05) are never persisted.

---

## Dependency Injection Summary

### New `@Binds` entries required in `BindingsModule`

```kotlin
// F-03
@Binds abstract fun bindCrisisAiEngine(impl: GeminiCrisisEngine): CrisisAiEngine

// F-04
@Binds abstract fun bindSermonAiEngine(impl: GeminiSermonEngine): SermonAiEngine

// F-05
@Binds abstract fun bindVerseRecommendationEngine(
    impl: GeminiVerseRecommendationEngine
): VerseRecommendationEngine

// F-06
@Binds abstract fun bindPersecutionCompanion(
    impl: DefaultPersecutionCompanion
): PersecutionCompanion
```

All new `Gemini*Engine` implementations accept `GeminiApi` and `@Named("geminiApiKey")
String` — both already provided by `AppModule`. No new `@Provides` methods needed.

---

## Testing Requirements

### Unit test coverage required for every feature

| File to test | Test file location | Key scenarios |
|---|---|---|
| `ExplanationPrompt` | `test/.../domain/explain/ExplanationPromptTest.kt` | Cultural note present; denomination note present for Catholic/Orthodox; absent for null denomination |
| `DefaultExplanationRepository` | `test/.../data/explain/DefaultExplanationRepositoryTest.kt` | Cache key includes denomination; stale key from old format not matched |
| `GeminiCrisisEngine` | `test/.../data/crisis/GeminiCrisisEngineTest.kt` | Valid response parsed; malformed response → Unavailable; IOException → Offline |
| `GeminiSermonEngine` | `test/.../data/sermon/GeminiSermonEngineTest.kt` | Valid response → Success; offline → Offline; denomination in prompt |
| `DefaultPersecutionCompanion` | `test/.../domain/crisis/PersecutionCompanionTest.kt` | All five categories return non-empty verse lists; all refs are in the 66-book canon |
| `CrisisModeViewModel` | `test/.../ui/crisis/CrisisModeViewModelTest.kt` | situationText cleared after submit; aiResponse set on success; isAiLoading false after result |
| `SermonHelperViewModel` | `test/.../ui/sermon/SermonHelperViewModelTest.kt` | outline inserted into draft content; isGeneratingOutline false after result |

All tests use the `StandardTestDispatcher` pattern established in `DenominationSettingsViewModelTest.kt`.
Engine implementations are faked via constructor injection — do not use Mockito.

### Integration constraints

- `GeminiCrisisEngine`, `GeminiSermonEngine`, `GeminiVerseRecommendationEngine` must not
  be called in unit tests. Use fake implementations that return canned results.
- No Robolectric or instrumented tests required for these features at launch.
  Instrumented tests are out of scope for Phase 2/3 AI work.
