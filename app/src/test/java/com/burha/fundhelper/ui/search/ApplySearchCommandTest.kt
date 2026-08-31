package com.burha.fundhelper.ui.search

import com.burha.fundhelper.data.AppEventLog
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
        val clock = FakeClock()
        return FundRepository(
            FakeFollowDao(snapshots),
            snapshots,
            FakeTefasClient(catalog = listOf(aak, aal), prices = listOf(aak.copy(price = 1.0))),
            clock,
            FakeFollowBackup(),
            AppEventLog(clock),
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
