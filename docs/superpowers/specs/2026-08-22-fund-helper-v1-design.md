# fund-helper v1 — Design Spec

**Date:** 2026-08-22
**Status:** Approved (frozen). Implementation planning and Android code come after this file, not in it.
**Source of truth for this freeze:** the approved plan `begin_fund-helper_0fec6e52.plan.md`, plus living product docs (`docs/context.md`, `docs/architecture.md`, `docs/decisions.md`).
**This file:** dated product design. Do not treat living docs as license to change v1 scope. If code and this spec diverge, update living maps; change this file only with an explicit product decision.

## 1. Goal

Ship a **follow-only Android watchlist** of Turkish TEFAS yatırım fonları that the developer can sideload onto a Samsung Galaxy A23.

The user searches funds by code or name, follows or unfollows them, and sees latest price and returns. Fund detail explains type, risk, and fees in short Turkish mapped from official TEFAS fields. The app is informational. It is not a chat consultant, broker, portfolio ledger, or investment adviser.

v1 is **one product**: a single-module Kotlin Compose app with local Room storage and a device-side TEFAS JSON client. Cursor harness, markdown memory, and Play Console work are not part of this spec’s implementation surface.

## 2. Users and delivery

- **Now:** the developer. First deliverable is a **debug APK** installed over USB on a Samsung Galaxy A23 (`assembleDebug`, then `adb install -r`). Confirm package `com.burha.fundhelper`.
- **Later (not this implementation plan):** Play Store users who already look up fonlar on tefas.gov.tr and want a simple follow list. Play Console, store listing, privacy policy (the app uses the network), financial-features declaration, and extra SPK-facing store copy wait until the A23 APK has been used.

`targetSdk` / `compileSdk` **36** are set in Gradle from the first skeleton so a later Play pass does not exist only to bump API level.

## 3. Product scope (v1)

In:

1. Search **YAT (yatırım fonları)** by fund **code** or **name**.
2. **Follow** and **unfollow**. The watchlist is the home screen.
3. Show **latest unit price** and **returns** on the watchlist and on fund detail.
4. On detail: type, risk, fees, a short Turkish explanation from official fields, and the disclaimer **“Yatırım tavsiyesi değildir.”**
5. Local follows and last-known snapshots on device.
6. Pull-to-refresh of **followed funds only**.
7. Empty, error, and offline states that never wipe the follow list.

## 4. Non-goals (out of v1)

Do not implement any of the following in the v1 plan:

- BES-only focus, BEFAS as the primary universe, or default search of EMK / BYF / GYF / GSYF
- Charts, sparklines, or custom date-range history
- Holdings, unit counts, TL amounts, cost basis, or portfolio totals
- Accounts, login, cloud sync, or any backend (including a TEFAS cache server)
- iOS or non-Android clients
- LLM / chat consultant
- Buy or sell recommendations, target prices, “size uygun” language, or ranking “best funds”
- Play Console listing, privacy-policy page, financial-features declaration, `bundleRelease`, or Play upload
- Background periodic sync (WorkManager / alarms). Refresh is user-initiated (screen open + pull-to-refresh)
- HTML scraping of tefas.gov.tr
- The retired `BindHistory*` / `fundturkey.com.tr/api/DB/BindHistory*` APIs
- Extra Cursor plugins, `sessionEnd` hooks, custom MCP servers, Play-store skills, or user `settings.json` changes
- Multi-module Gradle, Flutter, React Native, or a second app flavor for store vs sideload

If device-TEFAS later proves impossible on the A23, a cache backend is a **separate product decision**. It is not a v1 task. v1 still isolates `TefasClient` so UI and `FundRepository` would not change.

## 5. Product rules

- **No investment advice.** Screens never recommend buying, selling, holding, or switching. Turkish copy maps official fields only. Inventing missing numbers or types is forbidden.
- **Disclaimer:** Fund detail always shows **“Yatırım tavsiyesi değildir.”** as visible body text (not only in an overflow menu).
- **Language:** Default locale is Turkish. All user-visible strings live in `res/values` (Turkish). Do not ship English UI as the default.
- **Local-only:** No login, no accounts, no app server. Follows and snapshots live in Room on the phone.
- **Refresh budget:** TEFAS is rate-limited and the 2026 site has bot protection. Refresh **only followed funds**. Search is on demand. Do not bulk-download the whole market on a timer.
- **Failure:** Network or TEFAS errors keep the last cache and the follow list. Never delete follows because a call failed.

Launcher label: `fund-helper`. `applicationId`: `com.burha.fundhelper`.

