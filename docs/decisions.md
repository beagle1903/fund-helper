# Architecture Decision Records (ADR)

## 001 - Explicit git-tracked markdown over a memory plugin

**Context:** Later chats must not re-decide product and architecture. User-level `AGENTS.md` already has an empty `claude-mem` block; we will not depend on it.

**Decision:** Long-term memory is git-tracked files (`AGENTS.md` router, `docs/context.md`, `docs/architecture.md`, this file, dated specs/plans). Short-term handoff is a pruned `progress.md`. Cursor product Memories are not available via an agent tool, so the five orientation facts live in `docs/context.md` and a short list in `AGENTS.md`. Same choice as daily-coin.

**Consequences:** A clone orients from the repo. We must prune `progress.md`. No `sessionEnd` hook in v1; update that file by hand. Do not add these facts as user-global Cursor rules (they would leak into other repos).

## 002 - Kotlin + Jetpack Compose, one module

**Context:** Android-first (Samsung A23 sideload, Play later). Need a stack that Play and the phone already understand.

**Decision:** Native Kotlin, Jetpack Compose, Hilt, single module. `minSdk 26`, `targetSdk 36`.

**Consequences:** No Flutter/RN/iOS in v1. Fetch Compose/Hilt/Room/Gradle APIs via Context7 instead of guessing.

## 003 - Local-only storage

**Context:** v1 is a personal watchlist on one phone. Accounts would add a backend, login, and Play privacy work before the app has been used.

**Decision:** Follows and snapshots live in Room on device. No login, no server.

**Consequences:** Fastest path to a sideload APK. Multi-device sync is out of v1. Play privacy policy is still needed later because the app uses the network.

## 004 - Phone talks to TEFAS; isolate the client

**Context:** Real fund names, codes, and prices are required. TEFAS is rate-limited and the 2026 site has bot protection. A cache backend would be more reliable but is a server.

**Decision:** The device calls current `tefas.gov.tr/api/funds/...` JSON APIs. Room caches snapshots. Refresh only followed funds. UI depends on `FundRepository` only; `TefasClient` is swappable. The old `BindHistory*` API is retired — do not use it.

**Consequences:** If Akamai blocks the phone, we keep the same UI and change only `TefasClient` (or revisit a tiny cache backend). First try is a browser-like client.

## 005 - Explanations map official fields; no advice

**Context:** Fund detail should help a non-expert read type, risk, and fees without becoming a consultant.

**Decision:** `ExplanationMapper` turns official TEFAS fields into short Turkish sentences. No LLM, no buy/sell language, no holdings. Screens no longer include an on-screen disclaimer sentence; the mapper still has no buy/sell language.

**Consequences:** Copy stays testable and SPK-safer. We do not invent facts that are not in the payload. Detail no longer shows “Yatırım tavsiyesi değildir.”

## 006 - Follow codes mirrored to Downloads

**Context:** Uninstalling to refresh the launcher icon wiped Room follows. The user asked for the list to be permanent on this phone. v1 has no server or accounts. Frozen v1 said uninstall clears data and `INTERNET` only.

**Decision:** Keep Room as the live follow set. Mirror followed codes (not snapshots) to `Download/com.burha.fundhelper-follows.json` via MediaStore. Restore into Room only when the follow table is empty. Set `android:hasFragileUserData="true"` so Settings uninstall can keep the sandbox. Keep `allowBackup="true"`. No extra dangerous permission, no import UI, no all-files access.

**Consequences:** `adb install -r` already kept Room; that remains the way to update the APK. After uninstall, restore works if the new install can still read the Downloads file (best-effort on API 33+). `adb uninstall` still wipes Room. Auto Backup may also restore if a cloud backup actually ran. The list already lost in the icon-refresh uninstall cannot be recovered.

## 007 - On-screen disclaimer removed

**Context:** v1 showed “Yatırım tavsiyesi değildir.” as body text on detail. The first user does not want that line on the phone.

**Decision:** Remove the string from Detail and `R.string.disclaimer`. Living product docs drop the on-screen requirement. Frozen 2026-08-22 spec is historical. Mapper and screens still must not recommend buy/sell/hold. A store-facing disclaimer waits for Play.

**Consequences:** Detail no longer shows that sentence. Play/SPK copy is a later product decision, not this pass.
