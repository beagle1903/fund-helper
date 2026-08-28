package com.burha.fundhelper.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PercentChangeTest {

    @Test
    fun normal_ratio() {
        assertEquals(10.0, percentChange(1100.0, 1000.0))
        assertEquals(-50.0, percentChange(50.0, 100.0))
        assertEquals(0.0, percentChange(100.0, 100.0))
    }

    @Test
    fun missing_or_zero_previous_is_null() {
        assertNull(percentChange(10.0, null))
        assertNull(percentChange(null, 10.0))
        assertNull(percentChange(null, null))
        assertNull(percentChange(10.0, 0.0))
        assertNull(percentChange(0.0, 0.0))
    }

    @Test
    fun zero_current_with_nonzero_previous_is_minus_one_hundred() {
        assertEquals(-100.0, percentChange(0.0, 80.0))
    }
}
