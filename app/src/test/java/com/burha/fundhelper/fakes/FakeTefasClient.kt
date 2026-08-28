package com.burha.fundhelper.fakes

import com.burha.fundhelper.data.tefas.TefasClient
import com.burha.fundhelper.data.tefas.TefasFetchException
import com.burha.fundhelper.domain.FundSnapshot
import kotlinx.coroutines.delay

class FakeTefasClient(
    var catalog: List<FundSnapshot> = emptyList(),
    var prices: List<FundSnapshot> = emptyList(),
    var failCatalog: Boolean = false,
    var failPrices: Boolean = false,
    var catalogHoldMs: Long = 0L,
) : TefasClient {
    var catalogCalls: Int = 0
    var priceCalls: Int = 0

    override suspend fun fetchYatCatalog(): List<FundSnapshot> {
        catalogCalls += 1
        if (catalogHoldMs > 0L) delay(catalogHoldMs)
        if (failCatalog) throw TefasFetchException("catalog failed")
        return catalog
    }

    override suspend fun fetchLatestYatPrices(): List<FundSnapshot> {
        priceCalls += 1
        if (failPrices) throw TefasFetchException("prices failed")
        return prices
    }
}