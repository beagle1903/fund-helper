# In-app event log (Günlük) — Design Spec

**Date:** 2026-08-31
**Status:** Approved for implementation. Does not rewrite the frozen 2026-08-22 v1 spec.
**Problem:** TEFAS and other failures already keep the last cache, but the UI only shows a generic snackbar (`TEFAS verisi alınamadı.`) that disappears and drops the real reason (`HTTP 403`, `errorCode`, timeout, bad JSON). Catalog failure during bulk follow is silent. The first user (A23 sideload) needs to open a screen later and see whether a TEFAS or related error actually occurred.

## Goal

Add a session-only **Günlük** screen, opened from the watchlist overflow, that lists recent app events (TEFAS, follow, backup, search/reset) newest first, with errors visually distinct and the technical reason on the second line.

## Non-goals

- Persistent banner on watchlist / search / detail
- Android status-bar / system notifications
- Room or file persistence (logs vanish when the process dies)
- Filter chips, share/export, log levels beyond info/error
- Changing TEFAS retry, five-minute skip, or cache behavior
- Changing the existing snackbar + retry copy or when it appears
- Play-flavor hiding of Günlük (v1 user is the A23 sideload)
- logcat, Timber, extra permissions
- Buy/sell language, English as the default UI

## Approaches considered

1. **In-memory event ring + Günlük screen.** Hilt singleton `AppEventLog` (cap 100). `FundRepository` is the only writer. New screen lists events. **Chosen.**
2. **Richer snackbar only.** Put `e.message` on the existing snackbar. Cannot look back; silent paths stay invisible. Rejected.
3. **Room-backed log.** Survives process death. Rejected this pass (session memory is enough).

## Design

### Architecture

```
Compose screens → ViewModels → FundRepository → Room
                                 FundRepository → FollowBackup
                                 FundRepository → TefasClient
                                 FundRepository → AppEventLog
LogsScreen → LogsViewModel → AppEventLog
```

- Watchlist, search, and detail ViewModels still depend on `FundRepository` only. They never import `AppEventLog`, Room, or HTTP.
- `LogsViewModel` is the one exception: it depends on `AppEventLog` only, not on `FundRepository`. Living `docs/architecture.md` records this during implementation. Frozen specs are not edited.
- Screens never talk to Room or `TefasClient`.
- `AppEventLog` is `@Singleton`, in-memory, process-scoped. No Room table.

### `AppEventLog`

```kotlin
enum class AppEventLevel { Info, Error }

enum class AppEventKind {
    TefasCatalogOk,
    TefasCatalogError,
    TefasPricesOk,
    TefasPricesError,
    TefasRefreshSkipped,
    FollowAdded,
    FollowRemoved,
    FollowAll,
    FollowsCleared,
    BackupWriteFailed,
    BackupRestored,
    BackupReadFailed,
    SearchOk,
    SearchFailed,
}

data class AppEvent(
    val atMillis: Long,
    val level: AppEventLevel,
    val kind: AppEventKind,
    val detail: String? = null,
    val count: Int? = null,
    val durationMs: Long? = null,
)

class AppEventLog(private val clock: Clock) {
    fun observe(): Flow<List<AppEvent>>
    fun append(level: AppEventLevel, kind: AppEventKind, detail: String? = null, count: Int? = null, durationMs: Long? = null)
    fun clear()
}
```

- Cap **100** events. Newest first. Drop the oldest when full.
- `append` stamps `atMillis` from `Clock.nowMillis()`.
- `clear()` empties the list; observers emit empty.
- Thread-safe enough for repository coroutines (mutex or a single `MutableStateFlow` update).

### Who writes

Only `FundRepository` calls `append`. Do not append from ViewModels, screens, or `TefasClient`.

