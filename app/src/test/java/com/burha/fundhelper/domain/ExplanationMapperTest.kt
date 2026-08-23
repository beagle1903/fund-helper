package com.burha.fundhelper.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExplanationMapperTest {

    private val present = FundSnapshot(
        code = "AAK",
        name = "ÖRNEK FON",
        kind = FundKind.YAT,
        price = 12.34,
        priceDate = "2026-08-21",
        returns = mapOf(ReturnKeys.M1 to 1.2),
        fundType = "Hisse Senedi Fonu",
        risk = "5",
        fees = listOf(FeeLine(label = "Yönetim ücreti", value = "%2,00")),
        fetchedAt = 1L,
    )

    @Test
    fun maps_present_type_risk_and_fees() {
        val text = ExplanationMapper.explain(present)
        assertTrue(text.contains("Hisse Senedi Fonu"))
        assertTrue(text.contains("5"))
        assertTrue(text.contains("Yönetim ücreti"))
        assertTrue(text.contains("%2,00"))
        assertFalse(text.contains("Yatırım tavsiyesi değildir."))
    }

    @Test
    fun missing_fields_use_absence_sentence() {
        val text = ExplanationMapper.explain(
            present.copy(fundType = null, risk = null, fees = emptyList()),
        )
        assertTrue(text.contains("TEFAS kaydında bu bilgi yok."))
        assertFalse(text.contains("Hisse Senedi Fonu"))
    }

    @Test
    fun output_has_no_buy_sell_language() {
        val text = ExplanationMapper.explain(present)
        val banned = listOf("satın", "satmayın", "hedef fiyat", "size uygun", "Bu fonu alın")
        banned.forEach { token ->
            assertFalse("banned token: $token", text.contains(token, ignoreCase = true))
        }
    }
}