package com.burha.fundhelper.data.tefas

import com.burha.fundhelper.domain.ReturnKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TefasJsonMapperTest {

    private fun fixture(name: String): String {
        val stream = requireNotNull(javaClass.classLoader?.getResourceAsStream("fixtures/$name"))
        return stream.bufferedReader().use { it.readText() }
    }

    @Test
    fun parses_catalog_returns_type_and_risk() {
        val funds = TefasJsonMapper.parseCatalog(fixture("yat-catalog.json"))
        assertEquals(2, funds.size)
        val aak = funds.first { it.code == "AAK" }
        assertEquals("ATA PORTFÖY ÇOKLU VARLIK DEĞİŞKEN FON", aak.name)
        assertEquals("Değişken Şemsiye Fonu", aak.fundType)
        assertEquals("4", aak.risk)
        assertEquals(1.25, aak.returns[ReturnKeys.M1])
        assertEquals(3.5, aak.returns[ReturnKeys.M3])
        assertEquals(12.0, aak.returns[ReturnKeys.M12])
        assertEquals(40.0, aak.returns[ReturnKeys.M36])
        assertEquals(80.0, aak.returns[ReturnKeys.M60])
        assertTrue(aak.fees.isEmpty())
        assertNull(aak.price)
    }

    @Test
    fun parses_latest_nonzero_price_per_code() {
        val funds = TefasJsonMapper.parseLatestPrices(fixture("yat-prices.json"))
        val aak = funds.single { it.code == "AAK" }
        assertEquals(35.46418, aak.price)
        assertEquals("2026-08-21", aak.priceDate)
        assertTrue(funds.none { it.code == "AAL" })
    }

    @Test
    fun parses_pay_and_investor_from_latest_and_previous_priced_day() {
        val funds = TefasJsonMapper.parseLatestPrices(fixture("yat-prices.json"))
        val aak = funds.single { it.code == "AAK" }
        assertEquals(35.46418, aak.price)
        assertEquals("2026-08-21", aak.priceDate)
        assertEquals(1100.0, aak.payCount)
        assertEquals(1000.0, aak.prevPayCount)
        assertEquals(110.0, aak.investorCount)
        assertEquals(100.0, aak.prevInvestorCount)
        val bbb = funds.single { it.code == "BBB" }
        assertEquals(50.0, bbb.payCount)
        assertNull(bbb.prevPayCount)
        assertEquals(5.0, bbb.investorCount)
        assertNull(bbb.prevInvestorCount)
        assertTrue(funds.none { it.code == "AAL" })
    }

    @Test
    fun missing_count_fields_are_null_not_a_fetch_error() {
        val funds = TefasJsonMapper.parseLatestPrices(
            """{"errorCode":null,"resultList":[
              {"fonKodu":"ZZZ","fonUnvan":"X","tarih":"2026-08-20","fiyat":1.0},
              {"fonKodu":"ZZZ","fonUnvan":"X","tarih":"2026-08-21","fiyat":2.0}
            ]}""",
        )
        val zzz = funds.single { it.code == "ZZZ" }
        assertNull(zzz.payCount)
        assertNull(zzz.prevPayCount)
        assertNull(zzz.investorCount)
        assertNull(zzz.prevInvestorCount)
    }

    @Test(expected = TefasFetchException::class)
    fun rejects_html_challenge_as_failure() {
        TefasJsonMapper.parseCatalog(fixture("challenge.html"))
    }

    @Test(expected = TefasFetchException::class)
    fun rejects_error_code_payload() {
        TefasJsonMapper.parseCatalog("""{"errorCode":"ERR-224","resultList":[]}""")
    }
}