# In-app event log (Günlük) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a session-only Günlük screen, opened from the watchlist overflow, that lists TEFAS / follow / backup / search events so a TEFAS failure is inspectable after the snackbar is gone.

**Architecture:** In-memory Hilt singleton `AppEventLog` (cap 100, newest first). `FundRepository` is the only writer. `LogsViewModel` reads `AppEventLog` only. Watchlist / search / detail ViewModels still depend on `FundRepository` only. Existing snackbar + retry is unchanged.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Hilt, JUnit 4, existing `Clock` / fakes. Fetch Compose `DropdownMenu` / `TopAppBar` / Hilt `hiltViewModel` via Context7 (`/websites/developer_android_develop_ui_compose` and `/websites/developer_android_develop_libraries`) while implementing. Sideload with `:app:installDebug` only.

## Global Constraints

- Spec: `docs/superpowers/specs/2026-08-31-app-event-log-design.md`. Frozen 2026-08-22 spec is not edited.
- `applicationId` / namespace: `com.burha.fundhelper`. Default UI language: Turkish. User-visible chrome in `res/values/strings.xml`.
- Watchlist / search / detail ViewModels → `FundRepository` only. `LogsViewModel` → `AppEventLog` only. Screens never import Room, OkHttp, or `TefasClient`.
- `FundRepository` is the only `AppEventLog.append` caller. Cap 100, newest first, process memory only.
- Do not change snackbar copy, when it appears, TEFAS endpoints, five-minute skip length, or follow/backup semantics — only add log rows.
- No live `tefas.gov.tr` in unit tests. Do not uninstall the A23 app (`./gradlew :app:installDebug` / `adb install -r`).
- No Android system notifications, no Room log table, no filter chips, no Play-flavor hiding.
- Commit after each task. Do not edit frozen specs.

## File structure

- Create `app/src/main/java/com/burha/fundhelper/data/AppEventLog.kt` — `AppEventLevel`, `AppEventKind`, `AppEvent`, `AppEventLog`
- Create `app/src/test/java/com/burha/fundhelper/data/AppEventLogTest.kt`
- Modify `app/src/main/java/com/burha/fundhelper/data/tefas/TefasClient.kt` — add `tefasHttpErrorMessage`
- Create `app/src/test/java/com/burha/fundhelper/data/tefas/TefasHttpErrorMessageTest.kt`
- Modify `app/src/main/java/com/burha/fundhelper/data/tefas/OkHttpTefasClient.kt` — use the helper
- Modify `app/src/main/java/com/burha/fundhelper/data/FundRepository.kt` — inject log, append on spec paths
- Modify `app/src/test/java/com/burha/fundhelper/data/FundRepositoryTest.kt` — 6th constructor arg + event assertions
- Modify `app/src/test/java/com/burha/fundhelper/ui/search/ApplySearchCommandTest.kt` — 6th constructor arg
- Modify `app/src/main/res/values/strings.xml`
- Create `app/src/main/java/com/burha/fundhelper/ui/logs/LogsViewModel.kt`
- Create `app/src/main/java/com/burha/fundhelper/ui/logs/LogsScreen.kt`
- Modify `app/src/main/java/com/burha/fundhelper/ui/UiFormat.kt` — `formatLogTime`
- Modify `app/src/test/java/com/burha/fundhelper/ui/UiFormatTest.kt`
- Modify `app/src/main/java/com/burha/fundhelper/ui/watchlist/WatchlistScreen.kt` — overflow → Günlük
- Modify `app/src/main/java/com/burha/fundhelper/ui/FundHelperNav.kt` — `logs` route
- Modify `docs/architecture.md`, `progress.md`

---

### Task 1: `AppEventLog` ring buffer

**Files:**
- Create: `app/src/test/java/com/burha/fundhelper/data/AppEventLogTest.kt`
- Create: `app/src/main/java/com/burha/fundhelper/data/AppEventLog.kt`

