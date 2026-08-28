package com.burha.fundhelper.ui

import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.round

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

fun formatSignedPercent(value: Double?, dash: String): String {
    if (value == null) return dash
    val formatted = formatNumber(value)
    return when {
        value > 0 -> "+$formatted%"
        else -> "$formatted%"
    }
}

fun formatCount(value: Double): String =
    NumberFormat.getIntegerInstance(tr).format(round(value).toLong())

fun formatFetchedAt(millis: Long): String {
    val fmt = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm", tr)
    return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(fmt)
}