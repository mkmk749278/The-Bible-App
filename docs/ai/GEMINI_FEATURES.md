# Gemini AI Features — Specification

> **Status**: Approved for implementation  
> **Target audience**: Engineering, design, product  
> **Last updated**: 2026-06-25  
> **Maintained by**: Manna development team

This document is the authoritative specification for all planned Gemini-powered features
in the Manna Bible app. Each feature section defines user stories, functional requirements,
acceptance criteria, out-of-scope boundaries, and the data contracts the implementation
must satisfy. Implementation details and file paths live in [IMPLEMENTATION.md](IMPLEMENTATION.md).

---

## Table of Contents

1. [F-01 Indian Cultural Lens](#f-01-indian-cultural-lens)
2. [F-02 Oral Bible AI](#f-02-oral-bible-ai-spoken-explanations)
3. [F-03 Crisis AI Companion](#f-03-crisis-ai-companion)
4. [F-04 AI Sermon Builder](#f-04-ai-sermon-builder)
5. [F-05 Context-Aware Verse Cards](#f-05-context-aware-verse-cards)
6. [F-06 Persecution-Aware Comfort](#f-06-persecution-aware-comfort)
7. [Cross-Cutting Concerns](#cross-cutting-concerns)

---

## F-01 Indian Cultural Lens

### Problem

Every Bible app in existence explains Scripture through a Western theological and cultural
lens. A verse about "bread" presumes wheat; a verse about "yoke" presumes European farming;
an application about "rest" presumes industrial work rhythms. For the 230 million Indian
Christians this app serves — especially the 60%+ from rural or agrarian backgrounds — the
applications are simultaneously accurate and completely alienating.

The app already tracks denomination (Catholic, CSI, Mar Thoma, Orthodox, Protestant, …) and
Bible language (Tamil, Telugu, Hindi, Malayalam, …). Gemini must leverage both to produce
explanations whose *application* section is rooted in Indian lived experience.

### User Stories

| ID | Story |
|----|-------|
| US-01.1 | As a farmer in Andhra Pradesh reading John 15 ("I am the vine"), I want Gemini to anchor the application in my experience of crop dependency, not European vineyards. |
| US-01.2 | As a first-generation Christian from an OBC family reading Galatians 3:28, I want Gemini to explicitly name how caste is addressed by the Gospel, in Telugu. |
| US-01.3 | As a CSI pastor in Tamil Nadu preparing Sunday's sermon, I want cross-references that are relevant to the South Indian liturgical tradition, not the default Protestant American canon of "key passages". |
| US-01.4 | As a Catholic woman from Kerala reading Matthew 6:28 ("the lilies of the field"), I want the illustration to reference something I recognise — a paddy field, a coconut grove — not a Western meadow. |

### Functional Requirements

**FR-01.1 — Denomination-aware context**  
The explanation prompt must include the user's denomination so Gemini can:
- Reference liturgical traditions (lectionary cycle, sacramental context) when relevant.
- Avoid pushing Protestant-only interpretive conclusions to Catholic or Orthodox users.
- Surface the ecumenical range of interpretation for "show everything" users.

**FR-01.2 — Language-as-culture signal**  
The language code already tells Gemini which population it is addressing. The system
instruction must reinforce that the application section should use images, idioms, and
analogies natural to that language's cultural milieu, not translated from English.

**FR-01.3 — Agricultural and social anchoring**  
When the original passage contains agrarian, commercial, or social imagery (harvests,
debts, servants, banquets, widows, orphans), the application must note its Indian analog —
not as the only reading, but as the primary one for this audience.

**FR-01.4 — Caste and honour-shame acknowledgement**  
Passages addressing social equality, table fellowship, inheritance, or belonging must
explicitly acknowledge the caste and honour-shame dimensions relevant to the reader's
Indian social context, when such dimensions are textually present. This is an accuracy
requirement, not a political one.

**FR-01.5 — Preserve theological accuracy**  
The cultural bridge must never override the historic Christian reading. Where Indian
cultural parallels are imperfect or misleading (e.g., karma ≠ grace), Gemini must note
the difference clearly.

### Acceptance Criteria

- [ ] An explanation of John 6:35 ("I am the bread of life") in Telugu for a CSI user
      includes reference to rice as the staple of life, not wheat bread.
- [ ] An explanation of Galatians 3:28 ("no slave nor free, no Jew nor Greek") in Tamil
      explicitly names caste as the Indian instantiation of that division.
- [ ] An explanation of Isaiah 43:2 for a Catholic Malayalam user references the monsoon
      floods of Kerala, not abstract "waters".
- [ ] An explanation for an Orthodox user of 1 Corinthians 11 (Eucharist) references the
      Divine Liturgy tradition rather than a generic Protestant "Lord's Supper".
- [ ] For any passage, the explanation still faithfully represents the historic Christian
      consensus reading alongside the cultural application.

### Out of Scope

- Generating culturally-specific commentary beyond the existing PLAIN / PREACHING depth enum.
- Translating Scripture text itself into regional dialects.
- Adding new explanation depth levels for cultural study (may be addressed in a later feature).

### Data Contract Changes

`ExplanationRequest` gains one new field:

```kotlin
data class ExplanationRequest(
    val osisRef: String,
    val reference: String,
    val passageText: String,
    val uiLanguageCode: String,
    val depth: ExplainDepth,
    val denomination: Denomination?          // NEW — null = do not constrain
)
```

The cache key in `DefaultExplanationRepository` must include denomination to prevent
a user who changes denomination from receiving a stale culturally-mismatched explanation:

```
cacheKey = "${osisRef}|${uiLanguageCode}|${depth.name}|${denomination?.id ?: "any"}"
```

---

## F-02 Oral Bible AI (Spoken Explanations)

### Problem

Sixty percent of the target user base prefers listening over reading. Many are semi-literate.
The app already has a fully-working TTS pipeline (`DefaultTtsReader`, `AndroidSpeechEngine`,
`SpeechEngine`) that reads Bible chapters aloud in the Bible language. But when a user taps
"Explain this passage", they receive a wall of text. For the user who cannot read that text —
or who is lying in the dark at 3am — the explanation is inaccessible.

The fix is simple and already almost wired: pipe the Gemini explanation text through the
existing TTS engine the same way chapter text is read aloud.

### User Stories

| ID | Story |
|----|-------|
| US-02.1 | As an elderly woman in Tamil Nadu who cannot read but loves listening to Scripture, I want to tap "Explain this passage" and then immediately tap "Listen" to hear the explanation spoken in Tamil without reading any text. |
| US-02.2 | As a blind user using TalkBack, I want the explanation to be readable by the accessibility engine, but I also want the option to have the AI explanation read in my Bible language at the scripture TTS speed, not the system TalkBack speed. |
| US-02.3 | As a user driving and listening to a chapter, I want to hear an explanation of the current verse spoken automatically after the chapter finishes if I enable "auto-explain". |

### Functional Requirements

**FR-02.1 — Speak button on the explanation sheet**  
After a `ExplanationResult.Success` is displayed, the explanation sheet must show a
speak/stop icon button. Tapping it starts TTS playback of the explanation text. Tapping
again stops it. The icon must mirror the live playback state reactively.

**FR-02.2 — Language consistency**  
The TTS voice language must match `request.uiLanguageCode` (which is the Bible language
since the fix in PR #78). The engine must call `SpeechEngine.speak()` with the same
language tag used for Scripture reading.

**FR-02.3 — Exclusive audio**  
Starting explanation TTS must pause any ongoing Scripture TTS or narrated audio. Resuming
Scripture audio must stop explanation TTS. Use the existing `NarratedAudioPlayer` pause
signal.

**FR-02.4 — No new dependency**  
This feature must use `SpeechEngine` and `DefaultTtsReader` exactly as they exist. No
new TTS library or new audio session.

**FR-02.5 — Graceful degradation**  
If `SpeechEngine.speak()` returns an error (language not supported, TTS engine
initialisation failure), the explanation text remains readable on screen with no crash.

**FR-02.6 — Auto-explain mode (optional, Phase 2)**  
A Settings toggle "Speak explanations after reading". When on, after the last verse of a
chapter is read by TTS, Gemini explains the passage and speaks it. This requires the chapter
completion event from `TtsReader` and must not fire when the user manually stopped audio.

### Acceptance Criteria

- [ ] With a Telugu explanation on screen, tapping the speaker icon begins TTS in the Telugu
      voice within 500ms of the tap.
- [ ] Tapping the speaker icon a second time stops TTS immediately; the icon returns to the
      "play" state.
- [ ] Starting explanation TTS while Scripture narration is playing pauses the narration.
- [ ] After explanation TTS finishes, the icon returns to the "play" state; Scripture
      narration is not automatically resumed (user must resume explicitly).
- [ ] If the device has no Telugu TTS voice installed, the speak button is greyed out and a
      one-line note reads "Install Telugu TTS voice in Android Settings to listen".

### Out of Scope

- Streaming TTS (start speaking before Gemini finishes) — the explanation is short; stream
  from cache.
- Auto-explain without explicit user opt-in.
- Custom TTS voices beyond the Android system voices.

### Data Contract Changes

No new domain models. `ReaderUiState` gains:

```kotlin
data class ReaderUiState(
    // … existing fields …
    val explanationSpeaking: Boolean = false,   // NEW — true while explanation TTS is active
    val canSpeakExplanation: Boolean = false     // NEW — false if language not supported
)
```

`ReaderViewModel` gains:

```kotlin
fun speakExplanation()   // starts TTS for the current explanation text
fun stopExplanation()    // stops TTS
```

---

## F-03 Crisis AI Companion

### Problem

The current Crisis Mode shows a curated list of 14 comfort verses. The list is universal
and offline — which is its strength. Its weakness is that it is generic. A user whose
husband is threatening to leave them because of their faith needs different verses than a
user who cannot sleep. A user grieving a child needs different words than a user under
financial despair.

Gemini can receive an open-text description of the situation and return the single most
relevant passage — not a ranked list, not a general comfort, but the right passage for
*this* person at *this* moment — explained compassionately in their language.

The curated offline list remains as the primary, no-network fallback. The AI companion is
the enrichment layer.

### User Stories

| ID | Story |
|----|-------|
| US-03.1 | As a woman in a domestic violence situation who has opened the app at 2am, I want to describe what is happening in my own words and receive the scripture most directly relevant to my situation, in Malayalam, without having to scroll through a generic list. |
| US-03.2 | As a young man who has lost faith and opened the app in despair, I want to type "I don't know if God is real anymore" and receive a thoughtful, compassionate passage — not a pat answer — that invites me to keep seeking. |
| US-03.3 | As a user offline in a remote village, I still want the curated comfort list to work. The AI companion gracefully shows "offline" and falls through to the curated list. |

### Functional Requirements

**FR-03.1 — Free-text situation input**  
The Crisis Mode screen must include an optional text input above the curated verse list.
Placeholder: the Bible language equivalent of "Tell me what's happening…". The field is
optional; the curated list is always visible below it.

**FR-03.2 — AI response format**  
When the user submits a situation description, Gemini must:
1. Choose one passage (book, chapter, verse) most directly relevant.
2. Explain why it applies to this situation — 2–3 sentences maximum, written as if a
   compassionate pastor is speaking directly to the person.
3. Return the text in the Bible language.
4. Lead with comfort; never with judgement or instruction.

**FR-03.3 — Response caching**  
Responses are not cached (each situation is unique and caching personal crisis disclosures
raises privacy concerns). If the user resends the same text offline, show the "offline"
state, not a stored result.

**FR-03.4 — Privacy: no logging**  
The situation text must never be logged, written to Room, or included in any crash report.
The text goes directly from the text field to the Gemini API call and is discarded after
the response is received.

**FR-03.5 — Night window awareness**  
`NightWindow.isNight()` already exists. When it returns true, the Gemini prompt must be
prefaced with a softer system instruction: the responder knows it is the middle of the
night and the person is alone. The response must not open with energetic or instructional
language.

**FR-03.6 — Graceful offline fallback**  
If Gemini is unavailable (offline or not configured), the text input is greyed out with a
one-line message: "AI response needs internet. The verses below work offline." The curated
list remains fully functional.

**FR-03.7 — Spoken response**  
Per F-02, after the AI response is displayed the same "speak" button must be available.

### Acceptance Criteria

- [ ] Typing "My son just died" in Malayalam and submitting returns a response within 10s
      that: (a) begins in Malayalam, (b) references a specific passage about grief or
      resurrection hope, (c) does not include generic comfort-list language.
- [ ] The situation text is not present in any Room table, DataStore entry, or log file
      after the response is returned.
- [ ] With airplane mode on, the text field shows the offline message and the curated
      verse list displays normally.
- [ ] At 2am (NightWindow.isNight returns true), the response tone is quieter and more
      present; it does not begin with an action item.
- [ ] If the user clears the text field or navigates away, the AI response is discarded
      from UI state; no data is retained.

### Out of Scope

- Ongoing conversational multi-turn crisis counselling (single-turn only).
- Referral to human counsellors (may be added in Phase 3 with moderation).
- Recording or replaying crisis sessions.

### New Domain Contracts

```kotlin
// domain/crisis/CrisisAiEngine.kt
interface CrisisAiEngine {
    val isConfigured: Boolean
    suspend fun respond(situation: String, languageCode: String, isNight: Boolean): CrisisAiResult
}

sealed interface CrisisAiResult {
    data class Success(
        val passageRef: String,            // e.g. "Psalm 34:18"
        val osisRef: String,               // e.g. "PSA.34.18"
        val explanation: String            // 2–3 sentences in the Bible language
    ) : CrisisAiResult
    data class Offline(val message: String) : CrisisAiResult
    data class Unavailable(val reason: String) : CrisisAiResult
}
```

`CrisisUiState` additions:

```kotlin
data class CrisisUiState(
    // … existing fields …
    val situationText: String = "",
    val aiResponse: CrisisAiResult? = null,
    val isAiLoading: Boolean = false,
    val aiConfigured: Boolean = false
)
```

---

## F-04 AI Sermon Builder

### Problem

The current Sermon Helper is a local notepad: title, scripture reference, freeform content.
It is completely offline and deliberately minimal. Its weakness is that a village pastor
with no seminary training and a passage to preach on Saturday night still stares at a blank
page.

Gemini can take a passage reference, denomination, and congregation type, and generate a
complete sermon outline with three points, two illustrations drawn from Indian daily life,
and relevant cross-references — all in the preacher's language, in under 30 seconds.

The notepad remains the primary surface. The AI output is an editable starting point, not
a finished product.

### User Stories

| ID | Story |
|----|-------|
| US-04.1 | As a CSI lay preacher preparing Sunday's sermon on John 4 (the Samaritan woman), I want to tap "Build outline" and receive a three-point outline with illustrations relevant to Tamil Nadu village life, in Tamil. |
| US-04.2 | As a Catholic priest preparing a homily for a funeral Mass from 2 Corinthians 5:1, I want Gemini to produce an outline that honours the Catholic sacramental understanding of death and resurrection, not a generic Protestant overview. |
| US-04.3 | As a preacher who receives the AI outline, I want to edit every word of it before saving. The AI draft drops into my text editor as normal editable text. |

### Functional Requirements

**FR-04.1 — "Build outline" button**  
When a scripture reference is entered in the sermon editor, and the Gemini engine is
configured, a "Build outline" button appears below the reference field. It is hidden when
the reference field is empty.

**FR-04.2 — Congregation type selector**  
Before generating, the system offers three congregation profiles (no dialog required — a
segmented control or radio buttons directly in the UI):
- **General** — mixed Sunday congregation.
- **Youth** — teens and young adults, modern references welcome.
- **Grief** — a funeral or memorial service; tone must be comforting, not triumphalist.

**FR-04.3 — Outline format**  
The generated outline must follow this structure precisely:
1. **Introduction** — one sentence that opens with a question or observation the
   congregation will immediately relate to from Indian daily life.
2. **Point 1, 2, 3** — each with a subheading and 1–2 bullet supporting thoughts.
3. **Cross-references** — two or three related passages.
4. **Illustration** — one story or image from Indian village, farming, or family life.
5. **Conclusion** — one sentence closing thought.

**FR-04.4 — Draft insertion**  
After generation, the full outline text is inserted into the `content` field of the active
`SermonDraft`. The user is immediately placed in the edit view. The outline is editable
from the moment it appears.

**FR-04.5 — No outline caching**  
Outlines are not cached in Room; each generation is fresh. The saved sermon (after the
user edits and taps Save) is stored as the user's own content, not as an AI artefact.

**FR-04.6 — Offline state**  
If offline, the "Build outline" button is replaced with a note: "Outline generation
requires internet." The manual editor remains fully functional.

**FR-04.7 — Denomination wiring**  
The sermon AI engine must receive the denomination from `PreferencesStore.setupState` at
call time, not as a user-facing input in the sermon flow.

### Acceptance Criteria

- [ ] Entering "John 4:1-26" as reference for a CSI user, selecting "General", and tapping
      "Build outline" produces a Tamil outline within 30s with three named points and at
      least one South Indian illustration.
- [ ] The same reference for a Catholic user produces an outline that references the
      Sacrament of Baptism (the water imagery) as a Catholic homiletic point.
- [ ] The generated text appears in the `content` editor field, fully editable and saveable
      without triggering another Gemini call.
- [ ] With airplane mode on, "Build outline" button is absent; the reference and content
      fields are fully functional.
- [ ] The user's final saved sermon note contains only the text they saved, regardless of
      how much AI-generated text they deleted.

### Out of Scope

- Generating the full sermon prose (paragraph form) — outline only.
- Uploading or syncing sermon notes.
- Supporting lectionary passage auto-selection for the outline.

### New Domain Contracts

```kotlin
// domain/sermon/SermonAiEngine.kt
enum class CongregationType { GENERAL, YOUTH, GRIEF }

data class SermonOutlineRequest(
    val reference: String,
    val denomination: Denomination?,
    val languageCode: String,
    val congregationType: CongregationType
)

sealed interface SermonOutlineResult {
    data class Success(val outlineText: String) : SermonOutlineResult
    data class Offline(val message: String) : SermonOutlineResult
    data class Unavailable(val reason: String) : SermonOutlineResult
}

interface SermonAiEngine {
    val isConfigured: Boolean
    suspend fun generateOutline(request: SermonOutlineRequest): SermonOutlineResult
}
```

`SermonHelperUiState` additions:

```kotlin
data class SermonHelperUiState(
    // … existing fields …
    val congregationType: CongregationType = CongregationType.GENERAL,
    val isGeneratingOutline: Boolean = false,
    val outlineError: String? = null,
    val canGenerateOutline: Boolean = false  // true when ref non-blank + engine configured
)
```

---

## F-05 Context-Aware Verse Cards

### Problem

The Scripture Card feature (`ScriptureCardViewModel`, `VerseCardSheet`) already generates
a shareable verse image. The missing layer is *discovery*: a user who wants to send a
verse to a sick friend does not know which verse to send. They have to scroll or search.

Gemini can accept a short situation description ("my mother is in hospital", "my friend got
married today", "my student failed the exam") and return the single passage best suited to
share in that moment — along with a short personal message in the user's language.

### User Stories

| ID | Story |
|----|-------|
| US-05.1 | As a user wanting to encourage a friend who failed an exam, I want to type "failed exam" and receive a verse about perseverance, a card I can share to WhatsApp, and a two-line personal note — all in Tamil. |
| US-05.2 | As a user whose neighbour just had a baby, I want a quick verse appropriate for a new birth with a WhatsApp-ready card, without searching for the right passage. |

### Functional Requirements

**FR-05.1 — Situation input**  
A new surface ("Find a verse for someone") accessible from the Share sheet or a dedicated
route. A single text field: "What's the occasion or situation?" (in the Bible language).

**FR-05.2 — Gemini output**  
Gemini returns:
- The OSIS reference of the recommended passage.
- The verse text (fetched from the local active translation after Gemini returns the ref).
- A two-sentence personal message the user can customise before sharing.

**FR-05.3 — Card generation**  
The selected verse is passed to the existing `ScriptureCardRenderer` to produce the
shareable image. No new card rendering logic.

**FR-05.4 — Message pre-population**  
The Gemini-generated personal message pre-populates the text that accompanies the WhatsApp
share intent. The user can edit or delete it before sharing.

**FR-05.5 — Offline fallback**  
When offline, the text input is greyed out with a note. The existing manual "share verse"
flow is always available.

### Acceptance Criteria

- [ ] Typing "wedding today" in Telugu produces a passage from Ruth, Ephesians, or similar
      within 15s, with verse text from the active Telugu translation.
- [ ] The generated personal message is in Telugu and is pre-populated in the share intent.
- [ ] The produced card uses the existing `ScriptureCardRenderer` and is visually identical
      to a manually generated card.
- [ ] The feature is fully offline-safe: no crash when network is absent.

### New Domain Contracts

```kotlin
// domain/share/VerseRecommendationEngine.kt
data class VerseRecommendationRequest(
    val situationText: String,
    val languageCode: String,
    val denomination: Denomination?
)

sealed interface VerseRecommendation {
    data class Success(
        val osisRef: String,          // e.g. "EPH.5.25" — resolved from local DB
        val reference: String,        // e.g. "Ephesians 5:25"
        val verseText: String,        // from active translation
        val personalMessage: String   // 2-sentence note in Bible language
    ) : VerseRecommendation
    data class Offline(val message: String) : VerseRecommendation
    data class Unavailable(val reason: String) : VerseRecommendation
}

interface VerseRecommendationEngine {
    val isConfigured: Boolean
    suspend fun recommend(request: VerseRecommendationRequest): VerseRecommendation
}
```

---

## F-06 Persecution-Aware Comfort

### Problem

Persecution of Christians in India is a documented reality. Users in Odisha, Chhattisgarh,
Manipur, or Uttar Pradesh may be under active threat — job loss, family rejection, physical
danger. No Bible app anywhere acknowledges this. The current Crisis Mode list has some
relevant passages but they are buried in a general comfort list alongside sleeplessness
verses.

This feature is two-layered:
1. A curated offline set of passages organised by threat type — always available, no AI
   required.
2. An optional Gemini layer for open-text situation input (shared with F-03).

### User Stories

| ID | Story |
|----|-------|
| US-06.1 | As a Christian whose employer found out about their faith and threatened their job, I want to quickly find passages about standing firm and God's provision, in my language, without internet. |
| US-06.2 | As a new believer whose parents are forcing them to stop attending church, I want passages about family division that Jesus himself warned about — honestly addressed, not softened. |
| US-06.3 | As a persecuted Christian in a village where the church was burnt down, I want passages about the early church under persecution — not generic comfort, but the theology of suffering for the faith. |

### Functional Requirements

**FR-06.1 — Persecution category taxonomy**  
The feature defines five offline categories with curated verse sets:

| ID | Category | Representative passages |
|----|----------|------------------------|
| `FAMILY_REJECTION` | Family pressure, forced marriage, disownment | Matt 10:34-36; Luke 14:26; Gen 12:1 |
| `JOB_LIVELIHOOD` | Employment threat, economic exclusion | Phil 4:19; Matt 6:33; Heb 11:25-26 |
| `PHYSICAL_DANGER` | Physical threat, violence, mob | Ps 27:1-3; Isa 43:2; Acts 5:41 |
| `SOCIAL_EXCLUSION` | Ostracism, caste rejection, untouchability | Gal 3:28; John 15:18-19; 1 Pet 4:14 |
| `FAITH_CRISIS` | Doubt under pressure, near apostasy | Mark 9:24; Heb 12:1-2; Ps 22:1-2 |

Each category contains 8–12 passages from the 66-book Protestant canon (available in all
translations).

**FR-06.2 — Category selector UI**  
A new section within Crisis Mode (or a separate route): "I am facing persecution". Five
cards, one per category. Tapping a card shows its curated verse set. Entirely offline.

**FR-06.3 — AI enrichment**  
For users who are online, below the curated category verses, the same open-text Gemini
input from F-03 is available, pre-seeded with the selected category as context. The AI
prompt must acknowledge the specific category of pressure and not soften the reality.

**FR-06.4 — Stealth mode integration**  
When Stealth Mode is armed, this section must not appear on the lock screen or in any
notification preview.

### Acceptance Criteria

- [ ] With airplane mode on, all five categories load instantly from the curated verse sets.
- [ ] Passages in `FAMILY_REJECTION` include Matthew 10:34-36 ("I did not come to bring
      peace but a sword") with the full text in the active translation.
- [ ] With Stealth Mode enabled, the persecution category UI is not visible before PIN
      entry.
- [ ] The AI layer (when online) acknowledges the specific category: a PHYSICAL_DANGER
      request in Telugu does not produce a generic "God loves you" response.

### New Domain Contracts

```kotlin
// domain/crisis/PersecutionCompanion.kt
enum class PersecutionCategory(val id: String) {
    FAMILY_REJECTION("family_rejection"),
    JOB_LIVELIHOOD("job_livelihood"),
    PHYSICAL_DANGER("physical_danger"),
    SOCIAL_EXCLUSION("social_exclusion"),
    FAITH_CRISIS("faith_crisis")
}

interface PersecutionCompanion {
    fun categoriesForDenomination(denomination: Denomination?): List<PersecutionCategory>
    fun versesFor(category: PersecutionCategory): List<ReadingRef>
}
```

---

## Cross-Cutting Concerns

### Privacy Requirements (all features)

1. Crisis situation text (F-03) and persecution situation text (F-06) are never written
   to Room, DataStore, or logs. They exist only in ViewModel state for the duration of
   the request.
2. No user-entered text is included in Crashlytics reports.
3. All Gemini API calls use the same `GeminiApi` Retrofit interface with the injected
   key — no new network client.
4. When Stealth Mode is active, no AI-generated content appears in Android's Recent Apps
   thumbnails (the Activity's `FLAG_SECURE` already handles this at the Activity level).

### Performance Requirements

| Feature | Target latency |
|---------|---------------|
| F-01 Cultural lens enhancement | No additional latency (prompt change only) |
| F-02 Oral explanation start | < 500ms from tap to first spoken word |
| F-03 Crisis AI response | < 10s on a 4G connection |
| F-04 Sermon outline | < 30s on a 4G connection |
| F-05 Verse recommendation | < 15s on a 4G connection |
| F-06 Persecution verses (offline) | < 50ms (local data only) |

### Offline-first Guarantee

All AI features degrade gracefully with no crash when offline:
- Features F-01, F-02, F-06 have no online dependency at runtime (F-01 is prompt
  modification; F-02 is local TTS; F-06 curated tier is local data).
- Features F-03, F-04, F-05 show their offline state and fall through to the non-AI tier.
- The `HybridExplanationEngine` (Nano → Cloud) means F-01 may still work offline on
  supported devices via Gemini Nano.

### Feature Flag Assignments

| Feature | Flag | Default |
|---------|------|---------|
| F-01 Cultural Lens | `EXPLAIN_PASSAGE` (existing) | `true` |
| F-02 Oral AI | `ORAL_AI_EXPLANATION` (new) | `false` |
| F-03 Crisis AI | `CRISIS_AI_COMPANION` (new) | `false` |
| F-04 Sermon Builder AI | `SERMON_AI_BUILDER` (new) | `false` |
| F-05 Verse Cards AI | `VERSE_RECOMMENDATION_AI` (new) | `false` |
| F-06 Persecution Comfort | `PERSECUTION_COMFORT` (new) | `true` |

F-01 and F-06 are always-on because they have no network dependency at runtime. The AI
enrichment layers (F-02 through F-05) default off until QA passes.
