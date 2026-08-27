# Bulk follow + RESET Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** From the search box, a comma-separated code list appends found funds to the watchlist, and a typed `RESET` clears follows plus the Downloads backup, then both paths return to the watchlist.

**Architecture:** Pure `parseSearchCommand` in `domain/`. `SearchViewModel.submit` runs `applySearchCommand`, which calls `FundRepository.clearFollows()` or `followAll(codes)` and signals pop, or today’s `search()`. `followAll` matches catalog codes exactly (case-insensitive). `clearFollows` deletes all Room follows then writes an empty backup. No new screens.

**Tech Stack:** Kotlin, JUnit 4, existing `FundRepository`, Room `FollowDao`, Compose `LaunchedEffect`. No new libraries. Sideload with `adb install -r` only.

## Global Constraints

- Spec: `docs/superpowers/specs/2026-08-27-bulk-follow-reset-design.md`. Frozen v1 spec is unchanged.
- `applicationId` / namespace: `com.burha.fundhelper`. Default UI language: Turkish.
- UI → ViewModels → `FundRepository` only. Do not change TEFAS endpoints, watchlist sort, or `Ara` copy.
- Comma list = codes only, exact match. `RESET` in a list is not a wipe. Only the whole trimmed query `RESET` (any case, no comma) wipes.
- Catalog failure on bulk follow: add nothing, do not wipe, still pop. No snackbar.
- No live `tefas.gov.tr` in unit tests. Do not uninstall the A23 app (`adb install -r`).
- Do not commit unless the user asks.
- Room `@Query("DELETE FROM follows")` is a new DAO method, not a schema change: leave `AppDatabase` version at 1.
- Fetch Room / Compose APIs via Context7 (`/websites/developer_android`) at implementation time.

---

## File structure

| Path | Responsibility |
|------|----------------|
| Create `app/src/main/java/com/burha/fundhelper/domain/SearchCommand.kt` | `SearchCommand`, `parseSearchCommand` |
| Create `app/src/test/java/com/burha/fundhelper/domain/SearchCommandTest.kt` | Parser unit tests |
| Modify `app/src/main/java/com/burha/fundhelper/data/local/FollowDao.kt` | `deleteAll()` |
| Modify `app/src/test/java/com/burha/fundhelper/fakes/FakeFollowDao.kt` | Implement `deleteAll` |
| Modify `app/src/test/java/com/burha/fundhelper/fakes/FakeFollowBackup.kt` | `writeCount` |
| Modify `app/src/main/java/com/burha/fundhelper/data/FundRepository.kt` | `clearFollows()`, `followAll(codes)` |
| Modify `app/src/test/java/com/burha/fundhelper/data/FundRepositoryTest.kt` | Repository tests |
| Create `app/src/main/java/com/burha/fundhelper/ui/search/ApplySearchCommand.kt` | `applySearchCommand` |
| Create `app/src/test/java/com/burha/fundhelper/ui/search/ApplySearchCommandTest.kt` | Submit-path tests |
| Modify `app/src/main/java/com/burha/fundhelper/ui/search/SearchViewModel.kt` | Branch + `navigateBack` |
| Modify `app/src/main/java/com/burha/fundhelper/ui/search/SearchScreen.kt` | Pop when `navigateBack` |
| Modify `docs/architecture.md`, `progress.md` | Living handoff |

---

### Task 1: `parseSearchCommand`

**Files:**
- Create: `app/src/test/java/com/burha/fundhelper/domain/SearchCommandTest.kt`
- Create: `app/src/main/java/com/burha/fundhelper/domain/SearchCommand.kt`

**Interfaces:**
- Consumes: nothing
- Produces:

```kotlin
sealed class SearchCommand {
    data object Reset : SearchCommand()
    data class BulkFollow(val codes: List<String>) : SearchCommand()
    data class TextSearch(val query: String) : SearchCommand()
}

fun parseSearchCommand(raw: String): SearchCommand
```