## 6. Architecture

One Gradle **application** module. Kotlin. Jetpack Compose UI. Hilt DI. Room. `minSdk 26`, `compileSdk 36`, `targetSdk 36`.

```
Compose screens → ViewModels → FundRepository → Room
                                 FundRepository → TefasClient → tefas.gov.tr
                                 FundRepository → ExplanationMapper
```

| Unit | Responsibility | Depends on | Must not depend on |
|------|----------------|------------|--------------------|
| Screens (Watchlist, Search, Detail) | Render state, send user events | Their ViewModel | Room, HTTP, `TefasClient` |
| ViewModels | Immutable UI state (`StateFlow` + data class). Follow/refresh/search as explicit functions | `FundRepository` only | Room, HTTP, `TefasClient` |
| `FundRepository` | Only UI-facing data API: observe watchlist, search, follow/unfollow, refresh followed, observe one fund | Room, `TefasClient`, `ExplanationMapper` | Compose |
| Room | Persist follows and last-known fund snapshots | Android SQLite | TEFAS HTTP |
| `TefasClient` | All HTTP to current `https://www.tefas.gov.tr/api/funds/...` JSON | OkHttp | Compose, ViewModels, Room |
| `ExplanationMapper` | Pure map from official fields to short Turkish sentences | Domain types only | Network, Room, Android framework |

If TEFAS blocks the phone, **swap `TefasClient` only**. `FundRepository` and UI stay.

HTTP stack: **OkHttp**. Fetch current OkHttp + Compose/Hilt/Room APIs via Context7. Only `TefasClient` (and its tests) import OkHttp. UI must not.

Hilt: ViewModels and `FundRepository` are injected. Tests replace `TefasClient` with a fake.

## 7. Screens

Single `ComponentActivity`. Compose Navigation. Start destination: Watchlist.

Routes:

- `watchlist`
- `search`
- `detail/{fundCode}`

### 7.1 Watchlist (home)

- Lists followed funds: **code**, **name**, **latest unit price**, **headline return** (see §8.3), and **snapshot time** (`fetchedAt`) whenever a snapshot exists.
- Tap row → Detail.
- Unfollow control on the row (icon button). Unfollow does not require opening Detail.
- Pull-to-refresh **always** calls `FundRepository.refreshFollowed()`.
- When Watchlist is shown with a non-empty follow list, also call `refreshFollowed()` unless a call already **succeeded** in this process in the last 5 minutes (`FundRepository` holds that timestamp in memory). Cold start always attempts refresh. Empty follow list: skip network.
- **Empty:** title `Takip ettiğiniz fon yok.` and CTA `Fon ara` → Search. This is not an error.
- **Error with cache:** list stays; snackbar with retry (see §10).
- **Error with no snapshots yet but follows exist:** keep follow rows (code at minimum); snackbar + retry. Do not drop the follow list.

### 7.2 Search

- Reached from Watchlist empty CTA and from a Search action on Watchlist (app bar).
- Query field: fund **code** or **name**.
- Universe: **YAT only**. Do not query EMK/BES as the default or primary set.
- Matching: trim query; case-insensitive. A fund matches if `code` starts with the query **or** `name` contains the query.
- Empty query: no network; show `Kod veya fon adı yazın.` Do not list the whole market.
- Results: code, name, follow/unfollow control on the row; tap row → Detail.
- Search runs on **explicit submit** (IME search action or on-screen search button), not on each keystroke. Upsert rules: §8.4.
- No results: `Sonuç bulunamadı.` Failure: snackbar `TEFAS verisi alınamadı.` + `Yeniden dene`; do not clear existing follows.

### 7.3 Detail

- Shows one fund: code, name, latest unit price, price date if the payload has one, snapshot time, **all return periods present in the snapshot** (see §8.3), official **type**, **risk**, **fees**, the `ExplanationMapper` paragraph, and **“Yatırım tavsiyesi değildir.”**
- Follow/unfollow toggle.
- Pull-to-refresh and snackbar retry on Detail call `refreshFollowed()` (followed codes only; see §8.4). If this fund is not followed, Detail does not call `refreshFollowed()`; it shows the Room snapshot from search.
- Missing required identity (unknown code with no cache): Turkish error + back; still do not mutate other follows.
- Detail **renders** the `ExplanationMapper` paragraph from `FundRepository`. ViewModels and screens do not call `ExplanationMapper` themselves.

Material 3, system light/dark. No custom design system in v1.

## 8. TEFAS data (2026 JSON API)

### 8.1 Allowed transport

