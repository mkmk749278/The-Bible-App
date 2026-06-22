# Automated publishing (CD) to Play Console

The release pipeline can upload the signed AAB to Play using the
[Gradle Play Publisher](https://github.com/Triple-T/gradle-play-publisher) (GPP)
plugin. This is wired up but **opt-in and safe by default**.

## What was added

- **Version catalog** (`gradle/libs.versions.toml`): `playPublisher = "3.12.1"` +
  `play-publisher` plugin alias.
- **Root `build.gradle.kts`:** plugin declared `apply false`.
- **`app/build.gradle.kts`:** plugin applied + a `play { }` block:
  ```kotlin
  play {
      track.set("internal")                                  // testing track, not production
      defaultToAppBundles.set(true)                          // upload the AAB, not APKs
      releaseStatus.set(ReleaseStatus.DRAFT)                 // never auto-promote
      resolutionStrategy.set(ResolutionStrategy.IGNORE)      // don't touch listing/graphics
  }
  ```
- **`.github/workflows/android-release.yml`:** a new opt-in step
  *"Publish AAB to Play Console (internal)"*.

## Safety design

- **Credentials are never in source.** GPP reads the service-account JSON from the
  `ANDROID_PUBLISHER_CREDENTIALS` environment variable, which CI populates by
  base64-decoding the `PLAY_STORE_SERVICE_ACCOUNT` GitHub Secret. No file is
  committed and `app/build.gradle.kts` contains no credential path.
- **Opt-in only.** The publish step runs *only* on a manual `workflow_dispatch`
  with `publish_to_play = true`, and *only* when the build is signed. Tag pushes
  build + create a GitHub Release as before, but do **not** push to Play.
- **Draft, internal track.** Even when it runs, it uploads to the `internal`
  testing track as a **draft** — you still finish the rollout by hand in the
  console. Nothing reaches production automatically.
- **Listing untouched.** `ResolutionStrategy.IGNORE` means the binary is uploaded
  but the store listing text/graphics are not overwritten, so the manual /
  fastlane listing stays authoritative.

## One-time setup

1. **Create a service account** with the Play Developer API:
   - Google Cloud Console → create a service account → create a JSON key.
   - Play Console → **Users and permissions** → invite that service-account email
     → grant **Release** permissions for the app (at minimum: manage testing-track
     releases).
2. **Add the secret:** base64-encode the JSON and store it as the
   `PLAY_STORE_SERVICE_ACCOUNT` GitHub Secret:
   ```bash
   base64 -w0 play-service-account.json   # Linux
   base64 -i play-service-account.json | tr -d '\n'   # macOS
   ```
3. **Prerequisite:** the app must already exist in Play Console **and** have had
   one AAB uploaded manually (see [`01-create-app-guide.md`](01-create-app-guide.md)).
   The Play API cannot create a new app or do the very first upload for a fresh
   package.

## How to publish

Run the **Android Release** workflow via *Run workflow* (workflow_dispatch):
- `tag`: e.g. `v0.2.3`
- `publish_to_play`: **true**

The job builds the signed AAB, creates the GitHub Release, then runs
`./gradlew publishReleaseBundle` to upload to the internal track (draft).

## Run it locally (optional)

```bash
export ANDROID_PUBLISHER_CREDENTIALS="$(cat play-service-account.json)"
export KEYSTORE_PATH=/abs/path/release-keystore.jks
export KEYSTORE_PASSWORD=... KEY_ALIAS=... KEY_PASSWORD=...
./gradlew publishReleaseBundle
```

## Changing the target track / promotion

- Promote to a different track: edit `track.set("internal")` (e.g. `"alpha"`,
  `"beta"`, `"production"`) — but prefer promoting in the console UI until the
  pipeline is proven.
- To send live (not draft): change `releaseStatus` to `ReleaseStatus.COMPLETED`
  (or `IN_PROGRESS` for staged %). Keep it `DRAFT` while validating.
- To manage listing text/graphics via GPP later, switch `resolutionStrategy` and
  add the listing under the publisher's expected dir; until then keep listings
  manual to avoid clobbering the fastlane text.
