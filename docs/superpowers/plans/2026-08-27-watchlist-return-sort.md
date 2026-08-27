# Watchlist return sort Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Sort the watchlist by headline return (most negative first, most positive last) and make empty/search CTAs full-width on the A23.

**Architecture:** Pure `sortByHeadlineReturn` in `domain/`. `WatchlistViewModel` applies it to `observeWatchlist()` rows. Room and `FundRepository` keep code order. Empty watchlist and search **Ara** use `Modifier.fillMaxWidth()`.

**Tech Stack:** Kotlin, JUnit 4, existing `WatchlistRow`, Compose Material 3 `Button` / `Column`. No new libraries. Sideload with `adb install -r` only.

## Global Constraints

- Spec: `docs/superpowers/specs/2026-08-27-watchlist-return-sort-design.md`. Frozen v1 spec is unchanged.
- `applicationId` / namespace: `com.burha.fundhelper`. Default UI language: Turkish.
- UI → ViewModels → `FundRepository` only. Do not change Room, TEFAS, or follow-backup.
- Sort key is the card headline (`ReturnKeys.headline`), not a second period.
- Ascending returns; missing last; code tiebreak. No sort menu, no ranking copy.
- No live `tefas.gov.tr` in unit tests. Do not uninstall the A23 app (`adb install -r`).
- Do not commit unless the user asks.

## File structure

| Path | Responsibility |
|------|----------------|
| Create `app/src/main/java/com/burha/fundhelper/domain/WatchlistSort.kt` | Pure `sortByHeadlineReturn` |
| Create `app/src/test/java/com/burha/fundhelper/domain/WatchlistSortTest.kt` | Unit tests for the sort |
| Modify `app/src/main/java/com/burha/fundhelper/ui/watchlist/WatchlistViewModel.kt` | Apply sort when collecting rows |
| Modify `app/src/main/java/com/burha/fundhelper/ui/watchlist/WatchlistScreen.kt` | Centered empty state, full-width CTA |
| Modify `app/src/main/java/com/burha/fundhelper/ui/search/SearchScreen.kt` | Full-width **Ara** |
| Modify `app/src/main/res/values/strings.xml` | Empty-state body string |
| Modify `docs/architecture.md`, `progress.md` | Living handoff |

---

### Task 1: `sortByHeadlineReturn`

**Files:**
- Create: `app/src/test/java/com/burha/fundhelper/domain/WatchlistSortTest.kt`
- Create: `app/src/main/java/com/burha/fundhelper/domain/WatchlistSort.kt`

**Interfaces:**
- Consumes: nothing
- Produces: `fun <T> sortByHeadlineReturn(rows: List<T>, headlineReturn: (T) -> Double?, code: (T) -> String): List<T>`

- [ ] **Step 1: Write the failing test**

Use a tiny test row. Assert `-5, 0, 3`; missing last; equal returns order by code; empty in → empty out.

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests com.burha.fundhelper.domain.WatchlistSortTest`

Expected: FAIL (function not defined).

- [ ] **Step 3: Write minimal implementation**

`compareBy(nullsLast()) { headlineReturn(it) }.thenBy { code(it) }`.

- [ ] **Step 4: Run test to verify it passes**

Same Gradle command. Expected: PASS.

---

### Task 2: WatchlistViewModel applies the sort

**Files:**
- Modify: `app/src/main/java/com/burha/fundhelper/ui/watchlist/WatchlistViewModel.kt`

**Interfaces:**
- Consumes: `sortByHeadlineReturn`, `WatchlistRow.headlineReturn`, `WatchlistRow.code`
- Produces: `WatchlistUiState.rows` in return order

- [ ] **Step 1: Sort rows in the collect lambda** before `copy(rows = …)`. Do not change `FundRepository.observeWatchlist()`.

---

### Task 3: Empty watchlist + full-width Ara

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/java/com/burha/fundhelper/ui/watchlist/WatchlistScreen.kt`
- Modify: `app/src/main/java/com/burha/fundhelper/ui/search/SearchScreen.kt`

- [ ] **Step 1:** Add `watchlist_empty_body`: `Kod veya ad ile fon arayın.`
- [ ] **Step 2:** Empty column: `Arrangement.Center`, `Alignment.CenterHorizontally`, `titleLarge` + body, `Button(Modifier.fillMaxWidth())`.
- [ ] **Step 3:** Search **Ara** `Modifier.fillMaxWidth().padding(horizontal = 16.dp)`.

Compose APIs: `Column(verticalArrangement, horizontalAlignment)`, filled `Button`, `Modifier.fillMaxWidth()` (developer.android.com Compose layouts / Button).

---

### Task 4: Living docs + verify

**Files:**
- Modify: `docs/architecture.md`, `progress.md`

- [ ] **Step 1:** Architecture: watchlist UI sorts by headline return ascending after repository observe.
- [ ] **Step 2:** Progress: A23 verified on current APK; this amendment next; do not uninstall.
- [ ] **Step 3:** `.\gradlew.bat testDebugUnitTest` then sideload per `.cursor/skills/sideload-a23/SKILL.md`.