- **Use:** JSON under `https://www.tefas.gov.tr/api/funds/...` (2026 Next.js site). No API key and no TEFAS login for this public JSON.
- **Do not use:** `BindHistory*`, `fundturkey.com.tr/api/DB/BindHistory*`, or any retired SOAP/HTML-bind API.
- **Do not:** scrape HTML, parse the public website DOM, or drive a WebView as the data source.
- **First implementation:** send a desktop Chrome `User-Agent`. TEFAS may still challenge the phone (Akamai). That is an expected risk, not a spec hole.

Concrete path selection (for example `fonGnlBlgSiraliGetir` for fund info) happens inside `TefasClient`. Additional `/api/funds/*` **JSON** calls are allowed only when one response does not include type, risk, or fees needed for Detail. Portfolio-breakdown endpoints are **not** required for v1 (no holdings UI).

### 8.2 Domain snapshot (UI contract)

`TefasClient` maps JSON into a domain snapshot. Field names on the wire stay inside the client. The rest of the app uses:

| Field | Meaning |
|-------|---------|
| `code` | Official fund code (primary key) |
| `name` | Official fund name |
| `kind` | `YAT` for v1 search/watchlist |
| `price` | Latest unit price (birim pay fiyatı), if present |
| `priceDate` | Official price date, if present |
| `returns` | Period key → percent, only for periods TEFAS sent. Keys from this set when present: `1D`, `1W`, `1M`, `3M`, `6M`, `12M`, `36M`, `60M` |
| `fundType` | Official type/class text or code, if present |
| `risk` | Official risk value/text, if present |
| `fees` | List of official fee pairs `(label, value)` from the payload (include yönetim ücreti when TEFAS sends it). Empty if none |
| `fetchedAt` | Device clock when this snapshot was stored |

If a field is absent in JSON, store `null` / omit the period. Do not compute fake prices, types, or returns.

### 8.3 Which returns to show

v1 does **not** request chart history. It displays return figures that already arrive on the fund-info snapshot.

- **Watchlist headline return:** first present key in this order: `1M`, `1D`, `1W`, `3M`, `6M`, `12M`, `36M`, `60M`. If none, show `—` (not a fabricated number).
- **Detail:** list every present key from that same order. Omit missing periods. Do not interpolate.

Do not add extra HTTP only to fill a nicer return table.

### 8.4 Search vs refresh

- **YAT catalog (Search):** first successful Search submit in a process calls `TefasClient` for the YAT fund-info JSON **once** and keeps the list in memory. Later submits in that process filter the catalog. Search retry refetches the catalog. Empty query does not load the catalog.
- **Search upsert:** `FundRepository.search(query)` filters the catalog and upserts Room snapshots **for matches only** so Detail can open offline. That is not a watchlist refresh.
- **Watchlist refresh:** `FundRepository.refreshFollowed()` updates Room **only for currently followed codes**.
  - If the 2026 JSON API offers a per-code fund-info call, use it: one HTTP request per followed code, **sequential** (no parallel fan-out).
  - If it only offers a bulk YAT fund-info list, **one** bulk call is allowed; parse the list and upsert Room **followed codes only**. Do not write unfollowed rows on this path.
- Do not start a catalog download from Watchlist open unless refresh is using the bulk fallback above.

### 8.5 Turkish UI copy (v1)

| Surface | Text |
|---------|------|
| Watchlist empty title | Takip ettiğiniz fon yok. |
| Watchlist empty CTA | Fon ara |
| Search field hint | Kod veya fon adı |
| Search empty query | Kod veya fon adı yazın. |
| Search no results | Sonuç bulunamadı. |
| Fetch error snackbar | TEFAS verisi alınamadı. |
| Snackbar retry | Yeniden dene |
| Detail disclaimer | Yatırım tavsiyesi değildir. |
| Missing official field (mapper) | TEFAS kaydında bu bilgi yok. |

## 9. Persistence (Room)

Two concerns, same database:

1. **Follows:** set of fund codes the user follows. Follow writes Room only. Unfollow deletes the follow row. Unfollow does **not** delete the snapshot (re-follow can show last cache). Unfollowed snapshots are **not** shown on Watchlist and are **not** refreshed until followed again.
2. **Snapshots:** last-known domain snapshot per `code`, including `fetchedAt`.

Watchlist and Detail **read Room first**, then refresh followed funds. UI must remain usable offline with last snapshots.

No encrypted backup requirement in v1. Uninstall clears data (normal Android).

`versionCode` 1, `versionName` `0.1.0`. Debug minify/shrink off.

