package com.burha.fundhelper.domain

import java.util.Locale

sealed class SearchCommand {
    data object Reset : SearchCommand()
    data class BulkFollow(val codes: List<String>) : SearchCommand()
    data class TextSearch(val query: String) : SearchCommand()
}

fun parseSearchCommand(raw: String): SearchCommand {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return SearchCommand.TextSearch("")
    if (trimmed.contains(',')) {
        val codes = trimmed.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filter { !it.equals("RESET", ignoreCase = true) }
            .distinctBy { it.uppercase(Locale.ROOT) }
        return SearchCommand.BulkFollow(codes)
    }
    if (trimmed.equals("RESET", ignoreCase = true)) return SearchCommand.Reset
    return SearchCommand.TextSearch(trimmed)
}
