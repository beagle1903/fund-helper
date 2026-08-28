package com.burha.fundhelper.data.tefas

import com.burha.fundhelper.domain.FundKind
import com.burha.fundhelper.domain.FundSnapshot
import com.burha.fundhelper.domain.ReturnKeys
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object TefasJsonMapper {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parseCatalog(body: String): List<FundSnapshot> {
        val rows = resultList(body)
        return rows.mapNotNull { row ->
            val code = row.string("fonKodu")?.trim().orEmpty()
            if (code.isEmpty()) return@mapNotNull null
            val returns = buildMap {
                putIfPresent(ReturnKeys.M1, row.double("getiri1a"))
                putIfPresent(ReturnKeys.M3, row.double("getiri3a"))
                putIfPresent(ReturnKeys.M6, row.double("getiri6a"))
                putIfPresent(ReturnKeys.M12, row.double("getiri1y"))
                putIfPresent(ReturnKeys.M36, row.double("getiri3y"))
                putIfPresent(ReturnKeys.M60, row.double("getiri5y"))
            }
            FundSnapshot(
                code = code,
                name = row.string("fonUnvan").orEmpty(),
                kind = FundKind.YAT,
                price = null,
                priceDate = null,
                returns = returns,
                fundType = row.string("fonTurAciklama"),
                risk = row.intOrString("riskDegeri"),
                fees = emptyList(),
                fetchedAt = 0L,
            )
        }
    }

    fun parseLatestPrices(body: String): List<FundSnapshot> {
        data class Day(
            val name: String,
            val price: Double,
            val payCount: Double?,
            val investorCount: Double?,
        )
        val byCode = linkedMapOf<String, LinkedHashMap<String, Day>>()
        for (row in resultList(body)) {
            val code = row.string("fonKodu")?.trim().orEmpty()
            val price = row.double("fiyat") ?: continue
            if (code.isEmpty() || price <= 0.0) continue
            val date = row.string("tarih")?.take(10) ?: continue
            val days = byCode.getOrPut(code) { linkedMapOf() }
            days[date] = Day(
                name = row.string("fonUnvan").orEmpty(),
                price = price,
                payCount = row.double("tedPaySayisi"),
                investorCount = row.double("kisiSayisi"),
            )
        }
        return byCode.map { (code, days) ->
            val ordered = days.keys.sortedDescending()
            val latestDate = ordered.first()
            val latest = days.getValue(latestDate)
            val previous = ordered.getOrNull(1)?.let { days.getValue(it) }
            FundSnapshot(
                code = code,
                name = latest.name,
                kind = FundKind.YAT,
                price = latest.price,
                priceDate = latestDate,
                returns = emptyMap(),
                fundType = null,
                risk = null,
                fees = emptyList(),
                fetchedAt = 0L,
                payCount = latest.payCount,
                prevPayCount = previous?.payCount,
                investorCount = latest.investorCount,
                prevInvestorCount = previous?.investorCount,
            )
        }
    }

    private fun resultList(body: String): List<JsonObject> {
        val trimmed = body.trim()
        if (!trimmed.startsWith("{")) {
            throw TefasFetchException("TEFAS returned a non-JSON body")
        }
        val root = try {
            json.parseToJsonElement(trimmed).jsonObject
        } catch (e: Exception) {
            throw TefasFetchException("TEFAS returned malformed JSON", e)
        }
        val error = root["errorCode"]
        if (error != null && error !is JsonNull && error.jsonPrimitive.contentOrNull.isNullOrBlank().not()) {
            throw TefasFetchException("TEFAS errorCode=${error.jsonPrimitive.content}")
        }
        val list = root["resultList"] as? JsonArray ?: throw TefasFetchException("TEFAS JSON missing resultList")
        return list.map { it.jsonObject }
    }

    private fun MutableMap<String, Double>.putIfPresent(key: String, value: Double?) {
        if (value != null) put(key, value)
    }

    private fun JsonObject.string(key: String): String? =
        this[key]?.asPrimitive()?.contentOrNull?.takeIf { it.isNotBlank() }

    private fun JsonObject.double(key: String): Double? =
        this[key]?.asPrimitive()?.doubleOrNull

    private fun JsonObject.intOrString(key: String): String? {
        val primitive = this[key]?.asPrimitive() ?: return null
        return primitive.contentOrNull?.takeIf { it.isNotBlank() && it != "null" }
    }

    private fun JsonElement.asPrimitive(): JsonPrimitive? = this as? JsonPrimitive
}