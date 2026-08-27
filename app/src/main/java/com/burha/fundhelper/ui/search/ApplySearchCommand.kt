package com.burha.fundhelper.ui.search

import com.burha.fundhelper.data.FundRepository
import com.burha.fundhelper.data.SearchOutcome
import com.burha.fundhelper.domain.SearchCommand
import com.burha.fundhelper.domain.parseSearchCommand

sealed class SearchSubmitResult {
    data object NavigateBack : SearchSubmitResult()
    data class Stay(val outcome: SearchOutcome) : SearchSubmitResult()
}

suspend fun applySearchCommand(
    query: String,
    funds: FundRepository,
    refetchCatalog: Boolean = false,
): SearchSubmitResult {
    return when (val command = parseSearchCommand(query)) {
        SearchCommand.Reset -> {
            funds.clearFollows()
            SearchSubmitResult.NavigateBack
        }
        is SearchCommand.BulkFollow -> {
            funds.followAll(command.codes)
            SearchSubmitResult.NavigateBack
        }
        is SearchCommand.TextSearch ->
            SearchSubmitResult.Stay(funds.search(query, refetchCatalog))
    }
}
