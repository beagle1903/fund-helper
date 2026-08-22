# Architecture Decision Records (ADR)

## 001 - Explicit git-tracked markdown over a memory plugin

**Context:** Later chats must not re-decide product and architecture. User-level `AGENTS.md` already has an empty `claude-mem` block; we will not depend on it.

**Decision:** Long-term memory is git-tracked files (`AGENTS.md` router, `docs/context.md`, `docs/architecture.md`, this file, dated specs/plans). Short-term handoff is a pruned `progress.md`. Cursor Memories were planned as a 4–6 fact backup ping only — not the source of truth. Same choice as daily-coin.

**Consequences:** A clone orients from the repo. We must prune `progress.md`. No `sessionEnd` hook in v1; update that file by hand. Current Cursor no longer exposes a Memories UI or agent tool, so we do not create user-global rules as a substitute (those would leak into other repos). The five orientation facts live in the markdown above.

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

**Decision:** `ExplanationMapper` turns official TEFAS fields into short Turkish sentences. No LLM, no buy/sell language, no holdings. Screens include “yatırım tavsiyesi değildir”.

**Consequences:** Copy stays testable and SPK-safer. We do not invent facts that are not in the payload.

## 006 - Sideload on A23 first; Play is a later milestone

**Context:** Play Console, privacy policy, and financial-features declaration are real work, but they should follow a used APK.

**Decision:** First deliverable is `assembleDebug` installed over USB. Target API 36 is in Gradle from the start so we do not rebuild only for policy.

**Consequences:** Do not spend v1 on store listing copy. Privacy policy is required before Play because the app uses network.
