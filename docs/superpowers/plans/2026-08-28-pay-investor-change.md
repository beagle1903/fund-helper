# Pay adedi + yatırımcı change Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show day-over-day pay-adedi and yatırımcı signed % on watchlist cards and totals plus % on detail, from the existing 7-day price window; remove the on-screen disclaimer.

**Architecture:** `parseLatestPrices` keeps latest + previous priced day (`tedPaySayisi`, `kisiSayisi`). Four nullable counts on `FundSnapshot` / Room v2. Pure `percentChange` for UI. Refresh overwrites counts from the price row; search/`followAll` preserve them. No extra TEFAS URL.

**Tech Stack:** Kotlin, JUnit 4, Room 2.7.2 `Migration` + `addMigrations`, existing Compose Material 3 cards. Fetch Room builder APIs via Context7 (`/websites/developer_android`) at implementation time. Sideload with `adb install -r` only.

## Global Constraints

- Spec: `docs/superpowers/specs/2026-08-28-pay-investor-change-design.md`. Frozen 2026-08-22 spec file is not edited.
- `applicationId` / namespace: `com.burha.fundhelper`. Default UI language: Turkish.
- UI → ViewModels → `FundRepository` only. Wire JSON names stay inside `TefasJsonMapper`.
- Do not call `dagilimSiraliGetirT` or `fonFiyatBilgiGetir`. Do not add a third TEFAS URL.
- No green/red on pay/kişi %. Do not sort by these fields. Search cards unchanged.
- No live `tefas.gov.tr` in unit tests. Do not uninstall the A23 app (`adb install -r`).
- Do not commit unless the user asks.
- Room: additive v2, `exportSchema` stays false. **Never** `fallbackToDestructiveMigration` (wipes follows).

## File structure

| Path | Responsibility |
|------|----------------|
| Create `app/src/main/java/com/burha/fundhelper/domain/PercentChange.kt` | Pure `percentChange` |
| Create `app/src/test/java/com/burha/fundhelper/domain/PercentChangeTest.kt` | Unit tests for the formula |
| Modify `app/src/main/java/com/burha/fundhelper/domain/FundSnapshot.kt` | Four nullable count fields (defaults `null`) |
| Modify `app/src/test/resources/fixtures/yat-prices.json` | `tedPaySayisi` / `kisiSayisi` on two AAK days |
| Modify `app/src/main/java/com/burha/fundhelper/data/tefas/TefasJsonMapper.kt` | Latest + previous priced day |
| Modify `app/src/test/java/com/burha/fundhelper/data/tefas/TefasJsonMapperTest.kt` | Mapper count / `%` inputs |
| Modify `app/src/main/java/com/burha/fundhelper/data/local/SnapshotEntity.kt` | Four REAL columns |
| Modify `app/src/main/java/com/burha/fundhelper/data/local/SnapshotMapper.kt` | Map the four fields |
| Modify `app/src/main/java/com/burha/fundhelper/data/local/AppDatabase.kt` | version 2 + `MIGRATION_1_2` |
| Modify `app/src/main/java/com/burha/fundhelper/di/AppModule.kt` | `addMigrations(MIGRATION_1_2)` |
| Modify `app/src/main/java/com/burha/fundhelper/data/FundRepository.kt` | Merge rules + `WatchlistRow` % |
| Modify `app/src/test/java/com/burha/fundhelper/data/FundRepositoryTest.kt` | Overwrite / keep / search merge |
| Modify `app/src/main/java/com/burha/fundhelper/ui/UiFormat.kt` | `formatSignedPercent`, `formatCount` |
| Create `app/src/test/java/com/burha/fundhelper/ui/UiFormatTest.kt` | Formatter tests |
| Modify `app/src/main/res/values/strings.xml` | Card template + detail labels; delete disclaimer |
| Modify `app/src/main/java/com/burha/fundhelper/ui/watchlist/WatchlistScreen.kt` | Pay/kişi line |
| Modify `app/src/main/java/com/burha/fundhelper/ui/detail/DetailScreen.kt` | Totals + %; drop disclaimer |
| Modify living docs listed in Task 7 | Product rules |

Do not change `SearchScreen.kt` row content. Do not edit `docs/superpowers/specs/2026-08-22-fund-helper-v1-design.md`.

---

### Task 1: `percentChange`

