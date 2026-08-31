package com.burha.fundhelper.data.tefas

import org.junit.Assert.assertEquals
import org.junit.Test

class TefasHttpErrorMessageTest {

    @Test
    fun html_body_is_marked() {
        assertEquals("HTTP 403 (HTML)", tefasHttpErrorMessage(403, "<html><body>denied</body></html>"))
        assertEquals("HTTP 403 (HTML)", tefasHttpErrorMessage(403, "  <HTML>"))
        assertEquals("HTTP 500 (HTML)", tefasHttpErrorMessage(500, "challenge <html lang=\"tr\">"))
    }

    @Test
    fun json_or_empty_stays_plain() {
        assertEquals("HTTP 403", tefasHttpErrorMessage(403, """{"errorCode":"ERR"}"""))
        assertEquals("HTTP 502", tefasHttpErrorMessage(502, ""))
        assertEquals("HTTP 404", tefasHttpErrorMessage(404, "not found"))
    }
}
