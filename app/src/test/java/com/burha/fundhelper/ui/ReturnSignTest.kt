package com.burha.fundhelper.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ReturnSignTest {

    @Test
    fun null_and_zero_are_neutral() {
        assertEquals(ReturnSign.Neutral, returnSign(null))
        assertEquals(ReturnSign.Neutral, returnSign(0.0))
        assertEquals(ReturnSign.Neutral, returnSign(-0.0))
    }

    @Test
    fun positive_and_negative() {
        assertEquals(ReturnSign.Positive, returnSign(0.0001))
        assertEquals(ReturnSign.Negative, returnSign(-0.0001))
    }
}
