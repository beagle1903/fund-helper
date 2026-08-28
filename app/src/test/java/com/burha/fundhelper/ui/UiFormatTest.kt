package com.burha.fundhelper.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class UiFormatTest {

    @Test
    fun signed_percent_plus_minus_zero_and_dash() {
        assertEquals("+10%", formatSignedPercent(10.0, "—"))
        assertEquals("${formatNumber(-50.0)}%", formatSignedPercent(-50.0, "—"))
        assertEquals("0%", formatSignedPercent(0.0, "—"))
        assertEquals("—", formatSignedPercent(null, "—"))
    }

    @Test
    fun count_is_whole_number_with_tr_grouping() {
        assertEquals("1.100", formatCount(1100.0))
        assertEquals("5", formatCount(5.0))
    }
}
