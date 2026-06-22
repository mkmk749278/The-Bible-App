# Creating Manna in the Google Play Console

A step-by-step walkthrough, mapped to the assets that already live in this repo.
Do these in order. Anything marked **(repo)** has a source file you can copy from.

> **Prerequisite — developer account.** You need a Google Play Developer
> account ($25 one-time fee) registered to `mulakapati446@gmail.com`. Since 2023,
> *individual* accounts created for personal use must complete identity
> verification and, for some, a closed-test requirement (20 testers for 14 days)
> before production access. Plan for this — it gates your production launch, not
> your internal testing.

---

## 1. Create the app

1. Go to <https://play.google.com/console> → **Create app**.
2. Fill in:
   - **App name:** `Manna — Offline Audio Bible` **(repo:** `fastlane/.../title.txt`**)**
   - **Default language:** English (United States) – `en-US`
   - **App or game:** App
   - **Free or paid:** Free
3. Accept the Developer Program Policies and US export law declarations → **Create app**.

> The package name `com.manna.bible` is **not** chosen here — it is locked in
> from the first AAB you upload (it comes from `applicationId`). Make sure the
> first upload is the real one; the package name can never be changed afterwards.

---

## 2. Set up — the "Dashboard → Set up your app" tasks

Work through the **Set up your app** checklist on the dashboard.

### App access
- Select **All functionality is available without special access**.
- Manna needs no login — every core feature works without an account.

### Ads
- Select **No, my app does not contain ads.** (Confirmed: no ad SDKs.)

### Content rating
- Complete the IARC questionnaire. Use the prepared answers in
  [`02-data-safety-and-rating.md`](02-data-safety-and-rating.md) → expected rating
  is **Everyone / PEGI 3 / 3+**.

### Target audience and content
- **Target age groups:** 13+ (recommended). Manna is a general-audience Bible
  app; choosing 13+ avoids the stricter "Designed for Families" obligations while
  still being honest about the content. If you intend to target under-13s, you
  must comply with the Families policy and complete extra declarations.
- **Appeal to children:** No.

### Data safety
- Complete the **Data safety** form using
  [`02-data-safety-and-rating.md`](02-data-safety-and-rating.md). Summary: **no
  data collected, no data shared.**

### Government apps
- No.

### Financial features
- None.

### Health
- None.

### Privacy policy
- Paste the **public URL** where `PRIVACY_POLICY.md` **(repo)** is hosted.
  See [§6](#6-host-the-privacy-policy) below. A reachable URL is required even
  though the app collects no data.

---

## 3. Store listing (Main store listing)

All text is in **(repo)** `fastlane/metadata/android/en-US/`:

| Play Console field | Source file | Limit |
|--------------------|-------------|-------|
| App name | `title.txt` | 30 chars |
| Short description | `short_description.txt` | 80 chars |
| Full description | `full_description.txt` | 4000 chars |

Graphics are uploaded here too — see
[`03-graphics-checklist.md`](03-graphics-checklist.md) for exact sizes:
- App icon (512×512 PNG)
- Feature graphic (1024×500)
- Phone screenshots (2–8)
- (Optional but recommended) 7" and 10" tablet screenshots

**Category & contact:**
- **App category:** Books & Reference
- **Tags:** Bible, religion, audio (choose the closest available)
- **Contact email:** `mulakapati446@gmail.com`
- **Contact website:** optional (the privacy-policy URL is fine)

---

## 4. Build the release artifact

The signed **AAB** is what Play wants (not the APK). Produce it via CI:

1. Ensure these GitHub Secrets are set (see `CLAUDE.md` → GitHub Secrets):
   `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
2. Trigger the **Android Release** workflow (tag push `vX.Y.Z`, or manual
   `workflow_dispatch`). It runs `bundleRelease` and attaches the AAB to the
   GitHub Release / workflow artifacts.
3. Download `app/build/outputs/bundle/release/app-release.aab`.

> **Play App Signing.** When you create your first release, Play will offer to
> manage your app signing key (recommended). Your CI keystore then becomes the
> *upload* key. Keep both the upload keystore and its passwords safe and backed
> up — losing the upload key is recoverable via support; losing a self-managed
> app signing key is not.

---

## 5. Create the first release (Internal testing → Production)

Promote gradually. Start on **Internal testing**:

1. **Testing → Internal testing → Create new release.**
2. Upload the AAB from step 4.
3. **Release name:** `0.2.3 (5)`. **Release notes:** copy
   `fastlane/metadata/android/en-US/changelogs/5.txt` **(repo)** into the
   `<en-US>` notes block.
4. Save → Review release → **Start rollout to Internal testing.**
5. Add testers (email list or Google Group) and share the opt-in link.

Once validated, promote the same build: **Internal → Closed (if required) →
Open/Production.** For production, fill **Countries/regions** (e.g. India + any
others) and roll out (you can stage to a small %).

> If your developer account is subject to the **closed-testing requirement**
> (individual accounts), you must run a closed test with **20+ testers for 14
> continuous days** before the **Production** track unlocks. Do this early.

---

## 6. Host the privacy policy

Play requires a **publicly reachable URL**. Easiest options:

- **GitHub Pages / raw:** enable Pages on this repo, or use the raw URL of
  `PRIVACY_POLICY.md`. Raw markdown renders as plain text — acceptable, but a
  rendered HTML page is nicer.
- **GitHub Gist** rendered page, or any static host.

Whatever URL you choose, paste it into **Policy → App content → Privacy policy**
*and* into the store listing if prompted. Keep the `Last updated` date current.

---

## Quick pre-submission checklist

- [ ] App created; package `com.manna.bible` locked from first AAB
- [ ] App access = no login required
- [ ] Ads = none
- [ ] Content rating questionnaire submitted (Everyone)
- [ ] Target audience set (13+)
- [ ] Data safety form submitted (no data collected/shared)
- [ ] Privacy policy URL live and pasted
- [ ] Store listing text (title/short/full) filled from repo
- [ ] Icon + feature graphic + ≥2 phone screenshots uploaded
- [ ] Signed AAB uploaded to Internal testing
- [ ] Release notes added
- [ ] Countries/regions selected for the target track
