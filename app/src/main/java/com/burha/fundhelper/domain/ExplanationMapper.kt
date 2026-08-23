package com.burha.fundhelper.domain

object ExplanationMapper {
    const val MISSING = "TEFAS kaydında bu bilgi yok."

    fun explain(snapshot: FundSnapshot): String {
        val typeSentence = if (snapshot.fundType.isNullOrBlank()) {
            "Fonun resmi türü $MISSING"
        } else {
            "Fonun resmi türü ${snapshot.fundType}."
        }
        val riskSentence = if (snapshot.risk.isNullOrBlank()) {
            "Risk değeri $MISSING"
        } else {
            "Risk değeri TEFAS kaydındaki skorudur: ${snapshot.risk}."
        }
        val feesSentence = if (snapshot.fees.isEmpty()) {
            "Ücret bilgisi $MISSING"
        } else {
            snapshot.fees.joinToString(" ") { line ->
                "${line.label} resmi kayıtta ${line.value}."
            }
        }
        return "$typeSentence $riskSentence $feesSentence"
    }
}