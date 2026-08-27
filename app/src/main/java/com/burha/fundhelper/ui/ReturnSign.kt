package com.burha.fundhelper.ui

enum class ReturnSign {
    Positive,
    Negative,
    Neutral,
}

fun returnSign(value: Double?): ReturnSign = when {
    value == null -> ReturnSign.Neutral
    value > 0.0 -> ReturnSign.Positive
    value < 0.0 -> ReturnSign.Negative
    else -> ReturnSign.Neutral
}