- [ ] **Step 1: Write the failing test**

```kotlin
package com.burha.fundhelper.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchCommandTest {

    @Test
    fun reset_any_case_without_comma() {
        assertEquals(SearchCommand.Reset, parseSearchCommand("RESET"))
        assertEquals(SearchCommand.Reset, parseSearchCommand("reset"))
        assertEquals(SearchCommand.Reset, parseSearchCommand(" Reset "))
    }

    @Test
    fun comma_list_is_bulk_follow() {
        val command = parseSearchCommand("AAK, AAL")
        assertEquals(SearchCommand.BulkFollow(listOf("AAK", "AAL")), command)
    }

    @Test
    fun reset_inside_list_is_dropped_not_a_wipe() {
        assertEquals(
            SearchCommand.BulkFollow(listOf("AAK")),
            parseSearchCommand("RESET, AAK"),
        )
        assertEquals(
            SearchCommand.BulkFollow(emptyList()),
            parseSearchCommand("RESET,"),
        )
    }

    @Test
    fun unique_codes_first_wins_case_insensitive() {
        assertEquals(
            SearchCommand.BulkFollow(listOf("AAK")),
            parseSearchCommand("AAK, aak"),
        )
    }

    @Test
    fun empty_tokens_dropped() {
        assertEquals(
            SearchCommand.BulkFollow(listOf("AAK")),
            parseSearchCommand(" , AAK, ,"),
        )
    }

    @Test
    fun no_comma_is_text_search() {
        assertEquals(SearchCommand.TextSearch("AAK"), parseSearchCommand("AAK"))
        assertEquals(SearchCommand.TextSearch("ata"), parseSearchCommand(" ata "))
        assertEquals(SearchCommand.TextSearch(""), parseSearchCommand("  "))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests com.burha.fundhelper.domain.SearchCommandTest`

Expected: FAIL (`SearchCommand` / `parseSearchCommand` not defined).

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.burha.fundhelper.domain

import java.util.Locale

sealed class SearchCommand {
    data object Reset : SearchCommand()
    data class BulkFollow(val codes: List<String>) : SearchCommand()
    data class TextSearch(val query: String) : SearchCommand()
}

fun parseSearchCommand(raw: String): SearchCommand {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return SearchCommand.TextSearch("")
    if (trimmed.contains(',')) {
        val codes = trimmed.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filter { !it.equals("RESET", ignoreCase = true) }
            .distinctBy { it.uppercase(Locale.ROOT) }
        return SearchCommand.BulkFollow(codes)
    }
    if (trimmed.equals("RESET", ignoreCase = true)) return SearchCommand.Reset
    return SearchCommand.TextSearch(trimmed)
}
```

Classify **comma before** `RESET`, so `RESET, AAK` is bulk follow.

- [ ] **Step 4: Run test to verify it passes**

Same Gradle command. Expected: PASS.

---

### Task 2: `clearFollows`

**Files:**
- Modify: `app/src/main/java/com/burha/fundhelper/data/local/FollowDao.kt`
- Modify: `app/src/test/java/com/burha/fundhelper/fakes/FakeFollowDao.kt`
- Modify: `app/src/test/java/com/burha/fundhelper/fakes/FakeFollowBackup.kt`
- Modify: `app/src/main/java/com/burha/fundhelper/data/FundRepository.kt`
- Test: `app/src/test/java/com/burha/fundhelper/data/FundRepositoryTest.kt`

**Interfaces:**
- Consumes: `FollowDao`, `persistBackup()`, existing `follow()` / `search()`
- Produces: `suspend fun FollowDao.deleteAll()` and `suspend fun FundRepository.clearFollows()`

- [ ] **Step 1: Write the failing tests** (add to `FundRepositoryTest`; they will not compile until `clearFollows` exists)

```kotlin
@Test
fun clear_follows_empties_room_and_backup_keeps_snapshots() = runTest {
    val backup = FakeFollowBackup()
    val snapshots = FakeSnapshotDao()
    val tefas = FakeTefasClient(catalog = listOf(aak, aal), prices = listOf(aakPriced))
    val repository = FundRepository(FakeFollowDao(snapshots), snapshots, tefas, FakeClock(), backup)
    repository.search("AAK")
    repository.follow("AAK")
    repository.follow("AAL")
    repository.clearFollows()
    assertTrue(repository.observeWatchlist().first().isEmpty())
    assertTrue(backup.codes.isEmpty())
    assertNotNull(snapshots.get("AAK"))
}

