package com.burha.fundhelper.ui.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burha.fundhelper.data.FundRepository
import com.burha.fundhelper.data.WatchlistRow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WatchlistUiState(
    val rows: List<WatchlistRow> = emptyList(),
    val isRefreshing: Boolean = false,
    val showError: Boolean = false,
)

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val funds: FundRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(WatchlistUiState())
    val state: StateFlow<WatchlistUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            funds.observeWatchlist().collect { rows ->
                _state.update { it.copy(rows = rows) }
            }
        }
    }

    fun refresh(force: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, showError = false) }
            val result = funds.refreshFollowed(force)
            _state.update {
                it.copy(isRefreshing = false, showError = result.isFailure)
            }
        }
    }

    fun unfollow(code: String) {
        viewModelScope.launch { funds.unfollow(code) }
    }

    fun consumeError() {
        _state.update { it.copy(showError = false) }
    }
}