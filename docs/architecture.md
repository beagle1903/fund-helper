# Architecture

Living technical map. Update this file when the code changes. Dated specs under `docs/superpowers/specs/` stay frozen.

## Runtime

- Kotlin, one Gradle module
- Jetpack Compose UI
- Hilt for DI
- Room for local follows + snapshots
- Follow codes also mirrored to Downloads (`com.burha.fundhelper-follows.json`) so they can survive uninstall
- `minSdk 26`, `targetSdk 36` / `compileSdk 36`
- Default locale: Turkish
- `applicationId`: `com.burha.fundhelper`

v1 screens and repository are on `main`, including follow durability (Downloads mirror + `hasFragileUserData`, ADR 006) and Günlük (`AppEventLog`).

## Module boundaries

```
Compose screens → ViewModels → FundRepository → Room
                                 FundRepository → FollowBackup → Downloads file
                                 FundRepository → TefasClient → tefas.gov.tr
                                 FundRepository → ExplanationMapper
                                 FundRepository → AppEventLog
LogsScreen → LogsViewModel → AppEventLog
```

- **Screens:** Watchlist (home, pull-to-refresh, empty CTA, overflow **Günlük**) → Search (code/name) → Detail (price, returns, Pay adedi / Yatırımcı sayısı totals + day-over-day %, type/risk/fees, Turkish explanation; no disclaimer) → Günlük (session events, newest first, **Temizle**). Watchlist cards show an uncolored `Pay … · Kişi …` line on a full-width row under price/return (day-over-day % only). Search cards are unchanged (no pay/kişi line). Watchlist and search use teal Material 3 fund cards (`#038984`); detail uses the same theme with labeled sections and sign-colored returns. Watchlist cards are ordered by headline return, most negative first, most positive last (missing `%` last). Empty watchlist and search **Ara** are full-width. Search **Ara** classifies `RESET` / comma bulk-follow / text search (same **Ara** label, IME Search, no confirm, no new screen). After reset or bulk follow, pop to the watchlist. Watchlist sort is unchanged.
- **ViewModels:** UI state only; never Room or HTTP. Watchlist, search, and detail ViewModels depend on `FundRepository` only. `LogsViewModel` depends on `AppEventLog` only. `WatchlistViewModel` sorts `observeWatchlist()` with `sortByHeadlineReturn`. `SearchViewModel.submit` runs `applySearchCommand`.
- **parseSearchCommand:** pure function in `domain/`. Exact `RESET` (any case, no comma) is wipe. A comma list is bulk-follow of exact codes (`RESET` tokens dropped; `RESET,` is not a wipe). No comma is today's text search.
- **percentChange:** pure function in `domain/`. Day-over-day % from current vs previous; null when either side is missing or previous is zero.
- **FundRepository:** the only UI-facing data API. Follows, snapshots, search, refresh. Search matching folds Turkish letters both ways (i/ı, ş/s, ğ/g, ü/u, ö/o, ç/c). Mirrors follow codes to `FollowBackup`; restores into Room only when the follow table is empty. `followAll` appends exact catalog codes (canonical case, no prefix match) in one backup write; unknown codes and catalog failure add nothing and do not wipe. `clearFollows` empties Room follows and the Downloads file; snapshots stay. A backup write failure after `clearFollows` does not re-insert follows. `refreshFollowed` holds a mutex so overlapping watchlist and detail auto-refreshes share one TEFAS round trip; its merge writes four count fields (`payCount`, `prevPayCount`, `investorCount`, `prevInvestorCount`). Search merge preserves price-window fields (including those counts) when catalog rows lack them.
- **Room:** live follows and last-known fund snapshots (app private sandbox; deleted on uninstall). Snapshot schema version 2; `MIGRATION_1_2` is additive (`ALTER TABLE` for the four count columns). Never destructive.
- **FollowBackup:** on-device JSON of followed codes in shared Downloads. Not a server. Production is `MediaStoreFollowBackup`.
- **TefasClient:** HTTP to current `tefas.gov.tr/api/funds/...` endpoints. Isolated so it can be swapped if the phone is blocked (Akamai / bot protection). Use a browser-like client first.
- **TefasJsonMapper.parseLatestPrices:** latest priced day plus previous priced day; reads `tedPaySayisi` / `kisiSayisi` into current and previous count fields.
- **ExplanationMapper:** pure function from official fields to short Turkish copy. No LLM, no network.
- **AppEventLog:** in-memory ring buffer (cap 100, newest first). `FundRepository` is the only writer. Process death clears it.
- **sortByHeadlineReturn:** pure function. Ascending headline `%`, missing last, code tiebreak. Not advice.

The HTTP library (OkHttp or Ktor) lives only inside `TefasClient`. Choose it at implementation time via current docs (Context7). Do not import it from UI.

## Data flow

1. Search **Ara** classifies the query (`parseSearchCommand`). Text search asks the repository; the repository calls `TefasClient` (and may cache results). Reset and bulk follow call `clearFollows` / `followAll`, then pop to the watchlist.
2. Follow/unfollow writes Room, then mirrors codes to `FollowBackup`. `followAll` does one backup write for the resolved set. `clearFollows` deletes all Room follows, then writes an empty backup.
3. Watchlist and detail read Room first, then refresh **followed funds only** (serialized in `FundRepository`; five-minute skip after a success). On launch / refresh, if Room follows are empty, restore codes from the Downloads file.
4. Detail runs `ExplanationMapper` on official fields already in the snapshot.
5. On HTTP failure: keep Room data, surface a snackbar + retry. Do not delete follows.
6. APK update with `:app:installDebug` (same as `adb install -r`) keeps Room. Uninstall deletes Room; Settings uninstall may prompt to keep it (`hasFragileUserData`). The Downloads file is the on-machine copy. Pull requests and pushes to `main` run `:app:testDebugUnitTest` on GitHub Actions (no live TEFAS, no emulator).

## Failures

- Offline / TEFAS 4xx–5xx / Akamai challenge: last cache stays on screen.
- Catalog failure during bulk follow: add nothing, no wipe, still pop, no snackbar; appends `TefasCatalogError` to `AppEventLog`.
- Backup write failure after RESET: Room stays empty; a stale Downloads file can restore the list immediately on pop (WatchlistViewModel init / refresh), not only on a later launch.
- Backup file unreadable: watchlist stays empty; Room is not wiped.
- Tests use a fake `TefasClient` and a fake `FollowBackup`. Do not hit the live site from unit tests.

## Out of this map (later)

Play listing, privacy policy, a tiny cache backend if device-TEFAS is impossible. Same UI and `FundRepository`; only `TefasClient` (or a new client behind it) would change.
