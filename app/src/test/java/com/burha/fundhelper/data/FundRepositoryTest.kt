package com.burha.fundhelper.data

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

    private fun repo(
        tefas: FakeTefasClient = FakeTefasClient(catalog = listOf(aak, aal, yatirimFon), prices = listOf(aakPriced)),
        snapshots: FakeSnapshotDao = FakeSnapshotDao(),
        clock: FakeClock = FakeClock(),
        backup: FakeFollowBackup = FakeFollowBackup(),
    ): Triple<FundRepository, FakeTefasClient, FakeSnapshotDao> {
        val follows = FakeFollowDao(snapshots)
        val repository = FundRepository(follows, snapshots, tefas, clock, backup)
        return Triple(repository, tefas, snapshots)
    }

    private fun repoWithBackup(
        backup: FakeFollowBackup = FakeFollowBackup(),
        tefas: FakeTefasClient = FakeTefasClient(catalog = listOf(aak, aal), prices = listOf(aakPriced)),
        snapshots: FakeSnapshotDao = FakeSnapshotDao(),
        clock: FakeClock = FakeClock(),
    ): Pair<FundRepository, FakeFollowBackup> {
        val follows = FakeFollowDao(snapshots)
        return FundRepository(follows, snapshots, tefas, clock, backup) to backup
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
    fun refresh_restores_followed_codes_before_fetch() = runTest {
        val backup = FakeFollowBackup().apply { codes = listOf("AAK") }
        val tefas = FakeTefasClient(catalog = listOf(aak, aal), prices = listOf(aakPriced))
        val snapshots = FakeSnapshotDao()
        val repository = FundRepository(FakeFollowDao(snapshots), snapshots, tefas, FakeClock(), backup)
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
}
