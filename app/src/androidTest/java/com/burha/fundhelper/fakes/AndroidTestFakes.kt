package com.burha.fundhelper.fakes

import com.burha.fundhelper.data.FollowBackup
import com.burha.fundhelper.data.tefas.TefasClient
import com.burha.fundhelper.domain.FundKind
import com.burha.fundhelper.domain.FundSnapshot
import com.burha.fundhelper.domain.ReturnKeys
import javax.inject.Inject
import javax.inject.Singleton

/** In-memory TEFAS stand-in. Instrumented tests never hit tefas.gov.tr. */
@Singleton
class InstrumentedFakeTefasClient @Inject constructor() : TefasClient {
    @Volatile var catalog: List<FundSnapshot> = DEFAULT_CATALOG
    @Volatile var prices: List<FundSnapshot> = DEFAULT_CATALOG

    override suspend fun fetchYatCatalog(): List<FundSnapshot> = catalog
    override suspend fun fetchLatestYatPrices(): List<FundSnapshot> = prices

    companion object {
        val DEFAULT_CATALOG = listOf(
            fund("AAA", "Test Altın Fonu", 1.5),
            fund("BBB", "Test Hisse Fonu", -2.25),
        )

        fun fund(code: String, name: String, monthReturn: Double) = FundSnapshot(
            code = code,
            name = name,
            kind = FundKind.YAT,
            price = 1.234567,
            priceDate = "2026-11-16",
            returns = mapOf(ReturnKeys.M1 to monthReturn),
            fundType = null,
            risk = null,
            fees = emptyList(),
            fetchedAt = 0L,
        )
    }
}

/** Keeps follow codes in memory instead of the shared Downloads file. */
@Singleton
class InstrumentedFakeFollowBackup @Inject constructor() : FollowBackup {
    @Volatile var codes: List<String> = emptyList()

    override suspend fun writeCodes(codes: List<String>) {
        this.codes = codes
    }

    override suspend fun readCodes(): List<String> = codes
}
