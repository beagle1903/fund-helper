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

v1 screens and repository are on `main`. Follow durability (Downloads mirror + `hasFragileUserData`) is a later amendment.

## Module boundaries

```
Compose screens → ViewModels → FundRepository → Room
                                 FundRepository → FollowBackup → Downloads file
                                 FundRepository → TefasClient → tefas.gov.tr
                                 FundRepository → ExplanationMapper
```

- **Screens:** Watchlist (home, pull-to-refresh, empty CTA) → Search (code/name) → Detail (price, returns, type/risk/fees, Turkish explanation, disclaimer). Watchlist and search use teal Material 3 fund cards (`#038984`); detail uses the same theme with labeled sections and sign-colored returns.
- **ViewModels:** UI state only. They depend on `FundRepository`, never on Room or HTTP.
- **FundRepository:** the only UI-facing data API. Follows, snapshots, search, refresh. Search matching folds Turkish letters both ways (i/ı, ş/s, ğ/g, ü/u, ö/o, ç/c). Mirrors follow codes to `FollowBackup`; restores into Room only when the follow table is empty.
- **Room:** live follows and last-known fund snapshots (app private sandbox; deleted on uninstall).
- **FollowBackup:** on-device JSON of followed codes in shared Downloads. Not a server. Production is `MediaStoreFollowBackup`.
- **TefasClient:** HTTP to current `tefas.gov.tr/api/funds/...` endpoints. Isolated so it can be swapped if the phone is blocked (Akamai / bot protection). Use a browser-like client first.
- **ExplanationMapper:** pure function from official fields to short Turkish copy. No LLM, no network.

The HTTP library (OkHttp or Ktor) lives only inside `TefasClient`. Choose it at implementation time via current docs (Context7). Do not import it from UI.

## Data flow

1. Search asks the repository; the repository calls `TefasClient` (and may cache results).
2. Follow/unfollow writes Room, then mirrors codes to `FollowBackup`.
3. Watchlist and detail read Room first, then refresh **followed funds only**. On launch / refresh, if Room follows are empty, restore codes from the Downloads file.
4. Detail runs `ExplanationMapper` on official fields already in the snapshot.
5. On HTTP failure: keep Room data, surface a snackbar + retry. Do not delete follows.
6. APK update with `adb install -r` keeps Room. Uninstall deletes Room; Settings uninstall may prompt to keep it (`hasFragileUserData`). The Downloads file is the on-machine copy.

## Failures

- Offline / TEFAS 4xx–5xx / Akamai challenge: last cache stays on screen.
- Backup file unreadable: watchlist stays empty; Room is not wiped.
- Tests use a fake `TefasClient` and a fake `FollowBackup`. Do not hit the live site from unit tests.

## Out of this map (later)

Play listing, privacy policy, a tiny cache backend if device-TEFAS is impossible. Same UI and `FundRepository`; only `TefasClient` (or a new client behind it) would change.
