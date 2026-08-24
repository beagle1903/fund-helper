package com.burha.fundhelper.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burha.fundhelper.data.FundDetail
import com.burha.fundhelper.data.FundRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val fundCode: String,
    val detail: FundDetail? = null,
    val loaded: Boolean = false,
    val isRefreshing: Boolean = false,
    val showError: Boolean = false,
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val funds: FundRepository,
) : ViewModel() {
    private val fundCode: String = checkNotNull(savedStateHandle["fundCode"])
    private val _state = MutableStateFlow(DetailUiState(fundCode = fundCode))
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            funds.observeFund(fundCode).collect { detail ->
                _state.update { it.copy(detail = detail, loaded = true) }
            }
        }
    }

    fun refresh(force: Boolean) {
        val followed = _state.value.detail?.isFollowed == true
        if (!followed) return
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, showError = false) }
            val result = funds.refreshFollowed(force)
            _state.update { it.copy(isRefreshing = false, showError = result.isFailure) }
        }
    }

    fun toggleFollow() {
        viewModelScope.launch {
            val followed = _state.value.detail?.isFollowed == true
            if (followed) funds.unfollow(fundCode) else funds.follow(fundCode)
        }
    }

    fun consumeError() {
        _state.update { it.copy(showError = false) }
    }
}