@Test
fun clear_follows_then_restore_does_not_bring_codes_back() = runTest {
    val (repository, backup) = repoWithBackup()
    repository.follow("AAK")
    repository.clearFollows()
    repository.restoreFollowsIfNeeded()
    assertTrue(repository.observeWatchlist().first().isEmpty())
    assertTrue(backup.codes.isEmpty())
}

@Test
fun clear_follows_backup_write_failure_leaves_room_empty() = runTest {
    val backup = FakeFollowBackup()
    val (repository, _) = repoWithBackup(backup)
    repository.follow("AAK")
    backup.writeError = true
    repository.clearFollows()
    assertTrue(repository.observeWatchlist().first().isEmpty())
    assertEquals(listOf("AAK"), backup.codes)
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\gradlew.bat testDebugUnitTest --tests com.burha.fundhelper.data.FundRepositoryTest.clear_follows*`

Expected: FAIL (`clearFollows` not defined).

- [ ] **Step 3: DAO + fake + implementation**

`FollowDao` — same `@Query` style as existing `delete` (Context7 / developer.android.com Room Query). **Do not** bump `AppDatabase` version.

```kotlin
@Query("DELETE FROM follows")
suspend fun deleteAll()
```

`FakeFollowDao`:

```kotlin
override suspend fun deleteAll() {
    codes.value = emptyList()
}
```

`FundRepository`:

```kotlin
suspend fun clearFollows() {
    followDao.deleteAll()
    persistBackup()
}
```

Do not delete snapshots. Do not re-insert follows if `persistBackup()` swallows a write error.

- [ ] **Step 4: Run tests to verify they pass**

Same Gradle command (or full `FundRepositoryTest`). Expected: PASS. Existing follow/unfollow/restore tests still pass.

---

### Task 3: `followAll`

**Files:**
- Modify: `app/src/test/java/com/burha/fundhelper/fakes/FakeFollowBackup.kt`
- Modify: `app/src/main/java/com/burha/fundhelper/data/FundRepository.kt`
- Test: `app/src/test/java/com/burha/fundhelper/data/FundRepositoryTest.kt`

**Interfaces:**
- Consumes: `loadCatalog`, `FollowDao.insert`, `SnapshotDao.upsertAll`, `persistBackup()`, `TefasFetchException`
- Produces: `suspend fun FundRepository.followAll(codes: List<String>)`

Exact catalog `code` match, `Locale.ROOT` uppercase. Insert the **catalog** code, not the typed token. Prefix must **not** match (`"AA"` must not follow `"AAK"`). Empty `codes` → return without hitting TEFAS. Catalog failure → add nothing, do not throw. One backup write after a non-empty resolve.

- [ ] **Step 1: Count backup writes on the fake**

```kotlin
class FakeFollowBackup : FollowBackup {
    var codes: List<String> = emptyList()
    var writeCount: Int = 0
    var writeError: Boolean = false
    var readError: Boolean = false

    override suspend fun writeCodes(codes: List<String>) {
        writeCount += 1
        if (writeError) error("backup write failed")
        this.codes = codes
    }
    // readCodes unchanged
}
```

Increment `writeCount` **before** throwing so a failed write is still visible; `clearFollows` tests do not assert `writeCount`.

- [ ] **Step 2: Write the failing tests**

```kotlin
@Test
fun follow_all_appends_resolved_codes_and_writes_backup_once() = runTest {
    val backup = FakeFollowBackup()
    val snapshots = FakeSnapshotDao()
    val tefas = FakeTefasClient(catalog = listOf(aak, aal), prices = listOf(aakPriced))
    val repository = FundRepository(FakeFollowDao(snapshots), snapshots, tefas, FakeClock(), backup)
    repository.follow("AAK")
    val writesAfterFollow = backup.writeCount
    repository.followAll(listOf("aak", "AAL", "NOPE", "RESET"))
    assertEquals(listOf("AAK", "AAL"), repository.observeWatchlist().first().map { it.code })
    assertEquals(listOf("AAK", "AAL"), backup.codes)
    assertEquals(writesAfterFollow + 1, backup.writeCount)
    assertNotNull(snapshots.get("AAL"))
}

@Test
fun follow_all_does_not_prefix_match() = runTest {
    val (repository, _, _) = repo()
    repository.followAll(listOf("AA"))
    assertTrue(repository.observeWatchlist().first().isEmpty())
}

@Test
fun follow_all_empty_does_not_hit_client() = runTest {
    val (repository, tefas, _) = repo()
    repository.followAll(emptyList())
    assertEquals(0, tefas.catalogCalls)
}

@Test
fun follow_all_catalog_failure_adds_nothing() = runTest {
    val tefas = FakeTefasClient(catalog = listOf(aak, aal), prices = listOf(aakPriced))
    val (repository, _, _) = repo(tefas)
    repository.follow("AAK")
    tefas.failCatalog = true
    repository.followAll(listOf("AAL"))
    assertEquals(listOf("AAK"), repository.observeWatchlist().first().map { it.code })
}
```

- [ ] **Step 3: Run tests to verify they fail**

Run: `.\gradlew.bat testDebugUnitTest --tests com.burha.fundhelper.data.FundRepositoryTest.follow_all*`

Expected: FAIL (`followAll` not defined).

- [ ] **Step 4: Write minimal implementation**

```kotlin
suspend fun followAll(codes: List<String>) {
    if (codes.isEmpty()) return
    try {
        val catalog = loadCatalog(refetch = false)
        val byCode = catalog.associateBy { it.code.uppercase(Locale.ROOT) }
        val resolved = codes.mapNotNull { token ->
            byCode[token.uppercase(Locale.ROOT)]
        }.distinctBy { it.code }
        if (resolved.isEmpty()) return
        val now = clock.nowMillis()
        resolved.forEach { fund -> followDao.insert(FollowEntity(fund.code)) }
        snapshotDao.upsertAll(resolved.map { SnapshotMapper.toEntity(it.copy(fetchedAt = now)) })
        persistBackup()
    } catch (_: TefasFetchException) {
        // Unknown-or-failed codes are skipped; do not wipe; do not throw.
    }
}
```

Add `import java.util.Locale`.

- [ ] **Step 5: Run tests to verify they pass**

`.\gradlew.bat testDebugUnitTest --tests com.burha.fundhelper.data.FundRepositoryTest`

Expected: PASS.

---

### Task 4: Search submit + pop to watchlist

**Files:**
- Create: `app/src/main/java/com/burha/fundhelper/ui/search/ApplySearchCommand.kt`
- Create: `app/src/test/java/com/burha/fundhelper/ui/search/ApplySearchCommandTest.kt`
- Modify: `app/src/main/java/com/burha/fundhelper/ui/search/SearchViewModel.kt`
- Modify: `app/src/main/java/com/burha/fundhelper/ui/search/SearchScreen.kt`

**Interfaces:**
- Consumes: `parseSearchCommand`, `FundRepository.clearFollows`, `followAll`, `search`
- Produces:

```kotlin
sealed class SearchSubmitResult {
    data object NavigateBack : SearchSubmitResult()
    data class Stay(val outcome: SearchOutcome) : SearchSubmitResult()
}

suspend fun applySearchCommand(
    query: String,
    funds: FundRepository,
    refetchCatalog: Boolean = false,
): SearchSubmitResult
```

`SearchUiState.navigateBack: Boolean = false`. After `NavigateBack`, pop via existing `onBack` (`navController.popBackStack()`). Do not change the **Ara** string. IME Search already calls `submit()`.

Do **not** put `FundRepository` in `domain/`. Keep `applySearchCommand` under `ui/search`.

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.burha.fundhelper.ui.search

import com.burha.fundhelper.data.FundRepository
import com.burha.fundhelper.data.SearchOutcome
import com.burha.fundhelper.domain.FundKind
import com.burha.fundhelper.domain.FundSnapshot
import com.burha.fundhelper.domain.ReturnKeys
import com.burha.fundhelper.fakes.FakeClock
import com.burha.fundhelper.fakes.FakeFollowBackup
import com.burha.fundhelper.fakes.FakeFollowDao
import com.burha.fundhelper.fakes.FakeSnapshotDao
import com.burha.fundhelper.fakes.FakeTefasClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApplySearchCommandTest {

    private val aak = FundSnapshot(
        code = "AAK",
        name = "ATA PORTFÖY ÇOKLU VARLIK DEĞİŞKEN FON",
        kind = FundKind.YAT,
        price = null,
        priceDate = null,
        returns = mapOf(ReturnKeys.M1 to 1.25),
        fundType = "Değişken Şemsiye Fonu",
        risk = "4",
        fees = emptyList(),
        fetchedAt = 0L,
    )
    private val aal = aak.copy(code = "AAL", name = "ATA PORTFÖY PARA PİYASASI (TL) FONU")

    private fun repo(): FundRepository {
        val snapshots = FakeSnapshotDao()
        return FundRepository(
            FakeFollowDao(snapshots),
            snapshots,
            FakeTefasClient(catalog = listOf(aak, aal), prices = listOf(aak.copy(price = 1.0))),
            FakeClock(),
            FakeFollowBackup(),
        )
    }

    @Test
    fun reset_clears_and_navigates_back() = runTest {
        val funds = repo()
        funds.follow("AAK")
        val result = applySearchCommand("RESET", funds)
        assertEquals(SearchSubmitResult.NavigateBack, result)
        assertTrue(funds.observeWatchlist().first().isEmpty())
    }

    @Test
    fun comma_list_follows_and_navigates_back() = runTest {
        val funds = repo()
        val result = applySearchCommand("AAK, NOPE, AAL", funds)
        assertEquals(SearchSubmitResult.NavigateBack, result)
        assertEquals(listOf("AAK", "AAL"), funds.observeWatchlist().first().map { it.code })
    }

    @Test
    fun reset_with_comma_does_not_wipe() = runTest {
        val funds = repo()
        funds.follow("AAK")
        val result = applySearchCommand("RESET,", funds)
        assertEquals(SearchSubmitResult.NavigateBack, result)
        assertEquals(listOf("AAK"), funds.observeWatchlist().first().map { it.code })
    }

    @Test
    fun single_code_stays_and_searches() = runTest {
        val funds = repo()
        val result = applySearchCommand("AAK", funds)
        assertTrue(result is SearchSubmitResult.Stay)
        val stay = result as SearchSubmitResult.Stay
        assertTrue(stay.outcome is SearchOutcome.Success)
        assertEquals(listOf("AAK"), (stay.outcome as SearchOutcome.Success).matches.map { it.code })
        assertTrue(funds.observeWatchlist().first().isEmpty())
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\gradlew.bat testDebugUnitTest --tests com.burha.fundhelper.ui.search.ApplySearchCommandTest`

Expected: FAIL (`applySearchCommand` not defined).

- [ ] **Step 3: Implement `applySearchCommand`**

```kotlin
package com.burha.fundhelper.ui.search

import com.burha.fundhelper.data.FundRepository
import com.burha.fundhelper.data.SearchOutcome
import com.burha.fundhelper.domain.SearchCommand
import com.burha.fundhelper.domain.parseSearchCommand

sealed class SearchSubmitResult {
    data object NavigateBack : SearchSubmitResult()
    data class Stay(val outcome: SearchOutcome) : SearchSubmitResult()
}

suspend fun applySearchCommand(
    query: String,
    funds: FundRepository,
    refetchCatalog: Boolean = false,
): SearchSubmitResult {
    return when (val command = parseSearchCommand(query)) {
        SearchCommand.Reset -> {
            funds.clearFollows()
            SearchSubmitResult.NavigateBack
        }
        is SearchCommand.BulkFollow -> {
            funds.followAll(command.codes)
            SearchSubmitResult.NavigateBack
        }
        is SearchCommand.TextSearch ->
            SearchSubmitResult.Stay(funds.search(query, refetchCatalog))
    }
}
```

- [ ] **Step 4: Wire `SearchViewModel`**

Add `navigateBack: Boolean = false` to `SearchUiState`. Replace the body of `submit` so it calls `applySearchCommand` instead of `funds.search` directly. On `NavigateBack`, set `isSearching = false`, `navigateBack = true` (do not set `showError`). On `Stay`, keep the existing `EmptyQuery` / `Success` / `Failure` branches. Add:

```kotlin
fun consumeNavigateBack() {
    _state.update { it.copy(navigateBack = false) }
}
```

- [ ] **Step 5: Wire `SearchScreen`**

After the existing error `LaunchedEffect`, add:

```kotlin
LaunchedEffect(state.navigateBack) {
    if (state.navigateBack) {
        viewModel.consumeNavigateBack()
        onBack()
    }
}
```

`onBack` is already `navController.popBackStack()` in `FundHelperNav`. Do not add a new route.

- [ ] **Step 6: Run tests to verify they pass**

`.\gradlew.bat testDebugUnitTest --tests com.burha.fundhelper.ui.search.ApplySearchCommandTest`

Expected: PASS.

---

### Task 5: Living docs + verify

**Files:**
- Modify: `docs/architecture.md`
- Modify: `progress.md`

- [ ] **Step 1: Architecture** — Search **Ara** classifies `RESET` / comma bulk-follow / text search. `followAll` appends exact catalog codes (one backup write). `clearFollows` empties Room follows and the Downloads file; snapshots stay. After reset or bulk follow, pop to the watchlist. Watchlist sort unchanged.

- [ ] **Step 2: Progress** — Note this amendment on a feature branch; A23 still last verified on the PR #12 APK (sort APK may still not be on the phone). Sideload with `adb install -r` only. Do not uninstall.

- [ ] **Step 3: Full unit tests**

Run: `.\gradlew.bat testDebugUnitTest`

Expected: BUILD SUCCESSFUL, all tests PASS.

- [ ] **Step 4: Sideload** when a device is present, per `.cursor/skills/sideload-a23/SKILL.md` (`assembleDebug` then `adb install -r`). Confirm: `AAK, AAL` lands on the watchlist with both followed; unknown codes skipped; `RESET` empties the list; a single code still shows search results; `RESET, AAK` does not wipe.

---

## Spec coverage (self-review)

| Spec rule | Task |
|-----------|------|
| Exact `RESET` any case, no comma, clear Room + empty backup, pop | 1, 2, 4 |
| Comma list = exact codes, append, skip misses/`RESET` in list, pop | 1, 3, 4 |
| No comma = today’s search, stay | 1, 4 |
| `RESET,` is not a wipe | 1, 4 |
| Catalog failure: add nothing, no wipe, still pop, no snackbar | 3, 4 |
| Snapshots kept on RESET | 2 |
| Canonical catalog code, no prefix match, one backup write | 3 |
| Backup write failure does not re-insert follows | 2 |
| **Ara** label / IME Search / no confirm / no new screen | 4 |
| Watchlist sort unchanged | (do not touch `WatchlistSort`) |
| `adb install -r`, no uninstall | 5 |
