package com.burha.fundhelper.data

import com.burha.fundhelper.data.local.FollowDao
import com.burha.fundhelper.data.local.FollowEntity
import com.burha.fundhelper.data.local.SnapshotDao
import com.burha.fundhelper.data.local.SnapshotMapper
import com.burha.fundhelper.data.tefas.TefasClient
import com.burha.fundhelper.data.tefas.TefasFetchException
import com.burha.fundhelper.domain.Clock
import com.burha.fundhelper.domain.ExplanationMapper
import com.burha.fundhelper.domain.FundSnapshot
import com.burha.fundhelper.domain.ReturnKeys
import com.burha.fundhelper.domain.foldForSearch
import com.burha.fundhelper.domain.percentChange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

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

data class FundDetail(
    val snapshot: FundSnapshot,
    val explanation: String,
    val isFollowed: Boolean,
)

sealed class SearchOutcome {
    data object EmptyQuery : SearchOutcome()
    data class Success(val matches: List<FundSnapshot>) : SearchOutcome()
    data class Failure(val message: String) : SearchOutcome()
}

@Singleton
class FundRepository @Inject constructor(
    private val followDao: FollowDao,
    private val snapshotDao: SnapshotDao,
    private val tefas: TefasClient,
    private val clock: Clock,
    private val followBackup: FollowBackup,
) {
    private var catalogMemory: List<FundSnapshot>? = null
    private val refreshMutex = Mutex()
    @Volatile private var lastRefreshResult: Pair<Result<Unit>, Long>? = null

    fun observeWatchlist(): Flow<List<WatchlistRow>> = followDao.observeFollowed().map { rows ->
        rows.map { followed ->
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
        }
    }

    fun observeFund(code: String): Flow<FundDetail?> = combine(
        snapshotDao.observe(code),
        followDao.observeFollowed(),
    ) { entity, followed ->
        val snapshot = entity?.let(SnapshotMapper::toDomain) ?: return@combine null
        FundDetail(
            snapshot = snapshot,
            explanation = ExplanationMapper.explain(snapshot),
            isFollowed = followed.any { it.follow.code == code },
        )
    }

    suspend fun follow(code: String) {
        followDao.insert(FollowEntity(code))
        persistBackup()
    }

    suspend fun unfollow(code: String) {
        followDao.delete(code)
        persistBackup()
    }

    suspend fun clearFollows() {
        followDao.deleteAll()
        persistBackup()
    }

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
            val merged = resolved.map { listing ->
                val previous = snapshotDao.get(listing.code)?.let(SnapshotMapper::toDomain)
                listing.copy(
                    price = listing.price ?: previous?.price,
                    priceDate = listing.priceDate ?: previous?.priceDate,
                    name = listing.name.takeIf { it.isNotBlank() } ?: previous?.name ?: listing.name,
                    fundType = listing.fundType ?: previous?.fundType,
                    risk = listing.risk ?: previous?.risk,
                    returns = listing.returns.takeIf { it.isNotEmpty() } ?: previous?.returns ?: listing.returns,
                    fetchedAt = now,
                ).keepingPriceWindow(previous)
            }
            snapshotDao.upsertAll(merged.map(SnapshotMapper::toEntity))
            persistBackup()
        } catch (_: TefasFetchException) {
            // Unknown-or-failed codes are skipped; do not wipe; do not throw.
        }
    }

    suspend fun restoreFollowsIfNeeded() {
        if (followDao.getCodes().isNotEmpty()) return
        val codes = try {
            FollowBackupCodec.normalize(followBackup.readCodes())
        } catch (_: Exception) {
            return
        }
        if (codes.isEmpty()) return
        codes.forEach { code -> followDao.insert(FollowEntity(code)) }
        persistBackup()
    }

    suspend fun search(query: String, refetchCatalog: Boolean = false): SearchOutcome {
        val needle = query.trim()
        if (needle.isEmpty()) return SearchOutcome.EmptyQuery
        return try {
            val catalog = loadCatalog(refetchCatalog)
            val matches = catalog.filter { fund -> matchesQuery(fund, needle) }
            if (matches.isEmpty()) return SearchOutcome.Success(emptyList())
            val now = clock.nowMillis()
            val previousByCode = snapshotDao.getByCodes(matches.map { it.code })
                .associateBy { it.code }
            val merged = matches.map { match ->
                val previous = previousByCode[match.code]?.let(SnapshotMapper::toDomain)
                match.copy(fetchedAt = now).keepingPriceWindow(previous)
            }
            snapshotDao.upsertAll(merged.map(SnapshotMapper::toEntity))
            SearchOutcome.Success(merged)
        } catch (e: TefasFetchException) {
            SearchOutcome.Failure(e.message ?: "TEFAS")
        }
    }

    suspend fun refreshFollowed(force: Boolean): Result<Unit> = refreshMutex.withLock {
        restoreFollowsIfNeeded()
        val codes = followDao.getCodes()
        if (codes.isEmpty()) return@withLock Result.success(Unit)
        val now = clock.nowMillis()
        val cached = lastRefreshResult
        if (!force && cached != null && now - cached.second < FIVE_MINUTES_MS) {
            return@withLock cached.first
        }
        val result = try {
            val catalog = tefas.fetchYatCatalog().associateBy { it.code }
            catalogMemory = catalog.values.toList()
            val prices = tefas.fetchLatestYatPrices().associateBy { it.code }
            val merged = codes.mapNotNull { code ->
                val listing = catalog[code]
                val priceRow = prices[code]
                val previous = snapshotDao.get(code)?.let(SnapshotMapper::toDomain)
                val base = listing ?: previous ?: priceRow ?: return@mapNotNull null
                val catalogMerged = base.copy(
                    name = listing?.name?.takeIf { it.isNotBlank() } ?: base.name,
                    fundType = listing?.fundType ?: base.fundType,
                    risk = listing?.risk ?: base.risk,
                    returns = listing?.returns?.takeIf { it.isNotEmpty() } ?: base.returns,
                )
                val withWindow = if (priceRow != null) {
                    catalogMerged.replacingPriceWindow(priceRow)
                } else {
                    catalogMerged.keepingPriceWindow(previous)
                }
                withWindow.copy(fetchedAt = now)
            }
            snapshotDao.upsertAll(merged.map(SnapshotMapper::toEntity))
            Result.success(Unit)
        } catch (e: TefasFetchException) {
            Result.failure(e)
        }
        lastRefreshResult = result to now
        result
    }

    private suspend fun loadCatalog(refetch: Boolean): List<FundSnapshot> {
        val cached = catalogMemory
        if (!refetch && cached != null) return cached
        val fresh = tefas.fetchYatCatalog()
        catalogMemory = fresh
        return fresh
    }

    private suspend fun persistBackup() {
        try {
            followBackup.writeCodes(followDao.getCodes())
        } catch (_: Exception) {
            // Room is the live list; a backup miss must not roll back follows.
        }
    }

    private fun matchesQuery(fund: FundSnapshot, raw: String): Boolean {
        val q = foldForSearch(raw)
        val code = foldForSearch(fund.code)
        val name = foldForSearch(fund.name)
        return code.startsWith(q) || name.contains(q)
    }

    private companion object {
        const val FIVE_MINUTES_MS = 5 * 60 * 1000L
    }
}

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