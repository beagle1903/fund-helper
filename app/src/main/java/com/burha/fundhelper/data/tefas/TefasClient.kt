package com.burha.fundhelper.data.tefas

import com.burha.fundhelper.domain.FundSnapshot

object TefasEndpoints {
    const val CATALOG = "https://www.tefas.gov.tr/api/funds/fonGetiriBazliBilgiGetir"
    const val PRICES = "https://www.tefas.gov.tr/api/funds/fonGnlBlgSiraliGetir"
    const val ORIGIN = "https://www.tefas.gov.tr"
    const val REFERER = "https://www.tefas.gov.tr/tr/fon-detayli-analiz"
    const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
}

class TefasFetchException(message: String, cause: Throwable? = null) : Exception(message, cause)

interface TefasClient {
    suspend fun fetchYatCatalog(): List<FundSnapshot>
    suspend fun fetchLatestYatPrices(): List<FundSnapshot>
}