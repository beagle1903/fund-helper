package com.burha.fundhelper.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchlistSortTest {

    private data class Row(val code: String, val headlineReturn: Double?)

    private fun sort(rows: List<Row>): List<Row> =
        sortByHeadlineReturn(rows, { it.headlineReturn }, { it.code })

    @Test
    fun empty_stays_empty() {
        assertTrue(sort(emptyList()).isEmpty())
    }

    @Test
    fun most_negative_first_most_positive_last() {
        val sorted = sort(
            listOf(
                Row("POS", 3.0),
                Row("NEG", -5.0),
                Row("ZERO", 0.0),
            ),
        )
        assertEquals(listOf("NEG", "ZERO", "POS"), sorted.map { it.code })
    }

    @Test
    fun missing_return_last() {
        val sorted = sort(
            listOf(
                Row("MISS", null),
                Row("NEG", -1.0),
                Row("POS", 2.0),
            ),
        )
        assertEquals(listOf("NEG", "POS", "MISS"), sorted.map { it.code })
    }

    @Test
    fun equal_returns_break_ties_by_code() {
        val sorted = sort(
            listOf(
                Row("YAS", -1.0),
                Row("AFA", -1.0),
            ),
        )
        assertEquals(listOf("AFA", "YAS"), sorted.map { it.code })
    }
}
