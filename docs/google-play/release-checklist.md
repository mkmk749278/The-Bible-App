# Google Play — release checklist for Manna

A step-by-step path from "signed AAB" to "live on Play". The app is already
technically release-ready: the CI **Android Release** workflow builds a **signed AAB**
(`app-release.aab`) and attaches it to each GitHub Release.

## 0. One-time: signing model (Play App Signing)

Manna signs with the keystore in your GitHub secrets (`KEYSTORE_BASE64`, etc.). On Play:
- When you create the app, **enroll in Play App Signing** (default).
- Your keystore becomes the **upload key**. Google holds the final app-signing key and
  re-signs each release. Keep the upload keystore safe — you can reset it via Play
  support, but never lose your only copy.

## 1. Create the app

Play Console → **Create app**:
- App name: **Manna** (or your localized store name)
- Default language: English (United States) — add Tamil/Hindi/Telugu/Malayalam later
- App or game: **App**; Free or paid: **Free**
- Declarations: confirm Developer Program Policies and US export laws.

## 2. Store listing

Copy is version-controlled under `fastlane/metadata/android/en-US/`:
- **App name** → `title.txt`
- **Short description** (≤80 chars) → `short_description.txt`
- **Full description** (≤4000 chars) → `full_description.txt`
- **Release notes** → `changelogs/3.txt` (filename = versionCode)

### Graphic assets you still need to create (not code — design deliverables)
- **App icon:** 512×512 PNG, 32-bit (the in-app adaptive icon is the gold "M" on dark
  navy `#080C14` — match it).
- **Feature graphic:** 1024×500 PNG/JPG.
- **Phone screenshots:** 2–8, min 320px, 16:9 or 9:16 (capture: reader, audio bar,
  Simplified Mode, the Rosary/Japamala, the calendar, daily verse).
- *(Optional)* 7" and 10" tablet screenshots; a promo video (YouTube URL).

## 3. App content (left nav → "App content")

- **Privacy policy:** host `PRIVACY_POLICY.md` and paste the URL. Easiest: enable
  **GitHub Pages** for this repo, or paste the raw GitHub URL of `PRIVACY_POLICY.md`.
- **Data safety:** fill using `docs/google-play/data-safety.md` → result is
  "**No data collected or shared**".
- **Content rating:** fill using `docs/google-play/content-rating.md` → result
  "**Everyone / PEGI 3**".
- **Target audience & content:** target 13+ (or 18+) — not designed for children; do not
  opt into the Designed-for-Families programme.
- **Ads:** declare **No ads**.
- **Government app:** No.
- **Foreground service:** the app currently declares **no** foreground service (audio
  plays only while the app is in the foreground). If/when background playback + a
  `mediaPlayback` foreground service is added, declare it here. *(Tracked as a future
  enhancement — see "Known follow-ups".)*

## 4. Upload the build

Two options:

**A. Manual (recommended for the first release)**
- Testing → **Internal testing** → Create release → upload `app-release.aab` from the
  GitHub Release **v0.2.1** (or run the Android Release workflow and download it).
- Add your tester email list, roll out, and install via the opt-in link.

**B. Automated** (Gradle Play Publisher) — wire later, see below.

## 5. Promote

Internal testing → Closed testing (optional) → **Production**. Production review for a
new developer account can take several days; Play also requires a minimum testing period
for new personal developer accounts before production.

---

## Optional: automate AAB upload (Gradle Play Publisher)

Per CLAUDE.md, automated upload uses `com.github.triplet.play`. This is intentionally
**not wired yet** (it needs a Play service-account JSON and would otherwise no-op). To
enable:

1. Play Console → Setup → API access → create/link a **service account**, grant it
   "Release manager", download the JSON.
2. Base64-encode it and store as the `PLAY_STORE_SERVICE_ACCOUNT` GitHub secret.
3. In `app/build.gradle.kts`:
   ```kotlin
   plugins { id("com.github.triplet.play") version "3.11.0" }
   play {
       serviceAccountCredentials.set(file("play-service-account.json"))
       track.set("internal")
       defaultToAppBundles.set(true)
   }
   ```
4. In `.github/workflows/android-release.yml`, after the build, decode the secret to
   `app/play-service-account.json` and run `./gradlew publishBundle`. The store listing
   under `fastlane/metadata/...` is picked up automatically.

## Known follow-ups (not Play blockers)

- **Background audio + lock-screen controls:** add a Media3 `MediaSessionService` with a
  `mediaPlayback` foreground service so narrated/TTS audio continues with the screen off.
  Declare the foreground service in Play Console at that time.
- **Localized store listings** for Tamil/Hindi/Telugu/Malayalam (add
  `fastlane/metadata/android/<locale>/`).
