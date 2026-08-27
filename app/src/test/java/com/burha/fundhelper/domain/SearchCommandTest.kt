package com.burha.fundhelper.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchCommandTest {

    @Test
    fun reset_any_case_without_comma() {
        assertEquals(SearchCommand.Reset, parseSearchCommand("RESET"))
        assertEquals(SearchCommand.Reset, parseSearchCommand("reset"))
        assertEquals(SearchCommand.Reset, parseSearchCommand(" Reset "))
    }

    @Test
    fun comma_list_is_bulk_follow() {
        val command = parseSearchCommand("AAK, AAL")
        assertEquals(SearchCommand.BulkFollow(listOf("AAK", "AAL")), command)
    }

    @Test
    fun reset_inside_list_is_dropped_not_a_wipe() {
        assertEquals(
            SearchCommand.BulkFollow(listOf("AAK")),
            parseSearchCommand("RESET, AAK"),
        )
        assertEquals(
            SearchCommand.BulkFollow(emptyList()),
            parseSearchCommand("RESET,"),
        )
    }

    @Test
    fun unique_codes_first_wins_case_insensitive() {
        assertEquals(
            SearchCommand.BulkFollow(listOf("AAK")),
            parseSearchCommand("AAK, aak"),
        )
    }

    @Test
    fun empty_tokens_dropped() {
        assertEquals(
            SearchCommand.BulkFollow(listOf("AAK")),
            parseSearchCommand(" , AAK, ,"),
        )
    }

    @Test
    fun no_comma_is_text_search() {
        assertEquals(SearchCommand.TextSearch("AAK"), parseSearchCommand("AAK"))
        assertEquals(SearchCommand.TextSearch("ata"), parseSearchCommand(" ata "))
        assertEquals(SearchCommand.TextSearch(""), parseSearchCommand("  "))
    }
}
