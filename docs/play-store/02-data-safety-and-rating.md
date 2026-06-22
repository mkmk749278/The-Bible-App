# Data Safety form & Content Rating — prepared answers

These answers are derived directly from the code and
[`/PRIVACY_POLICY.md`](../../PRIVACY_POLICY.md). Re-verify them whenever the app
gains a feature that touches the network, storage, or device identifiers.

**Why "no data collected":** Google defines *collection* as data transmitted off
the device to you or a third party in a way tied to a user. Manna's network calls
send only generic content references (translation/book/chapter) and, optionally,
scripture text for "Explain this passage" — **no user/account/device identifiers,
no personal data**. Everything personal (notes, highlights, bookmarks, prayer
journal, reading position, settings) stays in the local Room/DataStore store and
is excluded from Google auto-backup.

---

## Data safety form — answers

### Data collection and security

| Question | Answer |
|----------|--------|
| Does your app collect or share any of the required user data types? | **No** |
| Is all of the user data encrypted in transit? | **Yes** — all network traffic is HTTPS. |
| Do you provide a way for users to request that their data is deleted? | **Yes** — users can clear app data / uninstall; nothing is held server-side. (Note in the explanation field that no data ever leaves the device.) |

Because the first answer is **No**, the form needs no per-type data mapping. If
the console still asks per category, mark **every** category as *Not collected*
and *Not shared*:

- Location — Not collected / Not shared
- Personal info (name, email, etc.) — Not collected / Not shared
- Financial info — Not collected / Not shared
- Health & fitness — Not collected / Not shared
- Messages — Not collected / Not shared
- Photos & videos — Not collected / Not shared
- Audio files / voice — Not collected / Not shared (TTS + narrated audio are
  played, not recorded or uploaded)
- Files & docs — Not collected / Not shared
- Calendar / Contacts — Not collected / Not shared
- App activity — Not collected / Not shared (no analytics SDK)
- Web browsing — Not collected / Not shared
- App info & performance (crash logs, diagnostics) — Not collected / Not shared
- Device or other IDs — Not collected / Not shared

> **If you later add Firebase Analytics/Crashlytics** (it is in the planned stack
> but **off by default**), you must revisit this form: Crashlytics →
> *App info and performance / Crash logs & Diagnostics*, Analytics →
> *App activity* and possibly *Device or other IDs*. As shipped today, none of
> these SDKs are integrated, so the honest answer is **No data collected**.

---

## Permissions — justification (for review notes if asked)

From `AndroidManifest.xml`:

| Permission | Why |
|------------|-----|
| `INTERNET`, `ACCESS_NETWORK_STATE` | Download Bibles; stream narrated audio; optional cloud "Explain". |
| `POST_NOTIFICATIONS` | Optional daily-verse reminder the user opts into. |
| `RECEIVE_BOOT_COMPLETED` | Re-arm the daily reminder alarm after reboot. |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Keep audio playback alive in the background. |
| `FOREGROUND_SERVICE_DATA_SYNC` | Background Bible download with a progress notification. |

No location, contacts, microphone, camera, SMS, or storage permissions are
requested. There are no high-risk / restricted permissions, so no Permissions
Declaration Form is required.

---

## Content rating (IARC) questionnaire — answers

Category: **Reference, News, or Educational** (or "Utility/Productivity" if that
category is offered — Books & Reference apps usually use the reference path).

Answer **No** to all of the following (Manna contains none of these):

- Violence (cartoon, fantasy, realistic, blood/gore) — **No**
- Sexuality / nudity — **No**
- Profanity or crude humor — **No**
- Controlled substances (drugs, alcohol, tobacco) — **No**
- Gambling (simulated or real) — **No**
- Fear / horror content — **No**
- User-generated content shared with others — **No** (notes/prayers are local-only)
- User-to-user communication / messaging — **No**
- Shares user location with others — **No**
- Digital purchases — **No**
- Personal information shared with third parties — **No**

**Miscellaneous:**
- Does the app share the user's current physical location with other users? **No**
- Does the app allow users to interact or exchange content? **No**
- Is this a web browser or search engine? **No**

**Expected outcome:** Everyone (Google Play) / PEGI 3 / ESRB Everyone / 3+.

> Note on religious content: IARC has no "religion" classification that raises a
> rating. References to biblical events do not count as the questionnaire's
> "violence." Answer truthfully as above and the rating lands at the lowest tier.
