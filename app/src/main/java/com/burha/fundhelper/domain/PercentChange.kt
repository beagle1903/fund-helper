package com.burha.fundhelper.domain

fun percentChange(current: Double?, previous: Double?): Double? {
    if (current == null || previous == null) return null
    if (previous == 0.0) return null
    return (current - previous) / previous * 100.0
}
