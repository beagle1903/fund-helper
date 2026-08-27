# Turkish search fold Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make fund search treat Turkish letters as equivalent to their ASCII pairs in both directions, without changing on-screen TEFAS spelling.

**Architecture:** Add a pure `foldForSearch` in `domain/`. `FundRepository.matchesQuery` folds the query, code, and name, then keeps existing `startsWith` / `contains`. No UI, Room, or TEFAS changes.

**Tech Stack:** Kotlin, JUnit 4, existing `FundRepository` + fakes. No new libraries. Sideload with `adb install -r` only.

## Global Constraints

- Spec: `docs/superpowers/specs/2026-08-27-turkish-search-fold-design.md`. Frozen v1 spec is unchanged.
- `applicationId` / namespace: `com.burha.fundhelper`. Default UI language: Turkish.
- UI → ViewModels → `FundRepository` only. Do not change Room, TEFAS, follow-backup, or Compose screens.
- Fold **both** query and catalog text (vice versa). Official names on cards stay as TEFAS sent them.
- After the letter map, lowercase with `Locale.ROOT`, never `tr-TR`.
- Circumflex `â` `î` `û` (and capitals) strip to `a` `i` `u`. They are not extra Turkish letters.
- No live `tefas.gov.tr` in unit tests. Do not uninstall the A23 app (`adb install -r`).

## File structure

| Path | Responsibility |
|------|----------------|
| Create `app/src/main/java/com/burha/fundhelper/domain/FoldForSearch.kt` | Pure `foldForSearch(String)` |
| Create `app/src/test/java/com/burha/fundhelper/domain/FoldForSearchTest.kt` | Unit tests for the fold map |
| Modify `app/src/main/java/com/burha/fundhelper/data/FundRepository.kt` | `matchesQuery` uses the fold; drop unused `tr` |
| Modify `app/src/test/java/com/burha/fundhelper/data/FundRepositoryTest.kt` | Search cases: `yatirim`, vice versa, `degisken`, `AAK`, non-match |
| Modify `docs/architecture.md`, `progress.md` | Living handoff |
| Modify `docs/superpowers/specs/2026-08-27-turkish-search-fold-design.md` | Status line `Approved` if not already |

---

### Task 1: `foldForSearch`

**Files:**
- Create: `app/src/test/java/com/burha/fundhelper/domain/FoldForSearchTest.kt`
- Create: `app/src/main/java/com/burha/fundhelper/domain/FoldForSearch.kt`

