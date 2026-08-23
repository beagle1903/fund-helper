package com.burha.fundhelper.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burha.fundhelper.data.FundRepository
import com.burha.fundhelper.data.SearchOutcome
import com.burha.fundhelper.domain.FundSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val matches: List<FundSnapshot> = emptyList(),
    val followedCodes: Set<String> = emptySet(),
    val emptyQueryHint: Boolean = true,
    val noResults: Boolean = false,
    val isSearching: Boolean = false,
    val showError: Boolean = false,
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val funds: FundRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            funds.observeWatchlist().collect { rows ->
                _state.update { it.copy(followedCodes = rows.map { row -> row.code }.toSet()) }
            }
        }
    }

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value) }
    }

    fun submit(refetchCatalog: Boolean = false) {
        viewModelScope.launch {
            _state.update { it.copy(isSearching = true, showError = false, noResults = false) }
            when (val outcome = funds.search(_state.value.query, refetchCatalog)) {
                SearchOutcome.EmptyQuery -> _state.update {
                    it.copy(
                        isSearching = false,
                        emptyQueryHint = true,
                        matches = emptyList(),
                        noResults = false,
                    )
                }
                is SearchOutcome.Success -> _state.update {
                    it.copy(
                        isSearching = false,
                        emptyQueryHint = false,
                        matches = outcome.matches,
                        noResults = outcome.matches.isEmpty(),
                    )
                }
                is SearchOutcome.Failure -> _state.update {
                    it.copy(isSearching = false, showError = true)
                }
            }
        }
    }

    fun toggleFollow(code: String, followed: Boolean) {
        viewModelScope.launch {
            if (followed) funds.unfollow(code) else funds.follow(code)
        }
    }

    fun consumeError() {
        _state.update { it.copy(showError = false) }
    }
}