**Files:**
- Create: `app/src/test/java/com/burha/fundhelper/domain/PercentChangeTest.kt`
- Create: `app/src/main/java/com/burha/fundhelper/domain/PercentChange.kt`

**Interfaces:**
- Consumes: nothing
- Produces: `fun percentChange(current: Double?, previous: Double?): Double?`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.burha.fundhelper.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PercentChangeTest {

    @Test
    fun normal_ratio() {
        assertEquals(10.0, percentChange(1100.0, 1000.0))
        assertEquals(-50.0, percentChange(50.0, 100.0))
        assertEquals(0.0, percentChange(100.0, 100.0))
    }

    @Test
    fun missing_or_zero_previous_is_null() {
        assertNull(percentChange(10.0, null))
        assertNull(percentChange(null, 10.0))
        assertNull(percentChange(null, null))
        assertNull(percentChange(10.0, 0.0))
        assertNull(percentChange(0.0, 0.0))
    }

    @Test
    fun zero_current_with_nonzero_previous_is_minus_one_hundred() {
        assertEquals(-100.0, percentChange(0.0, 80.0))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests com.burha.fundhelper.domain.PercentChangeTest`

Expected: FAIL (unresolved `percentChange`).

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.burha.fundhelper.domain

fun percentChange(current: Double?, previous: Double?): Double? {
    if (current == null || previous == null) return null
    if (previous == 0.0) return null
    return (current - previous) / previous * 100.0
}
```

- [ ] **Step 4: Run test to verify it passes**

Same Gradle command. Expected: PASS.

---

### Task 2: Parse latest + previous priced day

**Files:**
- Modify: `app/src/main/java/com/burha/fundhelper/domain/FundSnapshot.kt`
- Modify: `app/src/test/resources/fixtures/yat-prices.json`
- Modify: `app/src/test/java/com/burha/fundhelper/data/tefas/TefasJsonMapperTest.kt`
- Modify: `app/src/main/java/com/burha/fundhelper/data/tefas/TefasJsonMapper.kt`

**Interfaces:**
- Consumes: existing `JsonObject.double` / `string`; `FundSnapshot`
- Produces: `FundSnapshot.payCount`, `prevPayCount`, `investorCount`, `prevInvestorCount` (all `Double? = null`). `parseLatestPrices` still one snapshot per code with `fiyat > 0`.

- [ ] **Step 1: Add the four fields with defaults at the end of `FundSnapshot`** so existing positional constructors keep compiling:

```kotlin
    val fetchedAt: Long,
    val payCount: Double? = null,
    val prevPayCount: Double? = null,
    val investorCount: Double? = null,
    val prevInvestorCount: Double? = null,
)
```

- [ ] **Step 2: Extend `yat-prices.json`**

Keep AAL `fiyat: 0` (still skipped). On AAK days add counts. Add a one-day fund `BBB` with price > 0 so previous is null:

```json
{
  "errorCode": null,
  "resultList": [
    {
      "fonKodu": "AAK",
      "fonUnvan": "ATA PORTFÖY ÇOKLU VARLIK DEĞİŞKEN FON",
      "tarih": "2026-08-20",
      "fiyat": 34.1,
      "tedPaySayisi": 1000,
      "kisiSayisi": 100
    },
    {
      "fonKodu": "AAK",
      "fonUnvan": "ATA PORTFÖY ÇOKLU VARLIK DEĞİŞKEN FON",
      "tarih": "2026-08-21",
      "fiyat": 35.46418,
      "tedPaySayisi": 1100,
      "kisiSayisi": 110
    },
    {
      "fonKodu": "BBB",
      "fonUnvan": "TEK GÜN FON",
      "tarih": "2026-08-21",
      "fiyat": 1.5,
      "tedPaySayisi": 50,
      "kisiSayisi": 5
    },
    {
      "fonKodu": "AAL",
      "fonUnvan": "ATA PORTFÖY PARA PİYASASI (TL) FONU",
      "tarih": "2026-08-21",
      "fiyat": 0
    }
  ]
}
```

- [ ] **Step 3: Write the failing mapper assertions** (append to `TefasJsonMapperTest`)

```kotlin
    @Test
    fun parses_pay_and_investor_from_latest_and_previous_priced_day() {
        val funds = TefasJsonMapper.parseLatestPrices(fixture("yat-prices.json"))
        val aak = funds.single { it.code == "AAK" }
        assertEquals(35.46418, aak.price)
        assertEquals("2026-08-21", aak.priceDate)
        assertEquals(1100.0, aak.payCount)
        assertEquals(1000.0, aak.prevPayCount)
        assertEquals(110.0, aak.investorCount)
        assertEquals(100.0, aak.prevInvestorCount)
        val bbb = funds.single { it.code == "BBB" }
        assertEquals(50.0, bbb.payCount)
        assertNull(bbb.prevPayCount)
        assertEquals(5.0, bbb.investorCount)
        assertNull(bbb.prevInvestorCount)
        assertTrue(funds.none { it.code == "AAL" })
    }

    @Test
    fun missing_count_fields_are_null_not_a_fetch_error() {
        val funds = TefasJsonMapper.parseLatestPrices(
            """{"errorCode":null,"resultList":[
              {"fonKodu":"ZZZ","fonUnvan":"X","tarih":"2026-08-20","fiyat":1.0},
              {"fonKodu":"ZZZ","fonUnvan":"X","tarih":"2026-08-21","fiyat":2.0}
            ]}""",
        )
        val zzz = funds.single { it.code == "ZZZ" }
        assertNull(zzz.payCount)
        assertNull(zzz.prevPayCount)
        assertNull(zzz.investorCount)
        assertNull(zzz.prevInvestorCount)
    }
```

Keep existing `parses_latest_nonzero_price_per_code` (AAK price/date, AAL absent).

- [ ] **Step 4: Run tests to verify they fail**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests com.burha.fundhelper.data.tefas.TefasJsonMapperTest`

Expected: FAIL (counts still null / BBB unknown).

- [ ] **Step 5: Implement `parseLatestPrices`**

Replace the current “best by date” loop with: skip `fiyat` missing/`≤ 0`; per code keep a `tarih`→row map (later row wins on the same date); then latest = max `tarih`, previous = next-max `tarih` if any.

```kotlin
    fun parseLatestPrices(body: String): List<FundSnapshot> {
        data class Day(
            val name: String,
            val price: Double,
            val payCount: Double?,
            val investorCount: Double?,
        )
        val byCode = linkedMapOf<String, LinkedHashMap<String, Day>>()
        for (row in resultList(body)) {
            val code = row.string("fonKodu")?.trim().orEmpty()
            val price = row.double("fiyat") ?: continue
            if (code.isEmpty() || price <= 0.0) continue
            val date = row.string("tarih")?.take(10) ?: continue
            val days = byCode.getOrPut(code) { linkedMapOf() }
            days[date] = Day(
                name = row.string("fonUnvan").orEmpty(),
                price = price,
                payCount = row.double("tedPaySayisi"),
                investorCount = row.double("kisiSayisi"),
            )
        }
        return byCode.map { (code, days) ->
            val ordered = days.keys.sortedDescending()
            val latestDate = ordered.first()
            val latest = days.getValue(latestDate)
            val previous = ordered.getOrNull(1)?.let { days.getValue(it) }
            FundSnapshot(
                code = code,
                name = latest.name,
                kind = FundKind.YAT,
                price = latest.price,
                priceDate = latestDate,
                returns = emptyMap(),
                fundType = null,
                risk = null,
                fees = emptyList(),
                fetchedAt = 0L,
                payCount = latest.payCount,
                prevPayCount = previous?.payCount,
                investorCount = latest.investorCount,
                prevInvestorCount = previous?.investorCount,
            )
        }
    }
```

Skip a row with no `tarih` (cannot order days). Catalog parser stays unchanged (counts default null).

- [ ] **Step 6: Run mapper tests**

Same Gradle command. Expected: PASS.

---

### Task 3: Room v2 additive columns

**Files:**
- Modify: `app/src/main/java/com/burha/fundhelper/data/local/SnapshotEntity.kt`
- Modify: `app/src/main/java/com/burha/fundhelper/data/local/SnapshotMapper.kt`
- Modify: `app/src/main/java/com/burha/fundhelper/data/local/AppDatabase.kt`
- Modify: `app/src/main/java/com/burha/fundhelper/di/AppModule.kt`

**Interfaces:**
- Consumes: `FundSnapshot` four count fields
- Produces: `SnapshotEntity.payCount` / `prevPayCount` / `investorCount` / `prevInvestorCount`; `AppDatabase` version `2`; `val MIGRATION_1_2: Migration`

Room APIs (Context7 `/websites/developer_android`, Room 2.7.2): `androidx.room.migration.Migration`, `override fun migrate(db: SupportSQLiteDatabase)`, `Room.databaseBuilder(...).addMigrations(MIGRATION_1_2).build()`. Do **not** call `fallbackToDestructiveMigration`. `exportSchema` stays `false`. No `MigrationTestHelper` (CI has no emulator); follows survive because this SQL does not touch `follows`.

- [ ] **Step 1: Add four nullable `Double?` columns to `SnapshotEntity`** (same names as domain).

- [ ] **Step 2: Map them in `SnapshotMapper.toEntity` / `toDomain`.** Round-trip: null stays null.

- [ ] **Step 3: Bump `@Database(version = 2)` and add:**

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE snapshots ADD COLUMN payCount REAL")
        db.execSQL("ALTER TABLE snapshots ADD COLUMN prevPayCount REAL")
        db.execSQL("ALTER TABLE snapshots ADD COLUMN investorCount REAL")
        db.execSQL("ALTER TABLE snapshots ADD COLUMN prevInvestorCount REAL")
    }
}
```

Put `MIGRATION_1_2` in `AppDatabase.kt` (same file, not a new module). Import `androidx.room.migration.Migration` and `androidx.sqlite.db.SupportSQLiteDatabase`. If Room 2.7.2 requires `override fun migrate(connection: SQLiteConnection)` instead, use that override and `connection.execSQL(...)` — same four `ALTER TABLE` statements. Fetch the current `Migration` signature from Context7 (`/websites/developer_android`) before writing it.

- [ ] **Step 4: Wire the builder**

```kotlin
Room.databaseBuilder(context, AppDatabase::class.java, "fund-helper.db")
    .addMigrations(MIGRATION_1_2)
    .build()
```

- [ ] **Step 5: Compile**

Run: `.\gradlew.bat :app:compileDebugKotlin :app:compileDebugUnitTestKotlin`

Expected: SUCCESS. Existing `FundRepositoryTest` still compiles (`FundSnapshot` defaults).

---

### Task 4: Repository merge + watchlist %

**Files:**
- Modify: `app/src/main/java/com/burha/fundhelper/data/FundRepository.kt`
- Modify: `app/src/test/java/com/burha/fundhelper/data/FundRepositoryTest.kt`

**Interfaces:**
- Consumes: `percentChange`, four count fields on `FundSnapshot` / `SnapshotEntity`
- Produces: `WatchlistRow.payChangePct: Double?`, `WatchlistRow.investorChangePct: Double?`

Private helpers in `FundRepository.kt` (not a new public API):

```kotlin
private fun FundSnapshot.replacingPriceWindow(from: FundSnapshot) = copy(
    price = from.price,
    priceDate = from.priceDate,
    payCount = from.payCount,
    prevPayCount = from.prevPayCount,
    investorCount = from.investorCount,
    prevInvestorCount = from.prevInvestorCount,
)

private fun FundSnapshot.keepingPriceWindow(from: FundSnapshot?) = copy(
    price = price ?: from?.price,
    priceDate = priceDate ?: from?.priceDate,
    payCount = payCount ?: from?.payCount,
    prevPayCount = prevPayCount ?: from?.prevPayCount,
    investorCount = investorCount ?: from?.investorCount,
    prevInvestorCount = prevInvestorCount ?: from?.prevInvestorCount,
)
```

- [ ] **Step 1: Extend `WatchlistRow` and `observeWatchlist`**

```kotlin
data class WatchlistRow(
    val code: String,
    val name: String?,
    val price: Double?,
    val headlinePeriod: String?,
    val headlineReturn: Double?,
    val fetchedAt: Long?,
    val payChangePct: Double?,
    val investorChangePct: Double?,
)

// inside map:
val snapshot = followed.snapshot?.let(SnapshotMapper::toDomain)
val headline = snapshot?.returns?.let(ReturnKeys::headline)
WatchlistRow(
    code = followed.follow.code,
    name = snapshot?.name,
    price = snapshot?.price,
    headlinePeriod = headline?.first,
    headlineReturn = headline?.second,
    fetchedAt = snapshot?.fetchedAt,
    payChangePct = percentChange(snapshot?.payCount, snapshot?.prevPayCount),
    investorChangePct = percentChange(snapshot?.investorCount, snapshot?.prevInvestorCount),
)
```

- [ ] **Step 2: `refreshFollowed` merge** (inside the existing `codes.mapNotNull`):

If `priceRow != null`, `base.copy(... catalog fields ...).replacingPriceWindow(priceRow)` then `fetchedAt = now`.  
If `priceRow == null`, `keepingPriceWindow(previous)` for price/date/counts (catalog fields as today).

Catalog still owns name / type / risk / returns when present.

- [ ] **Step 3: `search` merge**

Before `snapshotDao.upsertAll`, for each match load `snapshotDao.get(code)` and `keepingPriceWindow(previous)` so catalog upsert cannot clear price or the four counts. Return the merged snapshots in `SearchOutcome.Success` (matches on screen may still lack counts; search **cards** still do not show them).

- [ ] **Step 4: `followAll` merge**

On the existing `listing.copy(...)`, also `keepingPriceWindow(previous)` so already-followed priced rows keep counts.

- [ ] **Step 5: Failing repository tests** (append to `FundRepositoryTest`)

Build a priced snapshot with counts:

```kotlin
    private val aakCounted = aakPriced.copy(
        payCount = 1100.0,
        prevPayCount = 1000.0,
        investorCount = 110.0,
        prevInvestorCount = 100.0,
    )
```

```kotlin
    @Test
    fun refresh_price_row_overwrites_counts_including_nulls() = runTest {
        val snapshots = FakeSnapshotDao()
        val tefas = FakeTefasClient(catalog = listOf(aak), prices = listOf(aakCounted))
        val repository = FundRepository(FakeFollowDao(snapshots), snapshots, tefas, FakeClock(), FakeFollowBackup())
        repository.follow("AAK")
        repository.refreshFollowed(force = true)
        val row = repository.observeWatchlist().first().single()
        assertEquals(10.0, row.payChangePct)
        assertEquals(10.0, row.investorChangePct)
        tefas.prices = listOf(aakPriced.copy(price = 36.0, priceDate = "2026-08-22"))
        repository.refreshFollowed(force = true)
        val cleared = repository.observeWatchlist().first().single()
        assertEquals(36.0, cleared.price)
        assertNull(cleared.payChangePct)
        assertNull(cleared.investorChangePct)
        assertNull(snapshots.get("AAK")?.payCount)
    }

    @Test
    fun refresh_without_price_row_keeps_counts() = runTest {
        val snapshots = FakeSnapshotDao()
        val tefas = FakeTefasClient(catalog = listOf(aak), prices = listOf(aakCounted))
        val repository = FundRepository(FakeFollowDao(snapshots), snapshots, tefas, FakeClock(), FakeFollowBackup())
        repository.follow("AAK")
        repository.refreshFollowed(force = true)
        tefas.prices = emptyList()
        repository.refreshFollowed(force = true)
        val row = repository.observeWatchlist().first().single()
        assertEquals(10.0, row.payChangePct)
        assertEquals(35.46, row.price)
    }

    @Test
    fun refresh_failure_keeps_counts() = runTest {
        val snapshots = FakeSnapshotDao()
        val tefas = FakeTefasClient(catalog = listOf(aak), prices = listOf(aakCounted))
        val repository = FundRepository(FakeFollowDao(snapshots), snapshots, tefas, FakeClock(), FakeFollowBackup())
        repository.follow("AAK")
        repository.refreshFollowed(force = true)
        tefas.failCatalog = true
        val failed = repository.refreshFollowed(force = true)
        assertTrue(failed.isFailure)
        assertEquals(10.0, repository.observeWatchlist().first().single().payChangePct)
    }

    @Test
    fun search_does_not_wipe_price_or_counts() = runTest {
        val snapshots = FakeSnapshotDao()
        val tefas = FakeTefasClient(catalog = listOf(aak), prices = listOf(aakCounted))
        val repository = FundRepository(FakeFollowDao(snapshots), snapshots, tefas, FakeClock(), FakeFollowBackup())
        repository.follow("AAK")
        repository.refreshFollowed(force = true)
        repository.search("AAK")
        val entity = snapshots.get("AAK")
        assertEquals(35.46, entity?.price)
        assertEquals(1100.0, entity?.payCount)
        assertEquals(1000.0, entity?.prevPayCount)
        val row = repository.observeWatchlist().first().single()
        assertEquals(10.0, row.payChangePct)
    }

    @Test
    fun follow_all_keeps_counts_on_already_followed_fund() = runTest {
        val (repository, _, snapshots) = repo(tefas = FakeTefasClient(catalog = listOf(aak, aal), prices = listOf(aakCounted)))
        repository.follow("AAK")
        repository.refreshFollowed(force = true)
        repository.followAll(listOf("AAK", "AAL"))
        assertEquals(1100.0, snapshots.get("AAK")?.payCount)
        assertEquals(1000.0, snapshots.get("AAK")?.prevPayCount)
    }
```

Default `repo()` still uses `aakPriced` without counts; existing tests stay valid (`payChangePct` null).

- [ ] **Step 6: Run tests**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests com.burha.fundhelper.data.FundRepositoryTest`

Expected: FAIL until merge is implemented, then PASS. Do not regress follow / search / sort tests (`.\gradlew.bat :app:testDebugUnitTest`).

---

### Task 5: Formatters + watchlist card

**Files:**
- Modify: `app/src/main/java/com/burha/fundhelper/ui/UiFormat.kt`
- Create: `app/src/test/java/com/burha/fundhelper/ui/UiFormatTest.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/java/com/burha/fundhelper/ui/watchlist/WatchlistScreen.kt`

**Interfaces:**
- Consumes: `WatchlistRow.payChangePct`, `investorChangePct`, `formatNumber`
- Produces: `fun formatSignedPercent(value: Double?, dash: String): String`, `fun formatCount(value: Double): String`

Do **not** use `ReturnPercentText` for pay/kişi. Do **not** change `SearchRow`.

- [ ] **Step 1: Failing formatter tests**

```kotlin
package com.burha.fundhelper.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class UiFormatTest {

    @Test
    fun signed_percent_plus_minus_zero_and_dash() {
        assertEquals("+10%", formatSignedPercent(10.0, "—"))
        assertEquals("${formatNumber(-50.0)}%", formatSignedPercent(-50.0, "—"))
        assertEquals("0%", formatSignedPercent(0.0, "—"))
        assertEquals("—", formatSignedPercent(null, "—"))
    }

    @Test
    fun count_is_whole_number_with_tr_grouping() {
        assertEquals("1.100", formatCount(1100.0))
        assertEquals("5", formatCount(5.0))
    }
}
```

`formatNumber` already uses `tr-TR` and trims decimals; signed % should wrap that (`+` only when `value > 0`). `formatCount` uses `NumberFormat.getIntegerInstance(Locale.forLanguageTag("tr-TR"))` on `kotlin.math.round(value).toLong()` (JSON sends `.0` doubles).

- [ ] **Step 2: Run to fail, then implement in `UiFormat.kt`, re-run.** Expected: PASS.

Note: `formatNumber(10.0)` is `"10"` not `"10,0000"` after trim, so `"+10%"`. Negative uses the formatter’s minus. Do not force two fraction digits.

- [ ] **Step 3: Strings**

Add:

```xml
    <string name="watchlist_pay_kisi">Pay %1$s · Kişi %2$s</string>
    <string name="detail_pay">Pay adedi</string>
    <string name="detail_investors">Yatırımcı sayısı</string>
```

Delete `<string name="disclaimer">Yatırım tavsiyesi değildir.</string>` in Task 6 with the screen; may delete here if grepping shows no remaining references after Task 6. Prefer deleting in Task 6 so this task still compiles if disclaimer is referenced.

- [ ] **Step 4: Watchlist supporting slot** — between price `Text` and fetched-at `Text`:

```kotlin
            Text(
                stringResource(
                    R.string.watchlist_pay_kisi,
                    formatSignedPercent(row.payChangePct, dash),
                    formatSignedPercent(row.investorChangePct, dash),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
```

Always both labels. Color is `onSurfaceVariant` only.

---

### Task 6: Detail rows + drop disclaimer

**Files:**
- Modify: `app/src/main/java/com/burha/fundhelper/ui/detail/DetailScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Grep: `disclaimer` in `app/` (must be gone from UI)

**Interfaces:**
- Consumes: `snapshot.payCount` / `prevPayCount` / `investorCount` / `prevInvestorCount`, `percentChange`, `formatCount`, `formatSignedPercent`
- Produces: two labeled rows after the returns card; no disclaimer `Text`

- [ ] **Step 1: After the returns `Card`, before type/risk/fees**, two blocks:

```kotlin
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.detail_pay), style = MaterialTheme.typography.titleSmall)
                    CountChangeLine(
                        count = snapshot.payCount,
                        changePct = percentChange(snapshot.payCount, snapshot.prevPayCount),
                        missing = missing,
                        dash = dash,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.detail_investors), style = MaterialTheme.typography.titleSmall)
                    CountChangeLine(
                        count = snapshot.investorCount,
                        changePct = percentChange(snapshot.investorCount, snapshot.prevInvestorCount),
                        missing = missing,
                        dash = dash,
                    )
```

Private composable in the same file:

- If `count == null` → `Text(missing)` only (existing `missing_field`).
- Else → `Text("${formatCount(count)}  ${formatSignedPercent(changePct, dash)}")` uncolored (`bodyMedium` / default on-surface). Total exists and % null → number + `—`.

- [ ] **Step 2: Delete the disclaimer `Text` and `R.string.disclaimer`.** `ExplanationMapperTest` already asserts the mapper does not emit that sentence — leave it.

- [ ] **Step 3: Grep `disclaimer` and `Yatırım tavsiyesi değildir` under `app/src/main`.** Only allowed leftover is mapper tests asserting absence. Compile: `.\gradlew.bat :app:compileDebugKotlin`

---

### Task 7: Living docs + verify

**Files:**
- Modify: `docs/context.md`
- Modify: `docs/architecture.md`
- Modify: `docs/decisions.md`
- Modify: `README.md`
- Modify: `progress.md`

Do not edit `docs/superpowers/specs/2026-08-22-fund-helper-v1-design.md`.

- [ ] **Step 1: `docs/context.md`**

- Drop “Always treat it as yatırım tavsiyesi değildir” as an on-screen requirement. Keep: informational, no buy/sell advice, mapper maps official fields only.
- v1 job detail line: type, risk, fees, explanation; **no** disclaimer. Mention pay/kişi day-over-day % on watchlist + totals on detail.
- Non-goals: keep holdings / TL amounts / cost basis out. This spec’s pay adedi is TEFAS tedavül, not the user’s unit count — do not list “unit counts” as a blanket ban.

- [ ] **Step 2: `docs/architecture.md`**

- Screens: watchlist card line `Pay … · Kişi …` uncolored; detail labeled Pay adedi / Yatırımcı sayısı; no disclaimer.
- `FundRepository.refreshFollowed` merge includes four counts; search merge preserves price-window fields.
- `TefasJsonMapper.parseLatestPrices`: latest + previous priced day; `tedPaySayisi` / `kisiSayisi`.
- Room snapshots version 2; `MIGRATION_1_2` additive; never destructive.
- `percentChange` in `domain/`.

- [ ] **Step 3: `docs/decisions.md`**

Amend **005** Consequences/Decision: screens no longer include the disclaimer sentence; mapper still has no buy/sell language.

Add:

```markdown
## 007 - On-screen disclaimer removed

**Context:** v1 showed “Yatırım tavsiyesi değildir.” as body text on detail. The first user does not want that line on the phone.

**Decision:** Remove the string from Detail and `R.string.disclaimer`. Living product docs drop the on-screen requirement. Frozen 2026-08-22 spec is historical. Mapper and screens still must not recommend buy/sell/hold. A store-facing disclaimer waits for Play.

**Consequences:** Detail no longer shows that sentence. Play/SPK copy is a later product decision, not this pass.
```

- [ ] **Step 4: `README.md`** — remove the two **Yatırım tavsiyesi değildir.** lines. Keep “informational / not investment advice” in one short sentence without that Turkish sentence if you still want an English hedge; do not put the Turkish disclaimer back.

- [ ] **Step 5: `progress.md`** — pruned: pay/kişi on watchlist+detail on A23 after sideload; disclaimer gone; do not uninstall; next = confirm TEFAS keys if counts show `—`.

- [ ] **Step 6: Full unit tests**

Run: `.\gradlew.bat :app:testDebugUnitTest`

Expected: PASS. No live TEFAS.

- [ ] **Step 7: Sideload** per `.cursor/skills/sideload-a23/SKILL.md` (`assembleDebug`, `adb install -r`, package `com.burha.fundhelper`). Manual: followed card shows both %; detail totals + %; no disclaimer; search card unchanged; airplane mode keeps the list.
