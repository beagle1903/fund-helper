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

    @Test(expected = TefasFetchException::class)
    fun rejects_html_challenge_as_failure() {
        TefasJsonMapper.parseCatalog(fixture("challenge.html"))
    }

    @Test(expected = TefasFetchException::class)
    fun rejects_error_code_payload() {
        TefasJsonMapper.parseCatalog("""{"errorCode":"ERR-224","resultList":[]}""")
    }
}