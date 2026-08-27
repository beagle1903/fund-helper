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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class WatchlistRow(
    val code: String,
    val name: String?,
    val price: Double?,
    val headlinePeriod: String?,
    val headlineReturn: Double?,
    val fetchedAt: Long?,
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
    @Volatile private var lastRefreshSuccessAt: Long = -1L

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
            val now = clock.nowMillis()
            snapshotDao.upsertAll(matches.map { SnapshotMapper.toEntity(it.copy(fetchedAt = now)) })
            SearchOutcome.Success(matches.map { it.copy(fetchedAt = now) })
        } catch (e: TefasFetchException) {
            SearchOutcome.Failure(e.message ?: "TEFAS")
        }
    }

    suspend fun refreshFollowed(force: Boolean): Result<Unit> {
        restoreFollowsIfNeeded()
        val codes = followDao.getCodes()
        if (codes.isEmpty()) return Result.success(Unit)
        val now = clock.nowMillis()
        if (!force && lastRefreshSuccessAt >= 0L && now - lastRefreshSuccessAt < FIVE_MINUTES_MS) {
            return Result.success(Unit)
        }
        return try {
            val catalog = tefas.fetchYatCatalog().associateBy { it.code }
            catalogMemory = catalog.values.toList()
            val prices = tefas.fetchLatestYatPrices().associateBy { it.code }
            val merged = codes.mapNotNull { code ->
                val listing = catalog[code]
                val priceRow = prices[code]
                val previous = snapshotDao.get(code)?.let(SnapshotMapper::toDomain)
                val base = listing ?: previous ?: priceRow ?: return@mapNotNull null
                base.copy(
                    price = priceRow?.price ?: base.price,
                    priceDate = priceRow?.priceDate ?: base.priceDate,
                    name = listing?.name?.takeIf { it.isNotBlank() } ?: base.name,
                    fundType = listing?.fundType ?: base.fundType,
                    risk = listing?.risk ?: base.risk,
                    returns = listing?.returns?.takeIf { it.isNotEmpty() } ?: base.returns,
                    fetchedAt = now,
                )
            }
            snapshotDao.upsertAll(merged.map(SnapshotMapper::toEntity))
            lastRefreshSuccessAt = now
            Result.success(Unit)
        } catch (e: TefasFetchException) {
            Result.failure(e)
        }
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