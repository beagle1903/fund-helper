package com.burha.fundhelper.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val tr = Locale.forLanguageTag("tr-TR")

fun periodLabel(period: String?): String = when (period) {
    "1D" -> "1 gün"
    "1W" -> "1 hafta"
    "1M" -> "1 ay"
    "3M" -> "3 ay"
    "6M" -> "6 ay"
    "12M" -> "12 ay"
    "36M" -> "36 ay"
    "60M" -> "60 ay"
    else -> period.orEmpty()
}

fun formatNumber(value: Double): String =
    String.format(tr, "%.4f", value).trimEnd('0').trimEnd(',')

fun formatFetchedAt(millis: Long): String {
    val fmt = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm", tr)
    return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(fmt)
}