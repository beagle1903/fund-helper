package com.burha.fundhelper.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.putJsonArray

object FollowBackupCodec {
    const val VERSION = 1

    fun encode(codes: List<String>): String {
        val normalized = normalize(codes)
        return buildJsonObject {
            put("version", JsonPrimitive(VERSION))
            putJsonArray("codes") {
                normalized.forEach { add(JsonPrimitive(it)) }
            }
        }.toString()
    }

    fun decode(body: String): List<String> {
        val trimmed = body.trim()
        if (trimmed.isEmpty()) return emptyList()
        return try {
            val root = Json.parseToJsonElement(trimmed) as? JsonObject ?: return emptyList()
            val codes = root["codes"] as? JsonArray ?: return emptyList()
            val parsed = codes.mapNotNull { element ->
                if (element is JsonNull) return@mapNotNull null
                (element as? JsonPrimitive)?.contentOrNull
            }
            normalize(parsed)
        } catch (_: Exception) {
            emptyList()
        }
    }

    internal fun normalize(codes: List<String>): List<String> =
        codes.map { it.trim().uppercase() }.filter { it.isNotEmpty() }.distinct().sorted()
}
