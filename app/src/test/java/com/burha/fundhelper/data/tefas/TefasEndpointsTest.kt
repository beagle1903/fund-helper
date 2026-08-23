package com.burha.fundhelper.data.tefas

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TefasEndpointsTest {
    @Test
    fun uses_2026_funds_json_not_bind_history() {
        val urls = listOf(TefasEndpoints.CATALOG, TefasEndpoints.PRICES)
        urls.forEach { url ->
            assertTrue(url.startsWith("https://www.tefas.gov.tr/api/funds/"))
            assertFalse(url.contains("BindHistory"))
            assertFalse(url.contains("fundturkey.com.tr"))
        }
    }
}