# Architecture

Living technical map. Dated specs under `docs/superpowers/specs/` stay frozen in the repo.

## Runtime

| Item | Value |
| --- | --- |
| Language | Kotlin, one Gradle module |
| UI | Jetpack Compose |
| DI | Hilt |
| Local storage | Room (follows + snapshots) |
| Follow backup | Downloads mirror (`com.burha.fundhelper-follows.json`) |
| SDK | `minSdk 26`, `targetSdk 36` / `compileSdk 36` |
| Locale | Turkish (default) |
| Package | `com.burha.fundhelper` |

## Module boundaries

```
Compose screens → ViewModels → FundRepository → Room
                                 FundRepository → FollowBackup → Downloads file
                                 FundRepository → TefasClient → tefas.gov.tr
                                 FundRepository → ExplanationMapper
                                 FundRepository → AppEventLog
LogsScreen → LogsViewModel → AppEventLog
```

### Screens

| Screen | Role |
| --- | --- |
| **Watchlist** | Home. Pull-to-refresh, empty CTA, overflow **Günlük**. Cards sorted by headline return (most negative first). |
| **Search** | Code/name search. **Ara** handles text search, comma bulk-follow, or `RESET`. |
| **Detail** | Price, returns, Pay adedi / Yatırımcı sayısı totals + day-over-day %, type/risk/fees, Turkish explanation. |
| **Günlük** | Session event log (newest first, **Temizle**). In-memory only; process death clears it. |

### Key components

- **FundRepository** — only UI-facing data API. Follows, snapshots, search, refresh.
- **TefasClient** — HTTP to `tefas.gov.tr/api/funds/...`. Swappable if Akamai blocks the phone.
- **ExplanationMapper** — official TEFAS fields → short Turkish copy. No LLM.
- **FollowBackup** — on-device JSON in Downloads (not a server).
- **AppEventLog** — in-memory ring buffer (cap 100). `FundRepository` is the only writer.

HTTP (OkHttp) lives only inside `TefasClient`. UI never imports it.

## Data flow

1. Search **Ara** classifies the query. Text search → repository → `TefasClient`. Reset/bulk follow → `clearFollows` / `followAll`, then pop to watchlist.
2. Follow/unfollow writes Room, then mirrors codes to `FollowBackup`.
3. Watchlist and detail read Room first, then refresh **followed funds only** (serialized; five-minute skip after success). Empty Room → restore from Downloads file.
4. Detail runs `ExplanationMapper` on fields already in the snapshot.
5. HTTP failure: keep Room data, snackbar + retry. Do not delete follows.
6. `:app:installDebug` keeps Room. Uninstall deletes Room; Downloads file is the on-machine copy.

## Failures

| Condition | Behavior |
| --- | --- |
| Offline / TEFAS 4xx–5xx / Akamai challenge | Last cache stays on screen |
| Catalog failure during bulk follow | Add nothing, no wipe; `TefasCatalogError` in Günlük |
| Backup write failure after RESET | Room empty; stale Downloads may restore on next refresh |
| Backup file unreadable | Watchlist empty; Room not wiped |

Unit tests use fake `TefasClient` and fake `FollowBackup`. No live TEFAS in CI.

## Out of this map (later)

Play listing, privacy policy, a tiny cache backend if device-TEFAS is impossible. Same UI and `FundRepository`; only `TefasClient` (or a new client behind it) would change.

---

*Canonical version: [docs/architecture.md](https://github.com/beagle1903/fund-helper/blob/main/docs/architecture.md)*