**Interfaces:**
- Consumes: nothing
- Produces: `fun foldForSearch(raw: String): String`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.burha.fundhelper.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class FoldForSearchTest {

    @Test
    fun i_family_folds_to_i() {
        assertEquals("i", foldForSearch("i"))
        assertEquals("i", foldForSearch("ı"))
        assertEquals("i", foldForSearch("I"))
        assertEquals("i", foldForSearch("İ"))
    }

    @Test
    fun turkish_consonants_and_vowels_fold() {
        assertEquals("s", foldForSearch("ş"))
        assertEquals("s", foldForSearch("Ş"))
        assertEquals("g", foldForSearch("ğ"))
        assertEquals("g", foldForSearch("Ğ"))
        assertEquals("u", foldForSearch("ü"))
        assertEquals("u", foldForSearch("Ü"))
        assertEquals("o", foldForSearch("ö"))
        assertEquals("o", foldForSearch("Ö"))
        assertEquals("c", foldForSearch("ç"))
        assertEquals("c", foldForSearch("Ç"))
    }

    @Test
    fun circumflex_strips_to_base_vowel() {
        assertEquals("a", foldForSearch("â"))
        assertEquals("a", foldForSearch("Â"))
        assertEquals("i", foldForSearch("î"))
        assertEquals("i", foldForSearch("Î"))
        assertEquals("u", foldForSearch("û"))
        assertEquals("u", foldForSearch("Û"))
        assertEquals("kar", foldForSearch("kâr"))
        assertEquals(foldForSearch("kar"), foldForSearch("kâr"))
    }

    @Test
    fun words_fold_both_directions() {
        assertEquals("yatirim", foldForSearch("Yatırım"))
        assertEquals(foldForSearch("yatirim"), foldForSearch("Yatırım"))
        assertEquals("degisken", foldForSearch("Değişken"))
        assertEquals(foldForSearch("degisken"), foldForSearch("DEĞİŞKEN"))
        assertEquals("aak", foldForSearch("AAK"))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests com.burha.fundhelper.domain.FoldForSearchTest`

Expected: FAIL (unresolved `foldForSearch`)

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.burha.fundhelper.domain

import java.util.Locale

fun foldForSearch(raw: String): String {
    val mapped = buildString(raw.length) {
        for (ch in raw) {
            append(
                when (ch) {
                    'ı', 'I', 'İ' -> 'i'
                    'ş', 'Ş' -> 's'
                    'ğ', 'Ğ' -> 'g'
                    'ü', 'Ü' -> 'u'
                    'ö', 'Ö' -> 'o'
                    'ç', 'Ç' -> 'c'
                    'â', 'Â' -> 'a'
                    'î', 'Î' -> 'i'
                    'û', 'Û' -> 'u'
                    else -> ch
                },
            )
        }
    }
    return mapped.lowercase(Locale.ROOT)
}
```

Map `İ` and `ı` **before** lowercase. Do not use `Locale("tr", "TR")` here. `'i'` can fall through to `else` then ROOT lowercase.

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests com.burha.fundhelper.domain.FoldForSearchTest`

Expected: PASS

- [ ] **Step 5: Commit**

```powershell
git add app/src/test/java/com/burha/fundhelper/domain/FoldForSearchTest.kt app/src/main/java/com/burha/fundhelper/domain/FoldForSearch.kt
git commit -m "feat: fold Turkish letters for search matching"
```

---

### Task 2: Wire search matching

**Files:**
- Modify: `app/src/main/java/com/burha/fundhelper/data/FundRepository.kt`
- Modify: `app/src/test/java/com/burha/fundhelper/data/FundRepositoryTest.kt`

**Interfaces:**
- Consumes: `foldForSearch(raw: String): String`
- Produces: `matchesQuery` still `Boolean`; search still `SearchOutcome`. Existing empty-query and catalog-cache tests must keep passing.

- [ ] **Step 1: Write the failing repository tests**

In `FundRepositoryTest`, add a third catalog fund next to `aak` / `aal` (keep those names as they are). Add:

```kotlin
    private val yatirimFon = aak.copy(code = "XYZ", name = "ÖRNEK YATIRIM FONU")
```

Change `repo()` default fake catalog to `listOf(aak, aal, yatirimFon)` so the new tests see it. `repoWithBackup` can stay on `aak, aal` unless a test needs the new fund.

Add these tests (do not remove existing search tests):

```kotlin
    @Test
    fun search_folds_turkish_letters_both_directions() = runTest {
        val (repository, _, _) = repo()
        val ascii = repository.search("yatirim")
        assertTrue(ascii is SearchOutcome.Success)
        assertEquals(listOf("XYZ"), (ascii as SearchOutcome.Success).matches.map { it.code })
        val dotted = repository.search("Yatırım")
        assertTrue(dotted is SearchOutcome.Success)
        assertEquals(listOf("XYZ"), (dotted as SearchOutcome.Success).matches.map { it.code })
    }

    @Test
    fun search_folds_existing_catalog_name() = runTest {
        val (repository, _, _) = repo()
        val outcome = repository.search("degisken")
        assertTrue(outcome is SearchOutcome.Success)
        val codes = (outcome as SearchOutcome.Success).matches.map { it.code }
        assertTrue(codes.contains("AAK"))
        assertTrue(!codes.contains("XYZ"))
    }

    @Test
    fun search_code_prefix_still_matches() = runTest {
        val (repository, _, _) = repo()
        val outcome = repository.search("AAK")
        assertTrue(outcome is SearchOutcome.Success)
        assertEquals(listOf("AAK"), (outcome as SearchOutcome.Success).matches.map { it.code })
    }

    @Test
    fun search_folded_query_does_not_match_unrelated() = runTest {
        val (repository, _, _) = repo()
        val outcome = repository.search("zzzz")
        assertTrue(outcome is SearchOutcome.Success)
        assertTrue((outcome as SearchOutcome.Success).matches.isEmpty())
    }
```

`aak.name` is `ATA PORTFÖY ÇOKLU VARLIK DEĞİŞKEN FON`, so `degisken` must match AAK **after** the fold is wired. These tests FAIL until Step 3 (`degisken` will not match `DEĞİŞKEN` under `lowercase(tr)`).

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests com.burha.fundhelper.data.FundRepositoryTest.search_folds_turkish_letters_both_directions --tests com.burha.fundhelper.data.FundRepositoryTest.search_folds_existing_catalog_name`

Expected: FAIL (`degisken` / `yatirim` do not match)

- [ ] **Step 3: Wire `matchesQuery`**

In `FundRepository.kt`:

1. Remove `import java.util.Locale` and `private val tr = Locale("tr", "TR")` if nothing else in the file uses them.
2. Keep `import com.burha.fundhelper.domain.FundSnapshot` / `ReturnKeys`. Add `import com.burha.fundhelper.domain.foldForSearch`.
3. Replace `matchesQuery` with:

```kotlin
    private fun matchesQuery(fund: FundSnapshot, raw: String): Boolean {
        val q = foldForSearch(raw)
        val code = foldForSearch(fund.code)
        val name = foldForSearch(fund.name)
        return code.startsWith(q) || name.contains(q)
    }
```

Do not change `search()` trim / empty-query / catalog / upsert. Do not change screens. Display names stay the snapshot strings.

- [ ] **Step 4: Run repository tests**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests com.burha.fundhelper.data.FundRepositoryTest`

Expected: PASS (old search tests + new fold tests)

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/burha/fundhelper/data/FundRepository.kt app/src/test/java/com/burha/fundhelper/data/FundRepositoryTest.kt
git commit -m "feat: search funds with folded Turkish letters"
```

---

### Task 3: Verify, docs, sideload

**Files:**
- Modify: `docs/architecture.md` (search matching sentence)
- Modify: `progress.md` (fold on current branch; sideload `-r`)
- Modify: `docs/superpowers/specs/2026-08-27-turkish-search-fold-design.md` status line to `Approved` if it still says Proposed

**Interfaces:**
- Consumes: Tasks 1–2
- Produces: full unit test run; debug APK if assembling

- [ ] **Step 1: Run all unit tests**

Run: `.\gradlew.bat :app:testDebugUnitTest`

Expected: BUILD SUCCESSFUL, all tests PASS (including `FoldForSearchTest` and `FundRepositoryTest`)

- [ ] **Step 2: Assemble debug APK**

Run: `.\gradlew.bat assembleDebug`

Expected: BUILD SUCCESSFUL. APK: `app/build/outputs/apk/debug/app-debug.apk`

- [ ] **Step 3: Sideload only if A23 is `device`**

Read `.cursor/skills/sideload-a23/SKILL.md`. adb: `C:\Users\burha\AppData\Local\Android\Sdk\platform-tools\adb.exe`

```powershell
& "C:\Users\burha\AppData\Local\Android\Sdk\platform-tools\adb.exe" install -r app/build/outputs/apk/debug/app-debug.apk
```

Do **not** uninstall. If no device, record that in `progress.md` and skip.

Manual: search `yatirim`, `degisken`, `AAK`; cards still show official spelling (`Yatırım` / `DEĞİŞKEN`, not folded ASCII).

- [ ] **Step 4: Update living docs**

In `docs/architecture.md`, extend the FundRepository (or Screens) bullet so search matching folds Turkish letters both ways. Do not rewrite the frozen v1 spec.

Replace `progress.md` with a pruned handoff: fold is on this branch; A23 sideload yes/no; next is stop (do not start another feature).

- [ ] **Step 5: Commit**

```powershell
git add docs/architecture.md progress.md docs/superpowers/specs/2026-08-27-turkish-search-fold-design.md
git commit -m "docs: record Turkish search fold handoff"
```

Then open a PR for `feat/turkish-search-fold` and stop.

---

## Spec coverage (self-review)

| Spec item | Task |
|-----------|------|
| Fold both query and catalog (vice versa) | 1, 2 |
| Letter map i/ı/ş/ğ/ü/ö/ç + ROOT lowercase | 1 |
| Circumflex strips to a/i/u, not a letter-map feature | 1 |
| `startsWith` code / `contains` name after fold | 2 |
| Empty query, snackbar, follows unchanged | 2 (existing tests) |
| Official spelling on UI | 2 (no screen edits) |
| `yatirim` / `Yatırım` / `degisken` / `AAK` / unrelated | 1, 2 |
| No live TEFAS; `adb install -r` | 3 |
| Living architecture + progress | 3 |
