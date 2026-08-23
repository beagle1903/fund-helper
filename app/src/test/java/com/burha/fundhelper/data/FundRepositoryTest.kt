package com.burha.fundhelper.data

import com.burha.fundhelper.domain.FundKind
import com.burha.fundhelper.domain.FundSnapshot
import com.burha.fundhelper.domain.ReturnKeys
import com.burha.fundhelper.fakes.FakeClock
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
    private val aakPriced = aak.copy(price = 35.46, priceDate = "2026-08-21")

    private fun repo(
        tefas: FakeTefasClient = FakeTefasClient(catalog = listOf(aak, aal), prices = listOf(aakPriced)),
        snapshots: FakeSnapshotDao = FakeSnapshotDao(),
        clock: FakeClock = FakeClock(),
    ): Triple<FundRepository, FakeTefasClient, FakeSnapshotDao> {
        val follows = FakeFollowDao(snapshots)
        val repository = FundRepository(follows, snapshots, tefas, clock)
        return Triple(repository, tefas, snapshots)
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
}