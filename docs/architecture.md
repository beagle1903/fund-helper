# Architecture

Living technical map. Update this file when the code changes. Dated specs under `docs/superpowers/specs/` stay frozen.

## Runtime

- Kotlin, one Gradle module
- Jetpack Compose UI
- Hilt for DI
- Room for local follows + snapshots
- `minSdk 26`, `targetSdk 36` / `compileSdk 36`
- Default locale: Turkish
- `applicationId`: `com.burha.fundhelper`

Skeleton through `TefasClient` exists on `feat/v1-watchlist` (Tasks 1–4). Room persistence, `FundRepository`, and the three screens are not in yet.

## Module boundaries

```
Compose screens → ViewModels → FundRepository → Room
                                 FundRepository → TefasClient → tefas.gov.tr
                                 FundRepository → ExplanationMapper
```

- **Screens:** Watchlist (home, pull-to-refresh, empty CTA) → Search (code/name) → Detail (price, returns, type/risk/fees, Turkish explanation, disclaimer).
- **ViewModels:** UI state only. They depend on `FundRepository`, never on Room or HTTP.
- **FundRepository:** the only UI-facing data API. Follows, snapshots, search, refresh.
- **Room:** persisted follows and last-known fund snapshots.
- **TefasClient:** HTTP to current `tefas.gov.tr/api/funds/...` endpoints. Isolated so it can be swapped if the phone is blocked (Akamai / bot protection). Use a browser-like client first.
- **ExplanationMapper:** pure function from official fields to short Turkish copy. No LLM, no network.

The HTTP library (OkHttp or Ktor) lives only inside `TefasClient`. Choose it at implementation time via current docs (Context7). Do not import it from UI.

## Data flow

1. Search asks the repository; the repository calls `TefasClient` (and may cache results).
2. Follow/unfollow writes Room only.
3. Watchlist and detail read Room first, then refresh **followed funds only**.
4. Detail runs `ExplanationMapper` on official fields already in the snapshot.
5. On HTTP failure: keep Room data, surface a snackbar + retry. Do not delete follows.

## Failures

- Offline / TEFAS 4xx–5xx / Akamai challenge: last cache stays on screen.
- Tests use a fake `TefasClient`. Do not hit the live site from unit tests.

## Out of this map (later)

Play listing, privacy policy, a tiny cache backend if device-TEFAS is impossible. Same UI and `FundRepository`; only `TefasClient` (or a new client behind it) would change.