**Interfaces:**
- Consumes: `com.burha.fundhelper.domain.Clock`, `com.burha.fundhelper.fakes.FakeClock`
- Produces: `enum class AppEventLevel { Info, Error }`; `enum class AppEventKind` with exactly the spec names; `data class AppEvent(val atMillis: Long, val level: AppEventLevel, val kind: AppEventKind, val detail: String? = null, val count: Int? = null, val durationMs: Long? = null)`; `@Singleton class AppEventLog @Inject constructor(private val clock: Clock)` with `fun observe(): Flow<List<AppEvent>>`, `fun append(level: AppEventLevel, kind: AppEventKind, detail: String? = null, count: Int? = null, durationMs: Long? = null)`, `fun clear()`. Cap 100, newest first.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.burha.fundhelper.data

import com.burha.fundhelper.fakes.FakeClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppEventLogTest {

    @Test
    fun newest_first_stamps_clock_and_caps_at_100() = runTest {
        val clock = FakeClock(1_000L)
        val log = AppEventLog(clock)
        repeat(101) { i ->
            clock.now = 1_000L + i
            log.append(AppEventLevel.Info, AppEventKind.FollowAdded, detail = "C$i")
        }
        val events = log.observe().first()
        assertEquals(100, events.size)
        assertEquals("C100", events.first().detail)
        assertEquals("C1", events.last().detail)
        assertEquals(1_100L, events.first().atMillis)
        assertEquals(AppEventLevel.Info, events.first().level)
        assertEquals(AppEventKind.FollowAdded, events.first().kind)
    }

    @Test
    fun clear_empties_observers() = runTest {
        val log = AppEventLog(FakeClock())
        log.append(AppEventLevel.Error, AppEventKind.TefasPricesError, detail = "HTTP 403")
        log.clear()
        assertTrue(log.observe().first().isEmpty())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests com.burha.fundhelper.data.AppEventLogTest --offline`

Expected: FAIL compiling (`AppEventLog` unresolved) or tests not found.

- [ ] **Step 3: Write minimal implementation**

Create `app/src/main/java/com/burha/fundhelper/data/AppEventLog.kt`:

```kotlin
package com.burha.fundhelper.data

import com.burha.fundhelper.domain.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

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

@Singleton
class AppEventLog @Inject constructor(
    private val clock: Clock,
) {
    private val _events = MutableStateFlow<List<AppEvent>>(emptyList())

    fun observe(): Flow<List<AppEvent>> = _events.asStateFlow()

    @Synchronized
    fun append(
        level: AppEventLevel,
        kind: AppEventKind,
        detail: String? = null,
        count: Int? = null,
        durationMs: Long? = null,
    ) {
        val event = AppEvent(
            atMillis = clock.nowMillis(),
            level = level,
            kind = kind,
            detail = detail,
            count = count,
            durationMs = durationMs,
        )
        _events.value = (listOf(event) + _events.value).take(MAX)
    }

    @Synchronized
    fun clear() {
        _events.value = emptyList()
    }

    private companion object {
        const val MAX = 100
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests com.burha.fundhelper.data.AppEventLogTest`

Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/burha/fundhelper/data/AppEventLog.kt app/src/test/java/com/burha/fundhelper/data/AppEventLogTest.kt
git commit -m "feat: add in-memory AppEventLog ring buffer"
```

---

### Task 2: HTTP HTML error detail

**Files:**
- Create: `app/src/test/java/com/burha/fundhelper/data/tefas/TefasHttpErrorMessageTest.kt`
- Modify: `app/src/main/java/com/burha/fundhelper/data/tefas/TefasClient.kt`
- Modify: `app/src/main/java/com/burha/fundhelper/data/tefas/OkHttpTefasClient.kt` (the `!response.isSuccessful` throw)

**Interfaces:**
- Consumes: existing `TefasFetchException`
- Produces: `fun tefasHttpErrorMessage(code: Int, body: String): String` in `TefasClient.kt`. HTML if trimmed body starts with `<` or contains `<html` case-insensitive → `"HTTP $code (HTML)"`, else `"HTTP $code"`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.burha.fundhelper.data.tefas

import org.junit.Assert.assertEquals
import org.junit.Test

class TefasHttpErrorMessageTest {

    @Test
    fun html_body_is_marked() {
        assertEquals("HTTP 403 (HTML)", tefasHttpErrorMessage(403, "<html><body>denied</body></html>"))
        assertEquals("HTTP 403 (HTML)", tefasHttpErrorMessage(403, "  <HTML>"))
        assertEquals("HTTP 500 (HTML)", tefasHttpErrorMessage(500, "challenge <html lang=\"tr\">"))
    }

    @Test
    fun json_or_empty_stays_plain() {
        assertEquals("HTTP 403", tefasHttpErrorMessage(403, """{"errorCode":"ERR"}"""))
        assertEquals("HTTP 502", tefasHttpErrorMessage(502, ""))
        assertEquals("HTTP 404", tefasHttpErrorMessage(404, "not found"))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests com.burha.fundhelper.data.tefas.TefasHttpErrorMessageTest`

Expected: FAIL compiling (`tefasHttpErrorMessage` unresolved).

- [ ] **Step 3: Write minimal implementation**

Add to `TefasClient.kt` (after `TefasFetchException`):

```kotlin
fun tefasHttpErrorMessage(code: Int, body: String): String {
    val trimmed = body.trimStart()
    val html = trimmed.startsWith("<") || trimmed.contains("<html", ignoreCase = true)
    return if (html) "HTTP $code (HTML)" else "HTTP $code"
}
```

In `OkHttpTefasClient.post`, replace `throw TefasFetchException("HTTP ${response.code}")` with `throw TefasFetchException(tefasHttpErrorMessage(response.code, body))`.

- [ ] **Step 4: Run tests**

Run: `./gradlew :app:testDebugUnitTest --tests com.burha.fundhelper.data.tefas.TefasHttpErrorMessageTest --tests com.burha.fundhelper.data.tefas.TefasJsonMapperTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/burha/fundhelper/data/tefas/TefasClient.kt app/src/main/java/com/burha/fundhelper/data/tefas/OkHttpTefasClient.kt app/src/test/java/com/burha/fundhelper/data/tefas/TefasHttpErrorMessageTest.kt
git commit -m "feat: mark TEFAS HTTP HTML bodies in error messages"
```

---

### Task 3: Repository appends

**Files:**
- Modify: `app/src/main/java/com/burha/fundhelper/data/FundRepository.kt`
- Modify: `app/src/test/java/com/burha/fundhelper/data/FundRepositoryTest.kt`
- Modify: `app/src/test/java/com/burha/fundhelper/ui/search/ApplySearchCommandTest.kt`

**Interfaces:**
- Consumes: `AppEventLog.append` / `observe` from Task 1; existing `TefasFetchException`, `FakeTefasClient`, `FakeFollowBackup`
- Produces: `FundRepository(..., private val events: AppEventLog)` as last constructor parameter. Logging per spec table. `loadCatalog` logs catalog OK/error when it actually hits the network. `refreshFollowed` uses `loadCatalog(refetch = true)` after the skip check so catalog and prices are separate events. Return types unchanged.

- [ ] **Step 1: Write failing tests first** (add to `FundRepositoryTest`; they will not compile until the 6th constructor arg exists — add the constructor arg in the same task after the tests are written, or add a dummy `events: AppEventLog` parameter first so tests compile and fail on assertions)

Add import `kotlinx.coroutines.flow.first` is already there. Add:

```kotlin
import com.burha.fundhelper.data.AppEventKind
import com.burha.fundhelper.data.AppEventLevel
import com.burha.fundhelper.data.AppEventLog
```

Change `repo` / `repoWithBackup` and every `FundRepository(` call to pass `AppEventLog(clock)` as the last argument. Give `repo` an `events: AppEventLog = AppEventLog(clock)` parameter (add `clock` param if missing).

Add tests:

```kotlin
@Test
fun refresh_price_failure_logs_catalog_ok_then_prices_error() = runTest {
    val clock = FakeClock()
    val events = AppEventLog(clock)
    val tefas = FakeTefasClient(catalog = listOf(aak), prices = listOf(aakPriced), failPrices = true)
    val snapshots = FakeSnapshotDao()
    val repository = FundRepository(
        FakeFollowDao(snapshots), snapshots, tefas, clock, FakeFollowBackup(), events,
    )
    repository.follow("AAK")
    events.clear()
    assertTrue(repository.refreshFollowed(force = true).isFailure)
    val kinds = events.observe().first().map { it.kind }
    assertEquals(listOf(AppEventKind.TefasPricesError, AppEventKind.TefasCatalogOk), kinds)
    assertEquals("prices failed", events.observe().first().first().detail)
    assertEquals(AppEventLevel.Error, events.observe().first().first().level)
}

@Test
fun follow_all_catalog_failure_logs_error_and_does_not_wipe() = runTest {
    val clock = FakeClock()
    val events = AppEventLog(clock)
    val tefas = FakeTefasClient(catalog = listOf(aak, aal), prices = listOf(aakPriced))
    val snapshots = FakeSnapshotDao()
    val repository = FundRepository(
        FakeFollowDao(snapshots), snapshots, tefas, clock, FakeFollowBackup(), events,
    )
    repository.follow("AAK")
    tefas.failCatalog = true
    events.clear()
    repository.followAll(listOf("AAL"))
    assertEquals(listOf("AAK"), repository.observeWatchlist().first().map { it.code })
    val logged = events.observe().first()
    assertEquals(listOf(AppEventKind.TefasCatalogError), logged.map { it.kind })
    assertEquals("catalog failed", logged.single().detail)
}

@Test
fun backup_write_failure_logs_error() = runTest {
    val clock = FakeClock()
    val events = AppEventLog(clock)
    val backup = FakeFollowBackup().apply { writeError = true }
    val snapshots = FakeSnapshotDao()
    val repository = FundRepository(
        FakeFollowDao(snapshots), snapshots, FakeTefasClient(), clock, backup, events,
    )
    repository.follow("AAK")
    val kinds = events.observe().first().map { it.kind }
    assertTrue(kinds.contains(AppEventKind.FollowAdded))
    assertTrue(kinds.contains(AppEventKind.BackupWriteFailed))
}

@Test
fun search_failure_logs_catalog_and_search_errors() = runTest {
    val clock = FakeClock()
    val events = AppEventLog(clock)
    val tefas = FakeTefasClient(failCatalog = true)
    val snapshots = FakeSnapshotDao()
    val repository = FundRepository(
        FakeFollowDao(snapshots), snapshots, tefas, clock, FakeFollowBackup(), events,
    )
    val outcome = repository.search("AAK")
    assertTrue(outcome is SearchOutcome.Failure)
    assertEquals(
        listOf(AppEventKind.SearchFailed, AppEventKind.TefasCatalogError),
        events.observe().first().map { it.kind },
    )
}

@Test
fun restore_read_failure_logs_error() = runTest {
    val clock = FakeClock()
    val events = AppEventLog(clock)
    val backup = FakeFollowBackup().apply {
        codes = listOf("AAK")
        readError = true
    }
    val snapshots = FakeSnapshotDao()
    val repository = FundRepository(
        FakeFollowDao(snapshots), snapshots, FakeTefasClient(), clock, backup, events,
    )
    repository.restoreFollowsIfNeeded()
    val logged = events.observe().first()
    assertEquals(listOf(AppEventKind.BackupReadFailed), logged.map { it.kind })
}

@Test
fun restore_logs_info() = runTest {
    val clock = FakeClock()
    val events = AppEventLog(clock)
    val backup = FakeFollowBackup().apply { codes = listOf("AAL", "AAK") }
    val snapshots = FakeSnapshotDao()
    val repository = FundRepository(
        FakeFollowDao(snapshots), snapshots, FakeTefasClient(), clock, backup, events,
    )
    repository.restoreFollowsIfNeeded()
    val restored = events.observe().first().first { it.kind == AppEventKind.BackupRestored }
    assertEquals(2, restored.count)
}

@Test
fun refresh_skip_logs_info() = runTest {
    val clock = FakeClock()
    val events = AppEventLog(clock)
    val tefas = FakeTefasClient(catalog = listOf(aak), prices = listOf(aakPriced))
    val snapshots = FakeSnapshotDao()
    val repository = FundRepository(
        FakeFollowDao(snapshots), snapshots, tefas, clock, FakeFollowBackup(), events,
    )
    repository.follow("AAK")
    repository.refreshFollowed(force = true)
    events.clear()
    assertTrue(repository.refreshFollowed(force = false).isSuccess)
    assertEquals(listOf(AppEventKind.TefasRefreshSkipped), events.observe().first().map { it.kind })
}
```

Also update `ApplySearchCommandTest.repo()` to pass `AppEventLog(FakeClock())`.

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests com.burha.fundhelper.data.FundRepositoryTest`

Expected: FAIL on missing events or wrong kinds until appends exist.

- [ ] **Step 3: Implement repository logging**

`FundRepository` constructor last param: `private val events: AppEventLog`.

Add `import kotlin.system.measureTimeMillis`.

Replace `loadCatalog` with:

```kotlin
private suspend fun loadCatalog(refetch: Boolean): List<FundSnapshot> {
    val cached = catalogMemory
    if (!refetch && cached != null) return cached
    return try {
        var fresh: List<FundSnapshot> = emptyList()
        val durationMs = measureTimeMillis {
            fresh = tefas.fetchYatCatalog()
        }
        catalogMemory = fresh
        events.append(
            AppEventLevel.Info,
            AppEventKind.TefasCatalogOk,
            count = fresh.size,
            durationMs = durationMs,
        )
        fresh
    } catch (e: TefasFetchException) {
        events.append(AppEventLevel.Error, AppEventKind.TefasCatalogError, detail = e.message)
        throw e
    }
}
```

`refreshFollowed`: after empty-codes return, on five-minute skip call `events.append(AppEventLevel.Info, AppEventKind.TefasRefreshSkipped)` then return cached. On fetch, replace the single `tefas.fetchYatCatalog()` with `loadCatalog(refetch = true)` then wrap prices:

```kotlin
val catalog = loadCatalog(refetch = true).associateBy { it.code }
var prices: Map<String, FundSnapshot> = emptyMap()
try {
    var priceRows: List<FundSnapshot> = emptyList()
    val durationMs = measureTimeMillis {
        priceRows = tefas.fetchLatestYatPrices()
    }
    prices = priceRows.associateBy { it.code }
    events.append(
        AppEventLevel.Info,
        AppEventKind.TefasPricesOk,
        count = priceRows.size,
        durationMs = durationMs,
    )
} catch (e: TefasFetchException) {
    events.append(AppEventLevel.Error, AppEventKind.TefasPricesError, detail = e.message)
    throw e
}
```

Keep the outer `try/catch (e: TefasFetchException) { Result.failure(e) }` so catalog errors from `loadCatalog` still become `Result.failure`. Do not double-append catalog errors in that outer catch.

`search`: after success `events.append(AppEventLevel.Info, AppEventKind.SearchOk, detail = needle, count = matches.size)` (use `matches.size` before merge is fine). In the existing catch: `events.append(AppEventLevel.Error, AppEventKind.SearchFailed, detail = e.message ?: "TEFAS")` after `loadCatalog` already appended catalog error.

`follow`: after insert, `events.append(AppEventLevel.Info, AppEventKind.FollowAdded, detail = code)` then `persistBackup()`.

`unfollow`: `events.append(AppEventLevel.Info, AppEventKind.FollowRemoved, detail = code)` then backup.

`clearFollows`: deleteAll, `events.append(AppEventLevel.Info, AppEventKind.FollowsCleared)`, then backup.

`followAll`: on `resolved.isEmpty()` after a successful catalog, `events.append(AppEventLevel.Info, AppEventKind.FollowAll, count = 0)` and return (no backup). On non-empty, after inserts/upsert, `events.append(AppEventLevel.Info, AppEventKind.FollowAll, detail = resolved.joinToString(", ") { it.code }, count = resolved.size)` then backup. Catalog throw already logged by `loadCatalog`; keep the existing catch with no extra append and no wipe.

`persistBackup` catch: `events.append(AppEventLevel.Error, AppEventKind.BackupWriteFailed, detail = e.message)`.

`restoreFollowsIfNeeded` read catch: `events.append(AppEventLevel.Error, AppEventKind.BackupReadFailed, detail = e.message)` then return. After inserting restored codes: `events.append(AppEventLevel.Info, AppEventKind.BackupRestored, count = codes.size)` then `persistBackup()`.

- [ ] **Step 4: Run tests**

Run: `./gradlew :app:testDebugUnitTest --tests com.burha.fundhelper.data.FundRepositoryTest --tests com.burha.fundhelper.ui.search.ApplySearchCommandTest`

Expected: PASS. Existing refresh/follow/search assertions still pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/burha/fundhelper/data/FundRepository.kt app/src/test/java/com/burha/fundhelper/data/FundRepositoryTest.kt app/src/test/java/com/burha/fundhelper/ui/search/ApplySearchCommandTest.kt
git commit -m "feat: append TEFAS follow backup and search events to AppEventLog"
```

---

### Task 4: Günlük screen and watchlist overflow

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/java/com/burha/fundhelper/ui/UiFormat.kt`
- Modify: `app/src/test/java/com/burha/fundhelper/ui/UiFormatTest.kt`
- Create: `app/src/main/java/com/burha/fundhelper/ui/logs/LogsViewModel.kt`
- Create: `app/src/main/java/com/burha/fundhelper/ui/logs/LogsScreen.kt`
- Modify: `app/src/main/java/com/burha/fundhelper/ui/watchlist/WatchlistScreen.kt`
- Modify: `app/src/main/java/com/burha/fundhelper/ui/FundHelperNav.kt`

**Interfaces:**
- Consumes: `AppEventLog.observe` / `clear`; `formatFetchedAt` locale pattern in `UiFormat.kt`; existing `hiltViewModel()` in `FundHelperNav.kt`
- Produces: route `logs`; `WatchlistScreen(..., onLogs: () -> Unit)`; Turkish strings listed below. Fetch Material 3 `DropdownMenu` / `DropdownMenuItem` / `TopAppBar` `actions` via Context7 before writing Compose.

- [ ] **Step 1: Add strings**

Append to `strings.xml`:

```xml
    <string name="more">Daha fazla</string>
    <string name="logs_title">Günlük</string>
    <string name="logs_clear">Temizle</string>
    <string name="logs_empty_title">Henüz olay yok.</string>
    <string name="logs_empty_body">Yenileme, arama, takip ve TEFAS çağrıları burada görünür.</string>
    <string name="logs_level_info">BİLGİ</string>
    <string name="logs_level_error">HATA</string>
    <string name="logs_kind_tefas_catalog">TEFAS katalog</string>
    <string name="logs_kind_tefas_prices">TEFAS fiyat</string>
    <string name="logs_kind_tefas_refresh">TEFAS yenileme</string>
    <string name="logs_kind_follow">Takip</string>
    <string name="logs_kind_reset">Sıfırla</string>
    <string name="logs_kind_backup">Yedek</string>
    <string name="logs_kind_search">Arama</string>
    <string name="logs_ok_detail">OK · %1$s · %2$d fon</string>
    <string name="logs_refresh_skipped">5 dk</string>
    <string name="logs_follow_added">%1$s eklendi</string>
    <string name="logs_follow_removed">%1$s çıkarıldı</string>
    <string name="logs_follow_all_none">Eşleşen kod yok</string>
    <string name="logs_follow_all">%1$s eklendi</string>
    <string name="logs_follows_cleared">Takip listesi silindi</string>
    <string name="logs_backup_restored">%1$d kod</string>
    <string name="logs_search_ok">\"%1$s\" · %2$d sonuç</string>
```

- [ ] **Step 2: `formatLogTime` + test**

Add to `UiFormat.kt`:

```kotlin
fun formatLogTime(millis: Long): String {
    val fmt = DateTimeFormatter.ofPattern("HH:mm", tr)
    return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(fmt)
}
```

In `UiFormatTest`:

```kotlin
@Test
fun log_time_is_hh_mm() {
    val millis = java.time.LocalDateTime.of(2026, 8, 31, 10, 31)
        .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    assertEquals("10:31", formatLogTime(millis))
}
```

Run: `./gradlew :app:testDebugUnitTest --tests com.burha.fundhelper.ui.UiFormatTest` — expect PASS after implementation.

- [ ] **Step 3: LogsViewModel**

```kotlin
package com.burha.fundhelper.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burha.fundhelper.data.AppEvent
import com.burha.fundhelper.data.AppEventLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LogsUiState(
    val events: List<AppEvent> = emptyList(),
)

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val events: AppEventLog,
) : ViewModel() {
    private val _state = MutableStateFlow(LogsUiState())
    val state: StateFlow<LogsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            events.observe().collect { rows ->
                _state.value = LogsUiState(events = rows)
            }
        }
    }

    fun clear() {
        events.clear()
    }
}
```

- [ ] **Step 4: LogsScreen**

Use the same `Scaffold` + `TopAppBar` + `Icons.AutoMirrored.Filled.ArrowBack` pattern as `DetailScreen`. Action: `TextButton` labeled `logs_clear`, `enabled = state.events.isNotEmpty()`, `onClick = viewModel::clear`.

Empty: centered `logs_empty_title` (`titleLarge`) and `logs_empty_body` (`bodyMedium`, `onSurfaceVariant`), same padding as watchlist empty.

List: `LazyColumn` `items(state.events, key = { it.atMillis.toString() + it.kind.name + it.detail })`. Row: `HH:mm` via `formatLogTime`; level `HATA` uses `MaterialTheme.colorScheme.error`, `BİLGİ` uses `onSurfaceVariant`; title from kind (catalog kinds → `logs_kind_tefas_catalog`, prices → `logs_kind_tefas_prices`, skip → `logs_kind_tefas_refresh`, follow added/removed/all → `logs_kind_follow`, cleared → `logs_kind_reset`, backup kinds → `logs_kind_backup`, search kinds → `logs_kind_search`). Second line `bodySmall` `onSurfaceVariant`:

- `TefasCatalogOk` / `TefasPricesOk`: `logs_ok_detail` with duration `String.format(java.util.Locale.forLanguageTag("tr-TR"), "%.1f sn", (event.durationMs ?: 0L) / 1000.0)` and `event.count ?: 0`
- `TefasRefreshSkipped`: `logs_refresh_skipped`
- `FollowAdded`: `logs_follow_added` / `event.detail.orEmpty()`
- `FollowRemoved`: `logs_follow_removed`
- `FollowAll` count 0: `logs_follow_all_none`; else `logs_follow_all` with detail
- `FollowsCleared`: `logs_follows_cleared`
- `BackupRestored`: `logs_backup_restored` with count
- `SearchOk`: `logs_search_ok` with detail + count
- Error kinds: `event.detail ?: stringResource(R.string.price_missing)` (`—`)

Do not add snackbar or pull-to-refresh on this screen.

- [ ] **Step 5: Watchlist overflow + nav**

`WatchlistScreen` add `onLogs: () -> Unit`. In `TopAppBar.actions`, **before** the search `IconButton`, a `Box` with `MoreVert` (`R.string.more`) and `DropdownMenu` one `DropdownMenuItem` text `logs_title` that calls `onLogs()`. Keep search icon. Query Context7 if `DropdownMenu` signature is unclear; do not guess deprecated `menuAnchor` APIs from memory if docs disagree.

`FundHelperNav`:

```kotlin
const val LOGS = "logs"
```

```kotlin
WatchlistScreen(
    viewModel = vm,
    onSearch = { navController.navigate(Routes.SEARCH) },
    onLogs = { navController.navigate(Routes.LOGS) },
    onOpen = { code -> navController.navigate(Routes.detail(code)) },
)
```

```kotlin
composable(Routes.LOGS) {
    val vm: LogsViewModel = hiltViewModel()
    LogsScreen(viewModel = vm, onBack = { navController.popBackStack() })
}
```

- [ ] **Step 6: Compile + unit tests**

Run: `./gradlew :app:testDebugUnitTest`

Expected: PASS, including `UiFormatTest`.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/java/com/burha/fundhelper/ui/UiFormat.kt app/src/test/java/com/burha/fundhelper/ui/UiFormatTest.kt app/src/main/java/com/burha/fundhelper/ui/logs/LogsViewModel.kt app/src/main/java/com/burha/fundhelper/ui/logs/LogsScreen.kt app/src/main/java/com/burha/fundhelper/ui/watchlist/WatchlistScreen.kt app/src/main/java/com/burha/fundhelper/ui/FundHelperNav.kt
git commit -m "feat: add Günlük screen and watchlist overflow entry"
```

---

### Task 5: Living docs and sideload note

**Files:**
- Modify: `docs/architecture.md`
- Modify: `progress.md`

**Interfaces:**
- Consumes: Tasks 1–4 behavior
- Produces: architecture map includes `AppEventLog`; screens bullet mentions watchlist overflow → Günlük; ViewModels bullet notes `LogsViewModel` → `AppEventLog` only; Failures bullet notes silent bulk-follow catalog failure now logs. `progress.md` pruned handoff.

- [ ] **Step 1: Update architecture.md**

In the module-boundaries diagram add `FundRepository → AppEventLog` and `LogsScreen → LogsViewModel → AppEventLog`.

Screens bullet: watchlist overflow **Günlük**; new logs screen (session events, newest first, Temizle).

ViewModels: `LogsViewModel` depends on `AppEventLog` only. Other ViewModels still `FundRepository` only.

New bullet **AppEventLog:** in-memory cap 100, newest first; repository is the only writer; process death clears it.

Failures: bulk-follow catalog failure still no snackbar / no wipe; it now appends `TefasCatalogError`.

- [ ] **Step 2: Sideload**

If `adb devices` shows the A23, run `./gradlew :app:installDebug` (do not uninstall). If no device, do not fail the task; write that sideload was skipped.

- [ ] **Step 3: progress.md**

Replace current status with: Günlük is on this branch (overflow on watchlist). Session-only. Snackbar unchanged. Sideload `:app:installDebug` if done, else skipped. Next: manual on A23 — empty Günlük, airplane-mode refresh shows HATA with HTTP/network detail, Temizle. Blockers: keep the uninstall warning.

- [ ] **Step 4: Full unit tests**

Run: `./gradlew :app:testDebugUnitTest`

Expected: PASS. No live TEFAS.

- [ ] **Step 5: Commit**

```bash
git add docs/architecture.md progress.md
git commit -m "docs: record Günlük event log in architecture and handoff"
```
