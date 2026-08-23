package com.burha.fundhelper.data.local

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object SnapshotJson {
    val json = Json { ignoreUnknownKeys = true }

    fun returnsToJson(returns: Map<String, Double>): String = json.encodeToString(returns)

    fun returnsFromJson(raw: String): Map<String, Double> =
        if (raw.isBlank()) emptyMap() else json.decodeFromString(raw)

    fun feesToJson(fees: List<Pair<String, String>>): String = json.encodeToString(fees)

    fun feesFromJson(raw: String): List<Pair<String, String>> =
        if (raw.isBlank()) emptyList() else json.decodeFromString(raw)
}