| Repository path | Event | Notes |
|---|---|---|
| `refreshFollowed` five-minute skip | `TefasRefreshSkipped` / Info | Log even when the cached `Result` was a failure. Do not skip the existing snackbar behavior. |
| `refreshFollowed` empty follow list | none | No TEFAS call. |
| `fetchYatCatalog` success inside refresh | `TefasCatalogOk` / Info | `count` = catalog size, `durationMs` = wall time of that call. |
| `fetchYatCatalog` throws | `TefasCatalogError` / Error | `detail` = `e.message`. Do not call prices. |
| `fetchLatestYatPrices` success inside refresh | `TefasPricesOk` / Info | `count` = price-row size, `durationMs` = wall time of that call. Log even if catalog already succeeded in the same refresh (two rows, matching the wireframe). |
| `fetchLatestYatPrices` throws | `TefasPricesError` / Error | `detail` = `e.message`. Catalog success row already logged stays. `Result` is still failure. |
| `search` empty query | none | Same as today’s `EmptyQuery`. |
| `search` success | `SearchOk` / Info | `detail` = trimmed query, `count` = match size (0 is OK). If `loadCatalog` actually hits the network, also log `TefasCatalogOk` for that fetch. In-memory catalog hits do not log a catalog event. |
| `search` `TefasFetchException` | `TefasCatalogError` then `SearchFailed` (both Error) if the catalog call threw; otherwise `SearchFailed` only | `detail` = `e.message`. |
| `follow(code)` | `FollowAdded` / Info | `detail` = code. Then backup (below). |
| `unfollow(code)` | `FollowRemoved` / Info | `detail` = code. Then backup. |
| `clearFollows` | `FollowsCleared` / Info | Then backup. |
| `followAll` catalog throws | `TefasCatalogError` / Error | Still add nothing, no wipe, no throw, no snackbar. This is the currently silent path. |
| `followAll` resolved empty (unknown codes, no throw) | `FollowAll` / Info | `count` = 0. No backup write if nothing changed. |
| `followAll` resolved non-empty | `FollowAll` / Info | `detail` = comma-separated canonical codes, `count` = size. Then backup. |
| `persistBackup` success | none | Avoid noise on every follow. |
| `persistBackup` throws | `BackupWriteFailed` / Error | `detail` = `e.message`. Follows stay as today. |
| `restoreFollowsIfNeeded` table not empty or backup empty | none | |
| `restoreFollowsIfNeeded` read throws | `BackupReadFailed` / Error | `detail` = `e.message`. Watchlist stays empty. |
| `restoreFollowsIfNeeded` restores codes | `BackupRestored` / Info | `count` = restored size. Then backup (write-fail still logs). |

`refreshFollowed` / `search` / `followAll` still return the same `Result` / `SearchOutcome` / void as today. Logging is additive.

### TEFAS HTTP detail (Akamai)

In `OkHttpTefasClient`, when the HTTP status is not successful, if the response body looks like HTML (trimmed body starts with `<` or contains `<html` / `<HTML`), throw `TefasFetchException("HTTP ${code} (HTML)")`. Otherwise keep `HTTP ${code}`. Successful HTTP with a non-JSON / HTML body still fails in `TefasJsonMapper` as today (`TEFAS returned a non-JSON body`). Extract a tiny pure helper so this is unit-tested without live TEFAS.

### UI

**Watchlist entry.** Top app bar keeps the search icon. Add a `MoreVert` overflow to the left of search with one item: **Günlük**. Search and detail have no entry point.

**Günlük screen.** Route `logs`. Top bar: back, title **Günlük**, action **Temizle** (disabled when the list is empty). Body:

- Empty: centered **Henüz olay yok.** plus a short body: events from refresh, search, follow, and TEFAS appear here.
- Non-empty: lazy list, newest first. Each row:
  - `HH:mm` from `atMillis` (device zone)
  - level label **HATA** (error color) or **BİLGİ** (`onSurfaceVariant`)
  - title from `kind` (strings below)
  - second line `bodySmall`: formatted detail (below)

No filter chips. No pull-to-refresh on this screen. Existing snackbars on watchlist / search / detail are unchanged.

**Kind → title (Turkish, `strings.xml`):**

| Kind | Title |
|---|---|
| `TefasCatalogOk`, `TefasCatalogError` | TEFAS katalog |
| `TefasPricesOk`, `TefasPricesError` | TEFAS fiyat |
| `TefasRefreshSkipped` | TEFAS yenileme |
| `FollowAdded`, `FollowRemoved`, `FollowAll` | Takip |
| `FollowsCleared` | Sıfırla |
| `BackupWriteFailed`, `BackupRestored`, `BackupReadFailed` | Yedek |
| `SearchOk`, `SearchFailed` | Arama |

**Second line:**

- Catalog/prices OK: `OK · {duration} · {count} fon` with duration as seconds using a comma decimal (e.g. `1,2 sn`).
- Refresh skipped: `5 dk`.
- Follow added / removed: `{code} eklendi` / `{code} çıkarıldı`.
- Follow all, count 0: `Eşleşen kod yok`.
- Follow all, count > 0: `{detail} eklendi` (`detail` is the code list).
- Follows cleared: `Takip listesi silindi`.
- Backup restored: `{count} kod`.
- Search OK: `"{query}" · {count} sonuç`.
- Any Error kind: `detail` as-is (HTTP / exception text). If `detail` is null, show `—`.

`Temizle` calls `AppEventLog.clear()`.

### Sideload

`:app:installDebug` / `adb install -r` only. Do not uninstall. Package `com.burha.fundhelper`.

## Testing

- No live `tefas.gov.tr`.
- Unit-test `AppEventLog`: cap 100, newest first, `clear`, observer updates.
- Unit-test the HTTP HTML helper (`HTTP 403 (HTML)` vs `HTTP 403`).
- Extend `FundRepositoryTest`: TEFAS price failure after catalog success appends catalog OK + prices error; `followAll` catalog failure appends `TefasCatalogError` and does not wipe; backup write failure appends `BackupWriteFailed`; search failure appends error event(s).
- Do not regress existing follow / search / refresh tests.
- Manual on A23: overflow → Günlük empty; pull-to-refresh with airplane mode or a TEFAS failure → HATA row with HTTP/network detail; Temizle empties the list; snackbar on watchlist still appears.
