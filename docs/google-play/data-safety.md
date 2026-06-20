# Play Console — Data Safety form answers

Use these answers in **Play Console → App content → Data safety**. They reflect the
app's actual behaviour (verified in code: no analytics, no ads, no account, local-only
user data; network only to `bible.helloao.org` and, optionally, the Gemini API).

## Overview questions

- **Does your app collect or share any of the required user data types?** → **No.**
  - The app stores user content (notes, highlights, prayers, settings) **only on the
    device**. It is never transmitted to us or a third party, so it is not "collected"
    or "shared" in Play's sense.
- **Is all of the user data encrypted in transit?** → **Yes** (all network calls are
  HTTPS). *(Answer this if the form surfaces it even with "No data collected.")*
- **Do you provide a way for users to request that their data be deleted?** → **Yes** —
  users delete all data by clearing app data or uninstalling (no server-side data
  exists).

## If the reviewer asks about the "Explain this passage" / AI feature

This is optional and processes **scripture text only** (no personal data, no account, no
identifiers). On supported devices it runs **on-device** (Gemini Nano) and sends
nothing off the device. When the cloud engine is used, the request goes to Google's
Generative Language API and is used only to return an explanation. Declare this as
**ephemeral processing, not collection** — no user data is stored or shared.

## Data types: select NONE in every category

Personal info, Financial info, Location, Web history, Contacts, Photos/videos,
Audio files, Files/docs, Calendar, App activity, Device IDs, Health → **none collected,
none shared.**

## Security practices

- Data is encrypted in transit (HTTPS). ✔
- The app follows the Families/Designed-for-Families-friendly practice of no ads and no
  tracking.
- Independent security review: not yet (leave unchecked).