Android permission: **`INTERNET` only**. No accounts, location, contacts, or notifications permission in v1.

## 10. Errors and cache

Treat offline, DNS failure, TLS failure, HTTP 4xx/5xx, empty/malformed JSON, and Akamai/bot-challenge HTML-instead-of-JSON as **fetch failure**.

| Condition | Behaviour |
|-----------|-----------|
| Refresh fails, Room has snapshots | Keep list and follows. Snackbar. Retry runs `refreshFollowed()`. |
| Refresh fails, follows exist, no snapshots | Show followed codes (and names if cached from search). Snackbar + retry. **Do not delete follows.** |
| Search fails | Snackbar + retry search. Follow list unchanged. |
| TEFAS returns a challenge page | Same as fetch failure. Do not parse it as funds. |
| Mapper field missing | Explain that the official field is absent. Do not invent. |
| App process killed | Follows and snapshots still in Room on next launch. |

Snackbar is the error surface (not a full-screen wipe of the watchlist). Retry is always explicit (snackbar action and/or pull-to-refresh).

Never: clear the follow table, clear all snapshots, or sign the user out (there is no session) because TEFAS failed.

## 11. ExplanationMapper (no advice)

Pure function: official snapshot fields → short Turkish sentences for **type**, **risk**, and **fees**.

- No LLM, no network, no buy/sell/hold language (“alın”, “satın”, “satmayın”, “hedef fiyat”, “size uygun”).
- If type, risk, or a fee field is missing, say that it is not in the TEFAS record used for this snapshot.
- Mapper output **does not** include the disclaimer sentence. Detail **always** renders **“Yatırım tavsiyesi değildir.”** as its own Text below the mapper paragraph.

Example of allowed vs forbidden tone (not literal production copy, but the rule):

- Forbidden: “Bu fonu alın; düşük riskli ve getirisi yüksek.”
- Allowed: “Fonun resmi türü … Risk değeri TEFAS kaydındaki skorudur. Yönetim ücreti resmi kayıtta …”

## 12. Testing

Unit/instrumentation tests **must not** hit live `tefas.gov.tr`. Inject a **fake `TefasClient`**.

Minimum coverage for the v1 plan:

1. **`ExplanationMapper`:** maps present fields; missing fields produce an explicit absence sentence; output contains no buy/sell language.
2. **`FundRepository`:** follow/unfollow persistence; watchlist reads follows; refresh updates only followed codes; on client failure, follows and previous snapshots remain; search does not require a fund to be followed.
3. **`TefasClient` mapping (fake JSON fixtures):** parses a representative 2026 `/api/funds/` JSON payload into the domain snapshot; rejects HTML/challenge bodies as failure; never references `BindHistory*`.

Compose UI tests are **not** in the v1 success bar. Mapper, repository, and fixture-mapping tests are required.

## 13. Implementation sequence (single plan)

One implementation plan, in this order:

1. Gradle Compose app skeleton, Hilt, Room, Turkish default locale, `applicationId` `com.burha.fundhelper`, `minSdk 26`, `compileSdk`/`targetSdk` 36, `INTERNET` permission.
2. Domain snapshot types + `ExplanationMapper` with unit tests.
3. `TefasClient` against current `tefas.gov.tr/api/funds/...` JSON (not `BindHistory*`). Fake client + fixture tests.
4. Room + `FundRepository` (follows + snapshots + refresh-followed + search).
5. Three screens with empty/error/offline states and the disclaimer on Detail.
6. `assembleDebug`; developer installs on the A23 over USB and confirms `com.burha.fundhelper`.

Play Store work is a **later milestone**, not a task in that plan.

## 14. Risk (expected, not in-scope to “solve with a backend”)

TEFAS may challenge a mobile user-agent (Akamai). v1’s first try is a desktop Chrome `User-Agent` from the phone. If that fails on the A23, keep the same UI and change only `TefasClient`. A cache backend stays a later decision, not this plan. Isolation of the client from day one is the mitigation.

## 15. Success criteria

v1 is done when all of the following are true:

- Debug APK installs on the A23 as `com.burha.fundhelper`.
- User can search YAT funds by code/name, follow and unfollow, and see the watchlist after restart (Room).
- Watchlist and Detail show last-known price/returns from cache when the network fails; follows are still there; snackbar + retry works.
- Detail shows type/risk/fees explanation from official fields plus **“Yatırım tavsiyesi değildir.”**
- Unit tests for mapper and repository pass without live TEFAS.
- No Play listing, no accounts, no charts, no holdings, no LLM.
