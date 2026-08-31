package com.burha.fundhelper.data

import com.burha.fundhelper.domain.FundKind
import com.burha.fundhelper.domain.FundSnapshot
import com.burha.fundhelper.domain.ReturnKeys
import com.burha.fundhelper.fakes.FakeClock
import com.burha.fundhelper.fakes.FakeFollowBackup
import com.burha.fundhelper.fakes.FakeFollowDao
import com.burha.fundhelper.fakes.FakeSnapshotDao
import com.burha.fundhelper.fakes.FakeTefasClient
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FundRepositoryTest {

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
    private val yatirimFon = aak.copy(code = "XYZ", name = "ÖRNEK YATIRIM FONU")
    private val aakPriced = aak.copy(price = 35.46, priceDate = "2026-08-21")
    private val aakCounted = aakPriced.copy(
        payCount = 1100.0,
        prevPayCount = 1000.0,
        investorCount = 110.0,
        prevInvestorCount = 100.0,
    )

    private fun repo(
        tefas: FakeTefasClient = FakeTefasClient(catalog = listOf(aak, aal, yatirimFon), prices = listOf(aakPriced)),
        snapshots: FakeSnapshotDao = FakeSnapshotDao(),
        clock: FakeClock = FakeClock(),
        backup: FakeFollowBackup = FakeFollowBackup(),
        events: AppEventLog = AppEventLog(clock),
    ): Triple<FundRepository, FakeTefasClient, FakeSnapshotDao> {
        val follows = FakeFollowDao(snapshots)
        val repository = FundRepository(follows, snapshots, tefas, clock, backup, events)
        return Triple(repository, tefas, snapshots)
    }

    private fun repoWithBackup(
        backup: FakeFollowBackup = FakeFollowBackup(),
        tefas: FakeTefasClient = FakeTefasClient(catalog = listOf(aak, aal), prices = listOf(aakPriced)),
        snapshots: FakeSnapshotDao = FakeSnapshotDao(),
        clock: FakeClock = FakeClock(),
        events: AppEventLog = AppEventLog(clock),
    ): Pair<FundRepository, FakeFollowBackup> {
        val follows = FakeFollowDao(snapshots)
        return FundRepository(follows, snapshots, tefas, clock, backup, events) to backup
    }

    @Test
    fun follow_and_unfollow_persist_watchlist_codes() = runTest {
        val (repository, _, _) = repo()
        repository.follow("AAK")
        assertEquals(listOf("AAK"), repository.observeWatchlist().first().map { it.code })
        repository.unfollow("AAK")
        assertTrue(repository.observeWatchlist().first().isEmpty())
    }

    @Test
    fun unfollow_keeps_snapshot() = runTest {
        val (repository, _, snapshots) = repo()
        repository.search("AAK")
        repository.follow("AAK")
        repository.unfollow("AAK")
        assertNotNull(snapshots.get("AAK"))
    }

    @Test
    fun refresh_updates_only_followed_codes() = runTest {
        val (repository, tefas, snapshots) = repo()
        repository.follow("AAK")
        val result = repository.refreshFollowed(force = true)
        assertTrue(result.isSuccess)
        assertEquals(1, tefas.catalogCalls)
        assertEquals(1, tefas.priceCalls)
        assertNotNull(snapshots.get("AAK"))
        assertNull(snapshots.get("AAL"))
        val row = repository.observeWatchlist().first().single()
        assertEquals(35.46, row.price)
        assertEquals(ReturnKeys.M1, row.headlinePeriod)
        assertEquals(1.25, row.headlineReturn)
    }

    @Test
    fun refresh_failure_keeps_follows_and_snapshots() = runTest {
        val tefas = FakeTefasClient(catalog = listOf(aak), prices = listOf(aakPriced))
        val (repository, _, _) = repo(tefas)
        repository.follow("AAK")
        repository.refreshFollowed(force = true)
        tefas.failCatalog = true
        val failed = repository.refreshFollowed(force = true)
        assertTrue(failed.isFailure)
        val rows = repository.observeWatchlist().first()
        assertEquals(listOf("AAK"), rows.map { it.code })
        assertEquals(35.46, rows.single().price)
    }

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
    fun search_does_not_require_follow_and_upserts_matches_only() = runTest {
        val (repository, tefas, snapshots) = repo()
        val outcome = repository.search("ata")
        assertTrue(outcome is SearchOutcome.Success)
        assertEquals(2, (outcome as SearchOutcome.Success).matches.size)
        assertEquals(1, tefas.catalogCalls)
        assertNotNull(snapshots.get("AAK"))
        assertNotNull(snapshots.get("AAL"))
        assertTrue(repository.observeWatchlist().first().isEmpty())
        repository.search("AAK")
        assertEquals(1, tefas.catalogCalls)
    }

    @Test
    fun empty_query_does_not_hit_client() = runTest {
        val (repository, tefas, _) = repo()
        val outcome = repository.search("  ")
        assertEquals(SearchOutcome.EmptyQuery, outcome)
        assertEquals(0, tefas.catalogCalls)
    }

    @Test
    fun search_retry_refetches_catalog() = runTest {
        val (repository, tefas, _) = repo()
        repository.search("AAK")
        repository.search("AAK", refetchCatalog = true)
        assertEquals(2, tefas.catalogCalls)
    }

    @Test
    fun auto_refresh_skips_within_five_minutes() = runTest {
        val clock = FakeClock(now = 0L)
        val (repository, tefas, _) = repo(clock = clock)
        repository.follow("AAK")
        repository.refreshFollowed(force = true)
        clock.now = 4 * 60 * 1000L
        repository.refreshFollowed(force = false)
        assertEquals(1, tefas.catalogCalls)
        clock.now = 5 * 60 * 1000L
        repository.refreshFollowed(force = false)
        assertEquals(2, tefas.catalogCalls)
    }

    @Test
    fun concurrent_unforced_refreshes_share_one_tefas_round_trip() = runTest {
        val tefas = FakeTefasClient(
            catalog = listOf(aak),
            prices = listOf(aakPriced),
            catalogHoldMs = 1_000L,
        )
        val (repository, _, _) = repo(tefas)
        repository.follow("AAK")
        val first = async { repository.refreshFollowed(force = false) }
        val second = async { repository.refreshFollowed(force = false) }
        assertTrue(first.await().isSuccess)
        assertTrue(second.await().isSuccess)
        assertEquals(1, tefas.catalogCalls)
        assertEquals(1, tefas.priceCalls)
    }

    @Test
    fun concurrent_unforced_refreshes_share_failure_result() = runTest {
        val tefas = FakeTefasClient(
            catalog = listOf(aak),
            prices = listOf(aakPriced),
            catalogHoldMs = 1_000L,
        )
        tefas.failCatalog = true
        val (repository, _, _) = repo(tefas)
        repository.follow("AAK")
        val first = async { repository.refreshFollowed(force = false) }
        val second = async { repository.refreshFollowed(force = false) }
        assertTrue(first.await().isFailure)
        assertTrue(second.await().isFailure)
        assertEquals(1, tefas.catalogCalls)
    }

    @Test
    fun follow_and_unfollow_mirror_codes_to_device_backup() = runTest {
        val (repository, backup) = repoWithBackup()
        repository.follow("AAK")
        repository.follow("AAL")
        assertEquals(listOf("AAK", "AAL"), backup.codes)
        repository.unfollow("AAK")
        assertEquals(listOf("AAL"), backup.codes)
        repository.unfollow("AAL")
        assertTrue(backup.codes.isEmpty())
    }

    @Test
    fun restore_fills_empty_room_from_device_backup() = runTest {
        val backup = FakeFollowBackup().apply { codes = listOf("AAL", "AAK") }
        val (repository, _) = repoWithBackup(backup)
        repository.restoreFollowsIfNeeded()
        assertEquals(listOf("AAK", "AAL"), repository.observeWatchlist().first().map { it.code })
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
    fun restore_does_not_readd_unfollowed_codes_when_room_has_follows() = runTest {
        val backup = FakeFollowBackup()
        val (repository, _) = repoWithBackup(backup)
        repository.follow("AAK")
        backup.codes = listOf("AAK", "ZZZ")
        repository.restoreFollowsIfNeeded()
        assertEquals(listOf("AAK"), repository.observeWatchlist().first().map { it.code })
        assertEquals(listOf("AAK", "ZZZ"), backup.codes)
    }

    @Test
    fun backup_write_failure_does_not_drop_room_follow() = runTest {
        val backup = FakeFollowBackup().apply { writeError = true }
        val (repository, _) = repoWithBackup(backup)
        repository.follow("AAK")
        assertEquals(listOf("AAK"), repository.observeWatchlist().first().map { it.code })
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
    fun restore_read_failure_leaves_watchlist_empty() = runTest {
        val backup = FakeFollowBackup().apply {
            codes = listOf("AAK")
            readError = true
        }
        val (repository, _) = repoWithBackup(backup)
        repository.restoreFollowsIfNeeded()
        assertTrue(repository.observeWatchlist().first().isEmpty())
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
    fun refresh_restores_followed_codes_before_fetch() = runTest {
        val backup = FakeFollowBackup().apply { codes = listOf("AAK") }
        val tefas = FakeTefasClient(catalog = listOf(aak, aal), prices = listOf(aakPriced))
        val snapshots = FakeSnapshotDao()
        val clock = FakeClock()
        val repository = FundRepository(
            FakeFollowDao(snapshots), snapshots, tefas, clock, backup, AppEventLog(clock),
        )
        val result = repository.refreshFollowed(force = true)
        assertTrue(result.isSuccess)
        assertEquals(1, tefas.catalogCalls)
        assertEquals(listOf("AAK"), repository.observeWatchlist().first().map { it.code })
        assertEquals(35.46, repository.observeWatchlist().first().single().price)
    }

    @Test
    fun observe_fund_includes_mapper_paragraph() = runTest {
        val (repository, _, _) = repo()
        repository.search("AAK")
        repository.follow("AAK")
        val detail = repository.observeFund("AAK").first()
        assertNotNull(detail)
        assertTrue(detail!!.explanation.contains("Değişken Şemsiye Fonu"))
        assertTrue(detail.isFollowed)
        assertTrue(!detail.explanation.contains("Yatırım tavsiyesi değildir."))
    }

    @Test
    fun search_folds_turkish_letters_both_directions() = runTest {
        val (repository, _, _) = repo()
        val ascii = repository.search("yatirim")
        assertTrue(ascii is SearchOutcome.Success)
        assertEquals(listOf("XYZ"), (ascii as SearchOutcome.Success).matches.map { it.code })
        assertEquals("ÖRNEK YATIRIM FONU", (ascii as SearchOutcome.Success).matches.single().name)
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

    @Test
    fun clear_follows_empties_room_and_backup_keeps_snapshots() = runTest {
        val backup = FakeFollowBackup()
        val snapshots = FakeSnapshotDao()
        val tefas = FakeTefasClient(catalog = listOf(aak, aal), prices = listOf(aakPriced))
        val clock = FakeClock()
        val repository = FundRepository(
            FakeFollowDao(snapshots), snapshots, tefas, clock, backup, AppEventLog(clock),
        )
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

    @Test
    fun follow_all_keeps_existing_price_on_already_followed_fund() = runTest {
        val (repository, _, snapshots) = repo()
        repository.follow("AAK")
        repository.refreshFollowed(force = true)
        assertEquals(35.46, snapshots.get("AAK")?.price)
        repository.followAll(listOf("AAK", "AAL"))
        assertEquals(35.46, snapshots.get("AAK")?.price)
        assertEquals("2026-08-21", snapshots.get("AAK")?.priceDate)
        assertEquals(listOf("AAK", "AAL"), repository.observeWatchlist().first().map { it.code })
        assertEquals(35.46, repository.observeWatchlist().first().first { it.code == "AAK" }.price)
    }

    @Test
    fun follow_all_appends_resolved_codes_and_writes_backup_once() = runTest {
        val backup = FakeFollowBackup()
        val snapshots = FakeSnapshotDao()
        val tefas = FakeTefasClient(catalog = listOf(aak, aal), prices = listOf(aakPriced))
        val clock = FakeClock()
        val repository = FundRepository(
            FakeFollowDao(snapshots), snapshots, tefas, clock, backup, AppEventLog(clock),
        )
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
    fun refresh_price_row_overwrites_counts_including_nulls() = runTest {
        val snapshots = FakeSnapshotDao()
        val tefas = FakeTefasClient(catalog = listOf(aak), prices = listOf(aakCounted))
        val clock = FakeClock()
        val repository = FundRepository(
            FakeFollowDao(snapshots), snapshots, tefas, clock, FakeFollowBackup(), AppEventLog(clock),
        )
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
        val clock = FakeClock()
        val repository = FundRepository(
            FakeFollowDao(snapshots), snapshots, tefas, clock, FakeFollowBackup(), AppEventLog(clock),
        )
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
        val clock = FakeClock()
        val repository = FundRepository(
            FakeFollowDao(snapshots), snapshots, tefas, clock, FakeFollowBackup(), AppEventLog(clock),
        )
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
        val clock = FakeClock()
        val repository = FundRepository(
            FakeFollowDao(snapshots), snapshots, tefas, clock, FakeFollowBackup(), AppEventLog(clock),
        )
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
    fun search_batches_previous_snapshots_via_getByCodes() = runTest {
        val snapshots = FakeSnapshotDao()
        val (repository, _, _) = repo(snapshots = snapshots)
        val getsBefore = snapshots.getCalls
        val outcome = repository.search("ata")
        assertTrue(outcome is SearchOutcome.Success)
        assertEquals(2, (outcome as SearchOutcome.Success).matches.size)
        assertEquals(1, snapshots.getByCodesCalls)
        assertEquals(getsBefore, snapshots.getCalls)
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
    fun follow_all_keeps_counts_on_already_followed_fund() = runTest {
        val (repository, _, snapshots) = repo(tefas = FakeTefasClient(catalog = listOf(aak, aal), prices = listOf(aakCounted)))
        repository.follow("AAK")
        repository.refreshFollowed(force = true)
        repository.followAll(listOf("AAK", "AAL"))
        assertEquals(1100.0, snapshots.get("AAK")?.payCount)
        assertEquals(1000.0, snapshots.get("AAK")?.prevPayCount)
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
}
