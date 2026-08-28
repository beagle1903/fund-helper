package com.burha.fundhelper.domain

enum class FundKind { YAT }

data class FeeLine(
    val label: String,
    val value: String,
)

data class FundSnapshot(
    val code: String,
    val name: String,
    val kind: FundKind,
    val price: Double?,
    val priceDate: String?,
    val returns: Map<String, Double>,
    val fundType: String?,
    val risk: String?,
    val fees: List<FeeLine>,
    val fetchedAt: Long,
    val payCount: Double? = null,
    val prevPayCount: Double? = null,
    val investorCount: Double? = null,
    val prevInvestorCount: Double? = null,
)

object ReturnKeys {
    const val D1 = "1D"
    const val W1 = "1W"
    const val M1 = "1M"
    const val M3 = "3M"
    const val M6 = "6M"
    const val M12 = "12M"
    const val M36 = "36M"
    const val M60 = "60M"

    val DISPLAY_ORDER = listOf(M1, D1, W1, M3, M6, M12, M36, M60)

    fun headline(returns: Map<String, Double>): Pair<String, Double>? {
        for (key in DISPLAY_ORDER) {
            val value = returns[key]
            if (value != null) return key to value
        }
        return null
    }
}